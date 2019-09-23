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
package org.n52.javaps.docker.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DockerUtils {
    private static final Logger LOG = LoggerFactory.getLogger(DockerUtils.class);
    private static final Pattern DOCKER_CGROUP_REGEX = Pattern.compile("^[0-9]+:[^:]*:/docker/([0-9a-z]{64})$");

    /**
     * Checks if the current process runs inside Docker.
     *
     * @return The container id, if running inside Docker.
     */
    public static Optional<String> isRunningInsideDocker() {
        try {
            return Files.readAllLines(Paths.get("/proc/1/cgroup")).stream()
                        .map(DOCKER_CGROUP_REGEX::matcher)
                        .filter(Matcher::matches)
                        .map(m -> m.group(1))
                        .findFirst();
        } catch (IOException ex) {
            LOG.warn("Can not determine if running inside container", ex);
            return Optional.empty();
        }
    }
}
