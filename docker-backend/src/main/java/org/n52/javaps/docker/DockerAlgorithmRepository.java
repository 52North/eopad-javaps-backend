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
import org.n52.javaps.algorithm.IAlgorithm;
import org.n52.javaps.description.TypedProcessDescription;
import org.n52.javaps.docker.process.DockerAlgorithm;
import org.n52.javaps.transactional.AbstractTransactionalAlgorithmRepository;
import org.n52.javaps.transactional.TransactionalAlgorithmRepository;
import org.n52.shetland.ogc.wps.ap.ApplicationPackage;
import org.n52.shetland.ogc.wps.ap.DockerExecutionUnit;
import org.n52.shetland.ogc.wps.ap.ExecutionUnit;
import org.n52.shetland.ogc.wps.description.ProcessDescription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.io.IOException;
import java.util.List;

/**
 * {@link TransactionalAlgorithmRepository} for Docker based algorithms.
 *
 * @author Matthes Rieke
 * @author Christian Autermann
 */
@Repository
public class DockerAlgorithmRepository extends AbstractTransactionalAlgorithmRepository {
    private static final Logger LOG = LoggerFactory.getLogger(DockerAlgorithmRepository.class);
    private final TypedDescriptionBuilder descriptionBuilder;
    private final DockerConfig dockerConfig;
    private final DockerClient dockerClient;

    /**
     * Creates a new {@link DockerAlgorithmRepository}.
     *
     * @param dockerConfig       The {@link DockerConfig}.
     * @param dockerClient       The {@link DockerClient}.
     * @param descriptionBuilder The {@link TypedDescriptionBuilder}.
     */
    @Autowired
    public DockerAlgorithmRepository(DockerConfig dockerConfig,
                                     DockerClient dockerClient,
                                     TypedDescriptionBuilder descriptionBuilder) {
        this.descriptionBuilder = descriptionBuilder;
        this.dockerConfig = dockerConfig;
        this.dockerClient = dockerClient;
    }

    @Override
    public boolean isSupported(ApplicationPackage applicationPackage) {
        List<ExecutionUnit> executionUnits = applicationPackage.getExecutionUnits();
        return executionUnits.size() == 1 && executionUnits.iterator().next() instanceof DockerExecutionUnit;
    }

    @Override
    public void destroy() {
        if (dockerClient != null) {
            try {
                dockerClient.close();
            } catch (IOException e) {
                LOG.error("error closing docker client", e);
            }
        }
    }

    @Override
    protected TypedProcessDescription createProcessDescription(ApplicationPackage applicationPackage) {
        ProcessDescription description = applicationPackage.getProcessDescription().getProcessDescription();
        return descriptionBuilder.createDescription(description);
    }

    @Override
    protected IAlgorithm createAlgorithm(ApplicationPackage applicationPackage) {
        return new DockerAlgorithm(dockerConfig, dockerClient, descriptionBuilder, applicationPackage);

    }
}
