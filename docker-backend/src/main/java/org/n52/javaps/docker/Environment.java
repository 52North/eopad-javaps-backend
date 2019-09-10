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

import org.n52.janmayen.function.Predicates;
import org.n52.shetland.ogc.ows.OwsCode;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import static java.util.stream.Collectors.joining;

public class Environment {
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
        this.values = Objects.requireNonNull(values);
        this.prefix = prefix;
    }

    public Environment withPrefix(Object prefix) {
        return withPrefix(String.valueOf(prefix));
    }

    public Environment withPrefix(String prefix) {
        return new Environment(join(this.prefix, prefix), this.values);
    }

    public void put(Object value) {
        put(null, value);
    }

    public void put(String key, Object value) {
        if (!hasPrefix() && (key == null || key.isEmpty())) {
            throw new IllegalStateException("null or empty keys are only supported with prefix");
        }

        this.values.put(join(prefix, key), value == null ? null : value.toString());
    }

    public boolean hasPrefix() {
        return this.prefix != null && !this.prefix.isEmpty();
    }

    public String[] encode() {
        return values.entrySet().stream().map(this::encodeEnvironment).toArray(String[]::new);
    }

    private String encodeEnvironment(Map.Entry<String, String> e) {
        String value = e.getValue();
        return String.format("%s=%s", e.getKey(), value == null ? "" : value);
    }

    public String getPrefix() {
        return prefix;
    }

    private static String join(String... components) {
        return Arrays.stream(components).filter(Objects::nonNull)
                     .filter(Predicates.not(String::isEmpty))
                     .collect(joining("_"));
    }

    public static String getVariableName(OwsCode id) {
        return getVariableName(id.getValue());
    }

    public static String getVariableName(String value) {
        return value.replaceAll("([a-z])([A-Z])", "$1_$2")
                    .replaceAll("[^a-zA-Z0-9]", "_")
                    .replaceAll("_{2,}", "_")
                    .toUpperCase(Locale.ROOT);
    }

}
