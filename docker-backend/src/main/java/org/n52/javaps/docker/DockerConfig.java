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

import org.n52.javaps.docker.process.DockerAlgorithm;

import java.time.Duration;
import java.util.Optional;

/**
 * Configuration for {@link DockerAlgorithmRepository} and {@link DockerAlgorithm}.
 *
 * @author Christian Autermann
 */
public interface DockerConfig {

    /**
     * Get the group the Docker container should run as.
     *
     * @return The group.
     */
    Optional<String> getGroup();

    /**
     * Get the user the Docker container should run as.
     *
     * @return The user.
     */
    Optional<String> getUser();

    /**
     * Get the timeout for processing containers.
     *
     * @return The timeout.
     */
    Optional<Duration> getProcessTimeout();

    /**
     * Get the timeout to stop a container before killing it.
     *
     * @return The timeout.
     */
    Optional<Duration> getStopTimeout();

    /**
     * Get the path inside the Docker container where inputs will be put.
     *
     * @return The input path.
     */
    String getInputPath();

    /**
     * Get the path inside the Docker container where the specified input will be put.
     *
     * @param file The file name.
     * @return The input path.
     */
    String getInputPath(String file);

    /**
     * Get the path inside the Docker container where outputs will be put.
     *
     * @return The output path.
     */
    String getOutputPath();

    /**
     * Get the path inside the Docker container where the specified output will be put.
     *
     * @param file The file name.
     * @return The input path.
     */
    String getOutputPath(String file);

    /**
     * Get the parent path for inputs and outputs.
     *
     * @return The data path.
     */
    String getDataPath();

    /**
     * Get the global environment for all processing containers.
     *
     * @return The {@link Environment}.
     */
    Environment getGlobalEnvironment();

    /**
     * Get the Docker host to use.
     *
     * @return The Docker host.
     */
    String getDockerHost();

    /**
     * Get the javaPS version.
     *
     * @return The javaPS version.
     */
    String getJavaPsVersion();
}
