/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2022 DBeaver Corp and others
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
import org.jkiss.dbeaver.model.secret.DBSSecret;
import org.jkiss.dbeaver.model.secret.DBSSecretBrowser;
import org.jkiss.dbeaver.model.secret.DBSSecretController;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Default secret controller.
 *
 * Uses Eclipse secure preferences to read/write secrets.
 */
public class DefaultSecretController implements DBSSecretController, DBSSecretBrowser {

    public static final String SECRET_PREFS_ROOT = "dbeaver";

    public static final DefaultSecretController INSTANCE = new DefaultSecretController("");

    private final Path root;

    public DefaultSecretController(String root) {
        this.root = Path.of(root);
    }

    public DefaultSecretController(DBSSecretController parent, String path) {
        if (parent instanceof DefaultSecretController) {
            this.root = (((DefaultSecretController) parent).root).resolve(path);
        } else {
            throw new IllegalArgumentException("Bad parent controller: " + parent);
        }
    }

    @Nullable
    @Override
    public String getSecretValue(@NotNull String secretId) throws DBException {
        try {
            Path keyPath = root.resolve(secretId);

            return getNodeByPath(keyPath.getParent())
                .get(keyPath.getFileName().toString(), null);
        } catch (StorageException e) {
            throw new DBException("Error getting preference value '" + secretId + "'", e);
        }
    }

    @Override
    public void setSecretValue(@NotNull String secretId, @Nullable String keyValue) throws DBException {
        try {
            Path keyPath = root.resolve(secretId);

            getNodeByPath(keyPath.getParent())
                .put(keyPath.getFileName().toString(), keyValue, true);
        } catch (StorageException e) {
            throw new DBException("Error setting preference value '" + secretId + "'", e);
        }
    }

    private static ISecurePreferences getNodeByPath(Path path) {
        ISecurePreferences rootNode = SecurePreferencesFactory.getDefault().node(SECRET_PREFS_ROOT);
        for (Path name : path) {
            rootNode = rootNode.node(name.toString());
        }
        return rootNode;
    }

    @NotNull
    @Override
    public List<DBSSecret> listSecrets() throws DBException {
        return Arrays.stream(getNodeByPath(root).keys())
            .map(k -> new DBSSecret(k, k))
            .collect(Collectors.toList());
    }

    @Nullable
    @Override
    public DBSSecret getSecret(@NotNull String secretId) throws DBException {
        try {
            Path keyPath = root.resolve(secretId);
            String keyId = keyPath.getFileName().toString();
            String value = getNodeByPath(keyPath.getParent()).get(keyId, null);
            if (value == null) {
                return null;
            }
            return new DBSSecret(keyId, keyId);
        } catch (StorageException e) {
            throw new DBException("Error getting secret info '" + secretId + "'", e);
        }
    }

    @Override
    public void deleteSecret(@NotNull String secretId) throws DBException {

    }

    @Override
    public void clearAllSecrets() throws DBException {
        getNodeByPath(root).removeNode();
    }

}