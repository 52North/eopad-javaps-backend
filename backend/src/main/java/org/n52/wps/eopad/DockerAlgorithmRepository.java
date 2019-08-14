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

package org.n52.wps.eopad;

import org.n52.faroe.annotation.Configurable;
import org.n52.faroe.annotation.Setting;
import org.n52.javaps.algorithm.AlgorithmRepository;
import org.n52.javaps.algorithm.IAlgorithm;
import org.n52.javaps.description.TypedProcessDescription;
import org.n52.shetland.ogc.ows.OwsCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;

import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author <a href="mailto:m.rieke@52north.org">Matthes Rieke</a>
 */
@Configurable
public class DockerAlgorithmRepository implements AlgorithmRepository, InitializingBean {

    private static final Logger LOG = LoggerFactory.getLogger(DockerAlgorithmRepository.class);

    @Autowired
    private DockerProcessRegistry processRegistry;

    private String scihubUsername;
    private String scihubPassword;
    private String dockerDataDirectory;

    @Setting("scihub.username")
    public void setScihubUsername(String scihubUsername) {
        this.scihubUsername = scihubUsername;
    }

    @Setting("scihub.password")
    public void setScihubPassword(String scihubPassword) {
        this.scihubPassword = scihubPassword;
    }

    @Setting("docker.dataDirectory")
    public void setDockerDataDirectory(String dockerDataDirectory) {
        this.dockerDataDirectory = dockerDataDirectory;
        LOG.info("using docker data directory {}", this.dockerDataDirectory);
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        LOG.info("DockerAlgorithmRepository initialised");
    }

    @Override
    public Set<OwsCode> getAlgorithmNames() {
        List<ProcessImage> images = this.processRegistry.resolveProcessImages();
        return images.stream()
                     .map(ProcessImage::getIdentifier)
                     .map(OwsCode::new)
                     .collect(Collectors.toSet());
    }

    @Override
    public Optional<IAlgorithm> getAlgorithm(OwsCode id) {
        LOG.trace("resolving algorithm for id {}", id.getValue());
        List<ProcessImage> images = this.processRegistry.resolveProcessImages();
        return images.stream()
                     .filter(i -> id.getValue().equals(i.getIdentifier()))
                     .map(this::createDockerAlgorithm)
                     .findFirst();
    }

    @Override
    public Optional<TypedProcessDescription> getProcessDescription(OwsCode id) {
        return this.getAlgorithm(id).map(IAlgorithm::getDescription);
    }

    @Override
    public boolean containsAlgorithm(OwsCode id) {
        return this.getAlgorithm(id).isPresent();
    }

    private IAlgorithm createDockerAlgorithm(ProcessImage i) {
        // TODO currently hardwired!!
        if (i.getName().equals("docker.52north.org/eopad/ndvi")) {
            return new NdviDockerAlgorithm(this.processRegistry.getDocker(), i, Paths.get(this.dockerDataDirectory), scihubUsername, scihubPassword);
        }

        return null;
    }

}
