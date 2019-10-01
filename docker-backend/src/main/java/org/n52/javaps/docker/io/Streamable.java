package org.n52.javaps.docker.io;

import org.n52.janmayen.function.ThrowingSupplier;

import java.io.IOException;
import java.io.InputStream;

@FunctionalInterface
public interface Streamable extends ThrowingSupplier<InputStream, IOException> {

}
