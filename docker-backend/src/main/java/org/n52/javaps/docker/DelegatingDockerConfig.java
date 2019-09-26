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

import java.time.Duration;
import java.util.Objects;
import java.util.Optional;

/**
 * {@link DockerConfig} that delegates to another {@link DockerConfig}.
 *
 * @author Christian Autermann
 */
public class DelegatingDockerConfig implements DockerConfig {
    private final DockerConfig delegate;

    /**
     * Create a new {@link DelegatingDockerConfig}.
     *
     * @param delegate The {@link DockerConfig} to delegate to.
     */
    public DelegatingDockerConfig(DockerConfig delegate) {
        this.delegate = Objects.requireNonNull(delegate);
    }

    /**
     * Get the delegate.
     *
     * @return The {@link DockerConfig} to delegate to.
     */
    protected DockerConfig getDelegate() {
        return delegate;
    }

    @Override
    public Optional<String> getGroup() {
        return delegate.getGroup();
    }

    @Override
    public Optional<String> getUser() {
        return delegate.getUser();
    }

    @Override
    public Optional<Duration> getProcessTimeout() {
        return delegate.getProcessTimeout();
    }

    @Override
    public Optional<Duration> getStopTimeout() {
        return delegate.getStopTimeout();
    }

    @Override
    public String getInputPath() {
        return delegate.getInputPath();
    }

    @Override
    public String getInputPath(String file) {
        return delegate.getInputPath(file);
    }

    @Override
    public String getOutputPath() {
        return delegate.getOutputPath();
    }

    @Override
    public String getOutputPath(String file) {
        return delegate.getOutputPath(file);
    }

    @Override
    public String getDataPath() {
        return delegate.getDataPath();
    }

    @Override
    public Environment getGlobalEnvironment() {
        return delegate.getGlobalEnvironment();
    }

    @Override
    public String getDockerHost() {
        return delegate.getDockerHost();
    }

    @Override
    public String getJavaPsVersion() {
        return delegate.getJavaPsVersion();
    }
}
