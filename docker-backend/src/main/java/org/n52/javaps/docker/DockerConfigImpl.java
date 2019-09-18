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

import java.time.Duration;
import java.util.Optional;

public class DockerConfigImpl implements DockerConfig {
    private String dataPath = "/data";
    private String inputPath = "/data/inputs";
    private String outputPath = "/data/outputs";
    private String dockerHost;
    private String user;
    private String group;
    private Duration timeout;
    private Environment environment;
    private String javaPsVersion;

    public DockerConfigImpl() {
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
    public String getJavaPsVersion() {
        return javaPsVersion;
    }

    public void setJavaPsVersion(String javaPsVersion) {
        this.javaPsVersion = javaPsVersion;
    }

    @Override
    public String getInputPath() {
        return inputPath;
    }

    public void setInputPath(String inputPath) {
        this.inputPath = inputPath;
    }

    @Override
    public String getOutputPath() {
        return outputPath;
    }

    public void setOutputPath(String outputPath) {
        this.outputPath = outputPath;
    }

    @Override
    public String getOutputPath(String file) {
        return String.format("%s/%s", outputPath, file);
    }

    @Override
    public String getInputPath(String file) {
        return String.format("%s/%s", inputPath, file);
    }

    @Override
    public String getDataPath() {
        return dataPath;
    }

    public void setDataPath(String dataPath) {
        this.dataPath = dataPath;
    }

    @Override
    public Environment getGlobalEnvironment() {
        return environment;
    }

    @Override
    public String getDockerHost() {
        return dockerHost;
    }

    public void setDockerHost(String dockerHost) {
        this.dockerHost = dockerHost;
    }

    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }
}
