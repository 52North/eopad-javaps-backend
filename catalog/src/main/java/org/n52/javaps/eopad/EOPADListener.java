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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.n52.faroe.Validation;
import org.n52.faroe.annotation.Configurable;
import org.n52.faroe.annotation.Setting;
import org.n52.iceland.service.ServiceSettings;
import org.n52.janmayen.function.Predicates;
import org.n52.janmayen.http.HTTPHeaders;
import org.n52.janmayen.lifecycle.Constructable;
import org.n52.janmayen.lifecycle.Destroyable;
import org.n52.janmayen.stream.Streams;
import org.n52.javaps.engine.Engine;
import org.n52.javaps.transactional.TransactionalAlgorithmRepositoryListener;
import org.n52.shetland.ogc.wps.ProcessOffering;
import org.n52.shetland.ogc.wps.ap.ApplicationPackage;
import org.n52.shetland.ogc.wps.description.ProcessDescription;
import org.n52.svalbard.decode.DecoderRepository;
import org.n52.svalbard.encode.Encoder;
import org.n52.svalbard.encode.EncoderRepository;
import org.n52.svalbard.encode.exception.EncodingException;
import org.n52.svalbard.encode.json.JSONEncoderKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.stream.Collectors.joining;

@Configurable
@Component
public class EOPADListener implements TransactionalAlgorithmRepositoryListener, Constructable, Destroyable {
    private static final Logger LOG = LoggerFactory.getLogger(EOPADListener.class);
    private static final String TYPE = "type";
    private static final String ID = "id";
    private static final String KIND = "kind";
    private static final String UPDATED = "updated";
    private static final String TITLE = "title";
    private static final String LINKS = "links";
    private static final String PROFILES = "profiles";
    private static final String HREF = "href";
    private static final String GEOMETRY = "geometry";
    private static final String IDENTIFIER = "identifier";
    private static final String ENDPOINT_DESCRIPTION = "endpointDescription";
    private static final String OFFERINGS = "offerings";
    private static final String CODE = "code";
    private static final String OPERATIONS = "operations";
    private static final String METHOD = "method";
    private static final String REQUEST = "request";
    private static final String APPLICATION_JSON = "application/json";
    private static final String GET = "GET";
    private static final String POST = "POST";
    private static final String CONTENT = "content";
    private static final String DESCRIBE_PROCESS = "DescribeProcess";
    private static final String DEPLOY_PROCESS = "DeployProcess";
    private static final String PROCESSES = "Processes";
    private static final String CONFORMANCE = "Conformance";
    private static final String SERVICE = "Service";
    private static final String LANDING_PAGE = "LandingPage";
    private static final String APPLICATION_OPENAPI_JSON_VERSION_3_0 = "application/openapi+json;version=3.0";
    private static final String SPEC_EOPAD_GEOJSON_CORE = "http://www.opengis.net/spec/eopad-geojson/1.0/req/core";
    private static final String SPEC_OWC_GEOJSON_CORE = "http://www.opengis.net/spec/owc-geojson/1.0/req/core";
    private static final String SPEC_EOPAD_GEOJSON_OGC_PROCESSES_API = "http://www.opengis.net/spec/eopad-geojson/1.0/req/ogc-processes-api";
    private static final String FEATURE = "Feature";
    private static final String SERVICE_TYPE = "http://inspire.ac.europa.eu/metadata-codelist/ResourceType/service";
    private static final String SPEC_OWC_GEOJSON_WPS = "http://www.opengis.net/spec/owc-geojson/1.0/req/wps";
    private static final String PROPERTIES = "properties";
    private static final String ACCEPT = "accept";
    private static final MediaType APPLICATION_GEO_JSON = MediaType.get("application/geo+json");
    private JsonNodeFactory nodeFactory;
    private HttpUrl serviceURL;
    private EncoderRepository encoderRepository;
    private DecoderRepository decoderRepository;
    private Engine engine;
    private OkHttpClient client;
    private ObjectMapper objectMapper;
    private HttpUrl cswServiceUrl = HttpUrl.get("https://cloud.csiss.gmu.edu/ows15/geonet/rest3a/ogc/cat3a/");

    @Autowired
    public void setObjectMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Autowired
    public void setDecoderRepository(DecoderRepository decoderRepository) {
        this.decoderRepository = decoderRepository;
    }

    @Autowired
    public void setEncoderRepository(EncoderRepository encoderRepository) {
        this.encoderRepository = encoderRepository;
    }

    @Autowired
    public void setClient(OkHttpClient client) {
        this.client = client;
    }

