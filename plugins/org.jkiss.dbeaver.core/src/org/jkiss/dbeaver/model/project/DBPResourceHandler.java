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

package org.jkiss.dbeaver.model.project;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.ui.IWorkbenchWindow;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.model.struct.DBSDataSourceContainer;

import java.util.Collection;

/**
 * Resource handler
 */
public interface DBPResourceHandler {

    public static final int FEATURE_OPEN            = 1;
    public static final int FEATURE_DELETE          = 2;
    public static final int FEATURE_CREATE_FOLDER   = 4;
    public static final int FEATURE_RENAME          = 8;
    public static final int FEATURE_MOVE_INTO       = 16;

    int getFeatures(IResource resource);

    DBNNode makeNavigatorNode(DBNNode parentNode, IResource resource) throws CoreException, DBException;

    void openResource(IResource resource, IWorkbenchWindow window) throws CoreException, DBException;

    String getTypeName(IResource resource);

    String getResourceDescription(IResource resource);

    Collection<DBSDataSourceContainer> getAssociatedDataSources(IResource resource);
}
