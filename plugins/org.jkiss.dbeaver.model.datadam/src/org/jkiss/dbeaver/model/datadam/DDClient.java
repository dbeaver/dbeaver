/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2026 DBeaver Corp and others
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jkiss.dbeaver.model.datadam;

import com.dbeaver.rest.client.AbstractRestClient;
import com.dbeaver.rest.client.interceptor.HttpInterceptor;
import org.jkiss.code.NotNull;

import java.net.http.HttpClient;
import java.util.ArrayList;
import java.util.List;

/** Common HTTP configuration for DataDam desktop clients. */
public abstract class DDClient extends AbstractRestClient {
    protected DDClient(
        @NotNull String url,
        int connectTimeoutMs,
        int readTimeoutMs,
        @NotNull List<HttpInterceptor> interceptors,
        @NotNull HttpClient.Redirect redirectPolicy
    ) {
        super(url, connectTimeoutMs, readTimeoutMs, withUserAgent(interceptors), redirectPolicy);
    }

    @NotNull
    private static List<HttpInterceptor> withUserAgent(@NotNull List<HttpInterceptor> interceptors) {
        List<HttpInterceptor> result = new ArrayList<>(interceptors);
        result.add(new DDUserAgentInterceptor());
        return result;
    }
}
