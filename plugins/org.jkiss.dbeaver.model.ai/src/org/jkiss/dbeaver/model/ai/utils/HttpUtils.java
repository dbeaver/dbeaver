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

import org.jkiss.dbeaver.DBException;

import java.net.URI;
import java.net.URISyntaxException;

public final class HttpUtils {

    private HttpUtils() {
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
}
