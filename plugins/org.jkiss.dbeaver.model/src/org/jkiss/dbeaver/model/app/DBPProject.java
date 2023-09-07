/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2023 DBeaver Corp and others
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

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPObject;
import org.jkiss.dbeaver.model.access.DBAPermissionRealm;
import org.jkiss.dbeaver.model.auth.SMAuthSpace;
import org.jkiss.dbeaver.model.auth.SMSession;
import org.jkiss.dbeaver.model.auth.SMSessionContext;
import org.jkiss.dbeaver.model.navigator.DBNModel;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.task.DBTTaskManager;

import javax.crypto.SecretKey;
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
    String getId();

    @NotNull
    String getName();

    @NotNull
    String getDisplayName();

    /**
     * Proiject UUID was deprecated. Use getId instead.
     */
    @Deprecated
    @NotNull
    UUID getProjectID();

    @NotNull
    Path getAbsolutePath();

    @Nullable
    IProject getEclipseProject();

    @Nullable
    IContainer getRootResource();

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
     * Secret key is used encrypt project data
     */
    SecretKey getLocalSecretKey();

    @NotNull
    DBPDataSourceRegistry getDataSourceRegistry();

    @NotNull
    DBTTaskManager getTaskManager();

    /**
     * Project auth context
     */
    @NotNull
    SMSessionContext getSessionContext();

    default SMSession getWorkspaceSession() {
        return getSessionContext().findSpaceSession(getWorkspace());
    }

    Object getProjectProperty(String propName);

    void setProjectProperty(String propName, Object propValue);

    /**
     * Returns logical resource path
     */
    String getResourcePath(@NotNull IResource resource);

    /**
     * Finds resources that match the supplied {@code properties} map.
     */
    @NotNull
    String[] findResources(@NotNull Map<String, ?> properties) throws DBException;

    Map<String, Object> getResourceProperties(@NotNull String resourcePath);

    @Nullable
    Object getResourceProperty(@NotNull String resourcePath, @NotNull String propName);

    void setResourceProperty(@NotNull String resourcePath, @NotNull String propName, @Nullable Object propValue);

    void moveResourceProperties(@NotNull String oldResourcePath, @NotNull String newResourcePath);

    void refreshProject(DBRProgressMonitor monitor);

    @Nullable
    DBNModel getNavigatorModel();

}
