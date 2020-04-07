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

/**
 * Docker label names.
 *
 * @author Christian Autermann
 */
public interface Label {
    /**
     * The id of the job.
     */
    String JOB_ID = "org.n52.javaps.job.id";
    /**
     * The start time of the job.
     */
    String JOB_TIME = "org.n52.javaps.job.time";
    /**
     * The process identifier.
     */
    String PROCESS_ID = "org.n52.javaps.process.id";
    /**
     * The title of the process description.
     */
    String PROCESS_TITLE = "org.n52.javaps.process.title";
    /**
     * The abstract of the process description.
     */
    String PROCESS_ABSTRACT = "org.n52.javaps.process.description";
    /**
     * The process version.
     */
    String PROCESS_VERSION = "org.n52.javaps.process.version";
    /**
     * The javaPS version.
     */
    String VERSION = "org.n52.javaps.version";
}
