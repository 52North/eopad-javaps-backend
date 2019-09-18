/*
 * Copyright 2019 52°North Initiative for Geospatial Open Source
 * Software GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.n52.javaps.docker.process;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.exception.DockerException;
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.Frame;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.Volume;
import com.github.dockerjava.core.command.LogContainerResultCallback;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.n52.javaps.algorithm.AbstractAlgorithm;
import org.n52.javaps.algorithm.ExecutionException;
import org.n52.javaps.description.TypedProcessDescription;
import org.n52.javaps.docker.DockerConfig;
import org.n52.javaps.docker.Environment;
import org.n52.javaps.docker.Labels;
import org.n52.javaps.docker.TypedDescriptionBuilder;
import org.n52.javaps.engine.ProcessExecutionContext;
import org.n52.shetland.ogc.ows.OwsLanguageString;
import org.n52.shetland.ogc.wps.OutputDefinition;
import org.n52.shetland.ogc.wps.ap.ApplicationPackage;
import org.n52.shetland.ogc.wps.ap.DockerExecutionUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;

/**
 * @author <a href="mailto:m.rieke@52north.org">Matthes Rieke</a>
 */
public class DockerAlgorithm extends AbstractAlgorithm {

    private static final Logger LOG = LoggerFactory.getLogger(DockerAlgorithm.class);
    private static final Duration DEFAULT_TIMEOUT = Duration.parse("PT30M");

    private static final String HELPER_CONTAINER_IMAGE = "busybox:latest";
    private final ApplicationPackage applicationPackage;
    private final DockerExecutionUnit executionUnit;
    private final DockerConfig dockerConfig;
    private final TypedDescriptionBuilder descriptionBuilder;
    private final DockerClient dockerClient;
    private DockerJobConfig jobConfig;

    public DockerAlgorithm(DockerConfig dockerConfig,
                           DockerClient dockerClient,
                           TypedDescriptionBuilder descriptionBuilder,
                           ApplicationPackage applicationPackage) {
        this.dockerConfig = Objects.requireNonNull(dockerConfig);
        this.dockerClient = Objects.requireNonNull(dockerClient);
        this.applicationPackage = Objects.requireNonNull(applicationPackage);
        this.descriptionBuilder = Objects.requireNonNull(descriptionBuilder);
        this.executionUnit = (DockerExecutionUnit) this.applicationPackage.getExecutionUnit();
    }

    private Logger getJobLog() {
        return jobConfig.getLog();
    }

    @Override
    protected TypedProcessDescription createDescription() {
        return descriptionBuilder.createDescription(applicationPackage.getProcessDescription().getProcessDescription());
    }

    @Override
    public void execute(ProcessExecutionContext context) throws ExecutionException {
        Environment environment = new Environment(executionUnit.getEnvironment());
        environment.putAll(dockerConfig.getGlobalEnvironment());
        this.jobConfig = new DockerJobConfigImpl(dockerConfig, dockerClient, getDescription(), context, environment);

        try {
            // create a volume to hold the inputs and outputs
            jobConfig.setVolumeId(createVolume());

            // create a container with the volume mounted, as we can't directly copy to a volume
            Volume dataVolume = new Volume(jobConfig.getDataPath());
            Bind dataVolumeBind = new Bind(jobConfig.getVolumeId(), dataVolume);
            // the container creates the necessary folders
            String cmd = createDirectories(jobConfig.getInputPath(), jobConfig.getOutputPath());
            CreateContainerCmd createHelperContainerCmd = createContainerCmd(HELPER_CONTAINER_IMAGE)
                                                                  .withVolumes(dataVolume)
                                                                  .withHostConfig(HostConfig.newHostConfig()
                                                                                            .withBinds(dataVolumeBind))

                                                                  .withCmd(cmd);
            jobConfig.setHelperContainerId(createContainer(createHelperContainerCmd));
            startContainer(jobConfig.getHelperContainerId());
            List<DockerOutputInfo> outputInfos;

            DockerInputProcessor inputProcessor = new DockerInputProcessor(jobConfig);
            DockerOutputDefinitionProcessor outputDefinitionProcessor = new DockerOutputDefinitionProcessor(jobConfig);

            // copy the inputs to the volume by copying them to the stopped helper container
            inputProcessor.process(context.getInputs());

            // set the environment variables for the process outputs
            outputInfos = outputDefinitionProcessor.process(getOutputDefinitions());

            // wait for the container to stop
            try {
                waitForCompletion(jobConfig.getHelperContainerId());
            } catch (InterruptedException ex) {
                getJobLog().warn("helper container did not stop", ex);
            }

            try {
                CreateContainerCmd createContainerCmd = createContainerCmd(executionUnit.getImage())
                                                                .withEnv(jobConfig.getGlobalEnvironment().encode())
                                                                .withVolumes(dataVolume)
                                                                .withHostConfig(HostConfig.newHostConfig()
                                                                                          .withBinds(dataVolumeBind));

                jobConfig.setProcessContainerId(createContainer(createContainerCmd));
                startContainer(jobConfig.getProcessContainerId());
                waitForCompletion(jobConfig.getProcessContainerId(), jobConfig.getTimeout().orElse(DEFAULT_TIMEOUT));
                InspectContainerResponse inspect = getExitCode(jobConfig.getProcessContainerId());

                Integer exitCode = inspect.getState().getExitCode();
                if (exitCode != null && exitCode != 0) {
                    throw new ExecutionException("process exited with non-zero exit code " + exitCode);
                }

            } catch (DockerException | InterruptedException ex) {
                throw new ExecutionException(ex);
            }
            DockerOutputProcessor outputProcessor = new DockerOutputProcessor(jobConfig);

            // copy the generated outputs
            outputProcessor.process(outputInfos);
        } finally {
            cleanUp();
        }
    }

