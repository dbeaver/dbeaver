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
package org.jkiss.dbeaver.ui.resources;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.model.navigator.DBNProject;
import org.jkiss.dbeaver.runtime.DBWorkbench;

/**
 * Project handler
 */
public class ProjectHandlerImpl extends AbstractResourceHandler {

    @NotNull
    @Override
    public String getTypeName(@NotNull IResource resource)
    {
        return "project";
    }

    @Override
    public int getFeatures(IResource resource)
    {
        if (resource != DBWorkbench.getPlatform().getProjectManager().getActiveProject()) {
            return FEATURE_DELETE | FEATURE_RENAME;
        }
        return FEATURE_RENAME;
    }

    @NotNull
    @Override
    public DBNProject makeNavigatorNode(@NotNull DBNNode parentNode, @NotNull IResource resource) throws CoreException, DBException
    {
        return new DBNProject(parentNode, (IProject)resource, this);
    }

    @Override
    public void openResource(@NotNull IResource resource) throws CoreException, DBException
    {
        // do nothing
    }
}
