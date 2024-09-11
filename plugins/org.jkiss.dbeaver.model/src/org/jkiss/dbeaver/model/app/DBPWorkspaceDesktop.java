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

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPExternalFileManager;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

/**
 * Desktop eclipse based workspace
 */
public interface DBPWorkspaceDesktop extends DBPWorkspaceEclipse, DBPExternalFileManager {

    @NotNull
    DBPResourceHandler getDefaultResourceHandler();
    @NotNull
    DBPResourceHandlerDescriptor[] getAllResourceHandlers();
    @Nullable
    DBPResourceHandler getResourceHandler(IResource resource);
    @Nullable
    IFolder getResourceDefaultRoot(@NotNull DBPProject project, @NotNull DBPResourceHandlerDescriptor handler, boolean forceCreate);
    @Nullable
    IFolder getResourceDefaultRoot(@NotNull DBPProject project, @NotNull Class<? extends DBPResourceHandler> handlerType, boolean forceCreate);

    /**
     * Reloads workspace contents. Creates missing projects, removes unexistent projects
     */
    void refreshWorkspaceContents(@NotNull DBRProgressMonitor monitor) throws DBException;

    @NotNull
    DBPProject createProject(@NotNull String name, @Nullable String description) throws DBException;

    void deleteProject(@NotNull DBPProject project, boolean deleteContents) throws DBException;

}
