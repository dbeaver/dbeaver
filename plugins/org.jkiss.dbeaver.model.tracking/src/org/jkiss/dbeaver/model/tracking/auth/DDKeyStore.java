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
package org.jkiss.dbeaver.model.tracking.auth;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.data.json.JSONUtils;
import org.jkiss.dbeaver.model.secret.DBSSecretController;
import org.jkiss.utils.CommonUtils;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import javax.crypto.SecretKey;

/**
 * Keeps the working keys of the account. Written and erased as a whole.
 */
public class DDKeyStore {

    private static final Log log = Log.getLog(DDKeyStore.class);

    @Deprecated(forRemoval = true)
    public static final String ACCESS_KEY_PREFIX = "ddgk_";

    private static final String SECRET_KEY_BUNDLE = "datadam.key-bundle";
    private static final String BUNDLE_SEPARATOR = "\\.";

    private DDKeyStore() {
    }

    @Nullable
    public static DDKeyBundle load() {
        try {
            String value = DBSSecretController.getGlobalSecretController()
                .getPrivateSecretValue(SECRET_KEY_BUNDLE);
            if (CommonUtils.isEmpty(value)) {
                return null;
            }
            return JSONUtils.GSON.fromJson(value, DDKeyBundle.class);
        } catch (DBException | RuntimeException e) {
            log.error("Error reading key bundle", e);
            return null;
        }
    }

    @NotNull
    public static DDKeyBundle unpack(
        @NotNull DDCryptoState state,
        @NotNull String recoveryPhrase
    ) throws DBException {
        if (!state.cryptoConfigured() || CommonUtils.isEmpty(state.encryptedBundle())) {
            throw new DBException("Encryption is not configured for this account");
        }
        if (CommonUtils.isEmpty(state.salt()) || state.iterations() == null) {
            throw new DBException("Key derivation settings are not configured for this account");
        }
        byte[] salt;
        byte[] encryptedBundle;
        try {
            salt = Base64.getDecoder().decode(state.salt());
            encryptedBundle = Base64.getDecoder().decode(state.encryptedBundle());
        } catch (IllegalArgumentException e) {
            throw new DBException("Invalid encrypted bundle", e);
        }
        SecretKey kek = DDCrypto.deriveKek(DDRecoveryPhrase.normalizeAndValidate(recoveryPhrase), salt, state.iterations());
        byte[] bundle = DDCrypto.decrypt(kek, encryptedBundle);

        String[] parts = new String(bundle, StandardCharsets.UTF_8).split(BUNDLE_SEPARATOR, 2);
        if (parts.length != 2 || CommonUtils.isEmpty(parts[0]) || CommonUtils.isEmpty(parts[1])) {
            throw new DBException("Key bundle is incomplete");
        }
        return new DDKeyBundle(
            state.accountId(),
            parts[0],
            parts[1],
            state.generation() == null ? 0 : state.generation());
    }

    public static void save(@NotNull DDKeyBundle bundle) throws DBException {
        DDKeyBundle current = load();
        if (current != null) {
            if (!current.accountId().equals(bundle.accountId())) {
                throw new DBException("The keys belong to another account");
            }
            if (bundle.generation() < current.generation()) {
                throw new DBException("The keys are older than the stored ones");
            }
        }
        DBSSecretController controller = DBSSecretController.getGlobalSecretController();
        controller.setPrivateSecretValue(SECRET_KEY_BUNDLE, JSONUtils.GSON.toJson(bundle));
        controller.flushChanges();
    }

    public static void clear() throws DBException {
        DBSSecretController controller = DBSSecretController.getGlobalSecretController();
        controller.setPrivateSecretValue(SECRET_KEY_BUNDLE, null);
        controller.flushChanges();
    }

    @Deprecated(forRemoval = true)
    @NotNull
    public static byte[] decodeAccessKey(@NotNull String accessKey) throws DBException {
        String value = accessKey.trim();
        if (value.startsWith(ACCESS_KEY_PREFIX)) {
            int separator = value.indexOf('.');
            if (separator < 0) {
                throw new DBException("Invalid access key format");
            }
            value = value.substring(separator + 1);
        }
        try {
            return Base64.getUrlDecoder().decode(value);
        } catch (IllegalArgumentException e) {
            throw new DBException("Invalid access key: " + e.getMessage(), e);
        }
    }
}
