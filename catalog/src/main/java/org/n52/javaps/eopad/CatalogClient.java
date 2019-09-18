package org.n52.javaps.eopad;

import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;

public interface CatalogClient {
    void updateOrInsert(JsonNode node) throws IOException;

    void delete(String id) throws IOException;

    void update(JsonNode content) throws IOException;

    void insert(JsonNode content) throws IOException;

    boolean exists(String id) throws IOException;
}
