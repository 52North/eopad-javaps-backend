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
package org.n52.javaps.docker.process;

import org.n52.javaps.description.TypedProcessOutputDescription;
import org.n52.javaps.description.TypedProcessOutputDescriptionContainer;
import org.n52.javaps.docker.Environment;
import org.n52.shetland.ogc.wps.OutputDefinition;

import java.util.ArrayList;
import java.util.List;

public class DockerOutputDefinitionProcessor
        extends AbstractDockerProcessor<List<OutputDefinition>, List<DockerOutputInfo>> {
    DockerOutputDefinitionProcessor(DockerJobConfig config) {
        super(config);
    }

    @Override
    public List<DockerOutputInfo> process(List<OutputDefinition> outputDefinitions) {
        return createOutputs(description(), getJobEnvironment().withPrefix(Environment.OUTPUT), outputDefinitions);
    }

    private List<DockerOutputInfo> createOutputs(TypedProcessOutputDescriptionContainer description,
                                                 Environment environment, List<OutputDefinition> outputDefinitions) {
        List<DockerOutputInfo> outputInfos = new ArrayList<>(outputDefinitions.size());
        for (OutputDefinition outputDefinition : outputDefinitions) {
            TypedProcessOutputDescription<?> outputDescription = description.getOutput(outputDefinition.getId());

            Environment e = environment.withPrefix(outputDefinition.getId());
            if (outputDescription.isGroup()) {
                List<DockerOutputInfo> outputs = createOutputs(outputDescription.asGroup(), e,
                                                               outputDefinition.getOutputs());
                outputInfos.add(new DockerOutputInfo(outputDefinition, outputDescription, outputs));
            } else {
                String outputPath = getOutputPath(e.getPrefix());
                e.put(outputPath);

                outputInfos.add(new DockerOutputInfo(outputDefinition, outputDescription, outputPath));

                if (outputDescription.isComplex()) {
                    createFormat(e, outputDefinition.getFormat());
                }
            }
        }
        return outputInfos;
    }
}
