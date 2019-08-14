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

import java.util.List;
import java.util.Optional;
import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.Test;
import org.n52.shetland.ogc.wps.description.ProcessInputDescription;
import org.n52.shetland.ogc.wps.description.ProcessOutputDescription;

/**
 *
 * @author <a href="mailto:m.rieke@52north.org">Matthes Rieke</a>
 */
public class DockerProcessRegistryIT {

    @Test
    public void testImageDiscovery() throws Exception {
        DockerProcessRegistry registry = new DockerProcessRegistry();
        registry.setDockerHost("unix:///var/run/docker.sock");
        registry.afterPropertiesSet();
        List<ProcessImage> images = registry.resolveProcessImages();

        Optional<ProcessImage> candidate = images.stream()
                .filter(i -> "docker.52north.org/eopad/ndvi".equals(i.getName()) &&
                        "latest".equals(i.getTag())).findFirst();

        Assert.assertTrue(candidate.isPresent());
        ProcessImage pi = candidate.get();

        Assert.assertThat(pi.getIdentifier(), CoreMatchers.equalTo("org.n52.eopad.ndvi"));
        Assert.assertThat(pi.getTitle(), CoreMatchers.equalTo("NDVI for Sentinel-2 Scenes"));
        Assert.assertThat(pi.getAbstrakt(), CoreMatchers.equalTo("NDVI for Sentinel-2 Scenes using the SNAP Toolbox NDVI process"));

        List<ProcessInputDescription> inputs = pi.getInputs();
        Assert.assertTrue(inputs.stream().filter(i -> i.getId().getValue().equals("INPUT_SOURCE")).findAny().isPresent());

        List<ProcessOutputDescription> outputs = pi.getOutputs();
        Assert.assertTrue(outputs.stream().filter(o -> o.getId().getValue().equals("OUTPUT_RASTER")).findAny().isPresent());
    }

}
