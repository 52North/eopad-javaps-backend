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

import org.n52.javaps.catalog.CatalogListener;
import org.n52.javaps.catalog.CatalogListenerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ListenerConfiguration {
    private static final String DEIMOS_URL = "http://servicecatalogue-ogctestbed15.deimos.pt/smi/";
    private static final String GMU_URL = "https://cloud.csiss.gmu.edu/ows15/geonet/rest3a/ogc/cat3a/";
    private CatalogListenerFactory catalogListenerFactory;

    @Autowired
    public void setCatalogListenerFactory(CatalogListenerFactory catalogListenerFactory) {
        this.catalogListenerFactory = catalogListenerFactory;
    }

    @Bean
    @ConditionalOnProperty(value = "listeners.deimos.enabled", matchIfMissing = true)
    public CatalogListener deimosCatalogListener() {
        return catalogListenerFactory.create(DEIMOS_URL);
    }

    @Bean
    @ConditionalOnProperty(value = "listeners.gmu.enabled", matchIfMissing = true)
    public CatalogListener gmuCatalogListener(@Value("${listener.gmu.username:}") String username,
                                              @Value("${listener.gmu.password:}") String password) {
        return catalogListenerFactory.create(GMU_URL, username, password);
    }
}
