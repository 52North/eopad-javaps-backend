package org.n52.javaps.eopad;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.n52.janmayen.http.HTTPHeaders;
import org.n52.svalbard.coding.json.JSONConstants;

import java.io.IOException;
import java.util.Objects;

public class CatalogClientImpl implements CatalogClient {
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final OkHttpClient client;
    private final CatalogConfiguration config;

    public CatalogClientImpl(CatalogConfiguration config, OkHttpClient client) {
        this.client = Objects.requireNonNull(client);
        this.config = Objects.requireNonNull(config);
    }

    @Override
    public void updateOrInsert(JsonNode node) throws IOException {
        String id = node.path(JsonConstants.PROPERTIES).path(JSONConstants.IDENTIFIER).textValue();
        if (exists(id)) {
            update(node);
        } else {
            insert(node);
        }
    }

    @Override
    public void delete(String id) throws IOException {
        Request request = new Request.Builder().delete().url(config.getCatalog().getURL(id)).build();
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful() && response.code() != 404) {
                throw asException(response);
            }
        }
    }

    @Override
    public void update(JsonNode content) throws IOException {
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

    @Override
    public void insert(JsonNode content) throws IOException {
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

    @Override
    public boolean exists(String id) throws IOException {
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
