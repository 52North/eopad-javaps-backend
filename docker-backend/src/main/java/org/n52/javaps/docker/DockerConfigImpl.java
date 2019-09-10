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

import com.github.dockerjava.api.DockerClient;
import org.n52.janmayen.function.Predicates;

import java.time.Duration;
import java.util.Optional;

public class DockerConfigImpl implements DockerConfig {

    private String user;
    private String group;
    private Duration timeout;
    private DockerClient client;
    private static final String DATA_FOLDER = "/data";
    private static final String INPUT_FOLDER = "/data/inputs";
    private static final String OUTPUT_FOLDER = "/data/outputs";

    public DockerConfigImpl() {
    }

    @Override
    public DockerClient getClient() {
        return client;
    }

    public void setClient(DockerClient client) {
        this.client = client;
    }

    public DockerConfigImpl(String user, String group) {
        this.user = user;
        this.group = group;
    }

    @Override
    public Optional<String> getGroup() {
        return Optional.ofNullable(this.group)
                       .filter(Predicates.not(String::isEmpty));
    }

    public void setGroup(String group) {
        this.group = group;
    }

    @Override
    public Optional<String> getUser() {
        return Optional.ofNullable(this.user)
                       .filter(Predicates.not(String::isEmpty));
    }

    public void setUser(String user) {
        this.user = user;
    }

    @Override
    public Optional<Duration> getTimeout() {
        return Optional.ofNullable(timeout);
    }

    public void setTimeout(Duration timeout) {
        this.timeout = timeout;
    }

    @Override
    public String getInputPath() {
        return INPUT_FOLDER;
    }

    @Override
    public String getOutputPath() {
        return OUTPUT_FOLDER;
    }

    @Override
    public String getOutputPath(String file) {
        return String.format("%s/%s", OUTPUT_FOLDER, file);
    }

    @Override
    public String getInputPath(String file) {
        return String.format("%s/%s", INPUT_FOLDER, file);
    }

    @Override
    public String getDataPath() {
        return DATA_FOLDER;
    }
}
