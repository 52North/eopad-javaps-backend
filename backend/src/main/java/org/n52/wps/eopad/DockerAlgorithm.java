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

package org.n52.wps.eopad;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.exception.DockerException;
import com.github.dockerjava.api.model.Frame;
import com.github.dockerjava.core.command.LogContainerResultCallback;
import org.n52.iceland.util.MoreFiles;
import org.n52.javaps.algorithm.AbstractAlgorithm;
import org.n52.javaps.algorithm.ExecutionException;
import org.n52.javaps.algorithm.ProcessInputs;
import org.n52.javaps.description.TypedBoundingBoxInputDescription;
import org.n52.javaps.description.TypedComplexInputDescription;
import org.n52.javaps.description.TypedGroupInputDescription;
import org.n52.javaps.description.TypedLiteralInputDescription;
import org.n52.javaps.description.TypedProcessDescription;
import org.n52.javaps.description.TypedProcessInputDescription;
import org.n52.javaps.description.TypedProcessInputDescriptionContainer;
import org.n52.javaps.description.TypedProcessOutputDescription;
import org.n52.javaps.description.TypedProcessOutputDescriptionContainer;
import org.n52.javaps.description.impl.TypedProcessDescriptionFactory;
import org.n52.javaps.description.impl.TypedProcessDescriptionImpl;
import org.n52.javaps.engine.ProcessExecutionContext;
import org.n52.javaps.io.Data;
import org.n52.javaps.io.EncodingException;
import org.n52.javaps.io.GroupInputData;
import org.n52.javaps.io.bbox.BoundingBoxData;
import org.n52.javaps.io.literal.LiteralData;
import org.n52.javaps.io.literal.LiteralType;
import org.n52.javaps.io.literal.LiteralTypeRepository;
import org.n52.javaps.io.literal.xsd.LiteralStringType;
import org.n52.shetland.ogc.ows.OwsBoundingBox;
import org.n52.shetland.ogc.ows.OwsCode;
import org.n52.shetland.ogc.wps.Format;
import org.n52.shetland.ogc.wps.OutputDefinition;
import org.n52.shetland.ogc.wps.ap.ApplicationPackage;
import org.n52.shetland.ogc.wps.ap.DockerExecutionUnit;
import org.n52.shetland.ogc.wps.description.BoundingBoxInputDescription;
import org.n52.shetland.ogc.wps.description.BoundingBoxOutputDescription;
import org.n52.shetland.ogc.wps.description.ComplexInputDescription;
import org.n52.shetland.ogc.wps.description.ComplexOutputDescription;
import org.n52.shetland.ogc.wps.description.GroupInputDescription;
import org.n52.shetland.ogc.wps.description.GroupOutputDescription;
import org.n52.shetland.ogc.wps.description.LiteralInputDescription;
import org.n52.shetland.ogc.wps.description.LiteralOutputDescription;
import org.n52.shetland.ogc.wps.description.ProcessDescription;
import org.n52.shetland.ogc.wps.description.ProcessInputDescription;
import org.n52.shetland.ogc.wps.description.ProcessOutputDescription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.NumberFormat;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.DoubleStream;

import static java.util.stream.Collectors.toList;

/**
 * @author <a href="mailto:m.rieke@52north.org">Matthes Rieke</a>
 */
public class DockerAlgorithm extends AbstractAlgorithm {

    private static final Logger LOG = LoggerFactory.getLogger(DockerAlgorithm.class);
    private static final String MIME_TYPE = "MIME_TYPE";
    private static final String SCHEMA = "SCHEMA";
    private static final String ENCODING = "ENCODING";
    private static final String UOM = "UOM";
    private static final String CRS = "CRS";
    private static final String INPUT = "INPUT";
    private static final String OUTPUT = "OUTPUT";

    private final DockerClient dockerClient;
    private final ApplicationPackage applicationPackage;
    private final LiteralTypeRepository literalTypeRepository;
    private TypedProcessDescriptionFactory descriptionFactory;

    public DockerAlgorithm(DockerClient dockerClient,
                           LiteralTypeRepository literalTypeRepository,
                           ApplicationPackage applicationPackage) {
        this.dockerClient = Objects.requireNonNull(dockerClient);
        this.applicationPackage = Objects.requireNonNull(applicationPackage);
        this.literalTypeRepository = Objects.requireNonNull(literalTypeRepository);
        this.descriptionFactory = TypedProcessDescriptionFactory.instance();
    }

