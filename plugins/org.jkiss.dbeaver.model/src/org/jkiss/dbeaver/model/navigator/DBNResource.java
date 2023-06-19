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
package org.jkiss.dbeaver.model.navigator;

import org.eclipse.core.internal.resources.Resource;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.*;
import org.jkiss.dbeaver.model.app.DBPPlatformDesktop;
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.model.app.DBPResourceHandler;
import org.jkiss.dbeaver.model.fs.DBFRemoteFileStore;
import org.jkiss.dbeaver.model.fs.nio.NIOResource;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.rm.RMConstants;
import org.jkiss.dbeaver.model.runtime.AbstractJob;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.dbeaver.utils.ResourceUtils;
import org.jkiss.utils.ArrayUtils;
import org.jkiss.utils.CommonUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * DBNResource
 */
public class DBNResource extends DBNNode implements DBNNodeWithResource, DBNStreamData {
    private static final Log log = Log.getLog(DBNResource.class);

    private static final DBNNode[] EMPTY_NODES = new DBNNode[0];

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat(DBConstants.DEFAULT_TIMESTAMP_FORMAT);

    private static final NumberFormat numberFormat = new DecimalFormat();

    private IResource resource;
    private DBPResourceHandler handler;
    private DBNNode[] children;
    private DBPImage resourceImage;

    public DBNResource(DBNNode parentNode, IResource resource, DBPResourceHandler handler) {
        super(parentNode);
        this.resource = resource;
        this.handler = handler;
    }

    /**
     * Actual content location can be changed
     */
    protected IResource getContentLocationResource() {
        return resource;
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
    public String getNodeName() {
        if (resource == null || handler == null) {
            return null;
        }
        return resource.getName();
    }

/*
    @Override
    protected String getSortName() {
        if (resource == null || handler == null) {
            return null;
        }
        return resource.getFullPath().removeFileExtension().lastSegment();
    }
*/


    @Override
//    @Property(viewable = false, order = 100)
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
        if (project != null && !project.hasRealmPermission(RMConstants.PERMISSION_PROJECT_RESOURCE_EDIT)) {
            iconImage = new DBIconComposite(iconImage, false, null, null, null, DBIcon.OVER_LOCK);
        }

        return iconImage;
    }

    @NotNull
    protected DBPImage getResourceNodeIcon() {
        if (resourceImage != null) {
            return resourceImage;
        }
/*
        if (resource instanceof IFile) {
            final IContentDescription contentDescription = ((IFile) resource).getContentDescription();
            contentDescription.getContentType().
        }
*/
        if (resource == null) {
            if (this.hasChildren(false)) {
                return DBIcon.TREE_FOLDER;
            } else {
                return DBIcon.TREE_PAGE;
            }
        }
        switch (resource.getType()) {
            case IResource.FOLDER:
                return resource.isLinked() ? DBIcon.TREE_FOLDER_LINK : DBIcon.TREE_FOLDER;
            case IResource.PROJECT:
                return DBIcon.PROJECT;
            default:
                return DBIcon.TREE_PAGE;
        }
    }

    @Override
    public String getNodeTargetName() {
        IResource resource = getResource();
        if (resource != null) {
            IPath location = resource.getLocation();
            if (location != null) {
                File localFile = location.toFile();
                if (localFile != null) {
                    return localFile.getAbsolutePath();
                }
            }
        }
        return super.getNodeTargetName();
    }

    @Override
    public boolean allowsChildren() {
        return resource instanceof IContainer;
    }

    @Override
    public DBNNode[] getChildren(DBRProgressMonitor monitor) throws DBException {
        if (children == null) {
            this.children = readChildNodes(monitor);
        }
        return children;
    }

