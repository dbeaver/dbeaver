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

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.app.DBPResourceHandler;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.model.navigator.DBNResource;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.editors.NodeEditorInput;
import org.jkiss.dbeaver.ui.editors.entity.FolderEditor;

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
    public void updateNavigatorNode(@NotNull DBNResource node, @NotNull IResource resource) {
        // Reset icon
        node.setResourceImage(null);
    }

    @Override
    public void openResource(@NotNull IResource resource) throws CoreException, DBException
    {
        if (resource instanceof IFolder) {
            DBNResource node = DBWorkbench.getPlatform().getNavigatorModel().getNodeByResource(resource);
            if (node != null) {
                NodeEditorInput nodeInput = new NodeEditorInput(node);
                UIUtils.getActiveWorkbenchWindow().getActivePage().openEditor(
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
        return DBWorkbench.getPlatform().getProjectManager().getResourceDefaultRoot(project, getClass(), false);
    }
}
