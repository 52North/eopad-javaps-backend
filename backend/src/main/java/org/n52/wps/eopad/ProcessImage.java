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

import org.n52.shetland.ogc.wps.description.ProcessInputDescription;
import org.n52.shetland.ogc.wps.description.ProcessOutputDescription;

import java.util.List;

/**
 * @author <a href="mailto:m.rieke@52north.org">Matthes Rieke</a>
 */
public class ProcessImage {

    private String name;
    private String tag;
    private String identifier;
    private String title;
    private String abstrakt;
    private List<ProcessInputDescription> inputs;
    private List<ProcessOutputDescription> outputs;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getAbstrakt() {
        return abstrakt;
    }

    public void setAbstrakt(String abstrakt) {
        this.abstrakt = abstrakt;
    }

    public List<ProcessInputDescription> getInputs() {
        return inputs;
    }

    public void setInputs(List<ProcessInputDescription> inputs) {
        this.inputs = inputs;
    }

    public List<ProcessOutputDescription> getOutputs() {
        return outputs;
    }

    public void setOutputs(List<ProcessOutputDescription> outputs) {
        this.outputs = outputs;
    }

}
