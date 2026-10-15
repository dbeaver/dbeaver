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
import com.google.gson.JsonParseException;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.datadam.auth.DDCryptoState;
import org.jkiss.dbeaver.model.datadam.auth.DDDesktopSsoSession;
import org.jkiss.utils.HttpConstants;

import java.lang.reflect.Type;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.util.List;
import java.util.Map;

/** Account requests authenticated by the current desktop SSO session. */
public class DDAccountClient extends AbstractRestClient {
    private static final String GRAPHQL_PATH = "graphql";
    private static final String CRYPTO_STATE_QUERY = """
        query GatewayCryptoState {
            gatewayCryptoState {
                accountId cryptoConfigured encryptedBundle generation salt iterations
            }
        }
        """;
    public DDAccountClient(@NotNull URI accountUrl) {
        super(accountUrl.resolve("/").toString(), DEFAULT_CONNECT_TIMEOUT, 10_000, List.of(), HttpClient.Redirect.NEVER);
    }

    @NotNull
    public DDCryptoState getCryptoState() throws DBException {
        CryptoResponse response = executePostRequest(GRAPHQL_PATH, Map.of(), Map.of("query", CRYPTO_STATE_QUERY), CryptoResponse.class);
        if (response == null || response.errors() != null && !response.errors().isEmpty()
            || response.data() == null || response.data().gatewayCryptoState() == null) {
            throw new DBException("Could not read encryption keys from DataDam Account");
        }
        DDCryptoState state = response.data().gatewayCryptoState();
        if (state.accountId() == null || state.accountId().isBlank()) {
            throw new DBException("DataDam Account response has no account ID");
        }
        return state;
    }

    @NotNull
    @Override
    protected <T> T execute(@NotNull HttpRequest.Builder builder, @NotNull Type type) throws DBException {
        builder.setHeader(HttpConstants.HEADER_AUTHORIZATION,
            HttpConstants.BEARER_PREFIX + DDDesktopSsoSession.accessFor(builder.build().uri()));
        return super.execute(builder, type);
    }

    @NotNull
    @Override
    protected DBException mapErrorResponse(int code, @NotNull String message, @NotNull URI uri) {
        return new DBException("DataDam Account request failed (HTTP " + code + ")");
    }

    @Override
    protected void handleRequestException(@NotNull String message, @NotNull Throwable e) throws DBException {
        if (e instanceof JsonParseException) {
            throw new DBException("Invalid DataDam Account response");
        }
        super.handleRequestException(message, e);
    }

    private record CryptoResponse(@Nullable CryptoData data, @Nullable List<Object> errors) {
    }

    private record CryptoData(@Nullable DDCryptoState gatewayCryptoState) {
    }
}
