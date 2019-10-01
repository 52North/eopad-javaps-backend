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
import com.github.dockerjava.api.command.PullImageCmd;
import com.github.dockerjava.api.exception.DockerClientException;
import com.github.dockerjava.api.exception.DockerException;
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.Volume;
import com.google.common.base.Throwables;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.n52.janmayen.stream.Streams;
import org.n52.javaps.algorithm.AbstractAlgorithm;
import org.n52.javaps.algorithm.ExecutionException;
import org.n52.javaps.description.TypedProcessDescription;
import org.n52.javaps.docker.DockerConfig;
import org.n52.javaps.docker.Environment;
import org.n52.javaps.docker.Label;
import org.n52.javaps.docker.TypedDescriptionBuilder;
import org.n52.javaps.engine.ProcessExecutionContext;
import org.n52.shetland.ogc.ows.OwsLanguageString;
import org.n52.shetland.ogc.wps.OutputDefinition;
import org.n52.shetland.ogc.wps.ap.ApplicationPackage;
import org.n52.shetland.ogc.wps.ap.DockerExecutionUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;

/**
 * @author Matthes Rieke
 * @author Christian Autermann
 */
public class DockerAlgorithm extends AbstractAlgorithm {

    private static final Logger LOG = LoggerFactory.getLogger(DockerAlgorithm.class);
    private static final String HELPER_IMAGE_NAME = "busybox";
    private final ApplicationPackage applicationPackage;
    private final DockerExecutionUnit executionUnit;
    private final DockerConfig dockerConfig;
    private final TypedDescriptionBuilder descriptionBuilder;
    private final DockerClient dockerClient;
    private DockerJobConfig jobConfig;

    public DockerAlgorithm(DockerConfig dockerConfig, DockerClient dockerClient,
                           TypedDescriptionBuilder descriptionBuilder, ApplicationPackage applicationPackage) {
        this.dockerConfig = Objects.requireNonNull(dockerConfig);
        this.dockerClient = Objects.requireNonNull(dockerClient);
        this.applicationPackage = Objects.requireNonNull(applicationPackage);
        this.descriptionBuilder = Objects.requireNonNull(descriptionBuilder);
        this.executionUnit = getExecutionUnit(applicationPackage);
    }

    private static DockerExecutionUnit getExecutionUnit(ApplicationPackage applicationPackage) {
        if (applicationPackage.getExecutionUnits().size() != 1 ||
            !(applicationPackage.getExecutionUnits().iterator().next() instanceof DockerExecutionUnit)) {
            throw new IllegalArgumentException("exactly one docker execution unit is required");
        }
        return (DockerExecutionUnit) applicationPackage.getExecutionUnits().iterator().next();
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

        context.onDestroy(this::cleanup);

        try {
            // create a volume to hold the inputs and outputs
            jobConfig.setVolumeId(createVolume());
            // create a container with the volume mounted, as we can't directly copy to a volume
            Volume dataVolume = new Volume(jobConfig.getDataPath());
            Bind dataVolumeBind = new Bind(jobConfig.getVolumeId(), dataVolume);

            pullImageIfNotPresent(HELPER_IMAGE_NAME);
            pullImageIfNotPresent(executionUnit.getImage());

            // the container creates the necessary folders
            String[] cmd = createDirectories(jobConfig.getInputPath(), jobConfig.getOutputPath());
            CreateContainerCmd createHelperContainerCmd = createContainerCmd(HELPER_IMAGE_NAME)
                                                                  .withVolumes(dataVolume)
                                                                  .withHostConfig(HostConfig.newHostConfig()
                                                                                            .withBinds(dataVolumeBind))

                                                                  .withCmd(cmd);
            jobConfig.setHelperContainerId(createContainer(createHelperContainerCmd));
            startContainer(jobConfig.getHelperContainerId());

            // copy the inputs to the volume by copying them to the stopped helper container
            new DockerInputProcessor(jobConfig).process(context.getInputs());

            // set the environment variables for the process outputs
            List<DockerOutputInfo> outputInfos = new DockerOutputDefinitionProcessor(jobConfig)
                                                         .process(getOutputDefinitions());

            // wait for the container to stop
            if (!waitForCompletion(jobConfig.getHelperContainerId(), Duration.of(5, ChronoUnit.MINUTES))) {
                stopContainer(jobConfig.getHelperContainerId());
                throw new ExecutionException("helper container did not stop");
            }

            CreateContainerCmd createContainerCmd = createContainerCmd(executionUnit.getImage())
                                                            .withEnv(jobConfig.getJobEnvironment().encode())
                                                            .withVolumes(dataVolume)
                                                            .withHostConfig(HostConfig.newHostConfig()
                                                                                      .withBinds(dataVolumeBind));

            jobConfig.setProcessContainerId(createContainer(createContainerCmd));
            startContainer(jobConfig.getProcessContainerId());

            if (!waitForCompletion(jobConfig.getProcessContainerId(), jobConfig.getProcessTimeout().orElse(null))) {
                stopContainer(jobConfig.getHelperContainerId());
            }

            Integer exitCode = getExitCode(jobConfig.getProcessContainerId());
            if (exitCode == null) {
                throw new ExecutionException("process did not exit");
            }
            if (exitCode != 0) {
                throw new ExecutionException(String.format("process exited with non-zero exit code %d", exitCode));
            }
            // copy the generated outputs
            new DockerOutputProcessor(jobConfig).process(outputInfos);
        } catch (Throwable t) {
            Throwables.throwIfInstanceOf(t, ExecutionException.class);
            throw new ExecutionException(t);
        }
    }

