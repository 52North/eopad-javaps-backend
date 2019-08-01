
package org.n52.wps.eopad;

import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.n52.javaps.algorithm.AlgorithmRepository;
import org.n52.javaps.algorithm.IAlgorithm;
import org.n52.javaps.description.TypedProcessDescription;
import org.n52.shetland.ogc.ows.OwsCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;

/**
 *
 * @author Matthes Rieke <m.rieke@52north.org>
 */
public class DockerAlgorithmRepository implements AlgorithmRepository, InitializingBean {
    
    private static final Logger LOG = LoggerFactory.getLogger(DockerAlgorithmRepository.class);

    @Autowired
    private DockerProcessRegistry processRegistry;

    @Override
    public void afterPropertiesSet() throws Exception {
        LOG.info("DockerAlgorithmRepository initialised");
    }

    @Override
    public Set<OwsCode> getAlgorithmNames() {
        List<ProcessImage> images = this.processRegistry.resolveProcessImages();
        return images.stream()
                .map(i -> {
                    return new OwsCode(i.getName(), URI.create("https://52north.org/javaps/docker"));
                }).collect(Collectors.toSet());
    }

    @Override
    public Optional<IAlgorithm> getAlgorithm(OwsCode id) {
        LOG.info("resolving algorithm for id {}", id.getValue());
        List<ProcessImage> images = this.processRegistry.resolveProcessImages();
        return images.stream()
                .filter(i -> id.getValue().equals(i.getName()))
                .map(i -> {
                    return this.createDockerAlgorithm(i);
                }).findFirst();
    }

    @Override
    public Optional<TypedProcessDescription> getProcessDescription(OwsCode id) {
        Optional<IAlgorithm> algo = this.getAlgorithm(id);
        if (!algo.isPresent()) {
            return Optional.empty();
        }
        return Optional.of(algo.get().getDescription());
    }

    @Override
    public boolean containsAlgorithm(OwsCode id) {
        return this.getAlgorithm(id).isPresent();
    }

    private IAlgorithm createDockerAlgorithm(ProcessImage i) {
        // TODO currently hardwired!!
        if (i.getName().equals("docker.52north.org/eopad/ndvi")) {
            return new NdviDockerAlgorithm(this.processRegistry.getDocker(), i);
        }
        
        return null;
    }
    

}
