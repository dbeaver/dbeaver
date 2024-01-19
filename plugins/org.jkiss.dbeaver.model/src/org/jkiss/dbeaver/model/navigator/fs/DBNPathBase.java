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

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBConstants;
import org.jkiss.dbeaver.model.DBIcon;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.DBPImage;
import org.jkiss.dbeaver.model.fs.DBFVirtualFileSystemRoot;
import org.jkiss.dbeaver.model.fs.nio.*;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.navigator.*;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.utils.ArrayUtils;
import org.jkiss.utils.ByteNumberFormat;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * DBNPath
 */
public abstract class DBNPathBase extends DBNNode implements DBNNodeWithResource, DBNLazyNode {

    private static final Log log = Log.getLog(DBNPath.class);

    private static final DBNNode[] EMPTY_NODES = new DBNNode[0];

    private static final ByteNumberFormat numberFormat = new ByteNumberFormat();

    private DBNNode[] children;
    private DBPImage resImage;
    // Cache expensive properties
    private transient Long size;
    private transient FileTime lastModified;

    protected DBNPathBase(DBNNode parentNode) {
        super(parentNode);
    }

    public abstract Path getPath();
    protected abstract void setPath(Path path);

    @Override
    protected void dispose(boolean reflect) {
        this.children = null;
        super.dispose(reflect);
    }

    @Override
    public IResource getResource() {
        return getAdapter(IResource.class);
    }

    @Override
    public DBPImage getResourceImage() {
        return resImage;
    }

    @Override
    public void setResourceImage(DBPImage resourceImage) {
        this.resImage = resourceImage;
    }

    @Override
    public String getNodeType() {
        return NodePathType.dbvfs.name() + ".path";
    }

    @Override
    @Property(id = DBConstants.PROP_ID_NAME, viewable = true, order = 1)
    public String getNodeDisplayName() {
        return getFileName();
    }

    // Path's file name may be null (e.g. forFS root folder)
    // Then try to extract it from URI or from toString
    private String getFileName() {
        return URLDecoder.decode(EFSNIOResource.getPathFileNameOrHost(getPath()), StandardCharsets.UTF_8);
    }

    @Override
//    @Property(viewable = false, order = 100)
    public String getNodeDescription() {
        return null;
    }

    @Override
    public DBPImage getNodeIcon() {
        if (resImage != null) {
            return resImage;
        }
        return allowsChildren() ? DBIcon.TREE_FOLDER : DBIcon.TREE_FILE;
    }

    @Override
    public boolean allowsChildren() {
        return Files.isDirectory(getPath());
    }

    @Override
    public DBNNode[] getChildren(DBRProgressMonitor monitor) throws DBException {
        if (children == null && allowsChildren()) {
            this.children = readChildNodes(monitor);
        }
        return children;
    }

