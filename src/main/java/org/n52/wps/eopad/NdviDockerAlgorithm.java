
package org.n52.wps.eopad;

import com.github.dockerjava.api.DockerClient;
import java.time.OffsetDateTime;
import org.n52.javaps.algorithm.ExecutionException;
import org.n52.javaps.engine.ProcessExecutionContext;

/**
 *
 * @author Matthes Rieke <m.rieke@52north.org>
 */
public class NdviDockerAlgorithm extends DockerAlgorithm {

    public NdviDockerAlgorithm(DockerClient docker, ProcessImage image) {
        super(docker, image);
    }

    @Override
    public void execute(ProcessExecutionContext context) throws ExecutionException {
        context.setEstimatedCompletion(OffsetDateTime.now().plusMinutes(5));
        context.setPercentCompleted(new Short("2"));
    }
    

}
