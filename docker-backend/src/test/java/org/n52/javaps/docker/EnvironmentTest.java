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

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ErrorCollector;

import static org.hamcrest.Matchers.is;

public class EnvironmentTest {

    @Rule
    public final ErrorCollector errors = new ErrorCollector();

    @Test
    public void test_asEnvironmentVariable() {
        errors.checkThat(Environment.getVariableName("asdfASDads"), is("ASDF_ASDADS"));
        errors.checkThat(Environment.getVariableName("asdfAS_Dads"), is("ASDF_AS_DADS"));
        errors.checkThat(Environment.getVariableName("org.n52.routing.ecere"), is("ORG_N52_ROUTING_ECERE"));
    }

}
