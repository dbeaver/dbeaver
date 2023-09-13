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

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPImage;
import org.jkiss.dbeaver.model.fs.DBFVirtualFileSystem;
import org.jkiss.dbeaver.model.fs.DBFVirtualFileSystemRoot;
import org.jkiss.dbeaver.model.fs.nio.NIOListener;
import org.jkiss.dbeaver.model.fs.nio.NIOMonitor;
import org.jkiss.dbeaver.model.fs.nio2.NIO2FileStore;
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
public class DBNFileSystemNIO2List extends DBNNode implements NIOListener {
    private DBNFileSystemNIO2[] children;

    public DBNFileSystemNIO2List(@NotNull DBNProject parent) {
        super(parent);

        NIOMonitor.addListener(this);
    }

    @Nullable
    public DBNFileSystemNIO2 getFileSystem(@Nullable String type, @NotNull String id) {
        if (children == null) {
            return null;
        }

        for (DBNFileSystemNIO2 fileSystem : children) {
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
        return null;
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
    public DBNNode[] getChildren(DBRProgressMonitor monitor) throws DBException {
        if (children == null) {
            children = Arrays.stream(DBWorkbench.getPlatform().getFileSystemRegistry().getFileSystemProviders())
                .flatMap(provider -> Arrays.stream(provider.getInstance().getAvailableFileSystems(monitor, getModel().getModelAuthContext())))
                .map(filesystem -> new DBNFileSystemNIO2(this, filesystem))
                .sorted(Comparator.comparing(DBNNode::getNodeName, String.CASE_INSENSITIVE_ORDER))
                .toArray(DBNFileSystemNIO2[]::new);
        }

        return children;
    }

    @Override
    public void resourceChanged(@NotNull NIO2FileStore fileStore, @NotNull Action action) {
        final Path path = fileStore.getPath();
        final DBFVirtualFileSystemRoot root = fileStore.getRoot();

        if (children == null) {
            return;
        }

        if (!Objects.equals(getOwnerProject().getEclipseProject(), fileStore.getProject().getEclipseProject())) {
            return;
        }

        for (DBNFileSystemNIO2 fs : children) {
            if (CommonUtils.equalObjects(fs.getFileSystem(), root.getFileSystem())) {
                final DBNFileSystemNIO2Resource rootNode = fs.getRoot(root);
                if (rootNode != null) {
                    final int names = path.getNameCount();
                    DBNFileSystemNIO2Resource parentNode = rootNode;

                    for (int i = 0; i < names - 1; i++) {
                        final Path name = path.getName(i);
                        parentNode = parentNode.getChild(name.toString());

                        if (parentNode == null) {
                            return;
                        }
                    }

                    switch (action) {
                        case CREATE:
                            parentNode.addChildResource(path.getFileName().toString(), fileStore.fetchInfo().isDirectory());
                            break;
                        case DELETE:
                            parentNode.removeChildResource(path.getFileName().toString());
                            break;
                        default:
                            break;
                    }
                }
                break;
            }
        }
    }
}