    @Override
    protected TypedProcessDescription createDescription() {
        ProcessDescription processDescription = applicationPackage.getProcessDescription().getProcessDescription();
        TypedProcessDescriptionImpl.Builder builder = descriptionFactory.process();
        builder.withDescription(processDescription);
        builder.withVersion(processDescription.getVersion());
        builder.withInput(processDescription.getInputDescriptions().stream()
                                            .map(this::createProcessInputDescription));
        builder.withOutput(processDescription.getOutputDescriptions().stream()
                                             .map(this::createProcessOutputDescription));
        return builder.build();
    }

    private TypedProcessOutputDescription createProcessOutputDescription(ProcessOutputDescription output) {
        if (output.isComplex()) {
            return createComplexOutputDescription(output.asComplex());
        } else if (output.isLiteral()) {
            return createLiteralOutputDescription(output.asLiteral());
        } else if (output.isGroup()) {
            return createGroupOutputDescription(output.asGroup());
        } else if (output.isBoundingBox()) {
            return creatBoundingBoxOutputDescription(output.asBoundingBox());
        } else {
            throw new IllegalArgumentException("unsupported output: " + output);
        }

    }

    private TypedProcessInputDescription createProcessInputDescription(ProcessInputDescription input) {
        if (input.isComplex()) {
            return createComplexInputDescription(input.asComplex());
        } else if (input.isLiteral()) {
            return createLiteralInputDescription(input.asLiteral());
        } else if (input.isGroup()) {
            return createGroupInputDescription(input.asGroup());
        } else if (input.isBoundingBox()) {
            return creatBoundingBoxInputDescription(input.asBoundingBox());
        } else {
            throw new IllegalArgumentException("unsupported input: " + input);
        }
    }

    private TypedProcessInputDescription createLiteralInputDescription(LiteralInputDescription input) {
        LiteralType<?> literalType = literalTypeRepository.getLiteralType(input).orElseGet(LiteralStringType::new);
        return descriptionFactory.literalInput(input).withType(literalType).build();
    }

    private TypedProcessOutputDescription createLiteralOutputDescription(LiteralOutputDescription output) {
        LiteralType<?> literalType = literalTypeRepository.getLiteralType(output).orElseGet(LiteralStringType::new);
        return descriptionFactory.literalOutput(output).withType(literalType).build();
    }

    private TypedProcessInputDescription createGroupInputDescription(GroupInputDescription input) {
        return descriptionFactory.groupInput().withDescription(input)
                                 .withInput(input.getInputDescriptions().stream()
                                                 .map(this::createProcessInputDescription))
                                 .build();
    }

    private TypedProcessOutputDescription createGroupOutputDescription(GroupOutputDescription output) {
        return descriptionFactory.groupOutput().withDescription(output)
                                 .withOutput(output.getOutputDescriptions().stream()
                                                   .map(this::createProcessOutputDescription))
                                 .build();
    }

    private TypedProcessInputDescription creatBoundingBoxInputDescription(BoundingBoxInputDescription input) {
        return descriptionFactory.boundingBoxInput(input).build();
    }

    private TypedProcessOutputDescription creatBoundingBoxOutputDescription(BoundingBoxOutputDescription output) {
        return descriptionFactory.boundingBoxOutput(output).build();
    }

    private TypedProcessInputDescription createComplexInputDescription(ComplexInputDescription input) {
        return descriptionFactory.complexInput(input).withType(DockerData.class).build();
    }

    private TypedProcessOutputDescription createComplexOutputDescription(ComplexOutputDescription output) {
        return descriptionFactory.complexOutput(output).withType(DockerData.class).build();
    }

