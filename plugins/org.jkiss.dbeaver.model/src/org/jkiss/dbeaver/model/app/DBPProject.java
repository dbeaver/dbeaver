/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2019 Serge Rider (serge@jkiss.org)
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
import org.eclipse.equinox.security.storage.ISecurePreferences;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.DBPObject;

import java.io.File;

/**
 * DBPWorkspace
 */
public interface DBPProject extends DBPObject
{
    String METADATA_FOLDER = ".dbeaver";

    @NotNull
    DBPWorkspace getWorkspace();

    @NotNull
    String getName();

    @NotNull
    File getAbsolutePath();

    @NotNull
    IProject getEclipseProject();

    @NotNull
    IFolder getMetadataFolder(boolean create);

    boolean isOpen();

    void ensureOpen();

    DBPDataSourceRegistry getDataSourceRegistry();

    @NotNull
    ISecurePreferences getSecurePreferences();

    Object getResourceProperty(IResource resource, String propName);

    void setResourceProperty(IResource resource, String propName, Object propValue);

}
