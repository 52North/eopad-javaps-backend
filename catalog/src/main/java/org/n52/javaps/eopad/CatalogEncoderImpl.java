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
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import okhttp3.HttpUrl;
import org.n52.faroe.annotation.Configurable;
import org.n52.janmayen.Json;
import org.n52.janmayen.function.Predicates;
import org.n52.janmayen.stream.Streams;
import org.n52.javaps.engine.Engine;
import org.n52.shetland.ogc.wps.ProcessOffering;
import org.n52.shetland.ogc.wps.ap.ApplicationPackage;
import org.n52.shetland.ogc.wps.description.ProcessDescription;
import org.n52.svalbard.encode.Encoder;
import org.n52.svalbard.encode.EncoderRepository;
import org.n52.svalbard.encode.exception.EncodingException;
import org.n52.svalbard.encode.json.JSONEncoderKey;
import org.springframework.beans.factory.annotation.Autowired;

import java.net.URI;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.stream.Collectors.joining;

@Configurable
public class CatalogEncoderImpl implements CatalogEncoder {

    private EncoderRepository encoderRepository;
    private Engine engine;
    private CatalogConfiguration config;

    public void setConfig(CatalogConfiguration config) {
        this.config = Objects.requireNonNull(config);
    }

    @Autowired
    public void setEncoderRepository(EncoderRepository encoderRepository) {
        this.encoderRepository = Objects.requireNonNull(encoderRepository);
    }

    @Autowired
    public void setEngine(Engine engine) {
        this.engine = Objects.requireNonNull(engine);
    }

    @Override
    public ObjectNode createProcessInsertion(ApplicationPackage applicationPackage)
            throws EncodingException {
        URI url = getProcessId(applicationPackage);
        JsonNode content = getApplicationPackageEncoder().encode(applicationPackage);

        ObjectNode root = Json.nodeFactory().objectNode()
                              .put(JsonConstants.TYPE, JsonConstants.FEATURE)
                              .put(JsonConstants.ID, url.toString())
                              .putNull(JsonConstants.GEOMETRY);

        ObjectNode properties = root.putObject(JsonConstants.PROPERTIES);
        properties.put(JsonConstants.IDENTIFIER, asIdentifier(HttpUrl.get(url)));
        properties.put(JsonConstants.KIND, Kinds.SERVICE_TYPE);
        properties.put(JsonConstants.TITLE, getTitle(applicationPackage));
        properties.put(JsonConstants.UPDATED, OffsetDateTime.now().toString());
        properties.putObject(JsonConstants.LINKS);

        ArrayNode offerings = properties.putArray(JsonConstants.OFFERINGS);
        ObjectNode offering = offerings.addObject()
                                       .put(JsonConstants.CODE, Specifications.OWC_GEOJSON_WPS);
        ArrayNode operations = offering.putArray(JsonConstants.OPERATIONS);
        ObjectNode operation = operations.addObject()
                                         .put(JsonConstants.CODE, Operations.DEPLOY_PROCESS)
                                         .put(JsonConstants.METHOD, HttpMethods.POST)
                                         .put(JsonConstants.TYPE, MediaTypes.APPLICATION_JSON)
                                         .put(JsonConstants.HREF, "data:");
        ObjectNode request = operation.putObject(JsonConstants.REQUEST);
        request.put(JsonConstants.TYPE, MediaTypes.APPLICATION_JSON)
               .set(JsonConstants.CONTENT, content);
        return root;
    }

    private URI getProcessId(ApplicationPackage applicationPackage) {

        return URI.create(applicationPackage.getProcessDescription().getProcessDescription().getId().getValue());

        //return URI.create(config.getServiceURL().toString() + "/processes/" + applicationPackage.getProcessDescription()
        // .getProcessDescription().getId());
    }

