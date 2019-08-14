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

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.InspectImageResponse;
import com.github.dockerjava.api.command.ListImagesCmd;
import com.github.dockerjava.api.model.Image;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.DockerClientConfig;
import org.n52.faroe.annotation.Configurable;
import org.n52.faroe.annotation.Setting;
import org.n52.javaps.description.impl.TypedComplexOutputDescriptionImpl;
import org.n52.javaps.description.impl.TypedLiteralInputDescriptionImpl;
import org.n52.javaps.io.data.binding.complex.GenericFileDataBinding;
import org.n52.javaps.io.literal.xsd.LiteralStringType;
import org.n52.shetland.ogc.ows.OwsAnyValue;
import org.n52.shetland.ogc.wps.Format;
import org.n52.shetland.ogc.wps.InputOccurence;
import org.n52.shetland.ogc.wps.description.LiteralDataDomain;
import org.n52.shetland.ogc.wps.description.ProcessInputDescription;
import org.n52.shetland.ogc.wps.description.ProcessOutputDescription;
import org.n52.shetland.ogc.wps.description.impl.LiteralDataDomainImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

import java.math.BigInteger;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * @author <a href="mailto:m.rieke@52north.org">Matthes Rieke</a>
 */
@Configurable
public class DockerProcessRegistry implements InitializingBean, DisposableBean {

    private static final Logger LOG = LoggerFactory.getLogger(DockerProcessRegistry.class);
    private static final InputOccurence ONE = new InputOccurence(BigInteger.ONE, BigInteger.ONE);
    private static final Format IMAGE_TIFF = new Format("image/tiff");

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
        DockerClientConfig config = DefaultDockerClientConfig
                                            .createDefaultConfigBuilder()
                                            .withDockerHost(Optional.ofNullable(this.dockerHost)
                                                                    .orElse("unix:///var/run/docker.sock"))
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

    List<ProcessImage> resolveProcessImages() {
        ListImagesCmd cmd = this.docker.listImagesCmd()
                                       .withLabelFilter(Collections.singletonMap("javaps.docker.version", "1.4.0"));
        return cmd.exec().stream()
                  .map(this::createProcessImage)
                  .filter(Objects::nonNull)
                  .filter(i -> !i.getName().equals("<none>"))
                  .collect(Collectors.toList());
    }

    private ProcessImage createProcessImage(Image i) {
        ProcessImage pi = new ProcessImage();

        if (i.getRepoTags().length > 0) {
            String[] tag = i.getRepoTags()[0].split(":");
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
        LiteralDataDomain ldd = new LiteralDataDomainImpl.Builder()
                                        .withValueDescription(OwsAnyValue.instance())
                                        .withDataType("plain")
                                        .withUOM("text")
                                        .build();
        return Collections.singletonList(
                new TypedLiteralInputDescriptionImpl.Builder()
                        .withIdentifier("INPUT_SOURCE")
                        .withTitle("Sentinel-2 Filename")
                        .withAbstract("Sentinel-2 full Filename in the format of MMM_MSIL1C_YYYYMMDDHHMMSS_Nxxyy_ROOO_Txxxxx_<Product Discriminator>")
                        .withOccurence(ONE)
                        .withDefaultLiteralDataDomain(ldd)
                        .withSupportedLiteralDataDomain(ldd)
                        .withType(new LiteralStringType())
                        .build());
    }

    private List<ProcessOutputDescription> createNdviOutputs() {
        return Collections.singletonList(new TypedComplexOutputDescriptionImpl.Builder()
                                                 .withIdentifier("OUTPUT_RASTER")
                                                 .withTitle("the resulting NDVI raster image")
                                                 .withAbstract("the resulting NDVI raster image")
                                                 .withDefaultFormat(IMAGE_TIFF)
                                                 .withSupportedFormat(IMAGE_TIFF)
                                                 .withMaximumMegabytes(2048)
                                                 .withType(GenericFileDataBinding.class)
                                                 .build());
    }

}
