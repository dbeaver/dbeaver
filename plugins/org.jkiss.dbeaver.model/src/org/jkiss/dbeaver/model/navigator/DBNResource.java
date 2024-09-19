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
package org.jkiss.dbeaver.model.navigator;

import org.eclipse.core.internal.resources.Resource;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.*;
import org.jkiss.dbeaver.model.app.*;
import org.jkiss.dbeaver.model.fs.DBFRemoteFileStore;
import org.jkiss.dbeaver.model.fs.nio.EFSNIOResource;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.rcp.RCPProject;
import org.jkiss.dbeaver.model.rm.RMConstants;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.dbeaver.utils.ResourceUtils;
import org.jkiss.utils.AlphanumericComparator;
import org.jkiss.utils.ArrayUtils;
import org.jkiss.utils.CommonUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * DBNResource
 */
public class DBNResource extends DBNNode implements DBNStreamData, DBNNodeWithCache, DBNLazyNode {
    private static final Log log = Log.getLog(DBNResource.class);

    //TODO: create real resource root node
    //this 'node' exist to avoid collision between resource folders and other root nodes
    //example: you can create 'datasources' folder, and nodeUri will be the same as for DBNProjectDatabases
    public static final String FAKE_RESOURCE_ROOT_NODE = "resources";

    private static final DBNNode[] EMPTY_NODES = new DBNNode[0];

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat(DBConstants.DEFAULT_TIMESTAMP_FORMAT);

    private static final NumberFormat numberFormat = new DecimalFormat();
    private static final Comparator<DBNNode> COMPARATOR = (o1, o2) -> {
        if (o1 instanceof DBNProjectDatabases) {
            return -1;
        } else if (o2 instanceof DBNProjectDatabases) {
            return 1;
        } else if (o1 instanceof DBNResource && o2 instanceof DBNResource) {
            IResource res1 = ((DBNResource) o1).getResource();
            IResource res2 = ((DBNResource) o2).getResource();
            if (res1 instanceof IFolder && !(res2 instanceof IFolder)) {
                return -1;
            } else if (res2 instanceof IFolder && !(res1 instanceof IFolder)) {
                return 1;
            }
        }

        return AlphanumericComparator.getInstance().compare(o1.getNodeDisplayName(), o2.getNodeDisplayName());
    };

    private IResource resource;
    private DBPResourceHandler handler;
    private DBNNode[] children;

    public DBNResource(DBNNode parentNode, IResource resource, DBPResourceHandler handler) {
        super(parentNode);
        this.resource = resource;
        this.handler = handler;
    }

    public DBPResourceHandler getHandler() {
        return handler;
    }

    public void setHandler(DBPResourceHandler handler) {
        this.handler = handler;
    }

    @Override
    public boolean isDisposed() {
        return resource == null || super.isDisposed();
    }

    @Override
    protected void dispose(boolean reflect) {
        if (children != null) {
            for (DBNNode child : children) {
                child.dispose(reflect);
            }
            children = null;
        }
        if (reflect) {
            getModel().fireNodeEvent(new DBNEvent(this, DBNEvent.Action.REMOVE, this));
        }
        this.resource = null;
        this.handler = null;
        super.dispose(reflect);
    }

    public int getFeatures() {
        return handler == null ? 0 : handler.getFeatures(resource);
    }

    @Override
    public String getNodeType() {
        return handler == null ? getResourceNodeType() : handler.getTypeName(resource);
    }

    protected String getResourceNodeType() {
        return "resource";
    }

    @Override
    @Property(id = DBConstants.PROP_ID_NAME, viewable = true, order = 1)
    public String getNodeDisplayName() {
        if (resource == null || handler == null) {
            return null;
        }
        return resource.getName();
    }

    @Override
    public String getNodeDescription() {
        if (getOwnerProject().isVirtual()) {
            // Do not read descriptions for virtual (remote) projects
            return null;
        }
        return handler == null || resource == null ? null : handler.getResourceDescription(resource);
    }

    @NotNull
    @Override
    public DBPImage getNodeIcon() {
        DBPImage iconImage = this.getResourceNodeIcon();

        DBPProject project = getOwnerProject();
        if (!project.hasRealmPermission(RMConstants.PERMISSION_PROJECT_RESOURCE_EDIT)) {
            iconImage = new DBIconComposite(iconImage, false, null, null, null, DBIcon.OVER_LOCK);
        }

        return iconImage;
    }

