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

import com.github.dockerjava.api.exception.DockerClientException;
import com.github.dockerjava.api.exception.DockerException;
import com.google.common.io.CharStreams;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.n52.javaps.algorithm.ExecutionException;
import org.n52.javaps.algorithm.ProcessOutputs;
import org.n52.javaps.docker.io.DockerOutputData;
import org.n52.javaps.docker.util.InputStreams;
import org.n52.javaps.io.DecodingException;
import org.n52.javaps.io.GroupOutputData;
import org.n52.javaps.io.bbox.BoundingBoxData;
import org.n52.javaps.io.literal.LiteralData;
import org.n52.javaps.io.literal.LiteralType;
import org.n52.shetland.ogc.ows.OwsBoundingBox;
import org.n52.shetland.ogc.wps.Format;
import org.n52.shetland.ogc.wps.description.ProcessOutputDescription;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class DockerOutputProcessor extends AbstractDockerProcessor<List<DockerOutputInfo>, Void> {

    public DockerOutputProcessor(DockerJobConfig config) {
        super(config);
    }

    @Override
    public Void process(List<DockerOutputInfo> outputInfos) throws ExecutionException {
        try {
            ProcessOutputs outputs = context().getOutputs();
            for (DockerOutputInfo outputInfo : outputInfos) {
                readOutput(outputInfo, outputs);
            }
            return null;
        } catch (DockerException | DecodingException | IOException ex) {
            throw new ExecutionException("could not copy outputs", ex);
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
        InputStream inputStream = client().copyArchiveFromContainerCmd(getProcessContainerId(), path.getPath()).exec();
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