    private void cleanUp() {
        // remove the helper container
        removeContainer(jobConfig.getHelperContainerId());
        // remove the helper container
        removeContainer(jobConfig.getProcessContainerId());
        // remove the data volume
        removeVolume(jobConfig.getVolumeId());
    }

    private InspectContainerResponse getExitCode(String containerId) throws InterruptedException {
        // on rare occasions this could happen before the container finished
        // lets wait five more minutes
        long waitStart = System.currentTimeMillis();
        long waitTime = jobConfig.getTimeout().orElse(DEFAULT_TIMEOUT).toMillis();
        while ((System.currentTimeMillis() - waitStart) < waitTime) {
            InspectContainerResponse inspectResp = inspectContainer(containerId);
            Boolean running = inspectResp.getState().getRunning();
            if (running == null || !running) {
                break;
            }

            LOG.info("Container still running, lost log listener. Waiting to finish");
            Thread.sleep(10000);
        }
        return inspectContainer(containerId);
    }

    private String createDirectories(Object... directories) {

        String parameters = Arrays.stream(directories).map(Object::toString).map(x -> String.format("\"%s\"", x))
                                  .collect(joining(" "));
        return String.format("mkdir -p %s", parameters);
    }

    private void removeVolume(String volumeId) {
        if (volumeId != null) {
            try {
                jobConfig.getClient().removeVolumeCmd(volumeId).exec();
            } catch (DockerException ex) {
                getJobLog().error("unable to remove volume " + volumeId, ex);
            }
        }
    }

    private void removeContainer(String containerId) {
        if (containerId != null) {
            try {
                jobConfig.getClient().removeContainerCmd(containerId).withForce(true).exec();
            } catch (DockerException ex) {
                getJobLog().error("unable to remove container " + containerId, ex);
            }
        }

    }

    private InspectContainerResponse inspectContainer(String containerId) {
        return jobConfig.getClient().inspectContainerCmd(containerId).exec();
    }

    private String createVolume() {
        return jobConfig.getClient().createVolumeCmd().exec().getName();
    }

    private void startContainer(String containerId) {
        getJobLog().debug("Starting container {}", containerId);
        jobConfig.getClient().startContainerCmd(containerId).exec();
        getJobLog().debug("Started container {}", containerId);
    }

    private String createContainer(CreateContainerCmd command) {
        String image = command.getImage();
        getJobLog().debug("Creating container from image {}", image);
        String containerId = command.exec().getId();
        getJobLog().debug("Created container {} from image {}", containerId, image);
        return containerId;
    }

    private CreateContainerCmd createContainerCmd(String image) {
        CreateContainerCmd command = jobConfig.getClient().createContainerCmd(image);
        jobConfig.getUser().map(user -> jobConfig.getGroup()
                                                 .map(group -> String.format("%s:%s", user, group))
                                                 .orElse(user))
                 .ifPresent(command::withUser);
        // add some metadata to the container
        command.withLabels(createLabels());
        return command;
    }

    private Map<String, String> createLabels() {
        Map<String, String> labels = new HashMap<>();
        labels.put(Labels.JOB_ID, jobConfig.getContext().getJobId().getValue());
        labels.put(Labels.JOB_TIME, DateTime.now(DateTimeZone.UTC).toString());
        labels.put(Labels.PROCESS_ID, getDescription().getId().getValue());
        labels.put(Labels.PROCESS_TITLE, getDescription().getTitle().getValue());
        labels.put(Labels.PROCESS_ABSTRACT, getDescription().getAbstract().map(OwsLanguageString::getValue).orElse(""));
        labels.put(Labels.PROCESS_VERSION, getDescription().getVersion());
        labels.put(Labels.VERSION, jobConfig.getJavaPsVersion());
        return labels;
    }

    private void waitForCompletion(String containerId) throws InterruptedException {
        waitForCompletion(containerId, null);
    }

    private void waitForCompletion(String containerId, Duration timeout) throws InterruptedException {
        LogContainerResultCallback callback = createCallback(containerId);
        if (timeout == null) {
            callback.awaitCompletion();
        } else {
            callback.awaitCompletion(timeout.toMillis(), TimeUnit.MILLISECONDS);
        }
    }

    private LogContainerResultCallback createCallback(String containerId) {
        return jobConfig.getClient().logContainerCmd(containerId)
                        .withStdErr(true).withStdOut(true).withFollowStream(true)
                        .withTailAll().exec(new LoggingCallback(containerId));
    }

    private List<OutputDefinition> getOutputDefinitions() {
        return getDescription().getOutputs().stream()
                               .map(jobConfig.getContext()::getOutputDefinition)
                               .filter(Optional::isPresent).map(Optional::get)
                               .collect(toList());
    }

    private class LoggingCallback extends LogContainerResultCallback {
        private final Logger log;

        LoggingCallback(String containerId) {
            this.log = LoggerFactory.getLogger(String.format("%s.%s", jobConfig.getLog().getName(), containerId));
        }

        @Override
        public void onNext(Frame item) {
            log.info("{}", item.toString());
        }
    }
}