    @NotNull
    protected DBPImage getResourceNodeIcon() {
        if (resource == null) {
            if (this.hasChildren(false)) {
                return DBIcon.TREE_FOLDER;
            } else {
                return DBIcon.TREE_PAGE;
            }
        }
        DBPImage resourceImage = handler.getResourceIcon(resource);
        if (resourceImage != null) {
            return resourceImage;
        }

        return switch (resource.getType()) {
            case IResource.FOLDER -> resource.isLinked() ? DBIcon.TREE_FOLDER_LINK : DBIcon.TREE_FOLDER;
            case IResource.PROJECT -> DBIcon.PROJECT;
            default -> DBIcon.TREE_PAGE;
        };
    }

    @Override
    public String getNodeTargetName() {
        IResource resource = getResource();
        IPath location = resource.getLocation();
        if (location != null) {
            Path localFile = location.toPath();
            if (localFile != null) {
                return localFile.toString();
            }
        }
        return super.getNodeTargetName();
    }

    @Override
    public boolean allowsChildren() {
        return resource instanceof IContainer;
    }

    @Override
    public DBNNode[] getChildren(@NotNull DBRProgressMonitor monitor) throws DBException {
        if (children == null && !monitor.isForceCacheUsage()) {
            this.children = readChildNodes(monitor, this);
        }
        return children;
    }

    @Override
    public DBNNode[] getCachedChildren() {
        return children;
    }

    @Override
    public void setCachedChildren(DBNNode[] children) {
        this.children = children;
    }

    public static DBNNode[] readChildNodes(DBRProgressMonitor monitor, DBNNode node) throws DBException {
        List<DBNNode> result = new ArrayList<>();
        try {
            IResource contentLocation = node.getAdapter(IResource.class);
            if (contentLocation instanceof IContainer && contentLocation.exists()) {
                IResource[] members = ((IContainer) contentLocation).members(false);
                members = addImplicitMembers(node, members);
                for (IResource member : members) {
                    DBNNode newChild = makeNode(node, member);
                    if (newChild != null) {
                        result.add(newChild);
                    }
                }
            }
        } catch (CoreException e) {
            throw new DBException("Can't read container's members", e);
        }
        if (result.isEmpty()) {
            return EMPTY_NODES;
        } else {
            final DBNNode[] childNodes = result.toArray(new DBNNode[0]);
            sortChildren(childNodes);
            return childNodes;
        }
    }

    static IResource[] addImplicitMembers(DBNNode node, IResource[] members) {
        IResource resource = node.getAdapter(IResource.class);
        if (resource instanceof IProject) {
            DBPProject project = node.getOwnerProject();
            DBPWorkspace workspace = project.getWorkspace();
            if (workspace instanceof DBPWorkspaceDesktop) {
                for (DBPResourceHandlerDescriptor rh : ((DBPWorkspaceDesktop)workspace).getAllResourceHandlers()) {
                    IFolder rhDefaultRoot = ((DBPWorkspaceDesktop)workspace).getResourceDefaultRoot(project, rh, false);
                    if (rhDefaultRoot != null && !rhDefaultRoot.exists()) {
                        // Add as explicit member
                        members = ArrayUtils.add(IResource.class, members, rhDefaultRoot);
                    }
                }
            }
            return members;
        }
        return members;
    }

    public DBNResource getChild(IResource resource) {
        if (children == null) {
            return null;
        }
        for (DBNNode child : children) {
            if (child instanceof DBNResource resNode && resource.equals(resNode.getResource())) {
                return resNode;
            }
        }
        return null;
    }

    public static boolean isRootResource(DBPProject ownerProject, IResource resource) {
        return ownerProject instanceof RCPProject rcpProject &&
               (CommonUtils.equalObjects(resource.getParent(), rcpProject.getRootResource()) ||
                CommonUtils.equalObjects(resource.getParent(), rcpProject.getEclipseProject()));
    }