    private void cleanup() {
        // remove the helper container
        removeContainer(jobConfig.getHelperContainerId());
        // remove the helper container
        removeContainer(jobConfig.getProcessContainerId());
        // remove the data volume
        removeVolume(jobConfig.getVolumeId());
    }

    private void removeContainer(String containerId) {
        if (containerId != null) {
            try {
                jobConfig.client().removeContainerCmd(containerId).withForce(true).exec();
            } catch (DockerException ex) {
                jobConfig.log().error("unable to remove container " + containerId, ex);
            }
        }
    }

    private void removeVolume(String volumeId) {
        if (volumeId != null) {
            try {
                jobConfig.client().removeVolumeCmd(volumeId).exec();
            } catch (DockerException ex) {
                jobConfig.log().error("unable to remove volume " + volumeId, ex);
            }
        }
    }

    private void pullImageIfNotPresent(String image) throws ExecutionException {
        Logger log = jobConfig.log();
        log.info("Pulling image {}.", image);
        pullImage(image);
        log.info("Successfully pulled image {}.", image);
    }

    private void pullImage(String image) throws ExecutionException {
        PullImageCmd cmd = pullImageCmd(DockerImage.fromString(image));
        try {
            cmd.exec(new PullCallback(jobConfig.log())).awaitCompletion();
        } catch (InterruptedException | DockerClientException e) {
            throw new ExecutionException("could not pull image " + image, e);
        }
    }

    private PullImageCmd pullImageCmd(DockerImage image) {
        PullImageCmd cmd = jobConfig.client().pullImageCmd(image.getRepository())
                                    .withTag(image.getTag().orElse(DockerImage.LATEST));
        image.getRegistry().ifPresent(cmd::withRegistry);
        return cmd;
    }

    private Integer getExitCode(String containerId) throws InterruptedException {
        while (isRunning(containerId)) {
            LOG.info("Container still running, waiting to finish");
            Thread.sleep(10000);
        }
        InspectContainerResponse inspectContainerResponse = inspectContainer(containerId);
        return inspectContainerResponse.getState().getExitCode();
    }

    private void stopContainer(String containerId) {
        Optional<Duration> timeout = jobConfig.getStopTimeout();
        if (timeout.isPresent()) {
            int seconds = timeout.get().getSeconds() >= Integer.MAX_VALUE
                          ? Integer.MAX_VALUE : (int) timeout.get().getSeconds();
            jobConfig.client().stopContainerCmd(containerId).withTimeout(seconds).exec();
        } else {
            jobConfig.client().killContainerCmd(containerId).exec();
        }
    }

    private boolean isRunning(String containerId) {
        Boolean running = inspectContainer(containerId).getState().getRunning();
        return running != null && running;
    }

    private String[] createDirectories(String... directories) {
        return Stream.concat(Stream.of("mkdir", "-p"), Streams.stream(directories)).toArray(String[]::new);
    }

    private InspectContainerResponse inspectContainer(String containerId) {
        return jobConfig.client().inspectContainerCmd(containerId).exec();
    }

    private String createVolume() {
        return jobConfig.client().createVolumeCmd().exec().getName();
    }

    private void startContainer(String containerId) {
        jobConfig.log().debug("Starting container {}", containerId);
        jobConfig.client().startContainerCmd(containerId).exec();
        jobConfig.log().debug("Started container {}", containerId);
    }

    private String createContainer(CreateContainerCmd command) {
        String image = command.getImage();
        jobConfig.log().debug("Creating container from image {}", image);
        String containerId = command.exec().getId();
        jobConfig.log().debug("Created container {} from image {}", containerId, image);
        return containerId;
    }

    private CreateContainerCmd createContainerCmd(String image) {
        CreateContainerCmd command = jobConfig.client().createContainerCmd(image).withLabels(createLabels());
        getUserAndGroup().ifPresent(command::withUser);
        return command;
    }

    private Optional<String> getUserAndGroup() {
        return jobConfig.getUser().map(user -> jobConfig.getGroup()
                                                        .map(group -> String.format("%s:%s", user, group))
                                                        .orElse(user));
    }

    private Map<String, String> createLabels() {
        Map<String, String> labels = new HashMap<>();
        labels.put(Label.JOB_ID, jobConfig.context().getJobId().getValue());
        labels.put(Label.JOB_TIME, DateTime.now(DateTimeZone.UTC).toString());
        labels.put(Label.PROCESS_ID, getDescription().getId().getValue());
        labels.put(Label.PROCESS_TITLE, getDescription().getTitle().getValue());
        labels.put(Label.PROCESS_ABSTRACT, getDescription().getAbstract().map(OwsLanguageString::getValue).orElse(""));
        labels.put(Label.PROCESS_VERSION, getDescription().getVersion());
        labels.put(Label.VERSION, jobConfig.getJavaPsVersion());
        return labels;
    }

    private boolean waitForCompletion(String containerId, Duration timeout) throws InterruptedException {
        LoggingCallback callback = createCallback(containerId);
        if (timeout == null) {
            callback.awaitCompletion();
            return true;
        } else {
            return callback.awaitCompletion(timeout.toMillis(), TimeUnit.MILLISECONDS);
        }
    }

    private LoggingCallback createCallback(String containerId) {
        return jobConfig.client().logContainerCmd(containerId)
                        .withStdErr(true)
                        .withStdOut(true)
                        .withFollowStream(true)
                        .withTailAll()
                        .exec(new LoggingCallback(jobConfig.log(), containerId));
    }

    private List<OutputDefinition> getOutputDefinitions() {
        return getDescription().getOutputs().stream()
                               .map(jobConfig.context()::getOutputDefinition)
                               .filter(Optional::isPresent).map(Optional::get)
                               .collect(toList());
    }

}

