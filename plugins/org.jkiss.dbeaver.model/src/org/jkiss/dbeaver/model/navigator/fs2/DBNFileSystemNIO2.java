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
import org.jkiss.dbeaver.model.navigator.DBNLazyNode;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

import java.util.Arrays;

/**
 * Represents a filesystem that may have multiple roots (devices).
 */
public class DBNFileSystemNIO2 extends DBNNode implements DBNLazyNode {
    private final DBFVirtualFileSystem fileSystem;

    private DBNFileSystemNIO2Resource[] children;

    public DBNFileSystemNIO2(@NotNull DBNFileSystemNIO2List parent, @NotNull DBFVirtualFileSystem fileSystem) {
        super(parent);
        this.fileSystem = fileSystem;
    }

    @Nullable
    public DBNFileSystemNIO2Resource getRoot(@NotNull String path) {
        if (children == null) {
            return null;
        }

        for (DBNFileSystemNIO2Resource root : children) {
            if (root.getRoot().getRootId().equals(path)) {
                return root;
            }
        }

        return null;
    }


    @Override
    public String getNodeType() {
        return "filesystem";
    }

    @Override
    public String getNodeName() {
        return fileSystem.getFileSystemDisplayName();
    }

    @Override
    public String getNodeDescription() {
        return fileSystem.getDescription();
    }

    @Override
    public DBPImage getNodeIcon() {
        return null;
    }

    @Override
    protected boolean allowsChildren() {
        return true;
    }

    @Override
    public boolean needsInitialization() {
        return children == null;
    }

    @Override
    public DBNNode[] getChildren(DBRProgressMonitor monitor) throws DBException {
        if (children == null) {
            children = Arrays.stream(fileSystem.getRootFolders(monitor))
                .map(root -> new DBNFileSystemNIO2Resource(this, null, root))
               // .sorted(Comparator.comparing(DBNNode::getNodeName, String.CASE_INSENSITIVE_ORDER))
                .toArray(DBNFileSystemNIO2Resource[]::new);
        }

        for (DBNFileSystemNIO2Resource root : children) {
            root.link();
        }

        return children;
    }

    @Override
    public String getNodeItemPath() {
        return null;
    }

    @NotNull
    public DBFVirtualFileSystem getFileSystem() {
        return fileSystem;
    }
}
