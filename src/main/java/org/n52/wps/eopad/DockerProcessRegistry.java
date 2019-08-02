package org.n52.wps.eopad;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.InspectImageResponse;
import com.github.dockerjava.api.command.ListImagesCmd;
import com.github.dockerjava.api.model.Image;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.DockerClientConfig;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.n52.faroe.annotation.Configurable;
import org.n52.faroe.annotation.Setting;
import org.n52.javaps.description.impl.TypedComplexOutputDescriptionImpl;
import org.n52.javaps.description.impl.TypedLiteralInputDescriptionImpl;
import org.n52.javaps.description.impl.TypedLiteralOutputDescriptionImpl;
import org.n52.javaps.io.data.binding.complex.GenericFileDataBinding;
import org.n52.javaps.io.data.binding.complex.GeotiffBinding;
import org.n52.javaps.io.literal.xsd.LiteralStringType;
import org.n52.shetland.ogc.ows.OwsAnyValue;
import org.n52.shetland.ogc.ows.OwsCode;
import org.n52.shetland.ogc.ows.OwsDomainMetadata;
import org.n52.shetland.ogc.ows.OwsLanguageString;
import org.n52.shetland.ogc.ows.OwsValue;
import org.n52.shetland.ogc.wps.Format;
import org.n52.shetland.ogc.wps.InputOccurence;
import org.n52.shetland.ogc.wps.description.LiteralDataDomain;
import org.n52.shetland.ogc.wps.description.ProcessInputDescription;
import org.n52.shetland.ogc.wps.description.ProcessOutputDescription;
import org.n52.shetland.ogc.wps.description.impl.ComplexOutputDescriptionImpl;
import org.n52.shetland.ogc.wps.description.impl.LiteralDataDomainImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

/**
 *
 * @author Matthes Rieke <m.rieke@52north.org>
 */
@Configurable
public class DockerProcessRegistry implements InitializingBean, DisposableBean {

    private static final Logger LOG = LoggerFactory.getLogger(DockerProcessRegistry.class);

    private DockerClient docker;
    private String dockerHost;
    private List<ProcessImage> availableImages;

    public String getDockerHost() {
        return dockerHost;
    }

    @Setting("docker.host")
    public void setDockerHost(String dockerHost) {
        this.dockerHost = dockerHost;
        LOG.info("using docker host {}", this.dockerHost);
    }

    public DockerClient getDocker() {
        return docker;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        DockerClientConfig config = DefaultDockerClientConfig.createDefaultConfigBuilder()
                .withDockerHost(Optional.ofNullable(this.dockerHost).orElse("unix:///var/run/docker.sock"))
                .build();
        this.docker = DockerClientBuilder.getInstance(config).build();

        // resolve the images
        this.availableImages = this.resolveProcessImages();

        LOG.info("DockerProcessRegistry initialized. Supported images: {}", this.availableImages);
    }

    @Override
    public void destroy() throws Exception {
        if (this.docker != null) {
            this.docker.close();
        }
    }

    protected List<ProcessImage> resolveProcessImages() {
        ListImagesCmd cmd = this.docker.listImagesCmd()
                .withLabelFilter(Collections.singletonMap("javaps.docker.version", "1.4.0"));
        return cmd.exec().stream()
                .map(i -> createProcessImage(i))
                .filter(i -> i != null)
                .filter(i -> !i.getName().equals("<none>"))
                .collect(Collectors.toList());
    }

    private ProcessImage createProcessImage(Image i) {
        ProcessImage pi = new ProcessImage();

        if (i.getRepoTags().length > 0) {
            String[] tag = i.getRepoTags()[0].split("\\:");
            pi.setName(tag[0]);
            pi.setTag(tag[1]);

            // resolve more labels
            InspectImageResponse inspectResp = this.docker.inspectImageCmd(i.getId()).exec();

            if (inspectResp.getConfig() != null) {
                Map<String, String> labels = inspectResp.getConfig().getLabels();

                pi.setIdentifier(labels.get("javaps.docker.process.identifier"));
                pi.setTitle(labels.get("javaps.docker.process.title"));
                pi.setAbstrakt(labels.get("javaps.docker.process.abstract"));
            }

            // hard-wired for the moment!!
            if ("org.n52.eopad.ndvi".equals(pi.getIdentifier())) {
                pi.setInputs(createNdviInputs());
                pi.setOutputs(createNdviOutputs());
            }

            return pi;
        }

        return null;
    }

    private List<ProcessInputDescription> createNdviInputs() {
        List<ProcessInputDescription> result = new ArrayList<>();

        LiteralDataDomain ldd = new LiteralDataDomainImpl(OwsAnyValue.instance(),
                new OwsDomainMetadata("plain"),
                new OwsDomainMetadata("text"),
                new OwsValue(""));

        TypedLiteralInputDescriptionImpl pid1 = new TypedLiteralInputDescriptionImpl(new OwsCode("INPUT_SOURCE"),
                new OwsLanguageString("Sentinel-2 Filename"),
                new OwsLanguageString("Sentinel-2 full Filename in the format of MMM_MSIL1C_YYYYMMDDHHMMSS_Nxxyy_ROOO_Txxxxx_<Product Discriminator>"),
                Collections.emptySet(),
                Collections.emptySet(),
                new InputOccurence(BigInteger.ONE, BigInteger.ONE),
                ldd,
                Collections.singleton(ldd),
                new LiteralStringType());
        result.add(pid1);
        
        return result;
    }

    private List<ProcessOutputDescription> createNdviOutputs() {
        List<ProcessOutputDescription> result = new ArrayList<>();

        TypedComplexOutputDescriptionImpl od = new TypedComplexOutputDescriptionImpl(new OwsCode("OUTPUT_RASTER"),
                new OwsLanguageString("the resulting NDVI raster image"),
                new OwsLanguageString("the resulting NDVI raster image"),
                Collections.emptySet(),
                Collections.emptySet(),
                new Format("image/tiff"),
                Collections.singleton(new Format("image/tiff")),
                BigInteger.valueOf(2048),
                GenericFileDataBinding.class
        );
        result.add(od);
        
        return result;
    }

}
