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
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.app.DBPWorkspaceDesktop;
import org.jkiss.dbeaver.model.fs.DBFVirtualFileSystemRoot;
import org.jkiss.dbeaver.model.fs.nio2.NIO2FileStore;
import org.jkiss.dbeaver.model.fs.nio2.NIO2FileSystem;
import org.jkiss.dbeaver.model.navigator.DBNEvent;
import org.jkiss.dbeaver.model.navigator.DBNLazyNode;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.model.navigator.DBNResource;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.ProxyProgressMonitor;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.utils.ArrayUtils;

public class DBNFileSystemNIO2Resource extends DBNResource implements DBNLazyNode {
    private static final Log log = Log.getLog(DBNFileSystemNIO2Resource.class);

    private final DBFVirtualFileSystemRoot root;

    public DBNFileSystemNIO2Resource(@NotNull DBNNode parent, @Nullable IResource resource, @NotNull DBFVirtualFileSystemRoot root) {
        super(parent, resource, ((DBPWorkspaceDesktop) DBWorkbench.getPlatform().getWorkspace()).getDefaultResourceHandler());
        this.root = root;
    }

    @Override
    protected DBNNode[] readChildNodes(DBRProgressMonitor monitor) throws DBException {
        refreshResource(monitor, false);
        return super.readChildNodes(monitor);
    }

    @Override
    public DBNNode refreshNode(DBRProgressMonitor monitor, Object source) throws DBException {
        children = null;
        refreshResource(monitor, true);
        fireNodeEvent(new DBNEvent(source, DBNEvent.Action.UPDATE, DBNEvent.NodeChange.REFRESH, this));
        return this;
    }

    @Nullable
    public DBNFileSystemNIO2Resource getChild(@NotNull String name) {
        if (children == null) {
            return null;
        }

        for (DBNNode child : children) {
            if (child.getNodeName().equals(name)) {
                return (DBNFileSystemNIO2Resource) child;
            }
        }

        return null;
    }

    void addChildResource(@NotNull String name, boolean directory) {
        if (children == null) {
            return;
        }

        final Path path = new Path(name);
        final IContainer container = (IContainer) getResource();
        final IResource member = directory ? container.getFolder(path) : container.getFile(path);

        if (member == null) {
            log.debug("Can't find member called " + name + " in resource " + container);
            return;
        }

        final DBNFileSystemNIO2Resource child = new DBNFileSystemNIO2Resource(this, member, root);

        children = ArrayUtils.add(DBNNode.class, children, child);
        sortChildren(children);
        fireNodeEvent(new DBNEvent(this, DBNEvent.Action.ADD, child));
    }

    void removeChildResource(@NotNull String name) {
        if (children == null) {
            return;
        }

        final DBNFileSystemNIO2Resource child = getChild(name);

        if (child == null) {
            log.debug("Can't find member called " + name + " in resource " + getResource());
            return;
        }

        children = ArrayUtils.remove(DBNNode.class, children, child);
        fireNodeEvent(new DBNEvent(this, DBNEvent.Action.REMOVE, child));
    }

    private void refreshResource(@NotNull DBRProgressMonitor monitor, boolean invalidateCache) throws DBException {
        final IResource resource = getResource();
        final NIO2FileStore store;

        try {
            store = (NIO2FileStore) EFS.getStore(resource.getLocationURI());
        } catch (CoreException e) {
            throw new DBException(e.getMessage(), e);
        }

        if (invalidateCache) {
            store.purgeCachedChildren();
        }

        try {
            store.setAllowedToLoadChildNames(true);
            resource.refreshLocal(IResource.DEPTH_ONE, new ProxyProgressMonitor(monitor));
        } catch (CoreException e) {
            log.error("Error refreshing resource: " + e.getMessage(), e);
        } finally {
            store.setAllowedToLoadChildNames(false);
        }
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
        setResource(NIO2FileSystem.toResource(getOwnerProject(), root, null));
    }
}
