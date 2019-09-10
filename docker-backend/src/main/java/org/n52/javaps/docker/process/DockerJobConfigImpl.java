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
import org.n52.javaps.docker.Environment;
import org.n52.javaps.engine.ProcessExecutionContext;
import org.n52.javaps.docker.DelegatingDockerConfig;
import org.n52.javaps.docker.DockerConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

public class DockerJobConfigImpl extends DelegatingDockerConfig implements DockerJobConfig {
    private final Logger log;
    private final TypedProcessDescription description;
    private final ProcessExecutionContext context;
    private final Environment environment;
    private String helperContainerId;
    private String processContainerId;
    private String volumeId;

    public DockerJobConfigImpl(DockerConfig delegate,
                               TypedProcessDescription description,
                               ProcessExecutionContext context,
                               Environment environment) {
        super(delegate);
        this.description = Objects.requireNonNull(description);
        this.context = Objects.requireNonNull(context);
        this.environment = Objects.requireNonNull(environment);
        this.log = LoggerFactory.getLogger(String.format("%s.%s",
                                                         description.getId().getValue(),
                                                         context.getJobId()));
    }

    @Override
    public Logger getLog() {
        return log;
    }

    @Override
    public TypedProcessDescription getDescription() {
        return this.description;
    }

    @Override
    public ProcessExecutionContext getContext() {
        return this.context;
    }

    @Override
    public Environment getEnvironment() {
        return this.environment;
    }

    @Override
    public String getHelperContainerId() {
        return this.helperContainerId;
    }

    @Override
    public void setHelperContainerId(String helperContainerId) {
        this.helperContainerId = helperContainerId;
    }

    @Override
    public String getProcessContainerId() {
        return this.processContainerId;
    }

    @Override
    public void setProcessContainerId(String processContainerId) {
        this.processContainerId = processContainerId;
    }

    @Override
    public String getVolumeId() {
        return this.volumeId;
    }

    @Override
    public void setVolumeId(String volumeId) {
        this.volumeId = volumeId;
    }
}
