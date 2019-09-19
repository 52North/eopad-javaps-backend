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
package org.n52.javaps.docker;

import org.n52.javaps.description.TypedProcessInputDescription;
import org.n52.javaps.description.TypedProcessOutputDescription;
import org.n52.javaps.io.Data;
import org.n52.javaps.io.EncodingException;
import org.n52.javaps.io.InputHandler;
import org.n52.javaps.io.OutputHandler;
import org.n52.shetland.ogc.wps.Format;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.Set;

@Component
public class DockerDataHandler implements InputHandler, OutputHandler {

    private static final int BUFFER_SIZE = 16384;

    @Override
    public Data<?> parse(TypedProcessInputDescription<?> description, InputStream input, Format format)
            throws IOException {
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            int n;
            byte[] buffer = new byte[BUFFER_SIZE];
            while ((n = input.read(buffer, 0, buffer.length)) != -1) {
                output.write(buffer, 0, n);
            }
            return new DockerData(output.toByteArray(), format);
        }
    }

    @Override
    public InputStream generate(TypedProcessOutputDescription<?> description, Data<?> data, Format format)
            throws EncodingException {
        DockerData dockerData = (DockerData) data;
        if (!format.isCompatible(dockerData.getFormat())) {
            throw new EncodingException("incompatible format");
        }
        return new ByteArrayInputStream(dockerData.getPayload());
    }

    @Override
    public Set<Format> getSupportedFormats() {
        return Collections.singleton(new Format("*/*"));
    }

    @Override
    public Set<Class<? extends Data<?>>> getSupportedBindings() {
        return Collections.singleton(DockerData.class);
    }

}
