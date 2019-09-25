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
package org.n52.javaps.eopad.http;

import okhttp3.Authenticator;
import okhttp3.Credentials;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.Route;
import org.n52.janmayen.http.HTTPHeaders;
import org.springframework.http.HttpStatus;

public class BasicAuthenticator implements Authenticator {
    private final String username;
    private final String password;

    public BasicAuthenticator(String username, String password) {
        this.username = username;
        this.password = password;
    }

    @Override
    public Request authenticate(Route route, Response response) {
        if (response.request().header(HTTPHeaders.AUTHORIZATION) != null ||
            HttpStatus.resolve(response.code()) == HttpStatus.FORBIDDEN ||
            responseCount(response) >= 3) {
            return null;
        }
        return response.request().newBuilder()
                       .header(HTTPHeaders.AUTHORIZATION, Credentials.basic(username, password))
                       .build();
    }

    private int responseCount(Response response) {
        Response r = response;
        int result = 1;
        while ((r = r.priorResponse()) != null) {
            result++;
        }
        return result;

    }
}
