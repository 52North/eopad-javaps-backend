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

import org.n52.janmayen.lifecycle.Destroyable;
import org.n52.javaps.docker.process.DockerJobConfig;
import org.n52.javaps.docker.util.DockerUtils;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

public class Closer implements Destroyable {
    private final AtomicInteger count = new AtomicInteger(0);
    private final DockerJobConfig config;

    public Closer(DockerJobConfig config) {
        this.config = Objects.requireNonNull(config);
    }

    public Closer increment() {
        count.incrementAndGet();
        return this;
    }

    public boolean isCleanupDeferred() {
        return count.get() > 0;
    }

    @Override
    public void destroy() {
        if (count.decrementAndGet() == 0) {
            DockerUtils.cleanup(config);
        }
    }
}
