/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
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
import org.jkiss.code.Nullable;

/**
 * ProjectManager
 */
public interface DBPProjectManager
{
    DBPResourceHandler[] getAllResourceHandlers();
    @Nullable
    DBPResourceHandler getResourceHandler(IResource resource);
    @Nullable
    IFolder getResourceDefaultRoot(IProject project, Class<? extends DBPResourceHandler> handlerType, boolean forceCreate);

    IProject getActiveProject();
    void setActiveProject(IProject project);
    void addProject(IProject project);
    void removeProject(IProject project);
    void addProjectListener(DBPProjectListener listener);
    void removeProjectListener(DBPProjectListener listener);

    @Nullable
    DBPDataSourceRegistry getDataSourceRegistry(IProject project);

    DBPResourceHandlerDescriptor[] getResourceHandlerDescriptors();
}
