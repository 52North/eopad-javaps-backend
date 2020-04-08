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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import okhttp3.HttpUrl;
import org.n52.faroe.annotation.Configurable;
import org.n52.janmayen.Json;
import org.n52.janmayen.http.HTTPMethods;
import org.n52.janmayen.i18n.LocalizedString;
import org.n52.javaps.engine.Engine;
import org.n52.shetland.ogc.ows.OwsAddress;
import org.n52.shetland.ogc.ows.OwsCode;
import org.n52.shetland.ogc.ows.OwsContact;
import org.n52.shetland.ogc.ows.OwsKeyword;
import org.n52.shetland.ogc.ows.OwsLanguageString;
import org.n52.shetland.ogc.ows.OwsResponsibleParty;
import org.n52.shetland.ogc.ows.OwsServiceIdentification;
import org.n52.shetland.ogc.ows.OwsServiceProvider;
import org.n52.shetland.ogc.wps.ProcessOffering;
import org.n52.shetland.ogc.wps.ap.ApplicationPackage;
import org.n52.shetland.ogc.wps.description.ProcessDescription;
import org.n52.shetland.w3c.xlink.Link;
import org.n52.svalbard.coding.json.JSONConstants;
import org.n52.svalbard.encode.Encoder;
import org.n52.svalbard.encode.EncoderRepository;
import org.n52.svalbard.encode.exception.EncodingException;
import org.n52.svalbard.encode.json.JSONEncoderKey;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toSet;

@Component
@Configurable
public class CatalogEncoderImpl implements CatalogEncoder {
    private static final String DATA_HREF = "data:";
    private static final String PROCESSES_PATH = "processes/";
    private static final String CONFORMANCE_PATH = "conformance/";
    private static final String API_PATH = "api/";

    private EncoderRepository encoderRepository;
    private Engine engine;

    @Autowired
    public CatalogEncoderImpl(Engine engine, EncoderRepository encoderRepository) {
        this.encoderRepository = Objects.requireNonNull(encoderRepository);
        this.engine = Objects.requireNonNull(engine);
    }

    @Override
    public ObjectNode createProcessInsertion(ApplicationPackage applicationPackage, CatalogConfiguration config)
            throws EncodingException {
        JsonNode content = getApplicationPackageEncoder().encode(applicationPackage);

        ObjectNode root = Json.nodeFactory().objectNode()
                              .put(JsonConstants.TYPE, JsonConstants.FEATURE)
                              .put(JsonConstants.ID, getIdentifier(applicationPackage))
                              .putNull(JsonConstants.GEOMETRY);

        ObjectNode properties = root.putObject(JsonConstants.PROPERTIES);
        properties.put(JsonConstants.IDENTIFIER, getIdentifier(applicationPackage));
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
                                         .put(JsonConstants.METHOD, HTTPMethods.POST)
                                         .put(JsonConstants.TYPE, MediaTypes.APPLICATION_JSON)
                                         .put(JsonConstants.HREF, DATA_HREF);
        ObjectNode request = operation.putObject(JsonConstants.REQUEST);
        request.put(JsonConstants.TYPE, MediaTypes.APPLICATION_JSON)
               .set(JsonConstants.CONTENT, content);
        return root;
    }

