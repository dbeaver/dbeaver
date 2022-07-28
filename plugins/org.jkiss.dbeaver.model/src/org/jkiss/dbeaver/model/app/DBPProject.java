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

package org.jkiss.dbeaver.model.app;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.DBPObject;
import org.jkiss.dbeaver.model.auth.SMAuthSpace;
import org.jkiss.dbeaver.model.auth.SMSessionContext;
import org.jkiss.dbeaver.model.task.DBTTaskManager;

import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;

/**
 * Project meta information.
 */
public interface DBPProject extends DBPObject, SMAuthSpace
{
    String METADATA_FOLDER = ".dbeaver";

    String PROP_SECURE_PROJECT = "secureProject";

    @NotNull
    DBPWorkspace getWorkspace();

    // In multi-user environment virtual project is a project owned by user
    boolean isVirtual();

    // Project with no persistent state
    boolean isInMemory();


    String getId();

    @NotNull
    String getName();

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

    boolean isModernProject();

    @NotNull
    DBPDataSourceRegistry getDataSourceRegistry();

    @NotNull
    DBTTaskManager getTaskManager();

    @NotNull
    DBASecureStorage getSecureStorage();

    /**
     * Project auth context
     */
    @NotNull
    SMSessionContext getSessionContext();

    Object getProjectProperty(String propName);

    void setProjectProperty(String propName, Object propValue);

    /**
     * Finds resources that match the supplied {@code properties} map.
     */
    @NotNull
    String[] findResources(@NotNull Map<String, ?> properties);

    @Nullable
    Object getResourceProperty(@NotNull String resourcePath, @NotNull String propName);

    @Nullable
    Object getResourceProperty(@NotNull IResource resource, @NotNull String propName);

    void setResourceProperty(@NotNull String resourcePath, @NotNull String propName, @Nullable Object propValue);

    void setResourceProperty(@NotNull IResource resource, @NotNull String propName, @Nullable Object propValue);
}
