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
package org.jkiss.dbeaver.model.datadam.auth;

import com.dbeaver.datadam.sso.api.DDSsoConstants;
import com.dbeaver.datadam.sso.api.model.DDSsoDesktopConfigResponse;
import com.dbeaver.datadam.sso.api.model.DDSsoDesktopTokenResponse;
import com.dbeaver.rest.client.AbstractRestClient;
import com.dbeaver.rest.client.MediaType;
import com.google.gson.JsonParseException;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.datadam.DDEndpoints;
import org.jkiss.utils.CommonUtils;
import org.jkiss.utils.oauth.OAuthConstants;

import java.net.URI;
import java.net.http.HttpClient;
import java.util.List;
import java.util.Map;

/** HTTP operations for a public desktop client. Credentials are never attached to a browser URL. */
public class DDDesktopSsoClient extends AbstractRestClient implements AutoCloseable {
    public DDDesktopSsoClient(@NotNull URI issuer) throws DBException {
        super(CommonUtils.removeTrailingSlash(issuer.toString()), DEFAULT_CONNECT_TIMEOUT, 10_000, List.of(), HttpClient.Redirect.NEVER);
        requireSecureServer(issuer);
    }

    @NotNull
    public URI discover() throws DBException {
        try {
            DDSsoDesktopConfigResponse config = executeGetRequest("sso/desktop/config", DDSsoDesktopConfigResponse.class);
            if (config == null || config.issuer() == null) {
                throw new DBException("Invalid desktop SSO configuration");
            }
            URI issuer = URI.create(config.issuer());
            requireSecureServer(issuer);
            return issuer;
        } catch (IllegalArgumentException e) {
            throw new DBException("Invalid desktop SSO configuration", e);
        }
    }

    @NotNull
    URI authorizationUrl(@NotNull String redirectUri, @NotNull String state, @NotNull String challenge) throws DBException {
        return buildUri(CommonUtils.removeLeadingSlash(DDSsoConstants.AUTHORIZE_PATH), Map.of(
            OAuthConstants.AUTH_PROP_CLIENT_ID, DDSsoConstants.DESKTOP_CLIENT_ID,
            OAuthConstants.PARAM_REDIRECT_URI, redirectUri,
            OAuthConstants.PARAM_STATE, state,
            OAuthConstants.PARAM_CODE_CHALLENGE, challenge,
            OAuthConstants.PARAM_CODE_CHALLENGE_METHOD, DDSsoConstants.PKCE_METHOD_S256));
    }

    @NotNull
    DDSsoDesktopTokenResponse exchange(@NotNull String code, @NotNull String verifier, @NotNull String redirectUri) throws DBException {
        DDSsoDesktopTokenResponse tokens = executePostRequest(CommonUtils.removeLeadingSlash(DDSsoConstants.TOKEN_PATH), Map.of(), Map.of(
            OAuthConstants.PARAM_GRANT_TYPE, OAuthConstants.GRANT_TYPE_AUTH_CODE,
            OAuthConstants.PARAM_CODE, code,
            OAuthConstants.PARAM_CODE_VERIFIER, verifier,
            OAuthConstants.AUTH_PROP_CLIENT_ID, DDSsoConstants.DESKTOP_CLIENT_ID,
                OAuthConstants.PARAM_REDIRECT_URI, redirectUri
            ), MediaType.FORM_URLENCODED, DDSsoDesktopTokenResponse.class
        );
        if (tokens == null || !DDSsoConstants.TOKEN_TYPE_BEARER.equals(tokens.tokenType()) || tokens.accessToken().isBlank()
            || tokens.idToken().isBlank() || tokens.expiresIn() <= 0) {
            throw new DBException("Invalid desktop SSO token response");
        }
        return tokens;
    }

    @NotNull
    @Override
    protected DBException mapErrorResponse(int code, @NotNull String message, @NotNull URI uri) {
        return new DBException("Desktop SSO request failed (HTTP " + code + ")");
    }

    @Override
    protected void handleRequestException(@NotNull String message, @NotNull Throwable e) throws DBException {
        if (e instanceof JsonParseException) {
            throw new DBException("Invalid desktop SSO response");
        }
        super.handleRequestException(message, e);
    }

    private void requireSecureServer(@NotNull URI url) throws DBException {
        DDEndpoints.requireSecureTransport(url);
        if (url.getRawQuery() != null || url.getRawFragment() != null) {
            throw new DBException("Expected a DataDam URL without query or fragment");
        }
    }

    @Override
    public void close() {
        getHttpClient().shutdownNow();
    }
}