    public static DBNNode makeNode(DBNNode parentNode, IResource resource) {
        boolean isRootResource = isRootResource(parentNode.getOwnerProject(), resource);
        if (isRootResource && resource.getName().startsWith(".")) {
            // Skip project config
            return null;
        }
        try {
            if (parentNode instanceof DBNResource resourceNode && resource instanceof IFolder && !isRootResource) {
                // Sub folder
                return resourceNode.getHandler().makeNavigatorNode(parentNode, resource);
            }
            DBPResourceHandler resourceHandler = DBPPlatformDesktop.getInstance().getWorkspace().getResourceHandler(resource);
            if (resourceHandler == null) {
                log.debug("Skip resource '" + resource.getName() + "'");
                return null;
            }

            return resourceHandler.makeNavigatorNode(parentNode, resource);
        } catch (Exception e) {
            log.error("Error creating navigator node for resource '" + resource.getName() + "'", e);
            return null;
        }
    }


    @Override
    public boolean isManagable() {
        return true;
    }

    @Override
    public DBNNode refreshNode(DBRProgressMonitor monitor, Object source) throws DBException {
        if (children != null) {
//            for (DBNNode child : children) {
//                child.dispose(false);
//            }
            children = null;
        }
        refreshThisResource(monitor, this);
        return this;
    }

    public static void refreshThisResource(DBRProgressMonitor monitor, DBNNode resNode) throws DBException {
        IResource resource = resNode.getAdapter(IResource.class);
        if (resource == null) {
            return;
        }
        try {
            refreshFileStore(monitor, resource);
            resource.refreshLocal(IResource.DEPTH_INFINITE, monitor.getNestedMonitor());

            IPath resourceLocation = resource.getLocation();
            if (resourceLocation != null && !resourceLocation.toFile().exists()) {
                log.debug("Resource '" + resource.getName() + "' doesn't exists on file system");
                //resource.delete(true, monitor.getNestedMonitor());
            }
        } catch (CoreException e) {
            throw new DBException("Can't refresh resource", e);
        }
    }

    public static void refreshFileStore(@NotNull DBRProgressMonitor monitor, IResource resource) throws DBException {
        if (resource instanceof Resource) {
            final DBFRemoteFileStore remoteFileStore = GeneralUtils.adapt(((Resource) resource).getStore(), DBFRemoteFileStore.class);
            if (remoteFileStore != null) {
                remoteFileStore.refresh(monitor);
            }
        }
    }

    @Deprecated
    @Override
    public String getNodeItemPath() {
        String projectPath = getRawNodeItemPath();
        return NodePathType.resource.getPrefix() + projectPath;
    }

    @NotNull
    public String getRawNodeItemPath() {
        StringBuilder pathName = new StringBuilder();

        for (DBNNode node = this; node instanceof DBNResource dbnResource; node = node.getParentNode()) {
            if (!pathName.isEmpty()) {
                pathName.insert(0, '/');
            }
            IResource resource = dbnResource.getResource();
            pathName.insert(0, resource.getName());
        }
        return pathName.toString();
    }

    @Override
    public boolean supportsRename() {
        return (getFeatures() & DBPResourceHandler.FEATURE_RENAME) != 0;
    }

    @Override
    public void rename(DBRProgressMonitor monitor, String newName) throws DBException {
        try {
            if (newName.indexOf('.') == -1 && resource instanceof IFile) {
                String ext = resource.getFileExtension();
                if (!CommonUtils.isEmpty(ext)) {
                    newName += "." + ext;
                }
            }
            if (!newName.equals(resource.getName())) {
                if (resource.isLinked()) {
                    resource.move(resource.getFullPath().removeLastSegments(1).append(newName), IResource.SHALLOW, monitor.getNestedMonitor());
                } else {
                    resource.move(resource.getFullPath().removeLastSegments(1).append(newName), true, monitor.getNestedMonitor());
                }
            }
        } catch (CoreException e) {
            throw new DBException("Cannot rename resource : " + e.getMessage(), e);
        }
    }

    @Override
    public boolean supportsDrop(DBNNode otherNode) {
        if (!(resource instanceof IFolder) || (getFeatures() & DBPResourceHandler.FEATURE_MOVE_INTO) == 0) {
            return false;
        }
        if (otherNode == null) {
            // Potentially any other node could be dropped in the folder
            return true;
        }

        // Drop supported only if both nodes are resource with the same handler and DROP feature is supported
        return otherNode.getAdapter(IResource.class) != null
            && otherNode != this
            && otherNode.getParentNode() != this;
    }