    protected DBNNode[] readChildNodes(DBRProgressMonitor monitor) throws DBException {
        List<DBNNode> result;
        Path path = getPath();
        if (allowsChildren() && Files.exists(path)) {
            try {
                try (Stream<Path> fileList = Files.list(path)) {
                    result = fileList.map(this::makeNode).collect(Collectors.toList());
                }
            } catch (IOException e) {
                throw new DBException("Error reading directory members", e);
            }
        } else {
            result = Collections.emptyList();
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

    public DBNPathBase getChild(Path thePath) {
        if (children == null) {
            return null;
        }
        for (DBNNode child : children) {
            if (child instanceof DBNPathBase && thePath.equals(((DBNPathBase) child).getPath())) {
                return (DBNPathBase) child;
            }
        }
        return null;
    }

    public DBNPathBase getChild(String name) {
        if (children == null) {
            return null;
        }
        for (DBNNode child : children) {
            if (child.getName().equals(name)) {
                return (DBNPathBase) child;
            }
        }
        return null;
    }

    public void addChildResource(Path path) {
        if (children == null) {
            return;
        }
        DBNPath child = new DBNPath(this, path);
        children = ArrayUtils.add(DBNNode.class, children, child);
        fireNodeEvent(new DBNEvent(this, DBNEvent.Action.ADD, child));
    }

    public void removeChildResource(Path path) {
        if (children == null) {
            return;
        }
        DBNPathBase child = getChild(path);
        if (child != null) {
            children = ArrayUtils.remove(DBNNode.class, children, child);
            fireNodeEvent(new DBNEvent(this, DBNEvent.Action.REMOVE, child));
        }
    }

    private DBNPathBase makeNode(Path resource) {
        return new DBNPath(this, resource);
    }

    @Override
    public boolean isManagable() {
        return true;
    }

    @Override
    public DBNNode refreshNode(DBRProgressMonitor monitor, Object source) throws DBException {
        children = null;
        size = null;
        lastModified = null;

        this.fireNodeEvent(new DBNEvent(source, DBNEvent.Action.UPDATE, DBNEvent.NodeChange.REFRESH, this));
        return this;
    }

    @Deprecated
    @Override
    public String getNodeItemPath() {
        return getParentNode().getNodeItemPath() + "/" + getName();
    }

    @Override
    public boolean supportsRename() {
        return false;
    }

    @Override
    public void rename(DBRProgressMonitor monitor, String newName) throws DBException {
        Path path = getPath();
        try {
            setPath(Files.move(path, path.getParent().resolve(newName)));
        } catch (IOException e) {
            throw new DBException("Can't rename resource", e);
        }
    }

    @Override
    public boolean supportsDrop(DBNNode otherNode) {
        if (otherNode == null) {
            return true;
        }

        if (Files.isRegularFile(getPath())) {
            return getParentNode().supportsDrop(otherNode);
        }

        // Drop supported only if both nodes are resource with the same handler and DROP feature is supported
        return otherNode.getAdapter(IResource.class) != null
            && otherNode != this
            && otherNode.getParentNode() != this
            && !this.isChildOf(otherNode);
    }

    @Override
    public void dropNodes(DBRProgressMonitor monitor, Collection<DBNNode> nodes) throws DBException {
        IContainer folder;
        IResource thisResource = getResource();
        if (thisResource instanceof IContainer) {
            folder = (IContainer) thisResource;
        } else {
            folder = thisResource.getParent();
        }
        if (!(folder instanceof IFolder)) {
            throw new DBException("Can't drop files into non-folder");
        }
        if (nodes.isEmpty()) {
            return;
        }

        // Confirm\
        {
            boolean doCopy = !isTheSameFileSystem(nodes.iterator().next());
            String action = (doCopy ? "Copy" : "Move") + " resource(s)";
            String message =
                action + "\n" +
                    nodes.stream().map(DBNNode::getNodeDisplayName).collect(Collectors.joining(",")) +
                "\ninto folder " + folder.getFullPath() + "?";
            if (!DBWorkbench.getPlatformUI().confirmAction(action, message)) {
                return;
            }
        }

        monitor.beginTask("Drop files", nodes.size());
        try {
            for (DBNNode node : nodes) {
                if (monitor.isCanceled()) {
                    break;
                }
                IResource resource = node.getAdapter(IResource.class);
                if (resource == null || !resource.exists()) {
                    log.debug("Resource " + resource + " doesn't not exists");
                    continue;
                }
                if (!(resource instanceof IFile)) {
                    log.debug("Resource " + resource + " is not a file");
                    continue;
                }
                if (resource.getParent().equals(folder)) {
                    // Already in this container
                    continue;
                }
                boolean doCopy = !isTheSameFileSystem(node);
                boolean doDelete = false;
                monitor.subTask((doCopy ? "Copy" : "Move") + " file " + resource.getName());
                try {

                    IFile targetFile = ((IFolder) folder).getFile(resource.getName());

                    if (!doCopy) {
                        // Try to move first
                        // Note that move is not supported by some file systems
                        boolean wasMoved = false;
                        if (targetFile instanceof EFSNIOFile nioFile) {
                            EFSNIOPath nioTargetPath = new EFSNIOPath(nioFile.getNioPath());
                            try {
                                resource.move(nioTargetPath, true, monitor.getNestedMonitor());
                                wasMoved = true;
                            } catch (UnsupportedOperationException e) {
                                log.debug("Underlying FS doesn't support file move. Do copy instead");
                            }
                        }
                        if (!wasMoved) {
                            doCopy = true;
                            doDelete = true;
                        }
                    }

                    // Copy files
                    if (doCopy) {
                        try (InputStream is = ((IFile) resource).getContents()) {
                            if (targetFile.exists()) {
                                targetFile.setContents(is, true, false, monitor.getNestedMonitor());
                            } else {
                                targetFile.create(is, true, monitor.getNestedMonitor());
                            }
                        }
                    }
                    if (doDelete) {
                        // Delete source file after copy
                        resource.delete(true, monitor.getNestedMonitor());
                    }
                } finally {
                    monitor.worked(1);
                }
            }
        } catch (Exception e) {
            throw new DBException("Error creating NIO resource", e);
        } finally {
            monitor.done();
        }
    }

    protected boolean isTheSameFileSystem(DBNNode node) {
        return false;
    }

    protected void filterChildren(List<DBNNode> list) {

    }

    protected void sortChildren(DBNNode[] list) {
        Arrays.sort(list, (o1, o2) -> {
            if (o1 instanceof DBNPathBase && o2 instanceof DBNPathBase) {
                Path res1 = ((DBNPathBase) o1).getPath();
                Path res2 = ((DBNPathBase) o2).getPath();
                if (res1 instanceof IFolder && !(res2 instanceof IFolder)) {
                    return -1;
                } else if (res2 instanceof IFolder && !(res1 instanceof IFolder)) {
                    return 1;
                }
            }
            return o1.getNodeDisplayName().compareToIgnoreCase(o2.getNodeDisplayName());
        });
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
        return getPath() == null ? "" : getPath().toAbsolutePath().toString();
    }

    @Property(viewable = false, order = 11)
    public String getResourceLocation() {
        return getPath() == null ? "" : getPath().toString();
    }

    @Property(viewable = true, order = 11)
    public String getResourceSize() throws IOException {
        if (size == null) {
            try {
                size = getPath() == null ? 0 : Files.size(getPath());
            } catch (IOException e) {
                log.debug("Error reading file '" + getPath() + "' size", e);
            }
            if (size == null) {
                size = 0L;
            }
        }
        return numberFormat.format(size);
    }

    @Property(viewable = true, order = 11)
    public String getResourceLastModified() throws IOException {
        if (lastModified == null) {
            lastModified = Files.getLastModifiedTime(getPath());
        }
        if (lastModified.toMillis() <= 0) {
            return null;
        }
        return lastModified.toString();
    }

    protected boolean isResourceExists() {
        return getPath() != null && Files.exists(getPath());
    }

    @Override
    public <T> T getAdapter(Class<T> adapter) {
        if (adapter == Path.class) {
            return adapter.cast(getPath());
        } else if (adapter == IResource.class) {
            DBNFileSystemRoot rootNode = this instanceof DBNFileSystemRoot ?
                (DBNFileSystemRoot) this :
                DBNUtils.getParentOfType(DBNFileSystemRoot.class, this);
            if (rootNode == null) {
                return null;
            }
            DBFVirtualFileSystemRoot fsRoot = rootNode.getRoot();
            EFSNIOFileSystemRoot root = new EFSNIOFileSystemRoot(
                getOwnerProject().getEclipseProject(),
                fsRoot,
                fsRoot.getFileSystem().getType() + "/" + fsRoot.getFileSystem().getId() + "/" + fsRoot.getRootId()
            );
            Path path = getPath();
            IResource resource;
            if (allowsChildren()) {
                resource = new EFSNIOFolder(root, path);
            } else {
                resource = new EFSNIOFile(root, path);
            }
            return adapter.cast(resource);
        }
        return super.getAdapter(adapter);
    }

    @Override
    public String toString() {
        Path path = getPath();
        return path == null ? super.toString() : path.toString();
    }

    @Override
    public boolean needsInitialization() {
        return children == null;
    }

    @Override
    public boolean isRemoteResource() {
        return true;
    }
}
