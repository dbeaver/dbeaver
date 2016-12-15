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

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.ui.part.FileEditorInput;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.core.DBeaverUI;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.navigator.DBNModel;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.model.navigator.DBNResource;
import org.jkiss.dbeaver.model.app.DBPResourceHandler;
import org.jkiss.dbeaver.ui.editors.entity.FolderEditor;
import org.jkiss.dbeaver.ui.editors.entity.NodeEditorInput;
import org.jkiss.dbeaver.ui.editors.sql.SQLEditor;

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

    @NotNull
    @Override
    public DBNResource makeNavigatorNode(@NotNull DBNNode parentNode, @NotNull IResource resource) throws CoreException, DBException
    {
        return new DBNResource(parentNode, resource, this);
    }

    @Override
    public void openResource(@NotNull IResource resource) throws CoreException, DBException
    {
        if (resource instanceof IFolder) {
            DBNResource node = DBeaverCore.getInstance().getNavigatorModel().getNodeByResource(resource);
            if (node != null) {
                NodeEditorInput nodeInput = new NodeEditorInput(node);
                DBeaverUI.getActiveWorkbenchWindow().getActivePage().openEditor(
                    nodeInput,
                    FolderEditor.class.getName());
            }
        }
        //throw new DBException("Resource open is not implemented");
    }

    @NotNull
    @Override
    public String getTypeName(@NotNull IResource resource)
    {
        return "resource";
    }

    @Override
    public String getResourceDescription(@NotNull IResource resource)
    {
        return resource.getName();
    }

    @Nullable
    @Override
    public Collection<DBPDataSourceContainer> getAssociatedDataSources(IResource resource)
    {
        return null;
    }

    @NotNull
    @Override
    public String getResourceNodeName(@NotNull IResource resource) {
        return resource.getName();
    }

    protected IFolder getDefaultRoot(IProject project) {
        return DBeaverCore.getInstance().getProjectRegistry().getResourceDefaultRoot(project, getClass(), false);
    }
}
