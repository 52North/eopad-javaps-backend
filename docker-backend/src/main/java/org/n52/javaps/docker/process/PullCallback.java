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

import com.github.dockerjava.api.exception.DockerClientException;
import com.github.dockerjava.api.model.PullResponseItem;
import com.github.dockerjava.core.async.ResultCallbackTemplate;
import org.slf4j.Logger;

import javax.annotation.CheckForNull;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Copy of {@link com.github.dockerjava.core.command.PullImageResultCallback} with an customizable logger.
 */
public class PullCallback extends ResultCallbackTemplate<PullCallback, PullResponseItem> {

    private final Logger log;

    private boolean isSwarm = false;
    private Map<String, PullResponseItem> results = null;

    @CheckForNull
    private PullResponseItem latestItem = null;

    public PullCallback(Logger log) {
        this.log = log;
    }

    @Override
    public void onNext(PullResponseItem item) {
        // only do it once
        if (results == null && latestItem == null) {
            checkForDockerSwarmResponse(item);
        }

        if (isSwarm) {
            handleDockerSwarmResponse(item);
        } else {
            handleDockerClientResponse(item);
        }

        log.debug(item.toString());
    }

    private void checkForDockerSwarmResponse(PullResponseItem item) {
        String status = item.getStatus();
        if (status != null && status.matches("Pulling\\s.+\\.{3}$")) {
            isSwarm = true;
            log.debug("Communicating with Docker Swarm.");
        }
    }

    private void handleDockerSwarmResponse(final PullResponseItem item) {
        if (results == null) {
            results = new HashMap<>();
        }

        // Swarm terminates a pull sometimes with an empty line.
        // Therefore keep first success message
        PullResponseItem currentItem = results.get(item.getId());
        if (currentItem == null || !currentItem.isPullSuccessIndicated()) {
            results.put(item.getId(), item);
        }
    }

    private void handleDockerClientResponse(PullResponseItem item) {
        latestItem = item;
    }

    private void checkDockerSwarmPullSuccessful() {
        if (results.isEmpty()) {
            throw new DockerClientException("Could not pull image through Docker Swarm");
        } else {
            boolean pullFailed = false;
            StringBuilder sb = new StringBuilder();

            for (PullResponseItem pullResponseItem : results.values()) {
                if (!pullResponseItem.isPullSuccessIndicated()) {
                    pullFailed = true;
                    sb.append("[").append(pullResponseItem.getId()).append(":")
                      .append(messageFromPullResult(pullResponseItem)).append("]");
                }
            }

            if (pullFailed) {
                throw new DockerClientException("Could not pull image: " + sb.toString());
            }
        }
    }

    private void checkDockerClientPullSuccessful() {
        if (latestItem == null) {
            throw new DockerClientException("Could not pull image");
        } else if (!latestItem.isPullSuccessIndicated()) {
            throw new DockerClientException("Could not pull image: " + messageFromPullResult(latestItem));
        }
    }

    private String messageFromPullResult(PullResponseItem pullResponseItem) {
        return (pullResponseItem.getError() != null) ? pullResponseItem.getError() : pullResponseItem.getStatus();
    }

    @Override
    protected void throwFirstError() {
        super.throwFirstError();
        if (isSwarm) {
            checkDockerSwarmPullSuccessful();
        } else {
            checkDockerClientPullSuccessful();
        }
    }

    /**
     * Awaits the image to be pulled successful.
     *
     * @throws DockerClientException if the pull fails.
     * @deprecated use {@link #awaitCompletion()} or {@link #awaitCompletion(long, TimeUnit)} instead
     */
    @Deprecated
    public void awaitSuccess() {
        try {
            awaitCompletion();
        } catch (InterruptedException e) {
            throw new DockerClientException("", e);
        }
    }
}
