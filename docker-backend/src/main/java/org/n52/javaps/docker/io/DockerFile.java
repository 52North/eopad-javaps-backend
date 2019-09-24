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

import org.n52.javaps.docker.process.DockerJobConfig;
import org.n52.javaps.docker.util.DockerUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

public class DockerFile {
    private final String path;
    private final DockerJobConfig config;

    public DockerFile(DockerJobConfig config, String path) {
        this.config = Objects.requireNonNull(config);
        this.path = Objects.requireNonNull(path);
    }

    public InputStream read() throws IOException {
        return DockerUtils.readFile(this.config, this.path);
    }
}
