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
package org.jkiss.dbeaver.model.navigator.fs;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBIcon;
import org.jkiss.dbeaver.model.DBPImage;
import org.jkiss.dbeaver.model.fs.DBFVirtualFileSystem;
import org.jkiss.dbeaver.model.fs.DBFVirtualFileSystemRoot;
import org.jkiss.dbeaver.model.fs.nio.NIOFileStore;
import org.jkiss.dbeaver.model.fs.nio.NIOListener;
import org.jkiss.dbeaver.model.fs.nio.NIOMonitor;
import org.jkiss.dbeaver.model.navigator.DBNEvent;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.model.navigator.DBNProject;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.utils.CommonUtils;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Objects;

/**
 * Represents a root for all filesystems.
 *
 * @see org.jkiss.dbeaver.model.fs.DBFVirtualFileSystemRoot
 * @see org.jkiss.dbeaver.model.fs.DBFVirtualFileSystem
 */
public class DBNFileSystemList extends DBNNode implements NIOListener {
    private DBNFileSystem[] children;

    public DBNFileSystemList(@NotNull DBNProject parent) {
        super(parent);

        NIOMonitor.addListener(this);
    }

    @Nullable
    public DBNFileSystem getFileSystem(@Nullable String type, @NotNull String id) {
        if (children == null) {
            return null;
        }

        for (DBNFileSystem fileSystem : children) {
            final DBFVirtualFileSystem fs = fileSystem.getFileSystem();

            if ((type == null || fs.getType().equals(type)) && fs.getId().equals(id)) {
                return fileSystem;
            }
        }

        return null;
    }

    @Override
    public String getNodeType() {
        return "filesystems";
    }

    @Override
    public String getNodeName() {
        return "Filesystems";
    }

    @Override
    public String getNodeDescription() {
        return "All filesystems";
    }

    @Override
    public DBPImage getNodeIcon() {
        return DBIcon.TREE_FILE;
    }

    @Override
    public String getNodeItemPath() {
        return getParentNode().getNodeItemPath() + "/" + getName();
    }

    @Override
    protected boolean allowsChildren() {
        return true;
    }

    @Override
    public DBNFileSystem[] getChildren(DBRProgressMonitor monitor) throws DBException {
        if (children == null) {
            children = Arrays.stream(DBWorkbench.getPlatform().getFileSystemRegistry().getFileSystemProviders())
                .flatMap(provider -> Arrays.stream(provider.getInstance().getAvailableFileSystems(monitor, getModel().getModelAuthContext())))
                .map(filesystem -> new DBNFileSystem(this, filesystem))
                .sorted(Comparator.comparing(DBNNode::getNodeName, String.CASE_INSENSITIVE_ORDER))
                .toArray(DBNFileSystem[]::new);
        }

        return children;
    }

    @Override
    public DBNNode refreshNode(DBRProgressMonitor monitor, Object source) throws DBException {
        children = null;
        return this;
    }

    @Override
    protected void dispose(boolean reflect) {
        children = null;
        super.dispose(reflect);
    }

    @Override
    public void resourceChanged(@NotNull NIOFileStore fileStore, @NotNull Action action) {
        final Path path = fileStore.getPath();
        final DBFVirtualFileSystemRoot root = fileStore.getRoot();

        if (children == null) {
            return;
        }

        if (!Objects.equals(getOwnerProject().getEclipseProject(), fileStore.getProject().getEclipseProject())) {
            return;
        }

        for (DBNFileSystem fs : children) {
            if (CommonUtils.equalObjects(fs.getFileSystem(), root.getFileSystem())) {
                final DBNFileSystemResource rootNode = fs.getRoot(root);
                final String[] parts = CommonUtils.removeLeadingSlash(path.toUri().getPath()).split("/");

                if (rootNode != null) {
                    DBNFileSystemResource parentNode = rootNode;

                    for (int i = 0; i < parts.length - 1; i++) {
                        parentNode = parentNode.getChild(parts[i]);

                        if (parentNode == null) {
                            return;
                        }
                    }

                    switch (action) {
                        case CREATE:
                            parentNode.addChildResource(parts[parts.length - 1], fileStore.fetchInfo().isDirectory());
                            break;
                        case DELETE:
                            parentNode.removeChildResource(parts[parts.length - 1]);
                            break;
                        default:
                            break;
                    }
                }

                break;
            }
        }
    }

    public void resetFileSystems() {
        children = null;
        getModel().fireNodeUpdate(this, this, DBNEvent.NodeChange.REFRESH);
    }

    @Nullable
    public DBNFileSystemResource getRootFolder(@NotNull DBRProgressMonitor monitor, @NotNull String id) throws DBException {
        for (DBNFileSystem fsNode : getChildren(monitor)) {
            DBNFileSystemResource rootFolder = fsNode.getChild(monitor, id);
            if (rootFolder != null) {
                return rootFolder;
            }
        }
        return null;
    }

    @Nullable
    public DBNFileSystemResource getNodeByPath(@NotNull DBRProgressMonitor monitor, @NotNull String path) throws DBException {
        getChildren(monitor);

        DBNFileSystemResource fsNode = null;
        DBNFileSystemResource curPath = null;
        for (String name : path.split("/")) {
            if (name.isEmpty() || (curPath == null && name.endsWith(":"))) {
                continue;
            }
            if (fsNode == null) {
                fsNode = getRootFolder(monitor, name);
                if (fsNode == null) {
                    return null;
                }
            } else {
                if (curPath == null) {
                    fsNode.getChildren(monitor);
                    curPath = fsNode.getChild(name);
                } else {
                    curPath.getChildren(monitor);
                    curPath = curPath.getChild(name);
                }
                if (curPath == null) {
                    return null;
                }
            }
        }
        return curPath == null ? fsNode : curPath;
    }
}