    @Autowired
    public void setEngine(Engine engine) {
        this.engine = engine;
    }

    @Setting(ServiceSettings.SERVICE_URL)
    public void setServiceURL(URI serviceURL) {
        Validation.notNull("serviceURL", serviceURL);
        this.serviceURL = HttpUrl.get(serviceURL).resolve("./rest").newBuilder().query(null).build();
    }

    @Override
    public void init() {
        try {
            updateOrInsert(createServiceInsertion());
        } catch (IOException | EncodingException e) {
            throw new Error(e);
        }
    }

    @Override
    public void destroy() {
        try {
            delete(serviceURL.toString());
        } catch (IOException e) {
            LOG.error("Error deleting service", e);
        }
    }

    private void updateOrInsert(JsonNode node) throws IOException {
        if (exists(node.path(ID).textValue())) {
            update(node);
        } else {
            insert(node);
        }
    }

    private void delete(String id) throws IOException {
        Request request = new Request.Builder().delete().url(getCatalogURL(id)).build();
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful() && response.code() != 404) {
                throw asException(response);
            }
        }
    }

    private void update(JsonNode content) throws IOException {
        Request request = new Request.Builder()
                                  .put(asRequestBody(content))
                                  .url(getCatalogURL(content.path(ID).textValue()))
                                  .addHeader(HTTPHeaders.CONTENT_TYPE, APPLICATION_GEO_JSON.toString()).build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw asException(response);
            }
        }
    }

    private void insert(JsonNode content) throws IOException {
        Request request = new Request.Builder()
                                  .post(asRequestBody(content))
                                  .url(getCatalogURL())
                                  .addHeader(HTTPHeaders.CONTENT_TYPE, APPLICATION_GEO_JSON.toString()).build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw asException(response);
            }
        }
    }

    private RequestBody asRequestBody(JsonNode content) throws JsonProcessingException {
        return RequestBody.create(objectMapper.writeValueAsString(content), EOPADListener.APPLICATION_GEO_JSON);
    }

    private IOException asException(Response response) throws IOException {
        ResponseBody responseBody = response.body();
        String body = responseBody == null ? null : responseBody.string();
        return new IOException(String.format("service responded with %d: %s", response.code(), body));
    }

    private boolean exists(String id) throws IOException {
        Request request = new Request.Builder().get().url(getCatalogURL(id))
                                               .addHeader(HTTPHeaders.ACCEPT, APPLICATION_GEO_JSON.toString()).build();
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

    private HttpUrl getCatalogURL(String id) {
        return cswServiceUrl.newBuilder().addPathSegment("service").addPathSegment(id).build();
    }

    private HttpUrl getCatalogURL() {
        return cswServiceUrl.newBuilder().addPathSegment("service").build();
    }

    @Override
    public void onRegister(ApplicationPackage applicationPackage) {
        try {
            ObjectNode root = createProcessInsertion(applicationPackage);
            // check if application package is present
            // if present PUT /services/{id}
            // else POST /services/{id}
        } catch (EncodingException e) {
            LOG.warn("Error encoding process registration", e);
        }
    }

    @Override
    public void onUnregister(ApplicationPackage applicationPackage) {
        // DELETE /services/{id}
    }

    private ObjectNode createProcessInsertion(ApplicationPackage applicationPackage)
            throws EncodingException {
        URI url = getProcessId(applicationPackage);
        JsonNode content = getApplicationPackageEncoder().encode(applicationPackage);

        ObjectNode root = nodeFactory.objectNode()
                                     .put(TYPE, FEATURE)
                                     .put(ID, url.toString())
                                     .putNull(GEOMETRY);

        ObjectNode properties = root.putObject(PROPERTIES);
        properties.put(IDENTIFIER, asIdentifier(HttpUrl.get(url)));
        properties.put(KIND, SERVICE_TYPE);
        properties.put(TITLE, applicationPackage.getProcessDescription().getProcessDescription()
                                                .getTitle().getValue());
        properties.put(UPDATED, OffsetDateTime.now().toString());
        properties.putObject(LINKS);

        ArrayNode offerings = properties.putArray(OFFERINGS);
        ObjectNode offering = offerings.addObject()
                                       .put(CODE, SPEC_OWC_GEOJSON_WPS);
        ArrayNode operations = offering.putArray(OPERATIONS);
        ObjectNode operation = operations.addObject()
                                         .put(CODE, DEPLOY_PROCESS)
                                         .put(METHOD, POST)
                                         .put(TYPE, APPLICATION_JSON)
                                         .put(HREF, "data:");
        ObjectNode request = operation.putObject(REQUEST);
        request.put(TYPE, APPLICATION_JSON)
               .set(CONTENT, content);
        return root;
    }

    private URI getProcessId(ApplicationPackage applicationPackage) {
        return URI.create(serviceURL.toString() + "/processes/" + applicationPackage.getProcessDescription()
                                                                                    .getProcessDescription().getId());
    }

    private ObjectNode createServiceInsertion() throws EncodingException {
        ObjectNode root = nodeFactory.objectNode().put(TYPE, FEATURE)
                                     .put(ID, serviceURL.toString())
                                     .putNull(GEOMETRY);

        ObjectNode properties = root.putObject(PROPERTIES);
        properties.put(IDENTIFIER, asIdentifier(serviceURL));
        properties.put(KIND, SERVICE_TYPE);
        properties.put(TITLE, "Processing API at " + serviceURL.host());
        properties.put(UPDATED, OffsetDateTime.now().toString());
        ObjectNode links = root.putObject(LINKS);
        ArrayNode profiles = links.putArray(PROFILES);
        profiles.addObject().put(HREF, SPEC_OWC_GEOJSON_CORE);
        profiles.addObject().put(HREF, SPEC_EOPAD_GEOJSON_CORE);
        // TODO hosts links
        properties.putArray(ENDPOINT_DESCRIPTION).add(serviceURL.toString());
        ArrayNode offerings = properties.putArray(OFFERINGS);

        ObjectNode offering = offerings.addObject()
                                       .put(CODE, SPEC_EOPAD_GEOJSON_OGC_PROCESSES_API);
        ArrayNode operations = offering.putArray(OPERATIONS);
        operations.addObject()
                  .put(CODE, LANDING_PAGE)
                  .put(METHOD, GET)
                  .put(HREF, serviceURL.toString())
                  .put(TYPE, APPLICATION_JSON);

        operations.addObject()
                  .put(CODE, SERVICE)
                  .put(METHOD, GET)
                  .put(HREF, serviceURL.toString() + "/api")
                  .put(TYPE, APPLICATION_OPENAPI_JSON_VERSION_3_0);

        operations.addObject()
                  .put(CODE, CONFORMANCE)
                  .put(METHOD, GET)
                  .put(HREF, serviceURL.toString() + "/conformance/")
                  .put(TYPE, APPLICATION_JSON);

        operations.addObject()
                  .put(CODE, PROCESSES)
                  .put(METHOD, GET)
                  .put(HREF, serviceURL.toString() + "/processes/")
                  .put(TYPE, APPLICATION_JSON);

        operations.addObject()
                  .put(CODE, DEPLOY_PROCESS)
                  .put(METHOD, POST)
                  .put(HREF, serviceURL.toString() + "/processes/")
                  .put(TYPE, APPLICATION_JSON)
                  .putObject(REQUEST).put(TYPE, APPLICATION_JSON);

        for (ProcessDescription description : engine.getProcessDescriptions()) {
            JsonNode content = getProcessOfferingEncoder().encode(new ProcessOffering(description));
            operations.addObject()
                      .put(CODE, DESCRIBE_PROCESS)
                      .put(METHOD, GET)
                      .put(HREF, getProcessURL(description))
                      .put(TYPE, APPLICATION_JSON)
                      .set(CONTENT, content);
        }
        return root;
    }

    private String getProcessURL(ProcessDescription description) {
        return serviceURL.toString() + "/processes/" + description.getId();
    }

    private Encoder<JsonNode, ProcessOffering> getProcessOfferingEncoder() {
        return encoderRepository
                       .<JsonNode, ProcessOffering>tryGetEncoder(new JSONEncoderKey(ProcessOffering.class))
                       .orElseThrow(() -> new RuntimeException("no process offering encoder found"));
    }

    private Encoder<JsonNode, ApplicationPackage> getApplicationPackageEncoder() {
        return encoderRepository
                       .<JsonNode, ApplicationPackage>tryGetEncoder(new JSONEncoderKey(ApplicationPackage.class))
                       .orElseThrow(() -> new RuntimeException("no process offering encoder found"));
    }

    private String asIdentifier(HttpUrl url) {
        List<String> domain = Streams.stream(url.host().split("\\.")).collect(Collectors.toList());
        Collections.reverse(domain);
        Stream<String> path = Arrays.stream(url.encodedPath().split("/")).filter(Predicates.not(String::isEmpty));
        return Stream.concat(domain.stream(), path).collect(joining("."));
    }
}
