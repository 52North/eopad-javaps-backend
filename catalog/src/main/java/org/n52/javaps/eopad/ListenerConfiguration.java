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

import okhttp3.OkHttpClient;
import org.n52.javaps.engine.Engine;
import org.n52.javaps.transactional.TransactionalAlgorithmRepository;
import org.n52.svalbard.encode.EncoderRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Collection;

@Configuration
public class ListenerConfiguration {

    @Bean
    @Deimos
    public CatalogClient deimosCatalogClient(@Deimos CatalogConfiguration configuration,
                                             OkHttpClient client) {
        return new CatalogClientImpl(configuration, client);
    }

    @Bean
    @Deimos
    public CatalogEncoder deimosCatalogEncoder(@Deimos CatalogConfiguration configuration,
                                               Engine engine, EncoderRepository encoderRepository) {
        return new CatalogEncoderImpl(configuration, engine, encoderRepository);
    }

    @Bean
    public CatalogListener deimosCatalogListener(@Deimos CatalogConfiguration configuration,
                                                 @Deimos CatalogEncoder encoder,
                                                 @Deimos CatalogClient client) {
        return new CatalogListener(configuration, encoder, client);
    }

    @Bean
    @Deimos
    public CatalogConfiguration deimosCatalogConfiguration(@Deimos Catalog catalog,
                                                           Collection<TransactionalAlgorithmRepository> repositories) {
        return new CatalogConfigurationImpl(catalog, repositories);
    }

    @Bean
    @Deimos
    public Catalog deimosCatalog(@Value("http://servicecatalogue-ogctestbed15.deimos.pt/smi/") String url) {
        return new CatalogImpl(url);
    }

    @Bean
    @GMU
    public CatalogClient gmuCatalogClient(@GMU CatalogConfiguration configuration,
                                          OkHttpClient client) {
        return new CatalogClientImpl(configuration, client);
    }

    @Bean
    @GMU
    public CatalogEncoder gmuCatalogEncoder(@GMU CatalogConfiguration configuration,
                                            Engine engine, EncoderRepository encoderRepository) {
        return new CatalogEncoderImpl(configuration, engine, encoderRepository);
    }

    @Bean
    public CatalogListener gmuCatalogListener(@GMU CatalogConfiguration configuration,
                                              @GMU CatalogEncoder encoder,
                                              @GMU CatalogClient client) {
        return new CatalogListener(configuration, encoder, client);
    }

    @Bean
    @GMU
    public CatalogConfiguration gmuCatalogConfiguration(@GMU Catalog catalog,
                                                        Collection<TransactionalAlgorithmRepository> repositories) {
        return new CatalogConfigurationImpl(catalog, repositories);
    }

    @Bean
    @GMU
    public Catalog gmuCatalog(@Value("https://cloud.csiss.gmu.edu/ows15/geonet/rest3a/ogc/cat3a/") String url) {
        return new CatalogImpl(url);
    }

    @Target({ElementType.FIELD, ElementType.METHOD, ElementType.TYPE, ElementType.PARAMETER})
    @Retention(RetentionPolicy.RUNTIME)
    @Qualifier
    @interface Deimos {}

    @Target({ElementType.FIELD, ElementType.METHOD, ElementType.TYPE, ElementType.PARAMETER})
    @Retention(RetentionPolicy.RUNTIME)
    @Qualifier
    @interface GMU {}

}
