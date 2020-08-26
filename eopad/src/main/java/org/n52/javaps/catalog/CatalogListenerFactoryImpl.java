/*
 * Copyright 2019-2020 52°North Initiative for Geospatial Open Source
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
package org.n52.javaps.catalog;

import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import org.n52.faroe.Validation;
import org.n52.faroe.annotation.Configurable;
import org.n52.faroe.annotation.Setting;
import org.n52.iceland.i18n.I18NSettings;
import org.n52.iceland.ogc.ows.OwsServiceMetadataRepository;
import org.n52.iceland.service.ServiceSettings;
import org.n52.janmayen.i18n.LocaleHelper;
import org.n52.javaps.catalog.http.BasicAuthenticator;
import org.n52.javaps.catalog.http.LoggingInterceptor;
import org.n52.javaps.transactional.TransactionalAlgorithmRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.util.Collections;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

@Component
@Configurable
public class CatalogListenerFactoryImpl implements CatalogListenerFactory {
    private Set<TransactionalAlgorithmRepository> repositories = Collections.emptySet();
    private OwsServiceMetadataRepository serviceMetadataRepository;
    private HttpUrl serviceURL;
    private Locale defaultLocale;
    private CatalogEncoder catalogEncoder;

    @Autowired
    public void setCatalogEncoder(CatalogEncoder catalogEncoder) {
        this.catalogEncoder = Objects.requireNonNull(catalogEncoder);
    }

    @Autowired
    public void setServiceMetadataRepository(OwsServiceMetadataRepository serviceMetadataRepository) {
        this.serviceMetadataRepository = Objects.requireNonNull(serviceMetadataRepository);
    }

    @Autowired(required = false)
    public void setRepositories(Set<TransactionalAlgorithmRepository> repositories) {
        this.repositories = Optional.ofNullable(repositories).orElseGet(Collections::emptySet);
    }

    @Setting(I18NSettings.I18N_DEFAULT_LANGUAGE)
    public void setDefaultLanguage(String lang) {
        this.defaultLocale = LocaleHelper.decode(lang);
    }

    @Setting(ServiceSettings.SERVICE_URL)
    public void setServiceURL(URI serviceURL) {
        Validation.notNull("serviceURL", serviceURL);
        HttpUrl httpUrl = HttpUrl.get(serviceURL);
        if (httpUrl == null) {
            throw new IllegalArgumentException();
        }
        httpUrl = httpUrl.resolve("./rest");
        if (httpUrl == null) {
            throw new IllegalArgumentException();
        }
        HttpUrl.Builder builder = httpUrl.newBuilder();
        this.serviceURL = builder.query(null).build();
    }

    @Override
    public CatalogListener create(String url) {
        return create(url, null, null);
    }

    @Override
    public CatalogListener create(String url, String username, String password) {
        Catalog catalog = new CatalogImpl(url);
        CatalogConfiguration catalogConfiguration = new CatalogConfigurationImpl(catalog,
                                                                                 repositories,
                                                                                 serviceMetadataRepository,
                                                                                 serviceURL,
                                                                                 defaultLocale);
        OkHttpClient.Builder clientBuilder = new OkHttpClient.Builder().addInterceptor(new LoggingInterceptor());
        if (username != null && !username.isEmpty() && password != null && !password.isEmpty()) {
            clientBuilder.authenticator(new BasicAuthenticator(username, password));
        }
        CatalogClient catalogClient = new CatalogClientImpl(catalogConfiguration, clientBuilder.build());
        return new CatalogListener(catalogConfiguration, catalogEncoder, catalogClient);
    }

}
