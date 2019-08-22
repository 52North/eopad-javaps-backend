package org.n52.wps.eopad;

import org.n52.javaps.io.complex.ComplexData;
import org.n52.shetland.ogc.wps.Format;

import java.util.Objects;

public class DockerData implements ComplexData<byte[]> {
    private final byte[] content;
    private final Format format;

    public DockerData(byte[] content, Format format) {
        this.content = Objects.requireNonNull(content);
        this.format = Objects.requireNonNull(format);
    }

    public byte[] getPayload() {
        return this.content;
    }

    public Format getFormat() {
        return format;
    }

    @Override
    public Class<?> getSupportedClass() {
        return byte[].class;
    }

}
