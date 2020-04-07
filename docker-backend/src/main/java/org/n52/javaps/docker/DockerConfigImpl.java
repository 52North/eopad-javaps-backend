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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;

/**
 * Implementation of {@link DockerConfig}.
 *
 * @author Christian Autermann
 */
@Component
public class DockerConfigImpl implements DockerConfig {
    private String dataPath = "/data";
    private String inputPath = "/data/inputs";
    private String outputPath = "/data/outputs";
    private String dockerHost;
    private String user;
    private String group;
    private Duration processTimeout;
    private Duration stopTimeout;
    private Environment environment;
    private String javaPsVersion;

    @Override
    public Optional<String> getGroup() {
        return Optional.ofNullable(this.group);
    }

    /**
     * Set the group the Docker container should run as.
     *
     * @param group The group.
     */
    @Value("${docker.group:}")
    public void setGroup(String group) {
        this.group = group == null || group.isEmpty() ? null : group;
    }

    @Override
    public Optional<String> getUser() {
        return Optional.ofNullable(this.user);
    }

    /**
     * Set the user the Docker container should run as.
     *
     * @param user The user.
     */
    @Value("${docker.user:}")
    public void setUser(String user) {
        this.user = user == null || user.isEmpty() ? null : user;
    }

    @Override
    public Optional<Duration> getProcessTimeout() {
        return Optional.ofNullable(processTimeout);
    }

    /**
     * Set the timeout for processing containers.
     *
     * @param processTimeout The timeout.
     */
    public void setProcessTimeout(Duration processTimeout) {
        this.processTimeout = processTimeout;
    }

    /**
     * Set the timeout for processing containers.
     *
     * @param timeout The timeout.
     */
    @Value("${docker.timeout.process:PT1H}")
    public void setProcessTimeout(String timeout) {
        setProcessTimeout(timeout == null ? null : Duration.parse(timeout));
    }

    @Override
    public Optional<Duration> getStopTimeout() {
        return Optional.ofNullable(stopTimeout);
    }

    /**
     * Set the timeout to stop a container before killing it.
     *
     * @param stopTimeout The timeout.
     */
    public void setStopTimeout(Duration stopTimeout) {
        this.stopTimeout = stopTimeout;
    }

    /**
     * Set the timeout to stop a container before killing it.
     *
     * @param timeout The timeout.
     */
    @Value("${docker.timeout.stop:PT5M}")
    public void setStopTimeout(String timeout) {
        setStopTimeout(timeout == null ? null : Duration.parse(timeout));
    }

    @Override
    public String getJavaPsVersion() {
        return javaPsVersion;
    }

    /**
     * Set the javaPS version.
     *
     * @param javaPsVersion The javaPS version.
     */
    @Value("${javaPS.version:1.4.0}")
    public void setJavaPsVersion(String javaPsVersion) {
        this.javaPsVersion = javaPsVersion;
    }

    /**
     * Set the path inside the Docker container where the specified input will be put.
     *
     * @param inputPath The input path.
     */
    @Value("${docker.inputPath:/data/inputs}")
    public void setInputPath(String inputPath) {
        this.inputPath = inputPath;
    }

    @Override
    public String getInputPath() {
        return inputPath;
    }

    @Override
    public String getInputPath(String file) {
        return combinePath(inputPath, file);
    }

    @Override
    public String getOutputPath() {
        return outputPath;
    }

    @Override
    public String getOutputPath(String file) {
        return combinePath(outputPath, file);
    }

    /**
     * Set the path inside the Docker container where outputs will be put.
     *
     * @param outputPath The output path.
     */
    @Value("${docker.outputPath:/data/outputs}")
    public void setOutputPath(String outputPath) {
        this.outputPath = outputPath;
    }

    @Override
    public String getDataPath() {
        return dataPath;
    }

    /**
     * Set the parent path for inputs and outputs.
     *
     * @param dataPath The data path.
     */
    @Value("${docker.dataPath:/data}")
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

    /**
     * Set the Docker host to use.
     *
     * @param dockerHost The Docker host.
     */
    @Value("${docker.host:unix:///var/run/docker.sock}")
    public void setDockerHost(String dockerHost) {
        this.dockerHost = dockerHost;
    }

    /**
     * Set the global environment for all processing containers.
     *
     * @param environment The {@link Environment}.
     */
    @Autowired
    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }

    /**
     * Combine two paths.
     *
     * @param directory The first part of the path.
     * @param file      The second part of the path.
     * @return The path.
     */
    private String combinePath(String directory, String file) {
        if (file.startsWith("/")) {
            return file;
        }

        return String.format("%s/%s",
                             directory.length() == 1 ? directory :
                             directory.endsWith("/") ? directory.substring(0, directory.length() - 1)
                                                     : directory,
                             file);

    }

}
