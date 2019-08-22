package org.n52.wps.eopad;

import org.n52.javaps.description.TypedProcessInputDescription;
import org.n52.javaps.description.TypedProcessOutputDescription;
import org.n52.javaps.io.Data;
import org.n52.javaps.io.EncodingException;
import org.n52.javaps.io.InputHandler;
import org.n52.javaps.io.OutputHandler;
import org.n52.shetland.ogc.wps.Format;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.Set;

public class DockerDataHandler implements InputHandler, OutputHandler {
    @Override
    public Data<?> parse(TypedProcessInputDescription<?> description, InputStream input, Format format)
            throws IOException {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            int n;
            byte[] buffer = new byte[16384];

            while ((n = input.read(buffer, 0, buffer.length)) != -1) {
                baos.write(buffer, 0, n);
            }
            return new DockerData(baos.toByteArray(), format);
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
