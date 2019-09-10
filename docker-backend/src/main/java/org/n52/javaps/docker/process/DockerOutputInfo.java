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
package org.n52.javaps.docker.process;

import org.n52.javaps.description.TypedProcessOutputDescription;
import org.n52.shetland.ogc.wps.OutputDefinition;

import java.util.List;
import java.util.Objects;

public class DockerOutputInfo {
    private final OutputDefinition definition;
    private final TypedProcessOutputDescription<?> description;
    private final String path;
    private final List<DockerOutputInfo> outputInfos;

    public DockerOutputInfo(OutputDefinition definition, TypedProcessOutputDescription<?> description, String path) {
        this.definition = Objects.requireNonNull(definition);
        this.description = Objects.requireNonNull(description);
        this.path = Objects.requireNonNull(path);
        this.outputInfos = null;
    }

    public DockerOutputInfo(OutputDefinition definition,
                            TypedProcessOutputDescription<?> description,
                            List<DockerOutputInfo> outputInfos) {
        this.definition = Objects.requireNonNull(definition);
        this.description = description.asGroup();
        this.outputInfos = Objects.requireNonNull(outputInfos);
        this.path = null;
    }

    public OutputDefinition getDefinition() {
        return definition;
    }

    public TypedProcessOutputDescription<?> getDescription() {
        return description;
    }

    public String getPath() {
        return path;
    }

    public List<DockerOutputInfo> getOutputInfos() {
        return outputInfos;
    }
}
