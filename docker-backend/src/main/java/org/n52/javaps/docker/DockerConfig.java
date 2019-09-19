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
package org.n52.javaps.docker;

import java.time.Duration;
import java.util.Optional;

public interface DockerConfig {

    Optional<String> getGroup();

    Optional<String> getUser();

    Optional<Duration> getTimeout();

    String getInputPath();

    String getInputPath(String file);

    String getOutputPath();

    String getOutputPath(String file);

    String getDataPath();

    Environment getGlobalEnvironment();

    String getDockerHost();

    String getJavaPsVersion();
}
