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

import com.github.dockerjava.api.command.CopyArchiveToContainerCmd;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.n52.javaps.algorithm.ExecutionException;
import org.n52.javaps.algorithm.ProcessInputs;
import org.n52.javaps.description.TypedBoundingBoxInputDescription;
import org.n52.javaps.description.TypedComplexInputDescription;
import org.n52.javaps.description.TypedGroupInputDescription;
import org.n52.javaps.description.TypedLiteralInputDescription;
import org.n52.javaps.description.TypedProcessInputDescription;
import org.n52.javaps.description.TypedProcessInputDescriptionContainer;
import org.n52.javaps.docker.io.DockerInputData;
import org.n52.javaps.docker.Environment;
import org.n52.javaps.io.Data;
import org.n52.javaps.io.EncodingException;
import org.n52.javaps.io.GroupInputData;
import org.n52.javaps.io.bbox.BoundingBoxData;
import org.n52.javaps.io.literal.LiteralData;
import org.n52.javaps.io.literal.LiteralType;
import org.n52.shetland.ogc.ows.OwsBoundingBox;
import org.n52.shetland.ogc.ows.OwsCode;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.text.NumberFormat;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.DoubleStream;

import static java.util.stream.Collectors.joining;

public class DockerInputProcessor extends AbstractDockerProcessor<ProcessInputs, Void> {

    private final String containerId;

    public DockerInputProcessor(DockerJobConfig config) {
        super(config);
        this.containerId = Objects.requireNonNull(config.getHelperContainerId());
    }

    @Override
    public Void process(ProcessInputs inputs) throws ExecutionException {
        try {
            createInputs(description(), getJobEnvironment().withPrefix(Environment.INPUT), inputs);
        } catch (IOException | EncodingException e) {
            throw new ExecutionException("error creating inputs", e);
        }
        return null;
    }

    private void createInputs(TypedProcessInputDescriptionContainer descriptionContainer,
                              Environment environment, ProcessInputs inputs)
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
                log().warn("Unsupported input type: {}", description);
            }
        }
    }

    private void createBoundingBoxInput(TypedBoundingBoxInputDescription description,
                                        Environment environment,
                                        List<Data<?>> values) {
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
                                    List<Data<?>> values) throws EncodingException {
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
                                    LiteralData literalData) throws EncodingException {
        LiteralType type = description.getType();
        String value = type.generate(literalData.getPayload());
        environment.put(value);
        literalData.getUnitOfMeasurement().ifPresent(uom -> environment.put(Environment.UOM, value));
    }

    private void createComplexInput(TypedComplexInputDescription description, Environment environment,
                                    List<Data<?>> values) throws IOException {
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
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (TarArchiveOutputStream tarOutput = new TarArchiveOutputStream(baos)) {
            TarArchiveEntry entry = new TarArchiveEntry(name);
            entry.setSize(payload.length);
            tarOutput.putArchiveEntry(entry);
            tarOutput.write(payload);
            tarOutput.closeArchiveEntry();
        }
        copyArchiveToContainerCmd().withRemotePath(getInputPath())
                                   .withTarInputStream(new ByteArrayInputStream(baos.toByteArray()))
                                   .exec();

        environment.put(getInputPath(name));
        createFormat(environment, data.getFormat());
    }

    private CopyArchiveToContainerCmd copyArchiveToContainerCmd() {
        return client().copyArchiveToContainerCmd(containerId);
    }

    private void createGroupInput(TypedGroupInputDescription description, Environment environment,
                                  List<Data<?>> values) throws EncodingException, IOException {
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
                                  GroupInputData data) throws EncodingException, IOException {
        createInputs(descriptionContainer, environment, data.getPayload());
    }

}
