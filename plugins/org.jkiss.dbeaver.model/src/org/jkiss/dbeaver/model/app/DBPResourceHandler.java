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

package org.jkiss.dbeaver.model.app;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.navigator.DBNNode;

import java.util.Collection;

/**
 * Resource handler
 */
public interface DBPResourceHandler {

    int FEATURE_OPEN            = 1;
    int FEATURE_DELETE          = 2;
    int FEATURE_CREATE_FOLDER   = 4;
    int FEATURE_RENAME          = 8;
    int FEATURE_MOVE_INTO       = 16;

    int getFeatures(IResource resource);

    @NotNull
    DBNNode makeNavigatorNode(@NotNull DBNNode parentNode, @NotNull IResource resource) throws CoreException, DBException;

    void openResource(@NotNull IResource resource) throws CoreException, DBException;

    @NotNull
    String getTypeName(@NotNull IResource resource);

    String getResourceDescription(@NotNull IResource resource);

    Collection<DBPDataSourceContainer> getAssociatedDataSources(IResource resource);

    @NotNull
    String getResourceNodeName(@NotNull IResource resource);
}
