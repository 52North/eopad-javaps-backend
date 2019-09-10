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

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OkHttpConfiguration {
    private static final Logger LOG = LoggerFactory.getLogger("okhttp3");

    @Bean
    public OkHttpClient client() {
        return new OkHttpClient.Builder().followRedirects(true).followSslRedirects(true).addInterceptor(chain -> {
            Request request = chain.request();
            Response response = chain.proceed(request);
            LOG.info("{} {} {}", request.method(), request.url(), response.code());
            return response;
        }).build();
    }

}
