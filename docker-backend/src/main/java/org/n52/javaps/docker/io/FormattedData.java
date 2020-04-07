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

import org.n52.javaps.io.complex.ComplexData;
import org.n52.shetland.ogc.wps.Format;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Objects;

/**
 * {@link ComplexData} that is aware of it's {@link Format}.
 *
 * @param <T> The payload type.
 * @author Christian Autermann
 */
public abstract class FormattedData<T> implements ComplexData<T> {
    private static final long serialVersionUID = 2698679543911095384L;
    private Format format;

    /**
     * Creates a new {@link FormattedData}.
     *
     * @param format The {@link Format}.
     */
    public FormattedData(Format format) {
        this.format = Objects.requireNonNull(format);
    }

    /**
     * Get the {@link Format}.
     *
     * @return The {@link Format}.
     */
    public Format getFormat() {
        return format;
    }

    private void writeObject(ObjectOutputStream out) throws IOException {
        out.writeUTF(format.getMimeType().orElse(null));
        out.writeUTF(format.getEncoding().orElse(null));
        out.writeUTF(format.getSchema().orElse(null));
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        this.format = new Format(in.readUTF(), in.readUTF(), in.readUTF());
    }

}
