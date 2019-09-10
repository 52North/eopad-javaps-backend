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

import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.DockerClientConfig;
import org.n52.faroe.annotation.Configurable;
import org.n52.faroe.annotation.Setting;
import org.n52.janmayen.function.Predicates;
import org.n52.javaps.algorithm.IAlgorithm;
import org.n52.javaps.description.TypedProcessDescription;
import org.n52.javaps.docker.process.DockerAlgorithm;
import org.n52.javaps.transactional.AbstractTransactionalAlgorithmRepository;
import org.n52.shetland.ogc.wps.ap.ApplicationPackage;
import org.n52.shetland.ogc.wps.ap.DockerExecutionUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.io.IOException;
import java.util.Optional;

/**
 * @author <a href="mailto:m.rieke@52north.org">Matthes Rieke</a>
 * @author <a href="mailto:c.autermann@52north.org">Christian Autermann</a>
 */
@Configurable
@Repository
public class DockerAlgorithmRepository extends AbstractTransactionalAlgorithmRepository {
    private static final Logger LOG = LoggerFactory.getLogger(DockerAlgorithmRepository.class);
    private static final String DEFAULT_DOCKER_HOST = "unix:///var/run/docker.sock";
    private String dockerHost;
    private TypedDescriptionBuilder descriptionBuilder;
    private DockerConfigImpl dockerConfig;

    public DockerAlgorithmRepository() {
        this.dockerConfig = new DockerConfigImpl();
    }

    @Setting("docker.host")
    public void setDockerHost(String dockerHost) {
        this.dockerHost = dockerHost;
        LOG.info("using docker host {}", this.dockerHost);
    }

    @Autowired
    public void setDescriptionBuilder(TypedDescriptionBuilder descriptionBuilder) {
        this.descriptionBuilder = descriptionBuilder;
    }

    @Override
    public void init() {
        DockerClientConfig config = DefaultDockerClientConfig
                                            .createDefaultConfigBuilder()
                                            .withDockerHost(Optional.ofNullable(dockerHost)
                                                                    .filter(Predicates.not(String::isEmpty))
                                                                    .orElse(DEFAULT_DOCKER_HOST))
                                            .build();
        dockerConfig.setClient(DockerClientBuilder.getInstance(config).build());
        LOG.info("DockerProcessRegistry initialized.");
    }

    @Override
    public boolean isSupported(ApplicationPackage applicationPackage) {
        return applicationPackage.getExecutionUnit() instanceof DockerExecutionUnit;
    }

    @Override
    public void destroy() {
        if (dockerConfig != null && dockerConfig.getClient() != null) {
            try {
                dockerConfig.getClient().close();
            } catch (IOException e) {
                LOG.error("error closing docker client", e);
            }
        }
    }

    @Override
    protected TypedProcessDescription createProcessDescription(ApplicationPackage applicationPackage) {
        return descriptionBuilder.createDescription(applicationPackage.getProcessDescription().getProcessDescription());
    }

    @Override
    protected IAlgorithm createAlgorithm(ApplicationPackage applicationPackage) {
        return new DockerAlgorithm(dockerConfig, descriptionBuilder, applicationPackage);

    }
}
