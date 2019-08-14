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
import org.n52.javaps.algorithm.AbstractAlgorithm;
import org.n52.javaps.algorithm.ExecutionException;
import org.n52.javaps.description.TypedProcessDescription;
import org.n52.javaps.description.impl.TypedProcessDescriptionImpl;
import org.n52.javaps.engine.ProcessExecutionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author <a href="mailto:m.rieke@52north.org">Matthes Rieke</a>
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
