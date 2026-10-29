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
package org.jkiss.dbeaver.ext.postgresql.model.net;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.postgresql.PostgreConstants;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.impl.net.SSLHandlerTrustStoreImpl;
import org.jkiss.dbeaver.model.net.DBWHandlerConfiguration;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.utils.CommonUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Prepares PostgreSQL client keys for pgjdbc and owns their per-data-source conversion cache.
 * Temporary files are registered with the data source so they remain available during SSL handshakes.
 */
public class PostgreSSLClientKeyManager {
    private static final Log log = Log.getLog(PostgreSSLClientKeyManager.class);

    private final Object lock = new Object();
    private final Consumer<Path> trackTempFile;
    private byte[] convertedKeyDigest;
    private Path convertedKeyFile;

    public PostgreSSLClientKeyManager(@NotNull Consumer<Path> trackTempFile) {
        this.trackTempFile = trackTempFile;
    }

    public void prepareConnectionProperties(
        @NotNull DBRProgressMonitor monitor,
        @NotNull Map<String, String> properties
    ) throws IOException {
        String keyFile = properties.get("sslkey");
        if (keyFile != null && CommonUtils.isEmpty(properties.get("sslfactory"))
            && !"disable".equals(properties.get("sslmode"))) {
            Path convertedKey = this.prepareKeyFileIfNeeded(monitor, Path.of(keyFile));
            if (convertedKey != null) {
                properties.put("sslkey", convertedKey.toString());
            }
        }
    }

    @Nullable
    public Path prepareKeyFileIfNeeded(@NotNull DBRProgressMonitor monitor, @NotNull Path keyFile) throws IOException {
        SSLHandlerTrustStoreImpl.PemPrivateKeyFormat format = SSLHandlerTrustStoreImpl.detectPemPrivateKeyFormat(keyFile);
        if (format == SSLHandlerTrustStoreImpl.PemPrivateKeyFormat.OTHER) {
            // Only the two identified PEM formats need preparation; leave other keys to pgjdbc unchanged.
            return null;
        }
        byte[] key = Files.readAllBytes(keyFile);
        final byte[] digest;
        try {
            digest = MessageDigest.getInstance("SHA-256").digest(key);
        } catch (NoSuchAlgorithmException e) {
            throw new IOException("SHA-256 is unavailable", e);
        }
        synchronized (this.lock) {
            if (this.convertedKeyFile != null && Files.isRegularFile(this.convertedKeyFile)
                && Arrays.equals(this.convertedKeyDigest, digest)) {
                // Reuse the unchanged key without overwriting a file another handshake may still read.
                return this.convertedKeyFile;
            }
            Path converted = Files.createTempFile(DBWorkbench.getPlatform().getTempFolder(monitor, "ssl"), "private-", ".pk8");
            try {
                if (format == SSLHandlerTrustStoreImpl.PemPrivateKeyFormat.RSA_PKCS1) {
                    // pgjdbc cannot read RSA/PKCS#1 PEM keys, so convert this one to supported PKCS#8 DER.
                    SSLHandlerTrustStoreImpl.savePkcs1PrivateKeyAsPkcs8File(key, converted);
                } else {
                    // Remove only the PEM envelope; keep the PKCS#8 encryption for pgjdbc's password callback.
                    SSLHandlerTrustStoreImpl.saveEncryptedPkcs8PemAsDerFile(key, converted);
                }
            } catch (IOException e) {
                Files.deleteIfExists(converted);
                throw e;
            }
            this.trackTempFile.accept(converted);
            this.convertedKeyDigest = digest;
            this.convertedKeyFile = converted;
            return converted;
        }
    }

    @Nullable
    public DBPConnectionConfiguration prepareRetryConfiguration(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBPConnectionConfiguration connectionInfo,
        @NotNull DBCException error
    ) {
        DBWHandlerConfiguration handler = connectionInfo.getHandler(PostgreConstants.HANDLER_SSL);
        if (handler == null || !handler.isEnabled() || !isSSLKeyReadError(error)
            || !SSLHandlerTrustStoreImpl.canRetryWithPkcs8PemKey(handler)) {
            return null;
        }
        try {
            DBPConnectionConfiguration retryConfig = new DBPConnectionConfiguration(connectionInfo);
            DBWHandlerConfiguration retryHandler = Objects.requireNonNull(
                retryConfig.getHandler(PostgreConstants.HANDLER_SSL)
            );
            Path dst = Files.createTempFile(DBWorkbench.getPlatform().getTempFolder(monitor, "ssl"), "private-", ".pk8");
            boolean tracked = false;
            try {
                if (SSLHandlerTrustStoreImpl.loadDerFromPem(retryHandler, dst)) {
                    // The driver may still need the key during the handshake; keep it until data source shutdown.
                    this.trackTempFile.accept(dst);
                    tracked = true;
                    if (CommonUtils.isEmpty(retryHandler.getStringProperty(SSLHandlerTrustStoreImpl.PROP_SSL_METHOD))) {
                        retryHandler.setProperty(PostgreConstants.PROP_SSL_CLIENT_KEY, dst.toString());
                    }
                    return retryConfig;
                }
            } finally {
                if (!tracked) {
                    Files.deleteIfExists(dst);
                }
            }
        } catch (IOException e) {
            log.error("Error converting SSL key", e);
        }
        return null;
    }

    public void clear() {
        synchronized (this.lock) {
            this.convertedKeyDigest = null;
            this.convertedKeyFile = null;
        }
    }

    private static boolean isSSLKeyReadError(@NotNull Throwable error) {
        String messages = CommonUtils.getAllExceptionMessages(error);
        return messages.contains("Could not read SSL key") || messages.contains("Could not load the private key");
    }
}
