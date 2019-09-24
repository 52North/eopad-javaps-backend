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
package org.n52.javaps.docker.io;

import org.n52.javaps.description.TypedProcessOutputDescription;
import org.n52.javaps.io.Data;
import org.n52.javaps.io.EncodingException;
import org.n52.javaps.io.OutputHandler;
import org.n52.shetland.ogc.wps.Format;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.Set;

@Component
public class DockerOutputDataHandler implements OutputHandler {
    private static final Logger LOG = LoggerFactory.getLogger(DockerOutputDataHandler.class);
    private static final Format ANY_FORMAT = new Format("*/*");

    public InputStream generate(TypedProcessOutputDescription<?> description, Data<?> data, Format format)
            throws EncodingException {
        DockerOutputData dockerData = (DockerOutputData) data;
        if (!format.isCompatible(dockerData.getFormat())) {
            throw new EncodingException("incompatible format");
        }
        return generate(dockerData);
    }

    private InputStream generate(DockerOutputData data) throws EncodingException {
        LOG.debug("Reading docker file");
        try {
            return new InputStreamWrapper(data.getPayload().read()) {
                @Override
                public void close() throws IOException {
                    LOG.debug("Read docker file");
                    super.close();
                    data.destroy();
                }
            };
        } catch (IOException e) {
            throw new EncodingException(e);
        }
    }

    @Override
    public Set<Format> getSupportedFormats() {
        return Collections.singleton(ANY_FORMAT);
    }

    @Override
    public Set<Class<? extends Data<?>>> getSupportedBindings() {
        return Collections.singleton(DockerOutputData.class);
    }

}
