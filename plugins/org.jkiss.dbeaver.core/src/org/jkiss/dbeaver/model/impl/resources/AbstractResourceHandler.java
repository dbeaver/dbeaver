/*
 * Copyright (C) 2010-2015 Serge Rieder
 * serge@jkiss.org
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package org.jkiss.dbeaver.model.impl.resources;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.ui.IWorkbenchWindow;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.model.navigator.DBNResource;
import org.jkiss.dbeaver.model.project.DBPResourceHandler;
import org.jkiss.dbeaver.model.struct.DBSDataSourceContainer;

import java.util.Collection;

/**
 * Abstract resource handler
 */
public abstract class AbstractResourceHandler implements DBPResourceHandler {

    @Override
    public int getFeatures(IResource resource)
    {
        if (resource instanceof IFolder) {
            if (resource.getParent() instanceof IFolder) {
                return FEATURE_DELETE | FEATURE_MOVE_INTO | FEATURE_RENAME | FEATURE_CREATE_FOLDER;
            }
            return FEATURE_MOVE_INTO | FEATURE_CREATE_FOLDER;
        }
        return 0;
    }

    @Override
    public DBNResource makeNavigatorNode(DBNNode parentNode, IResource resource) throws CoreException, DBException
    {
        return new DBNResource(parentNode, resource, this);
    }

    @Override
    public void openResource(IResource resource, IWorkbenchWindow window) throws CoreException, DBException
    {
        //throw new DBException("Resource open is not implemented");
    }

    @Override
    public String getTypeName(IResource resource)
    {
        return "resource";
    }

    @Override
    public String getResourceDescription(IResource resource)
    {
        return resource.getName();
    }

    @Nullable
    @Override
    public Collection<DBSDataSourceContainer> getAssociatedDataSources(IResource resource)
    {
        return null;
    }

}
