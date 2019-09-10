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
package org.n52.javaps.docker;

import org.n52.javaps.description.TypedBoundingBoxInputDescription;
import org.n52.javaps.description.TypedBoundingBoxOutputDescription;
import org.n52.javaps.description.TypedComplexInputDescription;
import org.n52.javaps.description.TypedComplexOutputDescription;
import org.n52.javaps.description.TypedGroupInputDescription;
import org.n52.javaps.description.TypedGroupOutputDescription;
import org.n52.javaps.description.TypedLiteralInputDescription;
import org.n52.javaps.description.TypedLiteralOutputDescription;
import org.n52.javaps.description.TypedProcessDescription;
import org.n52.javaps.description.TypedProcessInputDescription;
import org.n52.javaps.description.TypedProcessOutputDescription;
import org.n52.shetland.ogc.wps.description.BoundingBoxInputDescription;
import org.n52.shetland.ogc.wps.description.BoundingBoxOutputDescription;
import org.n52.shetland.ogc.wps.description.ComplexInputDescription;
import org.n52.shetland.ogc.wps.description.ComplexOutputDescription;
import org.n52.shetland.ogc.wps.description.GroupInputDescription;
import org.n52.shetland.ogc.wps.description.GroupOutputDescription;
import org.n52.shetland.ogc.wps.description.LiteralInputDescription;
import org.n52.shetland.ogc.wps.description.LiteralOutputDescription;
import org.n52.shetland.ogc.wps.description.ProcessDescription;
import org.n52.shetland.ogc.wps.description.ProcessInputDescription;
import org.n52.shetland.ogc.wps.description.ProcessOutputDescription;

public interface TypedDescriptionBuilder {
    TypedProcessDescription createDescription(ProcessDescription processDescription);

    TypedProcessOutputDescription<?> createProcessOutputDescription(ProcessOutputDescription output);

    TypedProcessInputDescription<?> createProcessInputDescription(ProcessInputDescription input);

    TypedLiteralInputDescription createLiteralInputDescription(LiteralInputDescription input);

    TypedLiteralOutputDescription createLiteralOutputDescription(LiteralOutputDescription output);

    TypedGroupInputDescription createGroupInputDescription(GroupInputDescription input);

    TypedGroupOutputDescription createGroupOutputDescription(GroupOutputDescription output);

    TypedBoundingBoxInputDescription creatBoundingBoxInputDescription(BoundingBoxInputDescription input);

    TypedBoundingBoxOutputDescription creatBoundingBoxOutputDescription(BoundingBoxOutputDescription output);

    TypedComplexInputDescription createComplexInputDescription(ComplexInputDescription input);

    TypedComplexOutputDescription createComplexOutputDescription(ComplexOutputDescription output);
}
