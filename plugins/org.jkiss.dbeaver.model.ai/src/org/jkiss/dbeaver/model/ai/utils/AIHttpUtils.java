/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2025 DBeaver Corp and others
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
package org.jkiss.dbeaver.model.ai.utils;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.ai.engine.TooManyRequestsException;
import org.jkiss.utils.HttpConstants;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpResponse;

public final class AIHttpUtils {

    private AIHttpUtils() {
    }

    /**
     * Resolves URI from base and paths
     */
    public static URI resolve(String base, String... paths) throws DBException {
        try {
            URI uri = new URI(base);
            for (String path : paths) {
                uri = uri.resolve(path);
            }
            return uri;
        } catch (URISyntaxException e) {
            throw new DBException("Incorrect URI", e);
        }
    }

    /**
     * <br>
     * HTTP errors:
     * <li>400 - invalid_request_error: There was an issue with the format or
     * content of your request. We may also use this error type for other 4XX status codes not listed below.</li>
     * <li>401 - authentication_error: There’s an issue with your API key.</li>
     * <li>403 - permission_error: Your API key does not have permission to use the specified resource.</li>
     * <li>404 - not_found_error: The requested resource was not found.</li>
     * <li>413 - request_too_large: Request exceeds the
     * maximum allowed number of bytes. The maximum request size is 32 MB for standard API endpoints.</li>
     * <li>429 - rate_limit_error: Your account has hit a rate limit.</li>
     * <li>500 - api_error: An unexpected error has occurred internal to AI’s systems.</li>
     * <li>529 - overloaded_error: Unofficial, site is overloaded</li>
     *
     * @param response http response
     * @return mapped exception
     */
    @NotNull
    public static DBException mapHttpError(@NotNull HttpResponse<String> response) {
        return switch (response.statusCode()) {
            case HttpConstants.CODE_BAD_REQUEST -> new DBException("Invalid request: " + response.body());
            case HttpConstants.CODE_UNAUTHORIZED -> new DBException("Authentication error: " + response.body());
            case HttpConstants.CODE_FORBIDDEN -> new DBException("Permission error: " + response.body());
            case HttpConstants.CODE_NOT_FOUND -> new DBException("Not found: " + response.body());
            case HttpConstants.CODE_PAYLOAD_TOO_LARGE -> new DBException("Request too large: " + response.body());
            case HttpConstants.CODE_TOO_MANY_REQUESTS -> new TooManyRequestsException("Too many requests: " + response.body());
            case HttpConstants.CODE_INTERNAL_SERVER_ERROR -> new DBException("Internal server error: " + response.body());
            case 529 -> new DBException("Service overloaded: " + response.body());
            default -> new AIHttpTransportException(response.statusCode(), response.body());
        };
    }

}
