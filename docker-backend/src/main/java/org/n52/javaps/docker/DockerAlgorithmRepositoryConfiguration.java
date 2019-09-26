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
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.DockerClientConfig;
import org.n52.janmayen.function.Predicates;
import org.n52.javaps.description.impl.TypedProcessDescriptionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;

/**
 * Spring configuration for this submodule.
 *
 * @author Christian Autermann
 */
@Configuration
public class DockerAlgorithmRepositoryConfiguration {
    private static final String DEFAULT_DOCKER_HOST = "unix:///var/run/docker.sock";
    private Set<DockerEnvironmentConfigurer> configurers = Collections.emptySet();

    /**
     * Set the {@linkplain DockerEnvironmentConfigurer Docker environment configurers}.
     *
     * @param configurers The configurers.
     */
    @Autowired(required = false)
    public void setConfigurers(Set<DockerEnvironmentConfigurer> configurers) {
        this.configurers = Optional.ofNullable(configurers).orElseGet(Collections::emptySet);
    }

    /**
     * Create the {@link TypedProcessDescriptionFactory}.
     *
     * @return The {@link TypedProcessDescriptionFactory}.
     */
    @Bean
    @ConditionalOnMissingBean
    public TypedProcessDescriptionFactory typedProcessDescriptionFactory() {
        return TypedProcessDescriptionFactory.instance();
    }

    /**
     * Create the global {@link Environment} to apply to every processing container.
     *
     * @return The global environment.
     * @see DockerEnvironmentConfigurer
     */
    @Bean
    public Environment environment() {
        Environment environment = new Environment();
        configurers.forEach(configurer -> configurer.configure(environment));
        return environment;
    }

    /**
     * Create the {@link DockerClient} to use.
     *
     * @param dockerConfig The {@link DockerConfig}.
     * @return The {@link DockerClient}.
     */
    @Bean
    public DockerClient dockerClient(DockerConfig dockerConfig) {
        DockerClientConfig config = DefaultDockerClientConfig
                                            .createDefaultConfigBuilder()
                                            .withDockerHost(Optional.ofNullable(dockerConfig.getDockerHost())
                                                                    .filter(Predicates.not(String::isEmpty))
                                                                    .orElse(DEFAULT_DOCKER_HOST))
                                            .build();
        return DockerClientBuilder.getInstance(config).build();
    }

}
