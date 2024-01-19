/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBConstants;
import org.jkiss.dbeaver.model.DBIcon;
import org.jkiss.dbeaver.model.DBPHiddenObject;
import org.jkiss.dbeaver.model.DBPImage;
import org.jkiss.dbeaver.model.fs.DBFFileSystemManager;
import org.jkiss.dbeaver.model.fs.DBFVirtualFileSystem;
import org.jkiss.dbeaver.model.fs.DBFVirtualFileSystemRoot;
import org.jkiss.dbeaver.model.fs.nio.EFSNIOListener;
import org.jkiss.dbeaver.model.fs.nio.EFSNIOMonitor;
import org.jkiss.dbeaver.model.fs.nio.EFSNIOResource;
import org.jkiss.dbeaver.model.messages.ModelMessages;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.navigator.DBNEvent;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.model.navigator.DBNProject;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.utils.CommonUtils;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

/**
 * DBNFileSystems
 */
public class DBNFileSystems extends DBNNode implements DBPHiddenObject, EFSNIOListener {

    private static final Log log = Log.getLog(DBNFileSystems.class);

    private DBNFileSystem[] children;

    public DBNFileSystems(DBNProject parentNode) {
        super(parentNode);
        EFSNIOMonitor.addListener(this);
    }

    @Override
    protected void dispose(boolean reflect) {
        super.dispose(reflect);
        this.disposeFileSystems();

        EFSNIOMonitor.removeListener(this);
    }

    @Override
    public String getNodeType() {
        return NodePathType.dbvfs.name();
    }

    @NotNull
    @Override
    public String getNodeId() {
        return NodePathType.dbvfs.name();
    }

    @Override
    public String getNodeTypeLabel() {
        return ModelMessages.fs_root;
    }

    @Override
    @Property(id = DBConstants.PROP_ID_NAME, viewable = true, order = 1)
    public String getNodeDisplayName() {
        return "Remote file systems";
    }

    @NotNull
    @Override
    public String getName() {
        return NodePathType.dbvfs.name();
    }

    @Override
//    @Property(viewable = false, order = 100)
    public String getNodeDescription() {
        return "All virtual file systems";
    }

    @Override
    public DBPImage getNodeIcon() {
        return DBIcon.TREE_FILE;
    }

    @Override
    public boolean allowsChildren() {
        return true;
    }

    public DBNFileSystem getFileSystem(@Nullable String type, @NotNull String id) {
        if (children == null) {
            return null;
        }
        for (DBNFileSystem fsNode : children) {
            DBFVirtualFileSystem fs = fsNode.getFileSystem();
            if ((type == null || fs.getType().equals(type)) && fs.getId().equals(id)) {
                return fsNode;
            }
        }
        return null;
    }

    public DBNFileSystemRoot getRootFolder(@NotNull DBRProgressMonitor monitor, @NotNull String id) throws DBException {
        for (DBNFileSystem fsNode : getChildren(monitor)) {
            DBNFileSystemRoot rootFolder = fsNode.getChild(monitor, id);
            if (rootFolder != null) {
                return rootFolder;
            }
        }
        return null;
    }

    @Override
    public DBNFileSystem[] getChildren(DBRProgressMonitor monitor) throws DBException {
        if (children == null) {
            try {
                this.children = readChildNodes(monitor, children);
            } catch (DBException e) {
                this.children = new DBNFileSystem[0];
                throw e;
            }
        }
        return children;
    }

    protected DBNFileSystem[] readChildNodes(
        @NotNull DBRProgressMonitor monitor,
        @Nullable DBNFileSystem[] mergeWith
    ) throws DBException {
        monitor.beginTask("Read available file systems", 1);
        List<DBNFileSystem> result = new ArrayList<>();
        var project = getOwnerProject();
        if (project == null) {
            return new DBNFileSystem[0];
        }
        DBFFileSystemManager fileSystemManager = project.getFileSystemManager();

        for (DBFVirtualFileSystem fs : fileSystemManager.getVirtualFileSystems()) {
            DBNFileSystem newChild = null;
            if (mergeWith != null) {
                for (DBNFileSystem oldFS : mergeWith) {
                    if (equalsFS(fs, oldFS.getFileSystem())) {
                        newChild = oldFS;
                        break;
                    }
                }
            }
            if (newChild == null) {
                newChild = new DBNFileSystem(this, fs);
            }
            result.add(newChild);
        }

        result.sort((o1, o2) -> o1.getNodeDisplayName().compareToIgnoreCase(o2.getNodeDisplayName()));
        monitor.done();
        return result.toArray(new DBNFileSystem[0]);
    }

