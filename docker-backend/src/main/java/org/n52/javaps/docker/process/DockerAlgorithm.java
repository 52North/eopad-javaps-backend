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
import com.github.dockerjava.api.command.CopyArchiveToContainerCmd;
import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.command.PullImageCmd;
import com.github.dockerjava.api.exception.DockerClientException;
import com.github.dockerjava.api.exception.DockerException;
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.Volume;
import com.google.common.base.Throwables;
import com.google.common.io.CharStreams;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.n52.janmayen.stream.Streams;
import org.n52.javaps.algorithm.AbstractAlgorithm;
import org.n52.javaps.algorithm.ExecutionException;
import org.n52.javaps.algorithm.ProcessInputs;
import org.n52.javaps.algorithm.ProcessOutputs;
import org.n52.javaps.description.TypedBoundingBoxInputDescription;
import org.n52.javaps.description.TypedComplexInputDescription;
import org.n52.javaps.description.TypedGroupInputDescription;
import org.n52.javaps.description.TypedLiteralInputDescription;
import org.n52.javaps.description.TypedProcessDescription;
import org.n52.javaps.description.TypedProcessInputDescription;
import org.n52.javaps.description.TypedProcessInputDescriptionContainer;
import org.n52.javaps.description.TypedProcessOutputDescription;
import org.n52.javaps.description.TypedProcessOutputDescriptionContainer;
import org.n52.javaps.docker.DockerConfig;
import org.n52.javaps.docker.Environment;
import org.n52.javaps.docker.Label;
import org.n52.javaps.docker.TypedDescriptionBuilder;
import org.n52.javaps.docker.io.DockerInputData;
import org.n52.javaps.docker.io.DockerOutputData;
import org.n52.javaps.docker.util.InputStreams;
import org.n52.javaps.engine.ProcessExecutionContext;
import org.n52.javaps.io.Data;
import org.n52.javaps.io.DecodingException;
import org.n52.javaps.io.EncodingException;
import org.n52.javaps.io.GroupInputData;
import org.n52.javaps.io.GroupOutputData;
import org.n52.javaps.io.bbox.BoundingBoxData;
import org.n52.javaps.io.literal.LiteralData;
import org.n52.javaps.io.literal.LiteralType;
import org.n52.shetland.ogc.ows.OwsBoundingBox;
import org.n52.shetland.ogc.ows.OwsCode;
import org.n52.shetland.ogc.ows.OwsLanguageString;
import org.n52.shetland.ogc.wps.Format;
import org.n52.shetland.ogc.wps.OutputDefinition;
import org.n52.shetland.ogc.wps.ap.ApplicationPackage;
import org.n52.shetland.ogc.wps.ap.DockerExecutionUnit;
import org.n52.shetland.ogc.wps.description.ProcessOutputDescription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.text.NumberFormat;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.DoubleStream;
import java.util.stream.Stream;

