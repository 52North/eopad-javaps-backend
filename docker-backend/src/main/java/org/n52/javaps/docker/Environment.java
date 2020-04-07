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
package org.n52.javaps.docker;

import com.google.common.annotations.VisibleForTesting;
import org.n52.janmayen.function.Predicates;
import org.n52.javaps.docker.util.MapDelegate;
import org.n52.shetland.ogc.ows.OwsCode;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static java.util.stream.Collectors.joining;

/**
 * Environment map that can have a prefix and converts keys to environment variable name conventions.
 *
 * @author Christian Autermann
 */
public class Environment extends MapDelegate<String, String> {
    public static final String MIME_TYPE = "MIME_TYPE";
    public static final String SCHEMA = "SCHEMA";
    public static final String ENCODING = "ENCODING";
    public static final String UOM = "UOM";
    public static final String CRS = "CRS";
    public static final String INPUT = "INPUT";
    public static final String OUTPUT = "OUTPUT";
    private final Map<String, String> values;
    private final String prefix;

    public Environment() {
        this(null, null);
    }

    public Environment(Map<? extends String, ? extends String> m) {
        this(null, m == null ? null : new HashMap<>(m));
    }

    private Environment(String prefix, Map<String, String> values) {
        this.values = Optional.ofNullable(values).orElseGet(HashMap::new);
        this.prefix = prefix;
    }

    @Override
    protected Map<String, String> getDelegate() {
        return values;
    }

    /**
     * Return a new {@link Environment} where all added keys will get the specified prefix.
     *
     * @param prefix The prefix.
     * @return The {@link Environment}.
     */
    public Environment withPrefix(Object prefix) {
        return withPrefix(String.valueOf(prefix));
    }

    /**
     * Return a new {@link Environment} where all added keys will get the specified prefix.
     *
     * @param prefix The prefix.
     * @return The {@link Environment}.
     */
    public Environment withPrefix(OwsCode prefix) {
        return withPrefix(getVariableName(prefix.getValue()));
    }

    /**
     * Return a new {@link Environment} where all added keys will get the specified prefix.
     *
     * @param prefix The prefix.
     * @return The {@link Environment}.
     */
    public Environment withPrefix(String prefix) {
        return new Environment(join(this.prefix, prefix), this.values);
    }

    /**
     * Put the value under the current prefix.
     *
     * @param value The value.
     * @return The previous value of that key.
     * @throws IllegalStateException If this {@link Environment} has no prefix.
     */
    public String put(Object value) throws IllegalStateException {
        return put(null, value);
    }

    /**
     * Put the value under the (prefixed) key.
     *
     * @param key   The key.
     * @param value The value.
     * @return The previous value of that key.
     * @throws IllegalStateException If this {@link Environment} has no prefix and {@code key} is {@code null}.
     */
    public String put(String key, Object value) throws IllegalStateException {
        return put(key, value == null ? null : value.toString());
    }

    /**
     * Put the value under the (prefixed) key.
     *
     * @param key   The key.
     * @param value The value.
     * @return The previous value of that key.
     * @throws IllegalStateException If this {@link Environment} has no prefix and {@code key} is {@code null}.
     */
    @Override
    public String put(String key, String value) throws IllegalStateException {
        if (!hasPrefix() && (key == null || key.isEmpty())) {
            throw new IllegalStateException("null or empty keys are only supported with prefix");
        }
        return this.values.put(key == null ? prefix : join(prefix, getVariableName(key)), value);
    }

    /**
     * Checks if this {@link Environment} has a prefix.
     *
     * @return If this {@link Environment} has a prefix.
     */
    public boolean hasPrefix() {
        return this.prefix != null && !this.prefix.isEmpty();
    }

    /**
     * Encode this {@link Environment} as a Docker environment.
     *
     * @return The list of environment variables.
     */
    public String[] encode() {
        return values.entrySet().stream().map(this::encodeEnvironment).toArray(String[]::new);
    }

    /**
     * Encode the environment variable as a Docker environment variable.
     *
     * @param entry The environment variable.
     * @return The Docker environment variable
     */
    private String encodeEnvironment(Map.Entry<String, String> entry) {
        String value = entry.getValue();
        return String.format("%s=%s", entry.getKey(), value == null ? "" : value);
    }

    /**
     * Get the prefix of this {@link Environment}.
     *
     * @return The prefix.
     */
    public String getPrefix() {
        return prefix;
    }

    /**
     * Join the supplied strings with an underscore.
     *
     * @param components The strings.
     * @return The joined string.
     */
    private static String join(String... components) {
        return Arrays.stream(components).filter(Objects::nonNull)
                     .filter(Predicates.not(String::isEmpty))
                     .collect(joining("_"));
    }

    /**
     * Transform the value to an environment variable name.
     *
     * @param value The value.
     * @return The environment variable name.
     */
    @VisibleForTesting
    static String getVariableName(String value) {
        return value.replaceAll("([a-z])([A-Z])", "$1_$2")
                    .replaceAll("[^a-zA-Z0-9]", "_")
                    .replaceAll("_{2,}", "_")
                    .toUpperCase(Locale.ROOT);
    }

}
