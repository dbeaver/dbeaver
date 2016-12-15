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
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.core.DBeaverUI;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.model.navigator.DBNResource;
import org.jkiss.dbeaver.ui.UIIcon;
import org.jkiss.dbeaver.ui.editors.EditorUtils;
import org.jkiss.dbeaver.ui.editors.sql.SQLEditor;

import java.util.Collection;
import java.util.Collections;

/**
 * Scripts handler
 */
public class ScriptsHandlerImpl extends AbstractResourceHandler {

    private static final Log log = Log.getLog(ScriptsHandlerImpl.class);

    @Override
    public int getFeatures(IResource resource)
    {
        if (resource instanceof IFile) {
            return FEATURE_OPEN | FEATURE_DELETE | FEATURE_RENAME;
        }
        return super.getFeatures(resource);
    }

    @NotNull
    @Override
    public String getTypeName(@NotNull IResource resource)
    {
        if (resource instanceof IFolder) {
            return "script folder"; //$NON-NLS-1$
        } else {
            return "script"; //$NON-NLS-1$
        }
    }

    @Override
    public String getResourceDescription(@NotNull IResource resource)
    {
        return ResourceUtils.getResourceDescription(resource);
    }

    @NotNull
    @Override
    public DBNResource makeNavigatorNode(@NotNull DBNNode parentNode, @NotNull IResource resource) throws CoreException, DBException
    {
        DBNResource node = super.makeNavigatorNode(parentNode, resource);
        if (resource instanceof IFolder) {
            if (resource.getParent() instanceof IProject) {
                node.setResourceImage(UIIcon.SCRIPTS);
            }
        } else {
            node.setResourceImage(UIIcon.SQL_SCRIPT);
        }
        return node;
    }

    @Override
    public void openResource(@NotNull IResource resource) throws CoreException, DBException
    {
        if (resource instanceof IFile) {
            FileEditorInput sqlInput = new FileEditorInput((IFile)resource);
            DBeaverUI.getActiveWorkbenchWindow().getActivePage().openEditor(
                sqlInput,
                SQLEditor.class.getName());
        } else {
            super.openResource(resource);
        }
    }

    @Nullable
    @Override
    public Collection<DBPDataSourceContainer> getAssociatedDataSources(IResource resource)
    {
        if (resource instanceof IFile) {
            DBPDataSourceContainer dataSource = EditorUtils.getFileDataSource((IFile) resource);
            return dataSource == null ? null : Collections.singleton(dataSource);
        }
        return null;
    }

    @NotNull
    @Override
    public String getResourceNodeName(@NotNull IResource resource) {
        if (resource.getParent() instanceof IProject && resource.equals(getDefaultRoot(resource.getProject()))) {
            return "SQL Scripts";
        } else {
            return super.getResourceNodeName(resource);
        }
    }


}
