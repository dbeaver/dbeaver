/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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
package org.jkiss.dbeaver.model.impl.app;

import org.eclipse.equinox.security.storage.ISecurePreferences;
import org.eclipse.equinox.security.storage.SecurePreferencesFactory;
import org.eclipse.equinox.security.storage.StorageException;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.exec.DBCFeatureNotSupportedException;
import org.jkiss.dbeaver.model.secret.DBSSecret;
import org.jkiss.dbeaver.model.secret.DBSSecretBrowser;
import org.jkiss.dbeaver.model.secret.DBSSecretController;
import org.jkiss.dbeaver.runtime.DBSecurityException;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Default local secret controller.
 *
 * Uses Eclipse secure preferences to read/write secrets.
 */
public class LocalSecretController implements DBSSecretController, DBSSecretBrowser {

    public static final String SECRET_PREFS_ROOT = "dbeaver";

    public static final LocalSecretController INSTANCE = new LocalSecretController("");

    private final Path root;

    public LocalSecretController(String root) {
        this.root = Path.of(root);
    }

    @Nullable
    @Override
    public String getSecretValue(@NotNull String secretId) throws DBException {
        try {
            Path keyPath = root.resolve(escapeSecretKey(secretId));

            return getNodeByPath(keyPath.getParent())
                .get(keyPath.getFileName().toString(), null);
        } catch (StorageException e) {
            if (e.getErrorCode() == StorageException.NO_PASSWORD) {
                throw new DBSecurityException("Cannot load secure settings - master password is not provided");
            }
            throw new DBSecurityException("Error getting preference value '" + secretId + "'", e);
        }
    }

    @Override
    public void setSecretValue(@NotNull String secretId, @Nullable String secretValue) throws DBException {
        try {
            Path keyPath = root.resolve(escapeSecretKey(secretId));

            ISecurePreferences node = getNodeByPath(keyPath.getParent());
            if (secretValue != null) {
                node.put(keyPath.getFileName().toString(), secretValue, true);
            } else {
                node.remove(keyPath.getFileName().toString());
            }
        } catch (StorageException e) {
            if (e.getErrorCode() == StorageException.NO_PASSWORD) {
                throw new DBSecurityException("Cannot save secure settings - master password is not provided");
            }
            throw new DBSecurityException("Error setting preference value '" + secretId + "'", e);
        }
    }

    @Override
    public void flushChanges() throws DBException {
        try {
            SecurePreferencesFactory.getDefault().flush();
        } catch (IOException e) {
            throw new DBException("Error flushing secure preferences", e);
        }
    }

    private static ISecurePreferences getNodeByPath(Path path) {
        ISecurePreferences rootNode = SecurePreferencesFactory.getDefault().node(SECRET_PREFS_ROOT);
        if (path != null) {
            for (Path name : path) {
                rootNode = rootNode.node(name.toString());
            }
        }
        return rootNode;
    }

    @NotNull
    @Override
    public List<DBSSecret> listSecrets(@Nullable String path) throws DBException {
        Path keyPath = path == null ? root : root.resolve(escapeSecretKey(path));
        return Arrays.stream(getNodeByPath(keyPath).keys())
            .map(k -> new DBSSecret(keyPath.resolve(escapeSecretKey(k)).toString(), k))
            .collect(Collectors.toList());
    }

    @Nullable
    @Override
    public DBSSecret getSecret(@NotNull String secretId) throws DBException {
        try {
            Path keyPath = root.resolve(escapeSecretKey(secretId));
            String keyId = keyPath.getFileName().toString();
            String value = getNodeByPath(keyPath.getParent()).get(keyId, null);
            if (value == null) {
                return null;
            }
            return new DBSSecret(keyPath.toString(), keyId);
        } catch (StorageException e) {
            throw new DBSecurityException("Error getting secret info '" + secretId + "'", e);
        }
    }

    @Override
    public void deleteSecret(@NotNull String secretId) throws DBException {
        throw new DBCFeatureNotSupportedException();
    }

    @Override
    public void clearAllSecrets(String keyPrefix) throws DBException {
        getNodeByPath(root.resolve(escapeSecretKey(keyPrefix))).removeNode();
    }

    private String escapeSecretKey(String key) {
        // Replace : with _ because Windows do not support : in path names
        return key.replace(':', '_');
    }

}