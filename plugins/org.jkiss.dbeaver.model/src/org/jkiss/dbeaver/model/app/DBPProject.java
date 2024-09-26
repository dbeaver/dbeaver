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

package org.jkiss.dbeaver.model.app;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPObject;
import org.jkiss.dbeaver.model.access.DBAPermissionRealm;
import org.jkiss.dbeaver.model.auth.SMAuthSpace;
import org.jkiss.dbeaver.model.auth.SMSession;
import org.jkiss.dbeaver.model.auth.SMSessionContext;
import org.jkiss.dbeaver.model.fs.DBFFileSystemManager;
import org.jkiss.dbeaver.model.navigator.DBNModel;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.secret.DBSValueEncryptor;
import org.jkiss.dbeaver.model.task.DBTTaskManager;

import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;

/**
 * Project meta information.
 */
public interface DBPProject extends DBPObject, SMAuthSpace, DBAPermissionRealm {
    String METADATA_FOLDER = ".dbeaver";

    @NotNull
    DBPWorkspace getWorkspace();

    // In multi-user environment virtual project is a project owned by user
    boolean isVirtual();

    // Project with no persistent state
    boolean isInMemory();

    /**
     * Project unique ID.
     * May start with RMProjectType prefix
     */
    @NotNull
    String getId();

    @NotNull
    String getName();

    @NotNull
    String getDisplayName();

    /**
     * Project UUID was deprecated. Use getId instead.
     */
    @Deprecated
    @NotNull
    UUID getProjectID();

    @NotNull
    Path getAbsolutePath();

    @NotNull
    Path getMetadataFolder(boolean create);

    boolean isOpen();

    void ensureOpen();

    boolean isRegistryLoaded();

    /**
     * Encrypted project configuration files are stored in encrypted form
     */
    boolean isEncryptedProject();

    /**
     * Is secret storage is enabled then all secret credentials are stored there.
     * Otherwise, credentials are stored locally.
     */
    boolean isUseSecretStorage();

    boolean isPrivateProject();

    /**
     * An encryptor that can be used to encrypt and decrypt project data.
     *
     * @throws DBException if the encryptor is not available
     */
    @NotNull
    DBSValueEncryptor getValueEncryptor() throws DBException;

    @NotNull
    DBPDataSourceRegistry getDataSourceRegistry();

    @NotNull
    default DBTTaskManager getTaskManager() {
        throw new IllegalStateException("Task manager is not supported by " + getClass().getSimpleName());
    }

    @Nullable
    default DBTTaskManager getTaskManager(boolean create) {
        return getTaskManager();
    }

    /**
     * Project auth context
     */
    @NotNull
    SMSessionContext getSessionContext();

    @Nullable
    default SMSession getWorkspaceSession() {
        return getSessionContext().findSpaceSession(getWorkspace());
    }

    @Nullable
    Object getProjectProperty(String propName);

    void setProjectProperty(@NotNull String propName, @Nullable Object propValue);

    /**
     * Finds resources that match the supplied {@code properties} map.
     */
    @NotNull
    String[] findResources(@NotNull Map<String, ?> properties) throws DBException;

    @Nullable
    Map<String, Object> getResourceProperties(@NotNull String resourcePath);

    void setResourceProperties(@NotNull String resourcePath, @NotNull Map<String, Object> newProps);

    @Nullable
    Object getResourceProperty(@NotNull String resourcePath, @NotNull String propName);

    void setResourceProperty(@NotNull String resourcePath, @NotNull String propName, @Nullable Object propValue);

    void moveResourceProperties(@NotNull String oldResourcePath, @NotNull String newResourcePath);

    void refreshProject(DBRProgressMonitor monitor);

    @Nullable
    DBNModel getNavigatorModel();

    @NotNull
    DBFFileSystemManager getFileSystemManager();

    /**
     * close project and all related resources
     */
    void dispose();

    /**
     * Get the runtime property. Runtime property is a property that doesn't be stored and exists only in runtime.
     *
     * @param key property key
     * @return property value
     */
    @Nullable
    String getRuntimeProperty(@NotNull String key);

    /**
     * Set the runtime property.
     *
     * @param key   property key
     * @param value property value
     */
    void setRuntimeProperty(@NotNull String key, @Nullable String value);
}
