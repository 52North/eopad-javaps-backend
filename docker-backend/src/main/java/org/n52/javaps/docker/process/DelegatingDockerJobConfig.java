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

import com.github.dockerjava.api.DockerClient;
import org.n52.javaps.description.TypedProcessDescription;
import org.n52.javaps.docker.DelegatingDockerConfig;
import org.n52.javaps.docker.Environment;
import org.n52.javaps.engine.ProcessExecutionContext;
import org.slf4j.Logger;

import java.util.Objects;

/**
 * {@link DockerJobConfig} that delegates to another {@link DockerJobConfig}.
 *
 * @author Christian Autermann
 */
public class DelegatingDockerJobConfig extends DelegatingDockerConfig implements DockerJobConfig {
    private DockerJobConfig delegate;

    /**
     * Create a new {@link DelegatingDockerJobConfig}.
     *
     * @param delegate The {@link DockerJobConfig} to delegate to.
     */
    public DelegatingDockerJobConfig(DockerJobConfig delegate) {
        super(delegate);
        this.delegate = Objects.requireNonNull(delegate);
    }

    /**
     * Get the delegate.
     *
     * @return The {@link DockerJobConfig} to delegate to.
     */
    @Override
    protected DockerJobConfig getDelegate() {
        return delegate;
    }

    @Override
    public Logger log() {
        return delegate.log();
    }

    @Override
    public DockerClient client() {
        return delegate.client();
    }

    @Override
    public TypedProcessDescription description() {
        return delegate.description();
    }

    @Override
    public ProcessExecutionContext context() {
        return delegate.context();
    }

    @Override
    public Environment getJobEnvironment() {
        return delegate.getJobEnvironment();
    }

    @Override
    public String getHelperContainerId() {
        return delegate.getHelperContainerId();
    }

    @Override
    public void setHelperContainerId(String helperContainerId) {
        delegate.setHelperContainerId(helperContainerId);
    }

    @Override
    public String getProcessContainerId() {
        return delegate.getProcessContainerId();
    }

    @Override
    public void setProcessContainerId(String processContainerId) {
        delegate.setProcessContainerId(processContainerId);
    }

    @Override
    public String getVolumeId() {
        return delegate.getVolumeId();
    }

    @Override
    public void setVolumeId(String volumeId) {
        delegate.setVolumeId(volumeId);
    }
}
