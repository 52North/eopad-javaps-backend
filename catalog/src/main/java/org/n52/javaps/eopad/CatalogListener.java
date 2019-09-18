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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.n52.faroe.annotation.Configurable;
import org.n52.janmayen.http.HTTPHeaders;
import org.n52.janmayen.lifecycle.Constructable;
import org.n52.janmayen.lifecycle.Destroyable;
import org.n52.javaps.transactional.TransactionalAlgorithmRepositoryListener;
import org.n52.shetland.ogc.wps.ap.ApplicationPackage;
import org.n52.svalbard.encode.exception.EncodingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.util.Objects;

@Configurable
public class CatalogListener implements TransactionalAlgorithmRepositoryListener, Constructable, Destroyable {
    private static final Logger LOG = LoggerFactory.getLogger(CatalogListener.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    private OkHttpClient client;
    private CatalogEncoder catalogEncoder;
    private CatalogConfiguration config;

    public void setConfig(CatalogConfiguration config) {
        this.config = Objects.requireNonNull(config);
    }

    public void setCatalogEncoder(CatalogEncoder catalogEncoder) {
        this.catalogEncoder = Objects.requireNonNull(catalogEncoder);
    }

    @Autowired
    public void setClient(OkHttpClient client) {
        this.client = Objects.requireNonNull(client);
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
            delete(config.getServiceURL().toString());
        } catch (IOException e) {
            LOG.error("Error deleting service", e);
        }
    }

    private void updateOrInsertApplicationPackage(ApplicationPackage applicationPackage) {
        try {
            updateOrInsert(catalogEncoder.createProcessInsertion(applicationPackage));
        } catch (EncodingException e) {
            LOG.warn("Error encoding application package", e);
        }
    }

    private void updateOrInsertServiceDescription() {
        try {
            updateOrInsert(catalogEncoder.createServiceInsertion());
        } catch (EncodingException e) {
            LOG.warn("Error encoding service description", e);
        }
    }

    private void updateOrInsert(JsonNode node) {
        String id = node.path(JsonConstants.ID).textValue();
        try {
            if (exists(id)) {
                update(node);
            } else {
                insert(node);
            }
        } catch (IOException e) {
            LOG.warn("Error inserting " + id, e);
        }
    }

    private void delete(String id) throws IOException {
        Request request = new Request.Builder().delete().url(config.getCatalog().getURL(id)).build();
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful() && response.code() != 404) {
                throw asException(response);
            }
        }
    }

    private void update(JsonNode content) throws IOException {
        Request request = new Request.Builder()
                                  .put(asRequestBody(content))
                                  .url(config.getCatalog().getURL(content.path(JsonConstants.ID).textValue()))
                                  .addHeader(HTTPHeaders.CONTENT_TYPE, MediaTypes.APPLICATION_GEO_JSON.toString())
                                  .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw asException(response);
            }
        }
    }

    private void insert(JsonNode content) throws IOException {
        Request request = new Request.Builder()
                                  .post(asRequestBody(content))
                                  .url(config.getCatalog().getURL())
                                  .addHeader(HTTPHeaders.CONTENT_TYPE, MediaTypes.APPLICATION_GEO_JSON.toString())
                                  .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw asException(response);
            }
        }
    }

    private boolean exists(String id) throws IOException {
        Request request = new Request.Builder().get()
                                               .url(config.getCatalog().getURL(id))
                                               .addHeader(HTTPHeaders.ACCEPT,
                                                          MediaTypes.APPLICATION_GEO_JSON.toString())
                                               .build();
        try (Response response = client.newCall(request).execute()) {
            if (response.isSuccessful()) {
                return true;
            } else if (response.code() == 404) {
                return false;
            } else {
                throw asException(response);
            }
        }
    }

    private RequestBody asRequestBody(JsonNode content) throws IOException {
        return RequestBody.create(objectMapper.writeValueAsString(content), MediaTypes.APPLICATION_GEO_JSON);
    }

    private IOException asException(Response response) throws IOException {
        ResponseBody responseBody = response.body();
        String body = responseBody == null ? null : responseBody.string();
        return new IOException(String.format("service responded with %d: %s", response.code(), body));
    }
}
