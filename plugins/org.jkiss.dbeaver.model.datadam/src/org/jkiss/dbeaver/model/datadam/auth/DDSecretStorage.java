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

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.impl.app.SecurePreferencesSecretController;
import org.jkiss.dbeaver.model.secret.DBSSecretController;
import org.jkiss.dbeaver.model.secret.DBSSecretObject;
import org.jkiss.dbeaver.model.secret.DBSSecretValue;
import org.jkiss.dbeaver.model.security.DBSecurityUtils;

import java.util.List;

/** Selects an OS-backed provider for DataDam without changing the application's default controller. */
public final class DDSecretStorage implements DBSSecretController {
    private static final Log log = Log.getLog(DDSecretStorage.class);
    private static final String NODE = "datadam";
    private static final DDSecretStorage INSTANCE = new DDSecretStorage();

    private final SecurePreferencesSecretController defaultController = SecurePreferencesSecretController.create(NODE, null);

    @Nullable
    private final SecurePreferencesSecretController nativeController;

    private DDSecretStorage() {
        String nativeProviderId = DBSecurityUtils.findNativeOSProvider();
        nativeController = nativeProviderId == null ? null : SecurePreferencesSecretController.create(NODE, nativeProviderId);
        if (nativeController == null) {
            log.warn("DataDam native secure storage provider not found; using the configured default provider");
        } else {
            log.info("DataDam using native secure storage provider '" + nativeProviderId + "'");
        }
    }

    @NotNull
    public static DBSSecretController getInstance() {
        return INSTANCE;
    }

    /** Checks the secure-storage index without decrypting a value. */
    public static boolean hasStoredSecret(@NotNull String secretId) throws DBException {
        return INSTANCE.defaultController.listSecrets(null).stream().anyMatch(secret -> secretId.equals(secret.getName()));
    }

    @Nullable
    @Override
    public String getPrivateSecretValue(@NotNull String secretId) throws DBException {
        log.debug("Reading DataDam secret through Equinox secure storage");
        return defaultController.getPrivateSecretValue(secretId);
    }

    @Override
    public void setPrivateSecretValue(@NotNull String secretId, @Nullable String secretValue) throws DBException {
        if (nativeController != null && secretValue != null) {
            log.debug("Writing DataDam secret with native secure storage provider");
            nativeController.setPrivateSecretValue(secretId, secretValue);
            if (nativeController.isPrivateSecretEncrypted(secretId)) {
                log.debug("DataDam secret encrypted with native secure storage provider");
                return;
            }
            // Equinox can silently store plaintext when the OS provider cannot supply a password.
            nativeController.setPrivateSecretValue(secretId, null);
            log.warn("DataDam native secure storage did not encrypt the secret; unencrypted value removed");
            throw new DBException("Native secure storage did not encrypt DataDam secret");
        }
        log.debug(secretValue == null ? "Removing DataDam secret" : "Writing DataDam secret with configured default provider");
        defaultController.setPrivateSecretValue(secretId, secretValue);
    }

    @Override
    public void flushChanges() throws DBException {
        defaultController.flushChanges();
    }

    @NotNull
    @Override
    public List<DBSSecretValue> discoverCurrentUserSecrets(@NotNull DBSSecretObject secretObject) throws DBException {
        return defaultController.discoverCurrentUserSecrets(secretObject);
    }
}
