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

import org.n52.javaps.io.complex.ComplexData;
import org.n52.shetland.ogc.wps.Format;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Objects;

/**
 * {@link ComplexData} for outputs of Docker containers. This class is <em>not</em> serializable.
 *
 * @author Christian Autermann
 */
public class DockerOutputData extends FormattedData<Streamable> {
    private static final long serialVersionUID = -5532313648697378332L;
    private final Streamable data;

    /**
     * Creates a new {@link DockerOutputData}.
     *
     * @param data   The {@link Streamable}.
     * @param format The {@link Format}.
     */
    public DockerOutputData(Streamable data, Format format) {
        super(format);
        this.data = Objects.requireNonNull(data);
    }

    @Override
    public Streamable getPayload() {
        return data;
    }

    @Override
    public Class<?> getSupportedClass() {
        return Streamable.class;
    }

    private void writeObject(ObjectOutputStream out) throws IOException {
        unserializable();
    }

    private void readObject(ObjectInputStream in) throws IOException {
        unserializable();
    }

    private void unserializable() throws IOException {
        throw new IOException(getClass().getName() + "is not serializable");
    }

}