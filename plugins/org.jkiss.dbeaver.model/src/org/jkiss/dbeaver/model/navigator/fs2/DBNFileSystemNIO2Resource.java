/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2023 DBeaver Corp and others
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
package org.jkiss.dbeaver.model.navigator.fs2;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.app.DBPWorkspaceDesktop;
import org.jkiss.dbeaver.model.fs.DBFVirtualFileSystemRoot;
import org.jkiss.dbeaver.model.fs.nio2.NIO2FileStore;
import org.jkiss.dbeaver.model.fs.nio2.NIO2FileSystem;
import org.jkiss.dbeaver.model.navigator.DBNLazyNode;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.model.navigator.DBNResource;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.ProxyProgressMonitor;
import org.jkiss.dbeaver.runtime.DBWorkbench;

public class DBNFileSystemNIO2Resource extends DBNResource implements DBNLazyNode {
    private static final Log log = Log.getLog(DBNFileSystemNIO2Resource.class);

    private final DBFVirtualFileSystemRoot root;

    public DBNFileSystemNIO2Resource(@NotNull DBNNode parent, @Nullable IResource resource, @NotNull DBFVirtualFileSystemRoot root) {
        super(parent, resource, ((DBPWorkspaceDesktop) DBWorkbench.getPlatform().getWorkspace()).getDefaultResourceHandler());
        this.root = root;
    }

    @Override
    protected DBNNode[] readChildNodes(DBRProgressMonitor monitor) throws DBException {
        final IResource resource = getResource();
        final NIO2FileStore store;

        try {
            store = (NIO2FileStore) EFS.getStore(resource.getLocationURI());
        } catch (CoreException e) {
            throw new DBException(e.getMessage(), e);
        }

        try {
            store.setAllowedToLoadChildNames(true);
            resource.refreshLocal(IResource.DEPTH_ONE, new ProxyProgressMonitor(monitor));
        } catch (CoreException e) {
            log.error("Error refreshing resource: " + e.getMessage(), e);
        } finally {
            store.setAllowedToLoadChildNames(false);
        }

        return super.readChildNodes(monitor);
    }

    @Nullable
    @Override
    protected DBNNode makeNode(IResource resource) {
        final DBNFileSystemNIO2Resource child = new DBNFileSystemNIO2Resource(this, resource, root);
        getHandler().updateNavigatorNodeFromResource(child, resource);
        return child;
    }

    @Override
    public boolean needsInitialization() {
        try {
            final NIO2FileStore store = (NIO2FileStore) EFS.getStore(getResource().getLocationURI());
            return !store.childrenCached();
        } catch (CoreException e) {
            return true;
        }
    }

    @Override
    public String getNodeType() {
        return "fileSystemRoot";
    }

    @Override
    public String getNodeName() {
        if (getParentNode() instanceof DBNFileSystemNIO2) {
            return root.getName();
        } else {
            return getResource().getName();
        }
    }

    @NotNull
    public DBFVirtualFileSystemRoot getRoot() {
        return root;
    }

    public void link() {
        final IProject project = getOwnerProject().getEclipseProject();

        if (project == null) {
            throw new IllegalStateException("No Eclipse project is present");
        }

        setResource(NIO2FileSystem.toResource(project, root, null));
    }
}
