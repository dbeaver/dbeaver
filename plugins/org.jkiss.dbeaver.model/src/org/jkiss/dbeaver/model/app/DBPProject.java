/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
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

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.DBPObject;
import org.jkiss.dbeaver.model.task.DBTTaskManager;

import java.io.File;
import java.util.Map;
import java.util.UUID;

/**
 * DBPWorkspace
 */
public interface DBPProject extends DBPObject
{
    String METADATA_FOLDER = ".dbeaver";

    String PROP_SECURE_PROJECT = "secureProject";

    @NotNull
    DBPWorkspace getWorkspace();

    @NotNull
    String getName();

    UUID getProjectID();

    @NotNull
    File getAbsolutePath();

    @NotNull
    IProject getEclipseProject();

    @NotNull
    IFolder getMetadataFolder(boolean create);

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

    Object getProjectProperty(String propName);

    void setProjectProperty(String propName, Object propValue);

    Object getResourceProperty(IResource resource, String propName);

    Map<String, Object> getResourceProperties(IResource resource);

    Map<String, Map<String, Object>> getResourceProperties();

    void setResourceProperty(IResource resource, String propName, Object propValue);

}
