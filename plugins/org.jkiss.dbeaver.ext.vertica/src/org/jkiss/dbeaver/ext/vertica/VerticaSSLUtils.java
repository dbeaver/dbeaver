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
package org.jkiss.dbeaver.ext.vertica;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.utils.HttpUtils;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;

public final class VerticaSSLUtils {

    public static final String EMPTY_KEYSTORE_PASSWORD = "";

    private static final String KEYSTORE_TYPE = "JKS";
    @Nullable
    private static Path emptyKeyStore;

    private VerticaSSLUtils() {
    }

    public static boolean needsEmptyKeyStore(
        @NotNull Properties properties,
        @Nullable String url,
        @Nullable String systemKeyStore
    ) {
        Map<String, String> settings = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        for (String name : properties.stringPropertyNames()) {
            settings.put(name, properties.getProperty(name));
        }
        if (url != null && url.indexOf('?') >= 0) {
            // Vertica URL parameters override the corresponding driver properties.
            try {
                int queryStart = url.indexOf('?') + 1;
                int fragmentStart = url.indexOf('#', queryStart);
                settings.putAll(HttpUtils.parseQuery(url.substring(queryStart, fragmentStart < 0 ? url.length() : fragmentStart)));
            } catch (IllegalArgumentException e) {
                // Leave malformed URLs to the driver rather than changing their configuration.
                return false;
            }
        }
        return VerticaConstants.TLS_MODE_REQUIRE.equalsIgnoreCase(settings.get(VerticaConstants.PROP_TLS_MODE))
            && !settings.containsKey(VerticaConstants.PROP_KEYSTORE_PATH)
            && settings.getOrDefault(VerticaConstants.PROP_KEYSTORE_PASSWORD, "").isEmpty()
            && settings.getOrDefault(VerticaConstants.PROP_SSL_SOCKET_FACTORY, "").isEmpty()
            && (systemKeyStore == null || systemKeyStore.isEmpty());
    }

    @NotNull
    public static synchronized Path getEmptyKeyStore(@NotNull Path tempFolder) throws IOException, GeneralSecurityException {
        if (emptyKeyStore == null || !Files.exists(emptyKeyStore)) {
            KeyStore keyStore = KeyStore.getInstance(KEYSTORE_TYPE);
            char[] password = EMPTY_KEYSTORE_PASSWORD.toCharArray();
            keyStore.load(null, password);
            Path path = Files.createTempFile(tempFolder, "vertica-empty-keystore-", ".jks");
            try (OutputStream output = Files.newOutputStream(path)) {
                keyStore.store(output, password);
            } catch (IOException | GeneralSecurityException e) {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException cleanupError) {
                    e.addSuppressed(cleanupError);
                }
                throw e;
            }
            path.toFile().deleteOnExit();
            emptyKeyStore = path;
        }
        return emptyKeyStore;
    }
}