    private boolean equalsFS(DBFVirtualFileSystem fs1, DBFVirtualFileSystem fs2) {
        return fs1.getType().equals(fs2.getType()) && fs1.getId().equals(fs2.getId());
    }

    public DBNPathBase findNodeByPath(@NotNull DBRProgressMonitor monitor, @NotNull String path) throws DBException {
        return findNodeByPath(monitor, path, false);
    }

    public DBNPathBase findNodeByPath(
        @NotNull DBRProgressMonitor monitor,
        @NotNull String path,
        boolean shortPath
    ) throws DBException {
        DBNNode curPath = null;
        URI uri;
        try {
            uri = new URI(path);
        } catch (URISyntaxException e) {
            throw new DBException("Bad path: " + path, e);
        }
        String plainPath = uri.getSchemeSpecificPart();
        for (String name : plainPath.split("/")) {
            if (name.isEmpty() || (curPath == null && name.endsWith(":"))) {
                continue;
            }
            {
                if (curPath == null) {
                    this.getChildren(monitor);
                    if (!shortPath) {
                        curPath = this.getFileSystem(uri.getScheme(), name);
                    } else {
                        curPath = this.getRootFolder(monitor, name);
                    }
                } else if (curPath instanceof DBNFileSystem fsNode) {
                    fsNode.getChildren(monitor);
                    curPath = fsNode.getRoot(name);
                } else {
                    DBNPathBase pathNode = (DBNPathBase) curPath;
                    pathNode.getChildren(monitor);
                    curPath = pathNode.getChild(name);
                }
                if (curPath == null) {
                    return null;
                }
            }
        }
        return curPath instanceof DBNPathBase ? (DBNPathBase) curPath : null;
    }

    @Override
    public boolean isManagable() {
        return true;
    }

    @Override
    public DBNNode refreshNode(DBRProgressMonitor monitor, Object source) throws DBException {
        refreshFileSystems(monitor);
        return this;
    }

    private void refreshFileSystems(DBRProgressMonitor monitor) throws DBException {
        if (children != null) {
            children = readChildNodes(monitor, children);
            getModel().fireNodeUpdate(this, this, DBNEvent.NodeChange.REFRESH);
        }
    }

    private void disposeFileSystems() {
        if (children != null) {
            for (DBNFileSystem fs : children) {
                fs.dispose(false);
            }
            children = null;
        }
    }

    @Deprecated
    @Override
    public String getNodeItemPath() {
        return NodePathType.ext.getPrefix() + ((DBNProject) getParentNode()).getProject().getId() + "/" + getName();
    }

    @Override
    public boolean supportsRename() {
        return false;
    }

    @Override
    public void resourceChanged(EFSNIOResource resource, Action action) {
        if (!CommonUtils.equalObjects(getOwnerProject().getEclipseProject(), resource.getProject())) {
            return;
        }
        if (children == null) {
            return;
        }
        DBFVirtualFileSystemRoot dbfRoot = resource.getRoot().getRoot();

        for (DBNFileSystem fs : children) {
            if (CommonUtils.equalObjects(fs.getFileSystem(), dbfRoot.getFileSystem())) {
                DBNFileSystemRoot rootNode = fs.getRoot(dbfRoot);
                if (rootNode != null) {
                    String[] pathSegments = fs.getFileSystem().getURISegments(resource.getFileStore().getPath().toUri());
                    //String[] pathSegments = getPathSegments(resource);

                    DBNPathBase parentNode = rootNode;
                    // NIO path format /[config-id]/root-id/folder1/file1
                    for (int i = 1; i < pathSegments.length - 1; i++) {
                        String itemName = pathSegments[i];
                        DBNPathBase childNode = parentNode.getChild(itemName);
                        if (childNode == null) {
                            log.debug("Cannot find child node '" + itemName + "' in '" + parentNode.getNodeUri() + "'");
                            return;
                        }
                        parentNode = childNode;
                    }

                    switch (action) {
                        case CREATE -> parentNode.addChildResource(resource.getNioPath());
                        case DELETE -> parentNode.removeChildResource(resource.getNioPath());
                        default -> {
                        }
                    }
                }
                break;
            }
        }
    }

    @Override
    public boolean isHidden() {
        return true;
    }

    @Override
    public String toString() {
        return "FileSystems(" + getOwnerProject().getName()  +")";
    }
}
