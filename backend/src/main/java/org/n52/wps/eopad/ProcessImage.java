
package org.n52.wps.eopad;

import org.n52.shetland.ogc.wps.description.ProcessInputDescription;
import org.n52.shetland.ogc.wps.description.ProcessOutputDescription;

import java.util.List;

/**
 * @author Matthes Rieke <m.rieke@52north.org>
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