    @Override
    public ObjectNode createServiceInsertion() throws EncodingException {
        ObjectNode root = Json.nodeFactory().objectNode()
                              .put(JsonConstants.TYPE, JsonConstants.FEATURE)
                              .put(JsonConstants.ID, config.getServiceURL().toString())
                              .putNull(JsonConstants.GEOMETRY);

        ObjectNode properties = root.putObject(JsonConstants.PROPERTIES);
        properties.put(JsonConstants.IDENTIFIER, asIdentifier(config.getServiceURL()));
        properties.put(JsonConstants.KIND, Kinds.SERVICE_TYPE);
        properties.put(JsonConstants.TITLE, "Processing API at " + config.getServiceURL().host());
        properties.put(JsonConstants.UPDATED, OffsetDateTime.now().toString());
        ObjectNode links = root.putObject(JsonConstants.LINKS);
        ArrayNode profiles = links.putArray(JsonConstants.PROFILES);
        profiles.addObject().put(JsonConstants.HREF, Specifications.OWC_GEOJSON_CORE);
        profiles.addObject().put(JsonConstants.HREF, Specifications.EOPAD_GEOJSON_CORE);
        // TODO hosts links

        ArrayNode hosts = links.putArray(JsonConstants.HOSTS);

        config.getApplicationPackages().forEach(ap -> hosts.addObject()
                                                           .put(JsonConstants.HREF, config.getCatalog().getURL(ap))
                                                           .put(JsonConstants.TYPE, MediaTypes.APPLICATION_JSON)
                                                           .put(JsonConstants.TITLE, getTitle(ap)));

        properties.putArray(JsonConstants.ENDPOINT_DESCRIPTION).add(config.getServiceURL().toString());
        ArrayNode offerings = properties.putArray(JsonConstants.OFFERINGS);

        ObjectNode offering = offerings.addObject()
                                       .put(JsonConstants.CODE, Specifications.EOPAD_GEOJSON_OGC_PROCESSES_API);
        ArrayNode operations = offering.putArray(JsonConstants.OPERATIONS);
        operations.addObject()
                  .put(JsonConstants.CODE, JsonConstants.LANDING_PAGE)
                  .put(JsonConstants.METHOD, HttpMethods.GET)
                  .put(JsonConstants.HREF, config.getServiceURL().toString())
                  .put(JsonConstants.TYPE, MediaTypes.APPLICATION_JSON);

        operations.addObject()
                  .put(JsonConstants.CODE, JsonConstants.SERVICE)
                  .put(JsonConstants.METHOD, HttpMethods.GET)
                  .put(JsonConstants.HREF, config.getServiceURL().toString() + "/api")
                  .put(JsonConstants.TYPE, MediaTypes.APPLICATION_OPENAPI_JSON_VERSION_3_0);

        operations.addObject()
                  .put(JsonConstants.CODE, JsonConstants.CONFORMANCE)
                  .put(JsonConstants.METHOD, HttpMethods.GET)
                  .put(JsonConstants.HREF, config.getServiceURL().toString() + "/conformance/")
                  .put(JsonConstants.TYPE, MediaTypes.APPLICATION_JSON);

        operations.addObject()
                  .put(JsonConstants.CODE, JsonConstants.PROCESSES)
                  .put(JsonConstants.METHOD, HttpMethods.GET)
                  .put(JsonConstants.HREF, config.getServiceURL().toString() + "/processes/")
                  .put(JsonConstants.TYPE, MediaTypes.APPLICATION_JSON);

        operations.addObject()
                  .put(JsonConstants.CODE, Operations.DEPLOY_PROCESS)
                  .put(JsonConstants.METHOD, HttpMethods.POST)
                  .put(JsonConstants.HREF, config.getServiceURL().toString() + "/processes/")
                  .put(JsonConstants.TYPE, MediaTypes.APPLICATION_JSON)
                  .putObject(JsonConstants.REQUEST).put(JsonConstants.TYPE, MediaTypes.APPLICATION_JSON);

        for (ProcessDescription description : engine.getProcessDescriptions()) {
            JsonNode content = getProcessOfferingEncoder().encode(new ProcessOffering(description));
            operations.addObject()
                      .put(JsonConstants.CODE, Operations.DESCRIBE_PROCESS)
                      .put(JsonConstants.METHOD, HttpMethods.GET)
                      .put(JsonConstants.HREF, getProcessURL(description))
                      .put(JsonConstants.TYPE, MediaTypes.APPLICATION_JSON)
                      .set(JsonConstants.CONTENT, content);
        }
        return root;
    }

    private String getTitle(ApplicationPackage applicationPackage) {
        return applicationPackage.getProcessDescription().getProcessDescription().getTitle().getValue();
    }

    private String getProcessURL(ProcessDescription description) {
        return config.getServiceURL().toString() + "/processes/" + description.getId();
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
        List<String> domain = Streams.stream(url.host().split("\\."))
                                     .collect(Collectors.toList());
        Collections.reverse(domain);
        Stream<String> path = Arrays.stream(url.encodedPath().split("/"))
                                    .filter(Predicates.not(String::isEmpty));
        return Stream.concat(domain.stream(), path).collect(joining("."));
    }
}