    @Override
    public void execute(ProcessExecutionContext context) throws ExecutionException {
        DockerExecutionUnit executionUnit = (DockerExecutionUnit) applicationPackage.getExecutionUnit();
        Map<String, String> environment = new HashMap<>(executionUnit.getEnvironment());
        Path tempDirectory = null;
        try {
            TypedProcessDescription description = getDescription();
            try {
                tempDirectory = Files.createTempDirectory("javaps-docker-inputs-");
                LOG.debug("Saving inputs to {}", tempDirectory);
                createInputs(description, INPUT, environment, tempDirectory, context.getInputs());
                createOutputs(description, OUTPUT, environment, tempDirectory,
                              getOutputDefinitions(description, context));
            } catch (IOException | EncodingException e) {
                throw new ExecutionException("error creating inputs and output definitions", e);
            }
            try {
                CreateContainerCmd createContainerCmd = dockerClient.createContainerCmd(executionUnit.getImage());

                createContainerCmd.withEnv(environment.entrySet().stream()
                                                      .map(e -> String.format("%s=%s", e.getKey(), e.getValue()))
                                                      .toArray(String[]::new));

                String containerId = createContainerCmd.exec().getId();

                dockerClient.startContainerCmd(containerId).exec();



                LogContainerResultCallback loggingCallback = new LogContainerResultCallback() {
                    @Override
                    public void onNext(Frame item) {
                        LOG.info("[DOCKER] {}", item.toString());
                    }
                };

                // this essentially test the since=0 case
                this.docker.logContainerCmd(createResp.getId())
                           .withStdErr(true)
                           .withStdOut(true)
                           .withFollowStream(true)
                           .withTailAll()
                           .exec(loggingCallback);

                loggingCallback.awaitCompletion(30, TimeUnit.MINUTES);

                // on rare occassions this could happen before the container finished
                // lets wait five more minutes
                long waitStart = System.currentTimeMillis();
                while ((System.currentTimeMillis() - waitStart) < (1000 * 60 * 5)) {
                    InspectContainerResponse inspectResp = this.docker.inspectContainerCmd(createResp.getId()).exec();
                    if (!inspectResp.getState().getRunning()) {
                        break;
                    }

                    LOG.info("Container still running, lost log listener. Waiting to finish");
                    Thread.sleep(10000);
                }

            } catch (DockerException ex) {
                throw new ExecutionException(ex);
            }
        } finally {
            if (tempDirectory != null) {
                try {
                    MoreFiles.deleteRecursively(tempDirectory);
                } catch (IOException e) {
                    LOG.warn("could not delete temporary directory " + tempDirectory, e);
                }
            }
        }
    }

    private List<OutputDefinition> getOutputDefinitions(TypedProcessDescription description,
                                                        ProcessExecutionContext context) {
        return description.getOutputs().stream()
                          .map(context::getOutputDefinition)
                          .filter(Optional::isPresent).map(Optional::get)
                          .collect(toList());
    }

    private void createOutputs(TypedProcessOutputDescriptionContainer description, String prefix,
                               Map<String, String> environment, Path directory,
                               List<OutputDefinition> outputDefinitions) throws IOException {

        for (OutputDefinition outputDefinition : outputDefinitions) {

            TypedProcessOutputDescription<?> outputDescription = description.getOutput(outputDefinition.getId());
            String thisPrefix = join(prefix, asEnvironmentVariable(outputDefinition.getId()));
            if (outputDescription.isGroup()) {
                createOutputs(outputDescription.asGroup(),
                              thisPrefix,
                              environment,
                              directory,
                              outputDefinition.getOutputs());
            } else if (outputDescription.isComplex()) {
                Path tempFile = Files.createTempFile(directory, prefix, null);
                environment.put(prefix, tempFile.toAbsolutePath().toString());
                Files.deleteIfExists(tempFile);
                createFormat(thisPrefix, environment, outputDefinition.getFormat());
            }

        }
    }

    private void createFormat(String prefix, Map<String, String> environment, Format format) {
        format.getMimeType().ifPresent(mimeType -> environment.put(join(prefix, MIME_TYPE), mimeType));
        format.getSchema().ifPresent(schema -> environment.put(join(prefix, SCHEMA), schema));
        format.getEncoding().ifPresent(encoding -> environment.put(join(prefix, ENCODING), encoding));
    }

    private void createInputs(TypedProcessInputDescriptionContainer descriptionContainer, String prefix,
                              Map<String, String> environment, Path tempDirectory, ProcessInputs inputs)
            throws EncodingException, IOException {
        for (Map.Entry<OwsCode, List<Data<?>>> entry : inputs.entrySet()) {
            TypedProcessInputDescription description = descriptionContainer.getInput(entry.getKey());
            String inputPrefix = join(prefix, asEnvironmentVariable(entry.getKey()));
            if (description.isLiteral()) {
                createLiteralInput(description.asLiteral(), inputPrefix, environment, entry.getValue());
            } else if (description.isBoundingBox()) {
                createBoundingBoxInput(description.asBoundingBox(), inputPrefix, environment, entry.getValue());
            } else if (description.isComplex()) {
                createComplexInput(description.asComplex(), inputPrefix, environment, tempDirectory, entry.getValue());
            } else if (description.isGroup()) {
                createGroupInput(description.asGroup(), inputPrefix, environment, tempDirectory, entry.getValue());
            } else {
                LOG.warn("Unsupported input type: {}", description);
            }
        }
    }

