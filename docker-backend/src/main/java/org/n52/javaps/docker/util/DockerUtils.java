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

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.exception.DockerClientException;
import com.github.dockerjava.api.exception.DockerException;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.n52.javaps.docker.process.DockerJobConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DockerUtils {
    private static final Logger LOG = LoggerFactory.getLogger(DockerUtils.class);
    private static final Pattern DOCKER_CGROUP_REGEX = Pattern.compile("^[0-9]+:[^:]*:/docker/([0-9a-z]{64})$");
    private static final InputStream EMPTY_INPUT_STREAM = new InputStream() {
        @Override
        public int read() { return -1; }
    };

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

    public static InputStream readFile(DockerClient client, String containerId, String path) throws IOException {
        InputStream inputStream = client.copyArchiveFromContainerCmd(containerId, path).exec();
        TarArchiveInputStream tarArchiveInputStream = new TarArchiveInputStream(inputStream);
        try {
            if (tarArchiveInputStream.getNextTarEntry() == null) {
                tarArchiveInputStream.close();
                return EMPTY_INPUT_STREAM;
            }
        } catch (IOException e) {
            tarArchiveInputStream.close();
            throw e;
        } catch (DockerClientException | DockerException e) {
            throw new IOException(e);
        }
        return tarArchiveInputStream;
    }

    public static InputStream readFile(DockerJobConfig config, String path) throws IOException {
        return readFile(config.client(), config.getProcessContainerId(), path);
    }

    public static void removeContainer(DockerJobConfig config, String containerId) {
        if (containerId != null) {
            try {
                config.client().removeContainerCmd(containerId).withForce(true).exec();
            } catch (DockerException ex) {
                config.log().error("unable to remove container " + containerId, ex);
            }
        }
    }

    public static void removeVolume(DockerJobConfig config, String volumeId) {
        if (volumeId != null) {
            try {
                config.client().removeVolumeCmd(volumeId).exec();
            } catch (DockerException ex) {
                config.log().error("unable to remove volume " + volumeId, ex);
            }
        }
    }

    public static void cleanup(DockerJobConfig config) {
        // remove the helper container
        removeContainer(config, config.getHelperContainerId());
        // remove the helper container
        removeContainer(config, config.getProcessContainerId());
        // remove the data volume
        removeVolume(config, config.getVolumeId());
    }
}
