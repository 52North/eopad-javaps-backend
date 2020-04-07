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
package org.n52.javaps.docker.process;

import com.github.dockerjava.api.DockerClient;
import org.n52.javaps.description.TypedProcessDescription;
import org.n52.javaps.docker.DockerConfig;
import org.n52.javaps.docker.Environment;
import org.n52.javaps.engine.ProcessExecutionContext;
import org.slf4j.Logger;

/**
 * Extension of {@link DockerConfig} for a single job.
 */
public interface DockerJobConfig extends DockerConfig {
    /**
     * Get the {@link Logger} to use for this job.
     *
     * @return The {@link Logger}.
     */
    Logger log();

    /**
     * Get the {@link DockerClient} to use for this job.
     *
     * @return The {@link DockerClient}.
     */
    DockerClient client();

    /**
     * Get the {@link TypedProcessDescription} of the process.
     *
     * @return The {@link TypedProcessDescription}.
     */
    TypedProcessDescription description();

    /**
     * Get the {@link ProcessExecutionContext} of this job.
     *
     * @return The {@link ProcessExecutionContext}.
     */
    ProcessExecutionContext context();

    /**
     * Get the {@link Environment} of this job.
     *
     * @return The {@link Environment}.
     */
    Environment getJobEnvironment();

    /**
     * Get the id of the helper container used for this job.
     *
     * @return The container id.
     */
    String getHelperContainerId();

    /**
     * Set the id of the helper container used for this job.
     *
     * @param helperContainerId The container id.
     */
    void setHelperContainerId(String helperContainerId);

    /**
     * Get the id of the process container used for this job.
     *
     * @return The container id.
     */
    String getProcessContainerId();

    /**
     * Set the id of the process container used for this job.
     *
     * @param processContainerId The container id.
     */
    void setProcessContainerId(String processContainerId);

    /**
     * Get the id of the volume used for this job.
     *
     * @return The volume id.
     */
    String getVolumeId();

    /**
     * Set the id of the volume used for this job.
     *
     * @param volumeId The volume id.
     */
    void setVolumeId(String volumeId);
}
