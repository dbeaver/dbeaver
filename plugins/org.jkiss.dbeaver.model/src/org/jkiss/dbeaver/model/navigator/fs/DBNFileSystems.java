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
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.*;
import org.jkiss.dbeaver.model.fs.DBFFileSystemManager;
import org.jkiss.dbeaver.model.fs.DBFVirtualFileSystem;
import org.jkiss.dbeaver.model.fs.DBFVirtualFileSystemRoot;
import org.jkiss.dbeaver.model.fs.nio.EFSNIOListener;
import org.jkiss.dbeaver.model.fs.nio.EFSNIOMonitor;
import org.jkiss.dbeaver.model.fs.nio.EFSNIOResource;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.navigator.DBNEvent;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.model.navigator.DBNProject;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.utils.CommonUtils;

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

        EFSNIOMonitor.removeListener(this);
    }

    @Override
    public String getNodeType() {
        return "dbvfs";
    }

    @Override
    @Property(id = DBConstants.PROP_ID_NAME, viewable = true, order = 1)
    public String getNodeName() {
        return "dbvfs";
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
            this.children = readChildNodes(monitor);
        }
        return children;
    }

    protected DBNFileSystem[] readChildNodes(DBRProgressMonitor monitor) throws DBException {
        monitor.beginTask("Read available file systems", 1);
        List<DBNFileSystem> result = new ArrayList<>();
        var project = getOwnerProject();
        if (project == null) {
            return new DBNFileSystem[0];
        }
        DBFFileSystemManager fileSystemManager = project.getFileSystemManager();

        for (DBFVirtualFileSystem fs : fileSystemManager.getVirtualFileSystems()) {
            DBNFileSystem newChild = new DBNFileSystem(this, fs);
            result.add(newChild);
        }

        result.sort(DBUtils.nameComparatorIgnoreCase());
        monitor.done();
        return result.toArray(new DBNFileSystem[0]);
    }

    public DBNPathBase findNodeByPath(DBRProgressMonitor monitor, String path) throws DBException {
        getChildren(monitor);

        DBNFileSystemRoot fsNode = null;
        DBNPathBase curPath = null;
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

    @Override
    public boolean isManagable() {
        return true;
    }

    @Override
    public DBNNode refreshNode(DBRProgressMonitor monitor, Object source) {
        children = null;
        return this;
    }

    public void resetFileSystems() {
        children = null;
        getModel().fireNodeUpdate(this, this, DBNEvent.NodeChange.REFRESH);
    }

    @Override
    public String getNodeItemPath() {
        return NodePathType.ext.getPrefix() + ((DBNProject) getParentNode()).getProject().getId() + "/" + getNodeName();
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
                    String[] pathSegments = resource.getFullPath().segments();
                    DBNPathBase parentNode = rootNode;
                    for (int i = 2; i < pathSegments.length - 1; i++) {
                        String itemName = pathSegments[i];
                        DBNPathBase childNode = parentNode.getChild(itemName);
                        if (childNode == null) {
                            log.debug("Cannot find child node '" + itemName + "' in '" + parentNode.getNodeItemPath() + "'");
                            return;
                        }
                        parentNode = childNode;
                    }

                    switch (action) {
                        case CREATE:
                            parentNode.addChildResource(resource.getNioPath());
                            break;
                        case DELETE:
                            parentNode.removeChildResource(resource.getNioPath());
                            break;
                        default:
                            break;
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
