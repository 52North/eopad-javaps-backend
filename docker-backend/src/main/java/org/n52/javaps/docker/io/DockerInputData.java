/*
 * Copyright 2019-2020 52°North Initiative for Geospatial Open Source
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

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.n52.javaps.io.complex.ComplexData;
import org.n52.shetland.ogc.wps.Format;

import java.util.Objects;

/**
 * {@link ComplexData} implementation that can hold arbitrary formatted input data.
 *
 * @author Christian Autermann
 */
public class DockerInputData extends FormattedData<byte[]> {
    private static final long serialVersionUID = -3925986655313130283L;
    private byte[] content;

    /**
     * Create a new {@link DockerInputData}.
     *
     * @param content The content.
     * @param format  The {@link Format} of the content.
     */
    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public DockerInputData(byte[] content, Format format) {
        super(format);
        this.content = Objects.requireNonNull(content);
    }

    @Override
    @SuppressFBWarnings("EI_EXPOSE_REP")
    public byte[] getPayload() {
        return this.content;
    }

    @Override
    public Class<?> getSupportedClass() {
        return byte[].class;
    }

}
