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

import org.n52.javaps.description.TypedProcessDescription;
import org.n52.javaps.docker.DockerConfig;
import org.n52.javaps.docker.Environment;
import org.n52.javaps.engine.ProcessExecutionContext;
import org.slf4j.Logger;

public interface DockerJobConfig extends DockerConfig {
    Logger getLog();

    TypedProcessDescription getDescription();

    ProcessExecutionContext getContext();

    Environment getEnvironment();

    String getHelperContainerId();

    void setHelperContainerId(String helperContainerId);

    String getProcessContainerId();

    void setProcessContainerId(String processContainerId);

    String getVolumeId();

    void setVolumeId(String volumeId);
}
