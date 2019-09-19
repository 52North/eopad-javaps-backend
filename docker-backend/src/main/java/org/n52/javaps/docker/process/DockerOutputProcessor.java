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

import com.github.dockerjava.api.command.CopyArchiveFromContainerCmd;
import com.github.dockerjava.api.exception.DockerException;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.n52.javaps.algorithm.ExecutionException;
import org.n52.javaps.algorithm.ProcessOutputs;
import org.n52.javaps.docker.DockerData;
import org.n52.javaps.io.DecodingException;
import org.n52.javaps.io.GroupOutputData;
import org.n52.javaps.io.bbox.BoundingBoxData;
import org.n52.javaps.io.literal.LiteralData;
import org.n52.javaps.io.literal.LiteralType;
import org.n52.shetland.ogc.ows.OwsBoundingBox;
import org.n52.shetland.ogc.wps.description.ProcessOutputDescription;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class DockerOutputProcessor extends AbstractDockerProcessor<List<DockerOutputInfo>, Void> {

    private final ProcessOutputs outputs;
    private final String containerId;

    public DockerOutputProcessor(DockerJobConfig config) {
        super(config);
        this.outputs = config.getContext().getOutputs();
        this.containerId = config.getProcessContainerId();
    }

    @Override
    public Void process(List<DockerOutputInfo> outputInfos) throws ExecutionException {
        try {
            for (DockerOutputInfo outputInfo : outputInfos) {
                readOutput(outputInfo, this.outputs);
            }
        } catch (DockerException | DecodingException | IOException ex) {
            throw new ExecutionException("could not copy outputs", ex);
        }
        return null;
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

    private void readBoundingBoxOutput(DockerOutputInfo outputInfo,
                                       ProcessOutputs outputs) {
        // TODO
        OwsBoundingBox owsBoundingBox = new OwsBoundingBox(new double[]{}, new double[]{});
        outputs.put(outputInfo.getDescription().getId(), new BoundingBoxData(owsBoundingBox));
    }

    private void readComplexOutput(DockerOutputInfo outputInfo, ProcessOutputs outputs)
            throws IOException {
        byte[] bytes = copyFile(containerId, outputInfo.getPath());
        DockerData data = new DockerData(bytes, outputInfo.getDefinition().getFormat());
        outputs.put(outputInfo.getDescription().getId(), data);
    }

    private void readLiteralOutput(DockerOutputInfo outputInfo, ProcessOutputs outputs)
            throws DecodingException, IOException {
        byte[] bytes = copyFile(containerId, outputInfo.getPath());
        String value = bytes == null ? "" : new String(bytes, StandardCharsets.UTF_8);
        LiteralType<?> literalType = outputInfo.getDescription().asLiteral().getType();
        LiteralData data = literalType.parseToBinding(value);
        outputs.put(outputInfo.getDescription().getId(), data);
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

    private byte[] copyFile(String containerId, String path) throws IOException {
        CopyArchiveFromContainerCmd command = getClient().copyArchiveFromContainerCmd(containerId, path);
        try (InputStream is = command.exec()) {
            return readArchiveFile(is);
        }
    }

    private byte[] readArchiveFile(InputStream exec) throws IOException {
        try (TarArchiveInputStream taris = new TarArchiveInputStream(exec);
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[16348];
            if (taris.getNextTarEntry() != null) {
                int count = 0;
                while ((count = taris.read(buffer)) != -1) {
                    baos.write(buffer, 0, count);
                }
                return baos.toByteArray();
            }
            return null;
        }

    }
}