    protected DBNNode[] readChildNodes(DBRProgressMonitor monitor) throws DBException {
        List<DBNNode> result = new ArrayList<>();
        try {
            IResource contentLocation = getContentLocationResource();
            if (contentLocation instanceof IContainer && contentLocation.exists()) {
                IResource[] members = ((IContainer) contentLocation).members(false);
                members = addImplicitMembers(members);
                for (IResource member : members) {
                    DBNNode newChild = makeNode(member);
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
            filterChildren(result);
            final DBNNode[] childNodes = result.toArray(new DBNNode[0]);
            sortChildren(childNodes);
            return childNodes;
        }
    }

    protected IResource[] addImplicitMembers(IResource[] members) {
        return members;
    }

    DBNResource getChild(IResource resource) {
        if (children == null) {
            return null;
        }
        for (DBNNode child : children) {
            if (child instanceof DBNResource && resource.equals(((DBNResource) child).getResource())) {
                return (DBNResource) child;
            }
        }
        return null;
    }

    private DBNNode makeNode(IResource resource) {
        boolean isRootResource = isRootResource(resource);
        if (isRootResource && resource.getName().startsWith(".")) {
            // Skip project config
            return null;
        }
        try {
            if (resource instanceof IFolder && !isRootResource) {
                // Sub folder
                return handler.makeNavigatorNode(this, resource);
            }
            DBPResourceHandler resourceHandler = DBPPlatformDesktop.getInstance().getWorkspace().getResourceHandler(resource);
            if (resourceHandler == null) {
                log.debug("Skip resource '" + resource.getName() + "'");
                return null;
            }

            return resourceHandler.makeNavigatorNode(this, resource);
        } catch (Exception e) {
            log.error("Error creating navigator node for resource '" + resource.getName() + "'", e);
            return null;
        }
    }

    public boolean isRootResource(IResource resource) {
        return CommonUtils.equalObjects(resource.getParent(), getOwnerProject().getRootResource()) ||
            CommonUtils.equalObjects(resource.getParent(), getOwnerProject().getEclipseProject());
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
        refreshThisResource(monitor);
        return this;
    }

    protected void refreshThisResource(DBRProgressMonitor monitor) throws DBException {
        if (resource == null) {
            return;
        }
        try {
            refreshFileStore(monitor);
            IResource clResource = getContentLocationResource();
            clResource.refreshLocal(IResource.DEPTH_INFINITE, monitor.getNestedMonitor());

            IPath resourceLocation = clResource.getLocation();
            if (resourceLocation != null && !resourceLocation.toFile().exists()) {
                log.debug("Resource '" + clResource.getName() + "' doesn't exists on file system");
                //resource.delete(true, monitor.getNestedMonitor());
            }
        } catch (CoreException e) {
            throw new DBException("Can't refresh resource", e);
        }
    }

    protected void refreshFileStore(@NotNull DBRProgressMonitor monitor) throws DBException {
        if (resource instanceof Resource) {
            final DBFRemoteFileStore remoteFileStore = GeneralUtils.adapt(((Resource) resource).getStore(), DBFRemoteFileStore.class);
            if (remoteFileStore != null) {
                remoteFileStore.refresh(monitor);
            }
        }
    }

    @Override
    public String getNodeItemPath() {
        String projectPath = getRawNodeItemPath();
        return NodePathType.resource.getPrefix() + projectPath;
    }

    @NotNull
    public String getRawNodeItemPath() {
        StringBuilder pathName = new StringBuilder();

        for (DBNNode node = this; node instanceof DBNResource; node = node.getParentNode()) {
            if (pathName.length() > 0) {
                pathName.insert(0, '/');
            }
            IResource resource = ((DBNResource) node).getResource();
            if (resource != null) {
                pathName.insert(0, resource.getName());
            } else {
                pathName.insert(0, "?");
            }
        }
        String projectPath = pathName.toString();
        return projectPath;
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
            throw new DBException("Can't rename resource", e);
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
    public void dropNodes(Collection<DBNNode> nodes) throws DBException {

        new AbstractJob("Drop files to workspace") {
            {
                setUser(true);
            }

            @Override
            protected IStatus run(DBRProgressMonitor monitor) {
                monitor.beginTask("Copy files", nodes.size());
                try {
                    if (!resource.exists()) {
                        if (resource instanceof IFolder) {
                            ((IFolder) resource).create(true, true, new NullProgressMonitor());
                        }
                    }
                    for (DBNNode node : nodes) {
                        IResource otherResource = node.getAdapter(IResource.class);
                        if (otherResource != null) {
                            try {
                                if (otherResource instanceof NIOResource) {
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
                                refreshFileStore(monitor);
                                resource.refreshLocal(IResource.DEPTH_ONE, monitor.getNestedMonitor());
                            } catch (CoreException e) {
                                throw new DBException("Can't copy " + otherResource.getName() + " to " + resource.getName(), e);
                            }
                        } else {
                            throw new DBException("Can't get resource from node " + node.getName());
                        }
                        monitor.worked(1);
                    }
                } catch (Exception e) {
                    return GeneralUtils.makeExceptionStatus(e);
                } finally {
                    monitor.done();
                }
                return Status.OK_STATUS;
            }
        }.schedule();
    }

    @Override
    @NotNull
    public IResource getResource() {
        return resource;
    }

    protected void filterChildren(List<DBNNode> list) {

    }

    protected void sortChildren(DBNNode[] list) {
        Arrays.sort(list, (o1, o2) -> {
            if (o1 instanceof DBNProjectDatabases) {
                return -1;
            } else if (o2 instanceof DBNProjectDatabases) {
                return 1;
            } else {
                if (o1 instanceof DBNResource && o2 instanceof DBNResource) {
                    IResource res1 = ((DBNResource) o1).getResource();
                    IResource res2 = ((DBNResource) o2).getResource();
                    if (res1 instanceof IFolder && !(res2 instanceof IFolder)) {
                        return -1;
                    } else if (res2 instanceof IFolder && !(res1 instanceof IFolder)) {
                        return 1;
                    }
                }
                return o1.getSortName().compareToIgnoreCase(o2.getSortName());
            }
        });
    }

    @Override
    public DBPImage getResourceImage() {
        return this.resourceImage;
    }

    @Override
    public void setResourceImage(DBPImage resourceImage) {
        this.resourceImage = resourceImage;
    }

    public Collection<DBPDataSourceContainer> getAssociatedDataSources() {
        return handler == null ? null : handler.getAssociatedDataSources(this);
    }

    public void refreshResourceState(Object source) {
        DBPResourceHandler newHandler = DBPPlatformDesktop.getInstance().getWorkspace().getResourceHandler(resource);
        if (newHandler != handler) {
            handler = newHandler;
        }
        if (handler != null) {
            handler.updateNavigatorNodeFromResource(this, resource);
        } else {
            log.error("Can't find handler for resource " + resource.getFullPath());
        }
        getModel().fireNodeEvent(new DBNEvent(source, DBNEvent.Action.UPDATE, this));
    }

    protected void handleResourceChange(IResourceDelta delta) {
        if (delta.getKind() == IResourceDelta.CHANGED) {
            // Update this node in navigator
            refreshResourceState(delta);
        }
        if (children == null) {
            // Child nodes are not yet read so nothing to change here - just return
            return;
        }
        //delta.getAffectedChildren(IResourceDelta.ALL_WITH_PHANTOMS, IContainer.INCLUDE_HIDDEN)
        for (IResourceDelta childDelta : delta.getAffectedChildren(IResourceDelta.ALL_WITH_PHANTOMS, IContainer.INCLUDE_HIDDEN)) {
            handleChildResourceChange(childDelta);
        }
    }

    protected void handleChildResourceChange(IResourceDelta delta) {
        final IResource deltaResource = delta.getResource();
        DBNResource childResource = getChild(deltaResource);
        if (childResource == null) {
            if (delta.getKind() == IResourceDelta.ADDED || delta.getKind() == IResourceDelta.CHANGED) {
                // New child or new "grand-child"
                DBNNode newChild = makeNode(deltaResource);
                if (newChild != null) {
                    children = ArrayUtils.add(DBNNode.class, children, newChild);
                    sortChildren(children);
                    getModel().fireNodeEvent(new DBNEvent(delta, DBNEvent.Action.ADD, newChild));

                    if (delta.getKind() == IResourceDelta.CHANGED) {
                        // Notify just created resource
                        // This may happen (e.g.) when first script created in just created script folder
                        childResource = getChild(deltaResource);
                        if (childResource != null) {
                            childResource.handleResourceChange(delta);
                        }
                    }
                }
            } else {
                //log.warn("Can't find resource '" + childDelta.getResource().getName() + "' in navigator model");
            }
        } else {
            if (delta.getKind() == IResourceDelta.REMOVED) {
                // Node deleted
                children = ArrayUtils.remove(DBNNode.class, children, childResource);
                childResource.dispose(true);
            } else {
                // Node changed - handle it recursive
                childResource.handleResourceChange(delta);
            }
        }
    }

    @Property(viewable = true, order = 10)
    public String getResourcePath() {
        return resource == null ? "" : resource.getFullPath().toOSString();
    }

    @Property(viewable = false, order = 11)
    public String getResourceLocation() {
        if (resource == null) {
            return null;
        }
        IPath location = resource.getLocation();
        return location == null ? null : location.toString();
    }

    @Property(viewable = true, order = 11)
    public String getResourceSize() throws CoreException {
        if (resource instanceof IFile) {
            return numberFormat.format(ResourceUtils.getFileLength(resource));
        }
        return null;
    }

    @Property(viewable = true, order = 11)
    public String getResourceLastModified() throws CoreException {
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
        if (resource != null && adapter.isAssignableFrom(resource.getClass())) {
            return adapter.cast(resource);
        }
        return super.getAdapter(adapter);
    }

/*
    @Override
    public IResource getAdaptedResource(IAdaptable adaptable) {
        if (adaptable instanceof DBNResource) {
            return ((DBNResource) adaptable).resource;
        }
        return null;
    }
*/

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
    public long getStreamSize() throws IOException {
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
}