import static java.util.stream.Collectors.joining;
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
            createInputs(context.getInputs());

            // set the environment variables for the process outputs
            List<DockerOutputInfo> outputInfos = createOutputInfos();

            // wait for the container to stop
            waitForCompletion(jobConfig.getHelperContainerId(), Duration.of(5, ChronoUnit.MINUTES));

            CreateContainerCmd createContainerCmd = createContainerCmd(executionUnit.getImage())
                                                            .withEnv(jobConfig.getJobEnvironment().encode())
                                                            .withVolumes(dataVolume)
                                                            .withHostConfig(HostConfig.newHostConfig()
                                                                                      .withBinds(dataVolumeBind));

            jobConfig.setProcessContainerId(createContainer(createContainerCmd));
            startContainer(jobConfig.getProcessContainerId());

            waitForCompletion(jobConfig.getProcessContainerId(), jobConfig.getProcessTimeout().orElse(null));

            // copy the generated outputs
            readOutputs(outputInfos);
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

    private void createFormat(Environment environment, Format format) {
        format.getMimeType().ifPresent(mimeType -> environment.put(Environment.MIME_TYPE, mimeType));
        format.getSchema().ifPresent(schema -> environment.put(Environment.SCHEMA, schema));
        format.getEncoding().ifPresent(encoding -> environment.put(Environment.ENCODING, encoding));
    }

    private List<DockerOutputInfo> createOutputInfos() {
        return createOutputInfos(jobConfig.description(),
                                 jobConfig.getJobEnvironment().withPrefix(Environment.OUTPUT),
                                 getOutputDefinitions());
    }

    private List<DockerOutputInfo> createOutputInfos(TypedProcessOutputDescriptionContainer description,
                                                     Environment environment,
                                                     List<OutputDefinition> outputDefinitions) {
        List<DockerOutputInfo> outputInfos = new ArrayList<>(outputDefinitions.size());
        for (OutputDefinition outputDefinition : outputDefinitions) {
            TypedProcessOutputDescription<?> outputDescription = description.getOutput(outputDefinition.getId());

            Environment e = environment.withPrefix(outputDefinition.getId());
            if (outputDescription.isGroup()) {
                List<DockerOutputInfo> outputs = createOutputInfos(outputDescription.asGroup(), e,
                                                                   outputDefinition.getOutputs());
                outputInfos.add(new DockerOutputInfo(outputDefinition, outputDescription, outputs));
            } else {
                String outputPath = jobConfig.getOutputPath(e.getPrefix());
                e.put(outputPath);

                outputInfos.add(new DockerOutputInfo(outputDefinition, outputDescription, outputPath));

                if (outputDescription.isComplex()) {
                    createFormat(e, outputDefinition.getFormat());
                }
            }
        }
        return outputInfos;
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

    private void waitForCompletion(String containerId, Duration timeout) throws ExecutionException {
        try {
            LoggingCallback callback = createCallback(containerId);
            if (timeout == null) {
                callback.awaitCompletion();
            } else {
                if (!callback.awaitCompletion(timeout.toMillis(), TimeUnit.MILLISECONDS)) {
                    stopContainer(containerId);
                }
            }
            Integer exitCode = getExitCode(containerId);
            if (exitCode == null) {
                throw new ExecutionException("process did not exit");
            }

            if (exitCode != 0) {
                throw new ExecutionException(String.format("process exited with non-zero exit code %d:\n%s",
                                                           exitCode, callback.getOutput()));
            }
        } catch (InterruptedException e) {
            throw new ExecutionException(e);
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

    private void createInputs(ProcessInputs inputs) throws ExecutionException {
        try {
            createInputs(jobConfig.description(), jobConfig.getJobEnvironment()
                                                           .withPrefix(Environment.INPUT), inputs);
        } catch (IOException | EncodingException e) {
            throw new ExecutionException("error creating inputs", e);
        }
    }

    private void createInputs(TypedProcessInputDescriptionContainer descriptionContainer,
                              Environment environment, ProcessInputs
                                                               inputs)
            throws EncodingException, IOException {
        for (Map.Entry<OwsCode, List<Data<?>>> entry : inputs.entrySet()) {
            TypedProcessInputDescription<?> description = descriptionContainer.getInput(entry.getKey());
            Environment e = environment.withPrefix(entry.getKey());
            if (description.isLiteral()) {
                createLiteralInput(description.asLiteral(), e, entry.getValue());
            } else if (description.isBoundingBox()) {
                createBoundingBoxInput(description.asBoundingBox(), e, entry.getValue());
            } else if (description.isComplex()) {
                createComplexInput(description.asComplex(), e, entry.getValue());
            } else if (description.isGroup()) {
                createGroupInput(description.asGroup(), e, entry.getValue());
            } else {
                jobConfig.log().warn("Unsupported input type: {}", description);
            }
        }
    }

    private void createBoundingBoxInput(TypedBoundingBoxInputDescription description,
                                        Environment environment,
                                        List<Data<
                                                         ?>> values) {
        if (description.getOccurence().isMultiple()) {
            int index = 0;
            for (Data<?> data : values) {
                createBoundingBoxInput(environment.withPrefix(++index), (BoundingBoxData) data);

            }
        } else if (!values.isEmpty()) {
            createBoundingBoxInput(environment, (BoundingBoxData) values.iterator().next());
        }
    }

    private void createBoundingBoxInput(Environment environment, BoundingBoxData data) {
        OwsBoundingBox payload = data.getPayload();
        NumberFormat numberFormat = NumberFormat.getInstance(Locale.ROOT);
        String value = DoubleStream.concat(Arrays.stream(payload.getLowerCorner()),
                                           Arrays.stream(payload.getUpperCorner()))
                                   .mapToObj(numberFormat::format)
                                   .collect(joining(","));
        environment.put(value);
        payload.getCRS().map(URI::toString).ifPresent(crs -> environment.put(Environment.CRS, crs));
    }

    private void createLiteralInput(TypedLiteralInputDescription description, Environment environment,
                                    List<Data<?>>
                                            values) throws EncodingException {
        if (description.getOccurence().isMultiple()) {
            int index = 0;
            for (Data<?> data : values) {
                createLiteralInput(description, environment.withPrefix(++index), (LiteralData) data);
            }
        } else if (!values.isEmpty()) {
            createLiteralInput(description, environment, (LiteralData) values.iterator().next());
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void createLiteralInput(TypedLiteralInputDescription description, Environment environment,
                                    LiteralData
                                            literalData) throws EncodingException {
        LiteralType type = description.getType();
        String value = type.generate(literalData.getPayload());
        environment.put(value);
        literalData.getUnitOfMeasurement().ifPresent(uom -> environment.put(Environment.UOM, value));
    }

    private void createComplexInput(TypedComplexInputDescription description, Environment environment,
                                    List<Data<?>>
                                            values) throws IOException {
        if (description.getOccurence().isMultiple()) {
            int index = 0;
            for (Data<?> data : values) {
                createComplexInput(environment.withPrefix(++index), (DockerInputData) data);
            }
        } else if (!values.isEmpty()) {
            createComplexInput(environment, (DockerInputData) values.iterator().next());
        }
    }

    private void createComplexInput(Environment environment, DockerInputData data) throws IOException {
        String name = environment.getPrefix();
        byte[] payload = data.getPayload();
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        try (TarArchiveOutputStream tarOutput = new TarArchiveOutputStream(byteArrayOutputStream)) {
            TarArchiveEntry entry = new TarArchiveEntry(name);
            entry.setSize(payload.length);
            tarOutput.putArchiveEntry(entry);
            tarOutput.write(payload);
            tarOutput.closeArchiveEntry();
        }
        copyArchiveToContainerCmd().withRemotePath(jobConfig.getInputPath())
                                   .withTarInputStream(new ByteArrayInputStream(byteArrayOutputStream
                                                                                        .toByteArray()))
                                   .exec();

        environment.put(jobConfig.getInputPath(name));
        createFormat(environment, data.getFormat());
    }

    private CopyArchiveToContainerCmd copyArchiveToContainerCmd() {
        return jobConfig.client().copyArchiveToContainerCmd(jobConfig.getHelperContainerId());
    }

    private void createGroupInput(TypedGroupInputDescription description, Environment environment,
                                  List<Data<?>>
                                          values) throws EncodingException, IOException {
        if (description.getOccurence().isMultiple()) {
            int index = 0;
            for (Data<?> data : values) {
                createGroupInput(description, environment.withPrefix(++index), (GroupInputData) data);
            }
        } else if (!values.isEmpty()) {
            createGroupInput(description, environment, (GroupInputData) values.iterator().next());
        }
    }

    private void createGroupInput(TypedGroupInputDescription descriptionContainer, Environment environment,
                                  GroupInputData
                                          data) throws EncodingException, IOException {
        createInputs(descriptionContainer, environment, data.getPayload());
    }

    private void readOutputs(List<DockerOutputInfo> outputInfos) throws IOException, DecodingException {
        ProcessOutputs outputs = jobConfig.context().getOutputs();
        for (DockerOutputInfo outputInfo : outputInfos) {
            readOutput(outputInfo, outputs);
        }
    }

    private void readOutput(DockerOutputInfo outputInfo, ProcessOutputs outputs)
            throws IOException, DecodingException {
        ProcessOutputDescription description = outputInfo.getDescription();
        if (description.isGroup()) {
            readGroupOutput(outputInfo, outputs);
        } else if (description.isLiteral()) {
            readLiteralOutput(outputInfo, outputs);
        } else if (description.isComplex()) {
            readComplexOutput(outputInfo, outputs);
        } else if (description.isBoundingBox()) {
            readBoundingBoxOutput(outputInfo, outputs);
        }
    }

    private void readBoundingBoxOutput(DockerOutputInfo outputInfo, ProcessOutputs outputs) {
        // FIXME currently there is no way to read the CRS of the bounding box from the output definition
        OwsBoundingBox owsBoundingBox = new OwsBoundingBox(new double[]{}, new double[]{});
        outputs.put(outputInfo.getDescription().getId(), new BoundingBoxData(owsBoundingBox));
        throw new UnsupportedOperationException("bounding box outputs are not supported");
    }

    private void readComplexOutput(DockerOutputInfo outputInfo, ProcessOutputs outputs) {
        Format format = outputInfo.getDefinition().getFormat();
        DockerOutputData data = new DockerOutputData(() -> readFile(outputInfo), format);
        outputs.put(outputInfo.getDescription().getId(), data);
    }

    private void readLiteralOutput(DockerOutputInfo outputInfo, ProcessOutputs outputs)
            throws DecodingException, IOException {
        try (InputStream stream = readFile(outputInfo);
             InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            LiteralType<?> literalType = outputInfo.getDescription().asLiteral().getType();
            LiteralData data = literalType.parseToBinding(CharStreams.toString(reader));
            outputs.put(outputInfo.getDescription().getId(), data);
        }
    }

    private void readGroupOutput(DockerOutputInfo outputInfo, ProcessOutputs outputs)
            throws IOException, DecodingException {
        ProcessOutputs childOutputs = new ProcessOutputs();
        for (DockerOutputInfo child : outputInfo.getOutputInfos()) {
            readOutput(child, childOutputs);
        }
        GroupOutputData data = new GroupOutputData(childOutputs);
        outputs.put(outputInfo.getDescription().getId(), data);
    }

    private InputStream readFile(DockerOutputInfo path) throws IOException {
        String containerId = jobConfig.getProcessContainerId();
        InputStream inputStream = jobConfig.client().copyArchiveFromContainerCmd(containerId, path.getPath())
                                           .exec();
        TarArchiveInputStream tarArchiveInputStream = new TarArchiveInputStream(inputStream);
        try {
            if (tarArchiveInputStream.getNextTarEntry() == null) {
                tarArchiveInputStream.close();
                return InputStreams.empty();
            }
        } catch (IOException e) {
            tarArchiveInputStream.close();
            throw e;
        } catch (DockerClientException | DockerException e) {
            throw new IOException(e);
        }
        return tarArchiveInputStream;
    }
}