    @Override
    public ObjectNode createServiceInsertion(CatalogConfiguration config) throws EncodingException {
        ObjectNode root = Json.nodeFactory().objectNode()
                              .put(JsonConstants.TYPE, JsonConstants.FEATURE)
                              .put(JsonConstants.ID, config.getServiceURL().toString())
                              .putNull(JsonConstants.GEOMETRY);

        ObjectNode properties = root.putObject(JsonConstants.PROPERTIES);
        properties.put(JsonConstants.IDENTIFIER, config.getServiceIdentifier());
        properties.put(JsonConstants.KIND, Kinds.SERVICE_TYPE);
        properties.put(JsonConstants.TITLE, getServiceTitle(config));
        properties.put(JsonConstants.UPDATED, OffsetDateTime.now().toString());
        ObjectNode links = properties.putObject(JsonConstants.LINKS);
        ArrayNode profiles = links.putArray(JsonConstants.PROFILES);
        profiles.addObject().put(JsonConstants.HREF, Specifications.OWC_GEOJSON_CORE);
        profiles.addObject().put(JsonConstants.HREF, Specifications.EOPAD_GEOJSON_CORE);

        ArrayNode hosts = links.putArray(JsonConstants.HOSTS);
        config.getApplicationPackages().forEach(ap -> hosts.addObject()
                                                           .put(JsonConstants.HREF, config.getCatalog().getURL(ap))
                                                           .put(JsonConstants.TYPE, MediaTypes.APPLICATION_GEO_JSON)
                                                           .put(JsonConstants.TITLE, getTitle(ap)));

        properties.putArray(JsonConstants.ENDPOINT_DESCRIPTION).add(config.getServiceURL().toString());

        OwsServiceIdentification serviceIdentification = config.getServiceIdentification();

        serviceIdentification.getTitle()
                             .flatMap(title -> title.getLocalization(config.getDefaultLocale()))
                             .map(LocalizedString::getText)
                             .ifPresent(title -> properties.put(JSONConstants.TITLE, title));

        serviceIdentification.getProfiles().stream().map(java.net.URI::toString)
                             .forEach(profile -> profiles.addObject().put(JsonConstants.HREF, profile));

        ArrayNode keywords = properties.putArray(JsonConstants.KEYWORD);
        Stream.concat(serviceIdentification.getKeywords().stream(),
                      engine.getProcessDescriptions().stream()
                            .map(ProcessDescription::getKeywords)
                            .flatMap(Set::stream))
              .map(OwsKeyword::getKeyword)
              .map(OwsLanguageString::getValue)
              .forEach(keywords::add);

        serviceIdentification.getAbstract()
                             .flatMap(description -> description.getLocalization(config.getDefaultLocale()))
                             .map(LocalizedString::getText)
                             .ifPresent(description -> properties.put(JsonConstants.ABSTRACT, description));

        OwsServiceProvider serviceProvider = config.getServiceProvider();

        OwsResponsibleParty serviceContact = serviceProvider.getServiceContact();

        ArrayNode contactPoint = properties.putArray(JsonConstants.CONTACT_POINT);
        ObjectNode organizationContactPoint = contactPoint.addObject();
        organizationContactPoint.put(JsonConstants.TYPE, ContactPointType.ORGANIZATION_TYPE)
                                .put(JsonConstants.NAME, serviceProvider.getProviderName());
        serviceProvider.getProviderSite()
                       .flatMap(Link::getHref)
                       .map(java.net.URI::toString)
                       .ifPresent(uri -> organizationContactPoint.put(JsonConstants.URI, uri));

        serviceContact.getIndividualName().map(individualName -> {
            ObjectNode individualContactPoint = Json.nodeFactory().objectNode();
            individualContactPoint.put(JsonConstants.TYPE, ContactPointType.INDIVIDUAL_TYPE)
                                  .put(JsonConstants.NAME, individualName);
            serviceContact.getContactInfo()
                          .flatMap(OwsContact::getAddress)
                          .map(OwsAddress::getElectronicMailAddress)
                          .filter(list -> !list.isEmpty())
                          .map(List::iterator)
                          .map(Iterator::next)
                          .ifPresent(mail -> individualContactPoint.put(JsonConstants.EMAIL, mail));
            serviceContact.getContactInfo()
                          .flatMap(OwsContact::getOnlineResource)
                          .flatMap(Link::getHref)
                          .map(java.net.URI::toString)
                          .ifPresent(uri -> individualContactPoint.put(JsonConstants.URI, uri));
            return individualContactPoint;
        }).ifPresent(contactPoint::add);

        ArrayNode offerings = properties.putArray(JsonConstants.OFFERINGS);

        ObjectNode offering = offerings.addObject()
                                       .put(JsonConstants.CODE, Specifications.EOPAD_GEOJSON_OGC_PROCESSES_API);
        ArrayNode operations = offering.putArray(JsonConstants.OPERATIONS);
        operations.addObject()
                  .put(JsonConstants.CODE, Operations.LANDING_PAGE)
                  .put(JsonConstants.METHOD, HTTPMethods.GET)
                  .put(JsonConstants.HREF, config.getServiceURL().toString())
                  .put(JsonConstants.TYPE, MediaTypes.APPLICATION_JSON);

        operations.addObject()
                  .put(JsonConstants.CODE, Operations.SERVICE)
                  .put(JsonConstants.METHOD, HTTPMethods.GET)
                  .put(JsonConstants.HREF, getRootUrl(config, API_PATH))
                  .put(JsonConstants.TYPE, MediaTypes.APPLICATION_OPENAPI_JSON_VERSION_3_0);

        operations.addObject()
                  .put(JsonConstants.CODE, Operations.CONFORMANCE)
                  .put(JsonConstants.METHOD, HTTPMethods.GET)
                  .put(JsonConstants.HREF, getRootUrl(config, CONFORMANCE_PATH))
                  .put(JsonConstants.TYPE, MediaTypes.APPLICATION_JSON);

        operations.addObject()
                  .put(JsonConstants.CODE, Operations.PROCESSES)
                  .put(JsonConstants.METHOD, HTTPMethods.GET)
                  .put(JsonConstants.HREF, getRootUrl(config, PROCESSES_PATH))
                  .put(JsonConstants.TYPE, MediaTypes.APPLICATION_JSON);

        operations.addObject()
                  .put(JsonConstants.CODE, Operations.DEPLOY_PROCESS)
                  .put(JsonConstants.METHOD, HTTPMethods.POST)
                  .put(JsonConstants.HREF, getRootUrl(config, PROCESSES_PATH))
                  .put(JsonConstants.TYPE, MediaTypes.APPLICATION_JSON)
                  .putObject(JsonConstants.REQUEST).put(JsonConstants.TYPE, MediaTypes.APPLICATION_JSON);

        Set<OwsCode> transactionalIdentifiers = config.getApplicationPackages()
                                                      .map(ApplicationPackage::getProcessDescription)
                                                      .map(ProcessOffering::getProcessDescription)
                                                      .map(ProcessDescription::getId)
                                                      .collect(toSet());

        for (ProcessDescription description : engine.getProcessDescriptions()) {
            JsonNode content = getProcessOfferingEncoder().encode(new ProcessOffering(description));
            String processUrl = config.getProcessUrl(description).toString();

            operations.addObject()
                      .put(JsonConstants.CODE, Operations.DESCRIBE_PROCESS)
                      .put(JsonConstants.METHOD, HTTPMethods.GET)
                      .put(JsonConstants.HREF, processUrl)
                      .put(JsonConstants.TYPE, MediaTypes.APPLICATION_JSON)
                      .putObject(JsonConstants.RESULT)
                      .put(JsonConstants.TYPE, MediaTypes.APPLICATION_JSON)
                      .set(JsonConstants.CONTENT, content);
            operations.addObject()
                      .put(JsonConstants.CODE, Operations.EXECUTE_PROCESS)
                      .put(JsonConstants.METHOD, HTTPMethods.POST)
                      .put(JsonConstants.HREF, config.getExecuteUrl(description).toString())
                      .put(JsonConstants.TYPE, MediaTypes.APPLICATION_JSON)
                      .putObject(JsonConstants.REQUEST).put(JsonConstants.TYPE, MediaTypes.APPLICATION_JSON);

            if (transactionalIdentifiers.contains(description.getId())) {
                operations.addObject()
                          .put(JsonConstants.CODE, Operations.DELETE_PROCESS)
                          .put(JsonConstants.METHOD, HTTPMethods.DELETE)
                          .put(JsonConstants.HREF, processUrl);
                operations.addObject()
                          .put(JsonConstants.CODE, Operations.UPDATE_PROCESS)
                          .put(JsonConstants.METHOD, HTTPMethods.PUT)
                          .put(JsonConstants.HREF, processUrl)
                          .put(JsonConstants.TYPE, MediaTypes.APPLICATION_JSON)
                          .putObject(JsonConstants.REQUEST).put(JsonConstants.TYPE, MediaTypes.APPLICATION_JSON);
            }
        }
        return root;
    }

