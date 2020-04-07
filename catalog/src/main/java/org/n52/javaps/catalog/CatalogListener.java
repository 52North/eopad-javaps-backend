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

import org.n52.faroe.annotation.Configurable;
import org.n52.janmayen.lifecycle.Constructable;
import org.n52.janmayen.lifecycle.Destroyable;
import org.n52.javaps.transactional.TransactionalAlgorithmRepositoryListener;
import org.n52.shetland.ogc.wps.ap.ApplicationPackage;
import org.n52.svalbard.encode.exception.EncodingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Objects;

@Configurable
public class CatalogListener implements TransactionalAlgorithmRepositoryListener, Constructable, Destroyable {
    private static final Logger LOG = LoggerFactory.getLogger(CatalogListener.class);
    private final CatalogEncoder catalogEncoder;
    private final CatalogClient client;
    private final CatalogConfiguration config;

    public CatalogListener(CatalogConfiguration config, CatalogEncoder catalogEncoder, CatalogClient client) {
        this.catalogEncoder = Objects.requireNonNull(catalogEncoder);
        this.client = Objects.requireNonNull(client);
        this.config = Objects.requireNonNull(config);
    }

    @Override
    public void init() {
        // be sure to that all application packages are inserted
        config.getApplicationPackages().forEach(this::updateOrInsertApplicationPackage);
        // then insert the service description
        updateOrInsertServiceDescription();
    }

    @Override
    public void onRegister(ApplicationPackage applicationPackage) {
        updateOrInsertApplicationPackage(applicationPackage);
        updateOrInsertServiceDescription();
    }

    @Override
    public void onUnregister(ApplicationPackage applicationPackage) {
        // TODO: delete application package
        // DELETE /services/{id}
        updateOrInsertServiceDescription();
    }

    @Override
    public void destroy() {
        try {
            client.delete(config.getServiceIdentifier());
        } catch (IOException e) {
            LOG.error("Error deleting service", e);
        }
    }

    private void updateOrInsertApplicationPackage(ApplicationPackage applicationPackage) {
        try {
            client.updateOrInsert(catalogEncoder.createProcessInsertion(applicationPackage, config));
        } catch (EncodingException | IOException e) {
            LOG.warn("Error inserting/updating application package", e);
        }
    }

    private void updateOrInsertServiceDescription() {
        try {
            client.updateOrInsert(catalogEncoder.createServiceInsertion(config));
        } catch (EncodingException | IOException e) {
            LOG.warn("Error inserting/updating service description", e);
        }
    }

    @Override
    public String toString() {
        return String.format("%s{catalog=%s}", getClass().getName(), config.getCatalog().getURL());
    }
}
