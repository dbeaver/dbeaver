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
import com.dbeaver.datadam.sso.api.model.DDSsoDesktopTokenResponse;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.utils.CommonUtils;
import org.jkiss.utils.oauth.OAuthConstants;
import org.jkiss.utils.oauth.OAuthUtils;
import org.jkiss.utils.oauth.code.IOAuthCodeResponseHandler;
import org.jkiss.utils.oauth.code.OAuthCodeHandler;
import org.jkiss.utils.oauth.code.OAuthCodeResponseHandler;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.time.Duration;
import java.util.Map;
import java.util.Objects;

/** DataDam adapter for the shared browser authorization flow and the desktop REST token client. */
public class DDDesktopSsoLogin {
    private static final Duration TIMEOUT = Duration.ofMinutes(15);
    private static final String CALLBACK_HOST = "127.0.0.1";
    private static final String CALLBACK_PATH = URI.create(DDSsoConstants.DESKTOP_CALLBACK_TEMPLATE).getPath();
    private static final String DONE_PAGE = """
        <html><body><h3>You are logged in</h3><p>Return to DBeaver.</p></body></html>
        """;

    @NotNull
    public DDSsoDesktopTokenResponse login(
        @NotNull URI issuer,
        @NotNull DBRProgressMonitor monitor,
        @NotNull BrowserOpener browser
    ) throws DBException, InterruptedException {
        try (DDDesktopSsoClient client = new DDDesktopSsoClient(issuer)) {
            String baseUrl = CommonUtils.removeTrailingSlash(issuer.toString());
            OAuthCodeHandler handler = new OAuthCodeHandler(
                DDSsoConstants.DESKTOP_CLIENT_ID, null,
                baseUrl + DDSsoConstants.AUTHORIZE_PATH, baseUrl + DDSsoConstants.TOKEN_PATH,
                CALLBACK_PATH, "http://" + CALLBACK_HOST + ":0" + CALLBACK_PATH, 0
            ) {
                @NotNull
                @Override
                protected IOAuthCodeResponseHandler createCodeResponseHandler() {
                    return new OAuthCodeResponseHandler(
                        new InetSocketAddress(CALLBACK_HOST, 0), CALLBACK_PATH, Objects.requireNonNull(state), DONE_PAGE
                    ) {
                        @Override
                        protected boolean isValidCallbackParameters(@NotNull Map<String, String> parameters) {
                            return parameters.size() == 2 && (parameters.containsKey(OAuthConstants.PARAM_ERROR)
                                || DDSsoConstants.BASE64URL_256_PATTERN.matcher(
                                    parameters.getOrDefault(OAuthConstants.PARAM_CODE, "")).matches());
                        }
                    };
                }

                @NotNull
                @Override
                protected String buildAuthUrl() throws IOException {
                    try {
                        return client.authorizationUrl(
                            getRedirectUri(), Objects.requireNonNull(state), Objects.requireNonNull(codeChallenge)).toString();
                    } catch (DBException e) {
                        throw new IOException(e);
                    }
                }

                @Override
                protected void createBrowser(@NotNull String url) throws IOException {
                    try {
                        browser.open(url);
                    } catch (DBException e) {
                        throw new IOException(e);
                    }
                }
            };
            handler.setTimeout((int) TIMEOUT.toSeconds());
            return handler.authorize((code, verifier, redirectUri) -> {
                try {
                    return client.exchange(code, verifier, redirectUri);
                } catch (DBException e) {
                    throw new IOException(e);
                }
            }, monitor::isCanceled);
        } catch (IOException e) {
            if (e.getCause() instanceof DBException exception) {
                throw exception;
            }
            throw new DBException("DataDam sign-in failed", e);
        }
    }

    static void openBrowser(@NotNull String url) throws DBException {
        try {
            OAuthUtils.openBrowser(url);
        } catch (IOException e) {
            throw new DBException("Cannot open web browser", e);
        }
    }

    @FunctionalInterface
    public interface BrowserOpener {
        void open(@NotNull String url) throws DBException;
    }
}
