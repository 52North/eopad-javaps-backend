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

import org.n52.janmayen.function.Predicates;

import javax.annotation.CheckReturnValue;
import java.util.Objects;
import java.util.Optional;

/**
 * Representation of a Docker image name in it's components.
 *
 * @author Christian Autermann
 */
public class DockerImage {
    public static final String LATEST = "latest";
    private final String repository;
    private final String tag;
    private final String registry;

    /**
     * Create a new {@link DockerImage}.
     *
     * @param repository The repository of the image.
     */
    public DockerImage(String repository) {
        this(null, repository, null);
    }

    /**
     * Create a new {@link DockerImage}.
     *
     * @param repository The repository of the image.
     * @param tag        The tag of the image.
     */
    public DockerImage(String repository, String tag) {
        this(null, repository, tag);
    }

    /**
     * Create a new {@link DockerImage}.
     *
     * @param registry   The registry of the image.
     * @param repository The repository of the image.
     * @param tag        The tag of the image.
     */
    public DockerImage(String registry, String repository, String tag) {
        this.repository = Objects.requireNonNull(repository);
        this.tag = tag;
        this.registry = registry;
    }

    /**
     * Get the registry for this image.
     *
     * @return The registry.
     */
    public Optional<String> getRegistry() {
        return Optional.ofNullable(registry).filter(Predicates.not(String::isEmpty));
    }

    /**
     * Get the tag of this image.
     *
     * @return The tag.
     */
    public Optional<String> getTag() {
        return Optional.ofNullable(tag).filter(Predicates.not(String::isEmpty));
    }

    /**
     * Get the repository of this image.
     *
     * @return The repository.
     */
    public String getRepository() {
        return repository;
    }

    /**
     * Return a new {@link DockerImage} with the specified tag.
     *
     * @param tag The tag.
     * @return The {@link DockerImage}.
     */
    @CheckReturnValue
    public DockerImage withTag(String tag) {
        return new DockerImage(registry, repository, tag);
    }

    /**
     * Return a new {@link DockerImage} without any tag.
     *
     * @return The {@link DockerImage}.
     */
    @CheckReturnValue
    public DockerImage withoutTag() {
        return withTag(null);
    }

    /**
     * Return a new {@link DockerImage} with {@value LATEST} as the tag.
     *
     * @return The {@link DockerImage}.
     */
    @CheckReturnValue
    public DockerImage latest() {
        return withTag(LATEST);
    }

    /**
     * If the tag of this image is specified return this image. Else create a new {@link DockerImage} with {@value
     * LATEST} as the tag.
     *
     * @return The {@link DockerImage}.
     */
    @CheckReturnValue
    public DockerImage latestIfNotSpecified() {
        if (!getTag().isPresent()) {
            return latest();
        } else {
            return this;
        }
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        getRegistry().ifPresent(r -> builder.append(r).append('/'));
        builder.append(repository);
        getTag().ifPresent(t -> builder.append(':').append(t));
        return builder.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof DockerImage)) {
            return false;
        }
        DockerImage that = (DockerImage) o;
        return repository.equals(that.repository) &&
               Objects.equals(tag, that.tag) &&
               Objects.equals(registry, that.registry);
    }

    @Override
    public int hashCode() {
        return Objects.hash(repository, tag, registry);
    }

    /**
     * Parse the name of a Docker image to it's components.
     *
     * @param name The name.
     * @return The {@link DockerImage}.
     */
    public static DockerImage fromString(String name) {
        String rest = name;
        String[] split = rest.split("/", 1);
        String repository;
        String registry = null;
        String tag = null;
        if (split.length > 1) {
            registry = split[0];
            rest = split[1];
        }
        split = rest.split(":", 1);
        repository = split[0];
        if (split.length > 1) {
            tag = split[1];
        }
        return new DockerImage(registry, repository, tag);
    }
}
