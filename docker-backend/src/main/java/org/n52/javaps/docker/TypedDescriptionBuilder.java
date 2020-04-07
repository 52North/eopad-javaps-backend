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

/**
 * Factory to create typed descriptions from untyped descriptions.
 *
 * @author Christian Autermann
 */
public interface TypedDescriptionBuilder {
    /**
     * Create a new {@link TypedProcessDescription} from the specified {@link ProcessDescription}.
     *
     * @param processDescription The {@link ProcessDescription}.
     * @return The {@link TypedProcessDescription}
     */
    TypedProcessDescription createDescription(ProcessDescription processDescription);

    /**
     * Create a new {@link TypedProcessOutputDescription} from the specified {@link ProcessOutputDescription}.
     *
     * @param output The {@link ProcessOutputDescription}.
     * @return The {@link TypedProcessOutputDescription}
     */
    default TypedProcessOutputDescription<?> createProcessOutputDescription(ProcessOutputDescription output) {
        if (output.isComplex()) {
            return createComplexOutputDescription(output.asComplex());
        } else if (output.isLiteral()) {
            return createLiteralOutputDescription(output.asLiteral());
        } else if (output.isGroup()) {
            return createGroupOutputDescription(output.asGroup());
        } else if (output.isBoundingBox()) {
            return creatBoundingBoxOutputDescription(output.asBoundingBox());
        } else {
            throw new IllegalArgumentException("unsupported output: " + output);
        }
    }

    /**
     * Create a new {@link TypedProcessInputDescription} from the specified {@link ProcessInputDescription}.
     *
     * @param input The {@link ProcessInputDescription}.
     * @return The {@link TypedProcessInputDescription}
     */
    default TypedProcessInputDescription<?> createProcessInputDescription(ProcessInputDescription input) {
        if (input.isComplex()) {
            return createComplexInputDescription(input.asComplex());
        } else if (input.isLiteral()) {
            return createLiteralInputDescription(input.asLiteral());
        } else if (input.isGroup()) {
            return createGroupInputDescription(input.asGroup());
        } else if (input.isBoundingBox()) {
            return creatBoundingBoxInputDescription(input.asBoundingBox());
        } else {
            throw new IllegalArgumentException("unsupported input: " + input);
        }
    }

    /**
     * Create a new {@link TypedLiteralInputDescription} from the specified {@link LiteralInputDescription}.
     *
     * @param input The {@link LiteralInputDescription}.
     * @return The {@link TypedLiteralInputDescription}
     */
    TypedLiteralInputDescription createLiteralInputDescription(LiteralInputDescription input);

    /**
     * Create a new {@link TypedLiteralOutputDescription} from the specified {@link LiteralOutputDescription}.
     *
     * @param output The {@link LiteralOutputDescription}.
     * @return The {@link TypedLiteralOutputDescription}
     */
    TypedLiteralOutputDescription createLiteralOutputDescription(LiteralOutputDescription output);

    /**
     * Create a new {@link TypedGroupInputDescription} from the specified {@link GroupInputDescription}.
     *
     * @param input The {@link GroupInputDescription}.
     * @return The {@link TypedGroupInputDescription}
     */
    TypedGroupInputDescription createGroupInputDescription(GroupInputDescription input);

    /**
     * Create a new {@link TypedGroupOutputDescription} from the specified {@link GroupOutputDescription}.
     *
     * @param output The {@link GroupOutputDescription}.
     * @return The {@link TypedGroupOutputDescription}
     */
    TypedGroupOutputDescription createGroupOutputDescription(GroupOutputDescription output);

    /**
     * Create a new {@link TypedBoundingBoxInputDescription} from the specified {@link BoundingBoxInputDescription}.
     *
     * @param input The {@link BoundingBoxInputDescription}.
     * @return The {@link TypedBoundingBoxInputDescription}
     */
    TypedBoundingBoxInputDescription creatBoundingBoxInputDescription(BoundingBoxInputDescription input);

    /**
     * Create a new {@link TypedBoundingBoxOutputDescription} from the specified {@link BoundingBoxOutputDescription}.
     *
     * @param output The {@link BoundingBoxOutputDescription}.
     * @return The {@link TypedBoundingBoxOutputDescription}
     */
    TypedBoundingBoxOutputDescription creatBoundingBoxOutputDescription(BoundingBoxOutputDescription output);

    /**
     * Create a new {@link TypedComplexInputDescription} from the specified {@link ComplexInputDescription}.
     *
     * @param input The {@link ComplexInputDescription}.
     * @return The {@link TypedComplexInputDescription}
     */
    TypedComplexInputDescription createComplexInputDescription(ComplexInputDescription input);

    /**
     * Create a new {@link TypedComplexOutputDescription} from the specified {@link ComplexOutputDescription}.
     *
     * @param output The {@link ComplexOutputDescription}.
     * @return The {@link TypedComplexOutputDescription}
     */
    TypedComplexOutputDescription createComplexOutputDescription(ComplexOutputDescription output);
}
