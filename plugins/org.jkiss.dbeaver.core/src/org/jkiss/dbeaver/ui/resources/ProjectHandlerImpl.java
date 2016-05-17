/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2016 Serge Rieder (serge@jkiss.org)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (version 2)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package org.jkiss.dbeaver.ui.resources;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.model.navigator.DBNProject;

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
        if (resource != DBeaverCore.getInstance().getProjectRegistry().getActiveProject()) {
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
