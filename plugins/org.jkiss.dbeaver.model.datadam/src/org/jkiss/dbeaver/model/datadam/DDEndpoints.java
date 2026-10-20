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

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;

import java.net.URI;

/**
 * Remote DataDam endpoints used for login, project synchronization and tracking.
 */
public final class DDEndpoints {
    private static final String BASE_DD_HOST = /*<DD-PROD-URL*/"datadam.cloud"/*/>*/;
    private static final String ACCOUNT_URL = "https://account." + BASE_DD_HOST;
    private static final String STORAGE_URL = "https://dbeaver-storage." + BASE_DD_HOST;

    private DDEndpoints() {
    }

    @NotNull
    public static String getAccountBaseUrl() {
        return ACCOUNT_URL;
    }

    @NotNull
    public static String getStorageBaseUrl() {
        return STORAGE_URL;
    }

    public static void requireSecureTransport(@NotNull URI url) throws DBException {
        if (url.getHost() == null || url.getUserInfo() != null
            || !("https".equals(url.getScheme()) || "http".equals(url.getScheme())
                && ("localhost".equals(url.getHost()) || "127.0.0.1".equals(url.getHost())))) {
            throw new DBException("Expected an HTTPS DataDam URL (HTTP is allowed on loopback only)");
        }
    }
}
