/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2021 DBeaver Corp and others
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

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBConstants;
import org.jkiss.dbeaver.model.DBIcon;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.DBPImage;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.navigator.DBNEvent;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * DBNResource
 */
public class DBNPath extends DBNNode
{
    private static final Log log = Log.getLog(DBNPath.class);

    private static final DBNNode[] EMPTY_NODES = new DBNNode[0];

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat(DBConstants.DEFAULT_TIMESTAMP_FORMAT);

    private Path path;
    private DBNNode[] children;
    private DBPImage resourceImage;

    public DBNPath(DBNNode parentNode, Path path) {
        super(parentNode);
        this.path = path;
    }

    @Override
    public boolean isDisposed() {
        return path == null || super.isDisposed();
    }

    @Override
    protected void dispose(boolean reflect) {
        children = null;
        this.path = null;
        super.dispose(reflect);
    }

    @Override
    public String getNodeType() {
        return "Path";
    }

    @Override
    @Property(id = DBConstants.PROP_ID_NAME, viewable = true, order = 1)
    public String getNodeName() {
        return path.getFileName().toString();
    }

    @Override
//    @Property(viewable = false, order = 100)
    public String getNodeDescription() {
        return null;
    }

    @Override
    public DBPImage getNodeIcon() {
//        try {
//            return Files.probeContentType(path);
//        } catch (IOException e) {
//            log.debug(e);
//        }
        return DBIcon.TREE_FILE;
    }

    @Override
    public boolean allowsChildren() {
        return Files.isDirectory(path);
    }

    @Override
    public DBNNode[] getChildren(DBRProgressMonitor monitor) throws DBException {
        if (children == null) {
            if (Files.isDirectory(path)) {
                this.children = readChildNodes(monitor);
            }
        }
        return children;
    }

    protected DBNNode[] readChildNodes(DBRProgressMonitor monitor) throws DBException {
        List<DBNNode> result = new ArrayList<>();
        if (Files.exists(path) && Files.isDirectory(path)) {
            try {
                Files.list(path).forEach(c -> {
                    DBNNode newChild = makeNode(c);
                    if (newChild != null) {
                        result.add(newChild);
                    }
                });
            } catch (IOException e) {
                throw new DBException("Error reading directory members", e);
            }
        }
        if (result.isEmpty()) {
            return EMPTY_NODES;
        } else {
            filterChildren(result);
            final DBNNode[] childNodes = result.toArray(new DBNNode[0]);
            sortChildren(childNodes);
            return childNodes;
        }
    }

    private DBNPath getChild(Path thePath) {
        if (children == null) {
            return null;
        }
        for (DBNNode child : children) {
            if (child instanceof DBNPath && thePath.equals(((DBNPath) child).getPath())) {
                return (DBNPath) child;
            }
        }
        return null;
    }

    private DBNPath makeNode(Path resource) {
//        if (resource.isHidden()) {
//            return null;
//        }
//        if (resource.getParent() instanceof IProject && resource.getName().startsWith(".")) {
//            // Skip project config
//            return null;
//        }
        try {
/*
            if (resource instanceof IFolder && resource.getParent() instanceof IFolder) {
                // Sub folder
                return handler.makeNavigatorNode(this, resource);
            }
            DBPResourceHandler resourceHandler = getModel().getPlatform().getWorkspace().getResourceHandler(resource);
            if (resourceHandler == null) {
                log.debug("Skip resource '" + resource.getName() + "'");
                return null;
            }

            return resourceHandler.makeNavigatorNode(this, resource);
*/
            return null;
        } catch (Exception e) {
            log.error("Error creating navigator node for resource '" + resource.toString() + "'", e);
            return null;
        }
    }

    @Override
    public boolean isManagable() {
        return true;
    }

    @Override
    public DBNNode refreshNode(DBRProgressMonitor monitor, Object source) throws DBException {
        children = null;
        return this;
    }

