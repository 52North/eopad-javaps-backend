
package org.n52.wps.eopad;

import com.github.dockerjava.api.DockerClient;
import org.n52.javaps.algorithm.AbstractAlgorithm;
import org.n52.javaps.algorithm.ExecutionException;
import org.n52.javaps.description.TypedProcessDescription;
import org.n52.javaps.description.impl.TypedProcessDescriptionImpl;
import org.n52.javaps.engine.ProcessExecutionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Matthes Rieke <m.rieke@52north.org>
 */
public class DockerAlgorithm extends AbstractAlgorithm {

    private static final Logger LOG = LoggerFactory.getLogger(DockerAlgorithm.class);

    protected final DockerClient docker;
    protected final ProcessImage image;

    public DockerAlgorithm(DockerClient docker, ProcessImage image) {
        this.docker = docker;
        this.image = image;
    }

    @Override
    protected TypedProcessDescription createDescription() {
        return new TypedProcessDescriptionImpl.Builder()
                       .withIdentifier(this.image.getIdentifier())
                       .withTitle(this.image.getTitle())
                       .withAbstract(this.image.getAbstrakt())
                       .withInput(this.image.getInputs())
                       .withOutput(this.image.getOutputs())
                       .withVersion("1.0.0")
                       .build();
    }

    @Override
    public void execute(ProcessExecutionContext context) throws ExecutionException {
    }

}