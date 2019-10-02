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

import com.github.dockerjava.api.model.Frame;
import com.github.dockerjava.core.async.ResultCallbackTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedList;
import java.util.List;

/**
 * Copy of {@link com.github.dockerjava.core.command.LogContainerResultCallback} with an customizable logger.
 *
 * @author Christian Autermann
 */
public class LoggingCallback extends ResultCallbackTemplate<LoggingCallback, Frame> {
    private final Logger log;
    private final List<String> stderr = new LinkedList<>();
    private final List<String> stdout = new LinkedList<>();

    public LoggingCallback(Logger parent, String containerId) {
        this.log = LoggerFactory.getLogger(String.format("%s.%s", parent.getName(), containerId));
    }

    public String getErrorOutput() {
        return String.join("\n", stderr);
    }

    public String getStandardOutput() {
        return String.join("\n", stdout);
    }

    @Override
    public void onNext(Frame item) {
        String payload = new String(item.getPayload());
        switch (item.getStreamType()) {
            case STDERR:
                stderr.add(payload);
                log.warn("{}", payload);
                break;
            case STDOUT:
                stdout.add(payload);
                log.info("{}", payload);
                break;
        }

    }
}
