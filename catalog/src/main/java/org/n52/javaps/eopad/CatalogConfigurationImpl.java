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
import org.n52.faroe.Validation;
import org.n52.faroe.annotation.Configurable;
import org.n52.faroe.annotation.Setting;
import org.n52.iceland.service.ServiceSettings;
import org.n52.javaps.transactional.TransactionalAlgorithmRepository;
import org.n52.shetland.ogc.wps.ap.ApplicationPackage;
import org.springframework.beans.factory.annotation.Autowired;

import java.net.URI;
import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

@Configurable
public class CatalogConfigurationImpl implements CatalogConfiguration {
    private HttpUrl serviceURL;

    private Collection<TransactionalAlgorithmRepository> repositories = Collections.emptyList();
    private Catalog catalog;

    @Override
    public Catalog getCatalog() {
        return catalog;
    }

    @Override
    public HttpUrl getServiceURL() {
        return serviceURL;
    }

    public void setCatalog(Catalog catalog) {
        this.catalog = Objects.requireNonNull(catalog);
    }

    @Autowired(required = false)
    public void setRepositories(Collection<TransactionalAlgorithmRepository> repositories) {
        this.repositories = Objects.requireNonNull(repositories);
    }

    @Setting(ServiceSettings.SERVICE_URL)
    public void setServiceURL(URI serviceURL) {
        Validation.notNull("serviceURL", serviceURL);
        this.serviceURL = HttpUrl.get(serviceURL).resolve("./rest").newBuilder().query(null).build();
    }

    @Override
    public Stream<ApplicationPackage> getApplicationPackages() {
        return repositories.stream().flatMap(this::getApplicationPackages);
    }

    private Stream<ApplicationPackage> getApplicationPackages(TransactionalAlgorithmRepository repository) {
        return repository.getAlgorithmNames().stream().map(repository::getApplicationPackage)
                         .filter(Optional::isPresent).map(Optional::get);
    }

}
