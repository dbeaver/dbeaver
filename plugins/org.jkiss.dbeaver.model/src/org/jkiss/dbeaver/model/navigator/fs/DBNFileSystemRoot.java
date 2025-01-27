/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2025 DBeaver Corp and others
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
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBConstants;
import org.jkiss.dbeaver.model.DBIcon;
import org.jkiss.dbeaver.model.DBPImage;
import org.jkiss.dbeaver.model.fs.DBFVirtualFileSystemRoot;
import org.jkiss.dbeaver.model.messages.ModelMessages;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.navigator.DBNLazyNode;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;

import java.nio.file.Path;

/**
 * DBNFileSystemRoot
 */
public class DBNFileSystemRoot extends DBNPathBase implements DBNLazyNode
{
    private static final Log log = Log.getLog(DBNFileSystemRoot.class);

    private DBFVirtualFileSystemRoot root;
    private Path path;

    public DBNFileSystemRoot(DBNFileSystem parentNode, DBFVirtualFileSystemRoot root) {
        super(parentNode);
        this.root = root;
    }

    @NotNull
    public DBFVirtualFileSystemRoot getRoot() {
        return root;
    }

    @Override
    public boolean isDisposed() {
        return root == null || super.isDisposed();
    }

    @Override
    protected void dispose(boolean reflect) {
        this.root = null;
        super.dispose(reflect);
    }

    @Override
    public boolean supportsRename() {
        return false;
    }

    @Override
    public String getNodeType() {
        return NodePathType.dbvfs.name() + ".folder";
    }

    @Override
    public String getNodeTypeLabel() {
        return ModelMessages.fs_folder;
    }

    @Override
    @Property(id = DBConstants.PROP_ID_NAME, viewable = true, order = 1)
    public String getNodeDisplayName() {
        return root.getName();
    }

    @Override
    public DBPImage getNodeIcon() {
        return DBIcon.TREE_FOLDER_CONSTRAINT;
    }

    @Override
    public boolean isDirectory() {
        return true;
    }

    @Override
    public DBNNode refreshNode(DBRProgressMonitor monitor, Object source) throws DBException {
        this.path = null;
        return super.refreshNode(monitor, source);
    }

    @Override
    public synchronized Path getPath() {
        if (path == null) {
            try {
                path = root.getRootPath(new VoidProgressMonitor());
            } catch (DBException e) {
                log.error("Error resolving file system root", e);
                path = Path.of(".nonexistentfolder");
            }
        }
        return path;
    }

    @Override
    protected synchronized void setPath(Path path) {
        this.path = path;
    }

}
