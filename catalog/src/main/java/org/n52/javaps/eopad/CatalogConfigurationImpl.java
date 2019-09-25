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
package org.n52.javaps.eopad;

import okhttp3.HttpUrl;
import org.n52.janmayen.function.Predicates;
import org.n52.janmayen.stream.Streams;
import org.n52.javaps.transactional.TransactionalAlgorithmRepository;
import org.n52.shetland.ogc.wps.ap.ApplicationPackage;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;

public class CatalogConfigurationImpl implements CatalogConfiguration {
    private final HttpUrl serviceURL;
    private final Collection<TransactionalAlgorithmRepository> repositories;
    private final Catalog catalog;

    public CatalogConfigurationImpl(Catalog catalog, Collection<TransactionalAlgorithmRepository> repositories,
                                    HttpUrl serviceURL) {
        this.catalog = Objects.requireNonNull(catalog);
        this.repositories = Objects.requireNonNull(repositories);
        this.serviceURL = Objects.requireNonNull(serviceURL);
    }

    @Override
    public Catalog getCatalog() {
        return catalog;
    }

    @Override
    public HttpUrl getServiceURL() {
        return serviceURL;
    }

    @Override
    public HttpUrl getExecuteUrl(String id) {
        return getProcessUrl(id).newBuilder()
                                .addPathSegment("jobs")
                                .build();
    }

    @Override
    public HttpUrl getProcessUrl(String id) {
        return getServiceURL().newBuilder()
                              .addPathSegment("processes")
                              .addPathSegment(id)
                              .build();
    }

    @Override
    public Stream<ApplicationPackage> getApplicationPackages() {
        return repositories.stream().flatMap(this::getApplicationPackages);
    }

    private Stream<ApplicationPackage> getApplicationPackages(TransactionalAlgorithmRepository repository) {
        return repository.getAlgorithmNames().stream().map(repository::getApplicationPackage)
                         .filter(Optional::isPresent).map(Optional::get);
    }

    @Override
    public String getServiceIdentifier() {
        return asIdentifier(getServiceURL());
    }

    private String asIdentifier(HttpUrl url) {
        List<String> domain = Streams.stream(url.host().split("\\.")).collect(toList());
        Collections.reverse(domain);
        Stream<String> path = Streams.stream(url.encodedPath().split("/")).filter(Predicates.not(String::isEmpty));
        return Stream.concat(domain.stream(), path).collect(joining("."));
    }
}