    @Override
    public String getNodeItemPath() {
        return getParentNode().getNodeItemPath() + "/" + path.getFileName().toString();
    }

    @Override
    public boolean supportsRename() {
        return false;
    }

    @Override
    public void rename(DBRProgressMonitor monitor, String newName) throws DBException {
        try {
            Files.move(path, path.getParent().resolve(newName));
        } catch (IOException e) {
            throw new DBException("Can't rename resource", e);
        }
    }

    @Override
    public boolean supportsDrop(DBNNode otherNode) {
        if (!Files.isDirectory(path)) {
            return false;
        }
        if (otherNode == null) {
            // Potentially any other node could be dropped in the folder
            return true;
        }

        // Drop supported only if both nodes are resource with the same handler and DROP feature is supported
        return otherNode instanceof DBNPath
            && otherNode != this
            && otherNode.getParentNode() != this
            && !this.isChildOf(otherNode);
    }

    @Override
    public void dropNodes(Collection<DBNNode> nodes) throws DBException {
        throw new DBException("Not implemented");
    }

    @Nullable
    public Path getPath() {
        return path;
    }

    protected void filterChildren(List<DBNNode> list) {

    }

    protected void sortChildren(DBNNode[] list) {
        Arrays.sort(list, (o1, o2) -> {
            if (o1 instanceof DBNPath && o2 instanceof DBNPath) {
                Path res1 = ((DBNPath) o1).getPath();
                Path res2 = ((DBNPath) o2).getPath();
                if (res1 instanceof IFolder && !(res2 instanceof IFolder)) {
                    return -1;
                } else if (res2 instanceof IFolder && !(res1 instanceof IFolder)) {
                    return 1;
                }
            }
            return o1.getNodeName().compareToIgnoreCase(o2.getNodeName());
        });
    }

    public void createNewFolder(String folderName) throws DBException {
        try {
            if (path instanceof IProject) {
                IFolder newFolder = ((IProject) path).getFolder(folderName);
                if (newFolder.exists()) {
                    throw new DBException("Folder '" + folderName + "' already exists in project '" + path.toString() + "'");
                }
                newFolder.create(true, true, new NullProgressMonitor());
            } else if (path instanceof IFolder) {
                IFolder parentFolder = (IFolder) path;
                if (!parentFolder.exists()) {
                    parentFolder.create(true, true, new NullProgressMonitor());
                }
                IFolder newFolder = parentFolder.getFolder(folderName);
                if (newFolder.exists()) {
                    throw new DBException("Folder '" + folderName + "' already exists in '" + path.toString() + "'");
                }
                newFolder.create(true, true, new NullProgressMonitor());
            }
        } catch (CoreException e) {
            throw new DBException("Can't create new folder", e);
        }
    }

    public Collection<DBPDataSourceContainer> getAssociatedDataSources() {
        return Collections.emptyList();
    }

    public void refreshResourceState(Object source) {
        //path.
        fireNodeEvent(new DBNEvent(source, DBNEvent.Action.UPDATE, this));
    }

    @Property(viewable = true, order = 10)
    public String getResourcePath() {
        return path == null ? "" : path.toAbsolutePath().toString();
    }

    @Property(viewable = false, order = 11)
    public String getResourceLocation() {
        return path == null ? "" : path.toString();
    }

    @Property(viewable = true, order = 11)
    public long getResourceSize() throws IOException {
        return path == null ? 0 : Files.size(path);
    }

    @Property(viewable = true, order = 11)
    public String getResourceLastModified() {
        return path == null ? null : DATE_FORMAT.format(path.toFile().lastModified());
    }

    protected boolean isResourceExists() {
        return path != null && Files.exists(path);
    }

    @Override
    public <T> T getAdapter(Class<T> adapter) {
        if (path != null && adapter.isAssignableFrom(path.getClass())) {
            return adapter.cast(path);
        }
        return super.getAdapter(adapter);
    }

    @Override
    public String toString() {
        return path == null ? super.toString() : path.toString();
    }

}
