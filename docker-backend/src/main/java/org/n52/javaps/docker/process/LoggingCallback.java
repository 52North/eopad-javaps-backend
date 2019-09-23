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

/**
 * Copy of {@link com.github.dockerjava.core.command.LogContainerResultCallback} with an customizable logger.
 */
public class LoggingCallback extends ResultCallbackTemplate<LoggingCallback, Frame> {
    private final Logger log;

    public LoggingCallback(Logger parent, String containerId) {
        this.log = LoggerFactory.getLogger(String.format("%s.%s", parent.getName(), containerId));
    }

    @Override
    public void onNext(Frame item) {
        log.info("{}", item.toString());
    }
}