    @Override
    public void dropNodes(DBRProgressMonitor monitor, Collection<DBNNode> nodes) throws DBException {
        monitor.beginTask("Copy files", nodes.size());
        try {
            if (!resource.exists()) {
                if (resource instanceof IFolder) {
                    try {
                        ((IFolder) resource).create(true, true, new NullProgressMonitor());
                    } catch (CoreException e) {
                        throw new DBException("Error creating folder " + resource.getName(), e);
                    }
                }
            }
            for (DBNNode node : nodes) {
                IResource otherResource = node.getAdapter(IResource.class);
                if (otherResource != null) {
                    try {
                        if (otherResource instanceof EFSNIOResource) {
                            otherResource.copy(
                                resource.getRawLocation().append(otherResource.getName()),
                                true,
                                monitor.getNestedMonitor());
                        } else {
                            if (DBWorkbench.isDistributed() && !CommonUtils.equalObjects(otherResource.getProject(), resource.getProject())) {
                                throw new DBException("Cross-project resource move is not supported in distributed workspaces");
                            }
                            otherResource.move(
                                resource.getFullPath().append(otherResource.getName()),
                                true,
                                monitor.getNestedMonitor());
                        }
                        refreshFileStore(monitor, resource);
                        resource.refreshLocal(IResource.DEPTH_ONE, monitor.getNestedMonitor());
                    } catch (CoreException e) {
                        throw new DBException("Can't copy " + otherResource.getName() + " to " + resource.getName(), e);
                    }
                } else {
                    throw new DBException("Can't get resource from node " + node.getName());
                }
                monitor.worked(1);
            }
        } finally {
            monitor.done();
        }
    }

    public boolean supportsPaste(@NotNull DBNNode other) {
        return false;
    }

    public void pasteNodes(@NotNull Collection<DBNNode> nodes) throws DBException {
        throw new DBException("Paste is not supported");
    }

    @NotNull
    public IResource getResource() {
        return resource;
    }

    protected static void sortChildren(DBNNode[] list) {
        Arrays.sort(list, COMPARATOR);
    }

    public Collection<DBPDataSourceContainer> getAssociatedDataSources() {
        return handler == null ? null : handler.getAssociatedDataSources(this);
    }

    public void refreshResourceState(Object source) {
        DBPResourceHandler newHandler = DBPPlatformDesktop.getInstance().getWorkspace().getResourceHandler(resource);
        if (newHandler != handler) {
            handler = newHandler;
        }
        getModel().fireNodeEvent(new DBNEvent(source, DBNEvent.Action.UPDATE, this));
    }

    @Property(viewable = true, order = 10)
    public String getResourcePath() {
        return resource == null ? "" : resource.getFullPath().toOSString();
    }

    @Property(order = 11)
    public String getResourceLocation() {
        if (resource == null) {
            return null;
        }
        IPath location = resource.getLocation();
        return location == null ? null : location.toString();
    }

    @Property(viewable = true, order = 11)
    public String getResourceSize() {
        if (resource instanceof IFile) {
            return numberFormat.format(ResourceUtils.getFileLength(resource));
        }
        return null;
    }

    @Property(viewable = true, order = 11)
    public String getResourceLastModified() {
        if (resource instanceof IFile) {
            long lastModified = ResourceUtils.getResourceLastModified(resource);
            return lastModified <= 0 ? "" : DATE_FORMAT.format(lastModified);
        }
        return null;
    }

    protected boolean isResourceExists() {
        return resource != null && resource.exists();
    }

    @Override
    public <T> T getAdapter(Class<T> adapter) {
        if (resource != null) {
            if (adapter.isAssignableFrom(resource.getClass())) {
                return adapter.cast(resource);
            }
            if (adapter == Path.class) {
                return adapter.cast(resource.getLocation().toPath());
            }
        }
        return super.getAdapter(adapter);
    }

    @Override
    public String toString() {
        return resource == null ? super.toString() : resource.toString();
    }

    @Override
    public boolean supportsStreamData() {
        // Supports streaming only for remote files
        return resource instanceof IFile && resource.getLocation() == null;
    }

    @Override
    public long getStreamSize() {
        return ResourceUtils.getFileLength(resource);
    }

    @Override
    public InputStream openInputStream() throws DBException, IOException {
        if (resource instanceof IFile) {
            try {
                return ((IFile) resource).getContents();
            } catch (CoreException e) {
                throw new IOException(e);
            }
        }
        throw new DBException("Resource '" + getNodeTargetName() + "' doesn't support streaming");
    }

    @Override
    public boolean needsInitialization() {
        return children == null;
    }

}
