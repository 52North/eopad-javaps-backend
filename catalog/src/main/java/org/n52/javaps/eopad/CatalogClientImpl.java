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
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.n52.janmayen.http.HTTPHeaders;
import org.n52.svalbard.coding.json.JSONConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Objects;

public class CatalogClientImpl implements CatalogClient {
    private static final Logger LOG = LoggerFactory.getLogger(CatalogClientImpl.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final OkHttpClient client;
    private final CatalogConfiguration config;

    public CatalogClientImpl(CatalogConfiguration config, OkHttpClient client) {
        this.client = Objects.requireNonNull(client);
        this.config = Objects.requireNonNull(config);
    }

    @Override
    public void updateOrInsert(JsonNode node) throws IOException {
        if (exists(getId(node))) {
            update(node);
        } else {
            insert(node);
        }
    }

    @Override
    public void delete(String id) throws IOException {
        Request request = new Request.Builder().delete().url(getURL(id))
                                               // GMU catalog requires this header...
                                               .addHeader(HTTPHeaders.ACCEPT, MediaTypes.APPLICATION_GEO_JSON)
                                               .build();
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful() && response.code() != 404) {
                throw asException(response);
            }
        }
    }

    @Override
    public void update(JsonNode content) throws IOException {
        Request request = new Request.Builder().put(asRequestBody(content)).url(getURL(content))
                                               // GMU catalog requires this header...
                                               .addHeader(HTTPHeaders.ACCEPT, MediaTypes.APPLICATION_GEO_JSON)
                                               .addHeader(HTTPHeaders.CONTENT_TYPE, MediaTypes.APPLICATION_GEO_JSON)
                                               .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw asException(response);
            }
        }
    }

    @Override
    public void insert(JsonNode content) throws IOException {
        Request request = new Request.Builder().post(asRequestBody(content)).url(getURL())
                                               .addHeader(HTTPHeaders.CONTENT_TYPE, MediaTypes.APPLICATION_GEO_JSON)
                                               .build();
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw asException(response);
            }
        }
    }

    @Override
    public boolean exists(String id) throws IOException {
        Request request = new Request.Builder().get().url(getURL(id))
                                               .addHeader(HTTPHeaders.ACCEPT, MediaTypes.APPLICATION_GEO_JSON)
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

    private HttpUrl getURL(JsonNode content) {
        return getURL(getId(content));
    }

    private HttpUrl getURL(String id) {
        return config.getCatalog().getURL(id);
    }

    private HttpUrl getURL() {
        return config.getCatalog().getURL();
    }

    private String getId(JsonNode node) {
        return node.path(JsonConstants.PROPERTIES).path(JSONConstants.IDENTIFIER).textValue();
    }

    private RequestBody asRequestBody(JsonNode content) throws IOException {
        String value = objectMapper.writeValueAsString(content);
        LOG.info("request-body: {}", value);
        return RequestBody.create(value, MediaTypes.APPLICATION_GEO_JSON_TYPE);
    }

    private IOException asException(Response response) throws IOException {
        ResponseBody responseBody = response.body();
        String body = responseBody == null ? null : responseBody.string();
        return new IOException(String.format("service responded with %d: %s", response.code(), body));
    }
}
