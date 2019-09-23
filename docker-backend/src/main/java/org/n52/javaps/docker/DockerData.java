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

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.n52.javaps.io.complex.ComplexData;
import org.n52.shetland.ogc.wps.Format;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Objects;

public class DockerData implements ComplexData<byte[]> {
    private static final long serialVersionUID = -3925986655313130283L;
    private byte[] content;
    private Format format;

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public DockerData(byte[] content, Format format) {
        this.content = Objects.requireNonNull(content);
        this.format = Objects.requireNonNull(format);
    }

    @Override
    @SuppressFBWarnings("EI_EXPOSE_REP")
    public byte[] getPayload() {
        return this.content;
    }

    public Format getFormat() {
        return format;
    }

    private void writeObject(ObjectOutputStream out) throws IOException {
        out.writeObject(content);
        out.writeUTF(format.getMimeType().orElse(null));
        out.writeUTF(format.getEncoding().orElse(null));
        out.writeUTF(format.getSchema().orElse(null));
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        this.content = (byte[]) in.readObject();
        this.format = new Format(in.readUTF(), in.readUTF(), in.readUTF());
    }

    @Override
    public Class<?> getSupportedClass() {
        return byte[].class;
    }

}
