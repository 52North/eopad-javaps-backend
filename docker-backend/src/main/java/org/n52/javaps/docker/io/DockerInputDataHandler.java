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
package org.n52.javaps.docker.io;

import com.google.common.io.ByteStreams;
import org.n52.javaps.description.TypedProcessInputDescription;
import org.n52.javaps.io.Data;
import org.n52.javaps.io.InputHandler;
import org.n52.shetland.ogc.wps.Format;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.Set;

/**
 * {@link InputHandler} for {@link DockerInputData}.
 *
 * @author Christian Autermann
 */
@Component
public class DockerInputDataHandler implements InputHandler {
    private static final Format ANY_FORMAT = new Format("*/*");

    @Override
    public Data<?> parse(TypedProcessInputDescription<?> description, InputStream input, Format format)
            throws IOException {
        return new DockerInputData(ByteStreams.toByteArray(input), format);
    }

    @Override
    public Set<Format> getSupportedFormats() {
        return Collections.singleton(ANY_FORMAT);
    }

    @Override
    public Set<Class<? extends Data<?>>> getSupportedBindings() {
        return Collections.singleton(DockerInputData.class);
    }

}
