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

import com.dbeaver.rest.client.AbstractGraphQlClient;
import com.dbeaver.rest.client.interceptor.HttpInterceptor;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.datadam.auth.desktop.DDDesktopAccessService;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.utils.HttpConstants;

import java.lang.reflect.Type;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.util.List;

/** Common HTTP configuration for DataDam desktop clients. */
public abstract class DDClient extends AbstractGraphQlClient {
    private final Log log = Log.getLog(getClass());
    protected final DDDesktopAccessService accessService;

    protected DDClient(
        @NotNull String url,
        int connectTimeoutMs,
        int readTimeoutMs,
        @NotNull List<HttpInterceptor> interceptors,
        @NotNull HttpClient.Redirect redirectPolicy,
        @NotNull DDDesktopAccessService accessService
    ) {
        super(url, connectTimeoutMs, readTimeoutMs, interceptors, redirectPolicy);
        this.accessService = accessService;
    }

    @NotNull
    @Override
    protected String getUserAgent() {
        return GeneralUtils.getProductUserAgent();
    }

    @Override
    protected void logDebug(@NotNull String message) {
        log.debug(message);
    }

    @NotNull
    @Override
    protected <T> T execute(@NotNull HttpRequest.Builder builder, @NotNull Type type) throws DBException {
        URI uri = builder.build().uri();
        DDEndpoints.requireSecureTransport(uri);
        builder.setHeader(HttpConstants.HEADER_AUTHORIZATION,
            HttpConstants.BEARER_PREFIX + accessService.getAccessTokenForResource(uri));
        return super.execute(builder, type);
    }
}
