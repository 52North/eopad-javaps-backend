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
import okhttp3.OkHttpClient;
import org.n52.faroe.Validation;
import org.n52.faroe.annotation.Configurable;
import org.n52.faroe.annotation.Setting;
import org.n52.iceland.service.ServiceSettings;
import org.n52.javaps.eopad.http.BasicAuthenticator;
import org.n52.javaps.eopad.http.LoggingInterceptor;
import org.n52.javaps.transactional.TransactionalAlgorithmRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.URI;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;

@Configuration
@Configurable
public class ListenerConfiguration {

    private Set<TransactionalAlgorithmRepository> repositories = Collections.emptySet();

    private HttpUrl serviceURL;

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

    @Autowired(required = false)
    public void setRepositories(Set<TransactionalAlgorithmRepository> repositories) {
        this.repositories = Optional.ofNullable(repositories).orElseGet(Collections::emptySet);
    }

    @Bean
    @ConditionalOnProperty(value = "listeners.deimos.enabled", matchIfMissing = true)
    public CatalogListener deimosCatalogListener(CatalogEncoder encoder,
                                                 @Value("${listener.deimos.username:}") String username,
                                                 @Value("${listener.deimos.password:}") String password) {
        String url = "http://servicecatalogue-ogctestbed15.deimos.pt/smi/";
        return createListener(encoder, username, password, url);
    }

    @Bean
    @ConditionalOnProperty(value = "listeners.gmu.enabled", matchIfMissing = true)
    public CatalogListener gmuCatalogListener(CatalogEncoder encoder,
                                              @Value("${listener.gmu.username:}") String username,
                                              @Value("${listener.gmu.password:}") String password) {
        String url = "https://cloud.csiss.gmu.edu/ows15/geonet/rest3a/ogc/cat3a/";
        return createListener(encoder, username, password, url);
    }

    private CatalogListener createListener(CatalogEncoder catalogEncoder,
                                           String username,
                                           String password, String url) {
        Catalog catalog = new CatalogImpl(url);
        CatalogConfiguration catalogConfiguration = createCatalogConfiguration(catalog);
        OkHttpClient.Builder clientBuilder = new OkHttpClient.Builder().addInterceptor(new LoggingInterceptor());
        if (username != null && !username.isEmpty() && password != null && !password.isEmpty()) {
            clientBuilder.authenticator(new BasicAuthenticator(username, password));
        }
        CatalogClient catalogClient = new CatalogClientImpl(catalogConfiguration, clientBuilder.build());
        return new CatalogListener(catalogConfiguration, catalogEncoder, catalogClient);
    }

    private CatalogConfigurationImpl createCatalogConfiguration(Catalog catalog) {
        return new CatalogConfigurationImpl(catalog, repositories, serviceURL);
    }

}
