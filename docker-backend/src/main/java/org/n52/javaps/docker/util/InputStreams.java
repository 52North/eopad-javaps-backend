package org.n52.javaps.docker.util;

import java.io.InputStream;

/**
 * Utility methods for {@link InputStream}.
 */
public class InputStreams {
    private static final InputStream EMPTY_INPUT_STREAM = new InputStream() {
        @Override
        public int read() {
            return -1;
        }
    };

    /**
     * Returns a empty {@link InputStream}.
     *
     * @return The empty {@link InputStream}.
     */
    public static InputStream empty() {
        return EMPTY_INPUT_STREAM;
    }
}