    private String getRootUrl(CatalogConfiguration config, String path) {
        HttpUrl url = config.getServiceURL().resolve(path);
        if (url == null) {
            throw new IllegalArgumentException();
        }
        return url.toString();
    }

    private String getServiceTitle(CatalogConfiguration config) {
        // TODO get this from the OwsServiceIdentificationFactory
        return String.format("Processing API at %s", config.getServiceURL().host());
    }

    private String getTitle(ApplicationPackage applicationPackage) {
        return applicationPackage.getProcessDescription().getProcessDescription().getTitle().getValue();
    }

    private String getIdentifier(ApplicationPackage applicationPackage) {
        return applicationPackage.getProcessDescription().getProcessDescription().getId().getValue();
    }

    private Encoder<JsonNode, ProcessOffering> getProcessOfferingEncoder() {
        return encoderRepository
                       .<JsonNode, ProcessOffering>tryGetEncoder(new JSONEncoderKey(ProcessOffering.class))
                       .orElseThrow(() -> new RuntimeException("no process offering encoder found"));
    }

    private Encoder<JsonNode, ApplicationPackage> getApplicationPackageEncoder() {
        return encoderRepository
                       .<JsonNode, ApplicationPackage>tryGetEncoder(new JSONEncoderKey(ApplicationPackage.class))
                       .orElseThrow(() -> new RuntimeException("no application package encoder found"));
    }
}
