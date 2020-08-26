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

import okhttp3.HttpUrl;
import org.n52.shetland.ogc.ows.OwsCode;
import org.n52.shetland.ogc.ows.OwsServiceIdentification;
import org.n52.shetland.ogc.ows.OwsServiceProvider;
import org.n52.shetland.ogc.wps.ProcessOffering;
import org.n52.shetland.ogc.wps.ap.ApplicationPackage;
import org.n52.shetland.ogc.wps.description.ProcessDescription;

import java.util.Locale;
import java.util.stream.Stream;

public interface CatalogConfiguration {
    Catalog getCatalog();

    HttpUrl getServiceURL();

    HttpUrl getExecuteUrl(String value);

    default HttpUrl getExecuteUrl(OwsCode id) {
        return getExecuteUrl(id.getValue());
    }

    default HttpUrl getExecuteUrl(ProcessDescription description) {
        return getExecuteUrl(description.getId());
    }

    HttpUrl getProcessUrl(String id);

    default HttpUrl getProcessUrl(OwsCode id) {
        return getProcessUrl(id.getValue());
    }

    default HttpUrl getProcessUrl(ProcessDescription description) {
        return getProcessUrl(description.getId());
    }

    default HttpUrl getProcessUrl(ProcessOffering offering) {
        return getProcessUrl(offering.getProcessDescription());
    }

    default HttpUrl getProcessUrl(ApplicationPackage applicationPackage) {
        return getProcessUrl(applicationPackage.getProcessDescription());
    }

    Stream<ApplicationPackage> getApplicationPackages();

    String getServiceIdentifier();

    OwsServiceIdentification getServiceIdentification();

    OwsServiceProvider getServiceProvider();

    Locale getDefaultLocale();
}