    private void createBoundingBoxInput(TypedBoundingBoxInputDescription description, String prefix,
                                        Map<String, String> environment, List<Data<?>> values) {
        if (description.getOccurence().isMultiple()) {
            int index = 0;
            for (Data<?> data : values) {
                createBoundingBoxInput(join(prefix, String.valueOf(++index)), environment, (BoundingBoxData) data);

            }
        } else if (!values.isEmpty()) {
            createBoundingBoxInput(prefix, environment, (BoundingBoxData) values.iterator().next());
        }
    }

    private void createBoundingBoxInput(String prefix, Map<String, String> environment, BoundingBoxData data) {
        OwsBoundingBox payload = data.getPayload();
        NumberFormat numberFormat = NumberFormat.getInstance(Locale.ROOT);
        String value = DoubleStream.concat(Arrays.stream(payload.getLowerCorner()),
                                           Arrays.stream(payload.getUpperCorner()))
                                   .mapToObj(numberFormat::format)
                                   .collect(Collectors.joining(","));
        environment.put(prefix, value);
        payload.getCRS().map(URI::toString).ifPresent(crs -> environment.put(join(prefix, CRS), crs));
    }

    private void createLiteralInput(TypedLiteralInputDescription description, String prefix,
                                    Map<String, String> environment, List<Data<?>> values)
            throws EncodingException {
        if (description.getOccurence().isMultiple()) {
            int index = 0;
            for (Data<?> data : values) {
                createLiteralInput(description, join(prefix, String.valueOf(++index)), environment, (LiteralData) data);
            }
        } else if (!values.isEmpty()) {
            createLiteralInput(description, prefix, environment, (LiteralData) values.iterator().next());
        }
    }

    @SuppressWarnings("unchecked")
    private void createLiteralInput(TypedProcessInputDescription description, String prefix,
                                    Map<String, String> environment, LiteralData literalData)
            throws EncodingException {
        LiteralType type = description.asLiteral().getType();
        String value = type.generate(literalData.getPayload());
        environment.put(prefix, value);
        literalData.getUnitOfMeasurement().ifPresent(uom -> {
            environment.put(join(prefix, UOM), value);
        });
    }

    private void createComplexInput(TypedComplexInputDescription description, String prefix,
                                    Map<String, String> environment, Path directory, List<Data<?>> values)
            throws IOException {
        if (description.getOccurence().isMultiple()) {
            int index = 0;
            for (Data<?> data : values) {
                createComplexInput(join(prefix, String.valueOf(++index)), environment, directory, (DockerData) data);
            }
        } else if (!values.isEmpty()) {
            createComplexInput(prefix, environment, directory, (DockerData) values.iterator().next());
        }
    }

    private void createComplexInput(String prefix, Map<String, String> environment, Path directory, DockerData data)
            throws IOException {
        Path tempFile = Files.createTempFile(directory, prefix, null);
        Files.write(tempFile, data.getPayload());
        environment.put(prefix, tempFile.toAbsolutePath().toString());
        createFormat(prefix, environment, data.getFormat());
    }

    private void createGroupInput(TypedGroupInputDescription description, String prefix,
                                  Map<String, String> environment, Path directory, List<Data<?>> values)
            throws EncodingException, IOException {
        if (description.getOccurence().isMultiple()) {
            int index = 0;
            for (Data<?> data : values) {
                createGroupInput(description, join(prefix, String.valueOf(++index)), environment, directory,
                                 (GroupInputData) data);
            }
        } else if (!values.isEmpty()) {
            createGroupInput(description, prefix, environment, directory, (GroupInputData) values.iterator().next());
        }
    }

    private void createGroupInput(TypedGroupInputDescription descriptionContainer,
                                  String prefix, Map<String, String> environment, Path directory,
                                  GroupInputData data) throws EncodingException, IOException {
        createInputs(descriptionContainer, prefix, environment, directory, data.getPayload());
    }

    private static String asEnvironmentVariable(OwsCode string) {
        // TODO encode string as environment variable
        return string.toString();
    }

    private static String join(String... components) {
        return Arrays.stream(components).filter(Objects::nonNull).collect(Collectors.joining("_"));
    }

}
