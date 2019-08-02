
package org.n52.wps.eopad;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.command.TopContainerResponse;
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.Frame;
import com.github.dockerjava.api.model.Volume;
import com.github.dockerjava.core.command.LogContainerResultCallback;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.n52.faroe.annotation.Configurable;
import org.n52.faroe.annotation.Setting;
import org.n52.javaps.algorithm.ExecutionException;
import org.n52.javaps.engine.ProcessExecutionContext;
import org.n52.javaps.io.literal.LiteralData;
import org.n52.javaps.settings.SettingsConstants;
import org.n52.shetland.ogc.ows.OwsCode;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Matthes Rieke <m.rieke@52north.org>
 */
public class NdviDockerAlgorithm extends DockerAlgorithm {
    
    private static final org.slf4j.Logger LOG = LoggerFactory.getLogger(NdviDockerAlgorithm.class);
    private final Path storageLocation;
    private String scihubUsername;
    private String scihubPassword;

    public NdviDockerAlgorithm(DockerClient docker, ProcessImage image, Path storageLocation, String scihubUsername, String scihubPassword) {
        super(docker, image);
        this.storageLocation = storageLocation;
        this.scihubUsername = scihubUsername;
        this.scihubPassword = scihubPassword;
    }
    
    @Override
    public void execute(ProcessExecutionContext context) throws ExecutionException {
        LOG.info("Executing {} with Input {}", context.getJobId(), context.getInputs().keySet()
                .stream()
                .map(k -> String.format("%s=%s", k, context.getInputs().get(k)))
                .collect(Collectors.joining("; ")));
        
        if (!Files.exists(storageLocation)) {
            try {
                Files.createDirectories(storageLocation);
            } catch (IOException ex) {
                LOG.warn("Could not create storageDirectory {}", storageLocation);
                throw new ExecutionException(ex.getMessage(), ex);
            }
        }
        
        Volume volume1 = new Volume("/data/outputs/");

        String targetFileName = UUID.randomUUID().toString() + ".tiff";
        
        CreateContainerCmd create = this.docker.createContainerCmd(this.image.getName())
                  .withVolumes(volume1)
                  .withBinds(new Bind(this.storageLocation.toFile().getAbsolutePath(), volume1))
                .withEnv("SCIHUB_USERNAME=" + this.scihubUsername,
                        "SCIHUB_PASSWORD=" + this.scihubPassword,
                        "OUTPUT_RASTER=/data/outputs/" + targetFileName,
                        "INPUT_SOURCE=" + context.getInputs().get(new OwsCode("INPUT_SOURCE"))
                        .get(0).getPayload());
        
        CreateContainerResponse createResp = create.exec();
                
        try {
            this.docker.startContainerCmd(createResp.getId()).exec();
            
            LogContainerResultCallback loggingCallback = new LogContainerResultCallback() {
                    @Override
                    public void onNext(Frame item) {
                        LOG.info("[DOCKER] {}", item.toString());
                    }
                };

            // this essentially test the since=0 case
            this.docker.logContainerCmd(createResp.getId())
                .withStdErr(true)
                .withStdOut(true)
                .withFollowStream(true)
                .withTailAll()
                .exec(loggingCallback);

            loggingCallback.awaitCompletion(30, TimeUnit.MINUTES);
            
            // on rare occassions this could happen before the container finished
            // lets wait five more minutes
            long waitStart = System.currentTimeMillis();
            while ((System.currentTimeMillis() - waitStart) < (1000 * 60 * 5)) {
                InspectContainerResponse inspectResp = this.docker.inspectContainerCmd(createResp.getId()).exec();
                if (!inspectResp.getState().getRunning()) {
                    break;
                }
                
                LOG.info("Container still running, lost log listener. Waiting to finish");
                Thread.sleep(10000);
            }
            
            Path targetPath = storageLocation.resolve(targetFileName);
            if (Files.exists(targetPath)) {
                context.getOutputs().put(new OwsCode("OUTPUT_RASTER"),
                        new LiteralData(targetPath.toFile().getAbsolutePath()));
            } else {
                throw new ExecutionException("Output file not available");
            }
        } catch (InterruptedException ex) {
            LOG.warn("Error on executing docker container: " + ex.getMessage());
            LOG.debug(ex.getMessage(), ex);
            throw new ExecutionException(ex.getMessage(), ex);
        } finally {
//            this.docker.removeContainerCmd(createResp.getId()).exec();
            LOG.info("removed temporary container");
        }
        
    }

}
