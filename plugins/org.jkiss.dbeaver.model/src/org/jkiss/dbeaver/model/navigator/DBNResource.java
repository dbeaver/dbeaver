/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
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

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBConstants;
import org.jkiss.dbeaver.model.DBIcon;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.DBPImage;
import org.jkiss.dbeaver.model.app.DBPResourceHandler;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.utils.ArrayUtils;
import org.jkiss.utils.CommonUtils;

import java.text.SimpleDateFormat;
import java.util.*;

/**
 * DBNResource
 */
public class DBNResource extends DBNNode// implements IContributorResourceAdapter
{
    private static final Log log = Log.getLog(DBNResource.class);
    private static final DBNNode[] EMPTY_NODES = new DBNNode[0];
    private static SimpleDateFormat DATE_FORMAT = new SimpleDateFormat(DBConstants.DEFAULT_TIMESTAMP_FORMAT);

    private IResource resource;
    private DBPResourceHandler handler;
    private DBNNode[] children;
    private DBPImage resourceImage;

    public DBNResource(DBNNode parentNode, IResource resource, DBPResourceHandler handler)
    {
        super(parentNode);
        this.resource = resource;
        this.handler = handler;
    }

    @Override
    public boolean isDisposed() {
        return resource == null || super.isDisposed();
    }

    @Override
    protected void dispose(boolean reflect)
    {
        if (this.handler != null) {
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
        }
        super.dispose(reflect);
    }

    public int getFeatures()
    {
        return handler == null ? 0 : handler.getFeatures(resource);
    }

    @Override
    public String getNodeType()
    {
        return handler == null ? null :handler.getTypeName(resource);
    }

    @Override
    @Property(id = DBConstants.PROP_ID_NAME, viewable = true, order = 1)
    public String getNodeName()
    {
        if (resource == null || handler == null) {
            return null;
        }
        return handler.getResourceNodeName(resource);
//        if (resource instanceof IFile) {
//
//        }
//        return resource.getFullPath().lastSegment();
    }

    @Override
//    @Property(viewable = false, order = 100)
    public String getNodeDescription()
    {
        return handler == null || resource == null ? null : handler.getResourceDescription(resource);
    }

    @Override
    public DBPImage getNodeIcon()
    {
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
            return null;
        }
        switch (resource.getType()) {
            case IResource.FOLDER: return resource.isLinked() ? DBIcon.TREE_FOLDER_LINK : DBIcon.TREE_FOLDER;
            case IResource.PROJECT: return DBIcon.PROJECT;
            default: return DBIcon.TREE_PAGE;
        }
    }

    @Override
    public boolean allowsChildren()
    {
        return resource instanceof IContainer;
    }

    @Override
    public DBNNode[] getChildren(DBRProgressMonitor monitor) throws DBException
    {
        if (children == null) {
            if (resource instanceof IContainer) {
                this.children = readChildNodes(monitor);
            }
        }
        return children;
    }

    protected DBNNode[] readChildNodes(DBRProgressMonitor monitor) throws DBException
    {
        List<DBNNode> result = new ArrayList<>();
        try {
            if (resource.exists()) {
                IResource[] members = ((IContainer) resource).members(false);
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

    DBNResource getChild(IResource resource)
    {
        if (children == null) {
            return null;
        }
        for (DBNNode child : children) {
            if (child instanceof DBNResource && resource.equals(((DBNResource)child).getResource())) {
                return (DBNResource) child;
            }
        }
        return null;
    }

    private DBNNode makeNode(IResource resource)
    {
//        if (resource.isHidden()) {
//            return null;
//        }
        if (resource.getParent() instanceof IProject && resource.getName().startsWith(".")) {
            // Skip project config
            return null;
        }
        try {
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
        } catch (Exception e) {
            log.error("Error creating navigator node for resource '" + resource.getName() + "'", e);
            return null;
        }
    }

    @Override
    public boolean isManagable()
    {
        return true;
    }

    @Override
    public DBNNode refreshNode(DBRProgressMonitor monitor, Object source) throws DBException
    {
        try {
            resource.refreshLocal(IResource.DEPTH_INFINITE, monitor.getNestedMonitor());

            if (!resource.getLocation().toFile().exists()) {
                log.debug("Resource '" + resource.getName() + "' doesn't exists on file system");
                //resource.delete(true, monitor.getNestedMonitor());
            }
        } catch (CoreException e) {
            throw new DBException("Can't refresh resource", e);
        }
        return this;
    }

    @Override
    public String getNodeItemPath() {
        StringBuilder pathName = new StringBuilder();

        for (DBNNode node = this; node instanceof DBNResource; node = node.getParentNode()) {
            if (pathName.length() > 0) {
                pathName.insert(0, '/');
            }
            IResource resource = ((DBNResource) node).getResource();
            if (resource != null) {
                pathName.insert(0, resource.getName());
            } else{
                pathName.insert(0, "?");
            }
        }
        return NodePathType.resource.getPrefix() + pathName.toString();
    }

    @Override
    public boolean supportsRename()
    {
        return (getFeatures() & DBPResourceHandler.FEATURE_RENAME) != 0;
    }

    @Override
    public void rename(DBRProgressMonitor monitor, String newName) throws DBException
    {
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
    public boolean supportsDrop(DBNNode otherNode)
    {
        if (!(resource instanceof IFolder) || (getFeatures() & DBPResourceHandler.FEATURE_MOVE_INTO) == 0) {
            return false;
        }
        if (otherNode == null) {
            // Potentially any other node could be dropped in the folder
            return true;
        }

        // Drop supported only if both nodes are resource with the same handler and DROP feature is supported
        return otherNode instanceof DBNResource
            && otherNode != this
            && otherNode.getParentNode() != this
            && !this.isChildOf(otherNode)
            && ((DBNResource)otherNode).handler == this.handler;
    }

    @Override
    public void dropNodes(Collection<DBNNode> nodes) throws DBException
    {
        for (DBNNode node : nodes) {
            DBNResource resourceNode = (DBNResource) node;
            IResource otherResource = resourceNode.getResource();
            if (otherResource != null) {
                try {
                    otherResource.move(
                        resource.getFullPath().append(otherResource.getName()),
                        true,
                        new NullProgressMonitor());
                } catch (CoreException e) {
                    throw new DBException("Can't delete resource", e);
                }
            }
        }
    }

    @Nullable
    public IResource getResource()
    {
        return resource;
    }

    protected void filterChildren(List<DBNNode> list)
    {

    }

    protected void sortChildren(DBNNode[] list)
    {
        Arrays.sort(list, new Comparator<DBNNode>() {
            @Override
            public int compare(DBNNode o1, DBNNode o2)
            {
                if (o1 instanceof DBNProjectDatabases) {
                    return -1;
                } else if (o2 instanceof DBNProjectDatabases) {
                    return 1;
                } else {
                    if (o1 instanceof DBNResource && o2 instanceof DBNResource) {
                        IResource res1 = ((DBNResource)o1).getResource();
                        IResource res2 = ((DBNResource)o2).getResource();
                        if (res1 instanceof IFolder && !(res2 instanceof IFolder)) {
                            return -1;
                        } else if (res2 instanceof IFolder && !(res1 instanceof IFolder)) {
                            return 1;
                        }
                    }
                    return o1.getNodeName().compareToIgnoreCase(o2.getNodeName());
                }
            }
        });
    }

    public void setResourceImage(DBPImage resourceImage)
    {
        this.resourceImage = resourceImage;
    }

    public void createNewFolder(String folderName)
        throws DBException
    {
        try {
            if (resource instanceof IProject) {
                IFolder newFolder = ((IProject)resource).getFolder(folderName);
                if (newFolder.exists()) {
                    throw new DBException("Folder '" + folderName + "' already exists in project '" + resource.getName() + "'");
                }
                newFolder.create(true, true, new NullProgressMonitor());
            } else if (resource instanceof IFolder) {
                IFolder parentFolder = (IFolder) resource;
                if (!parentFolder.exists()) {
                    parentFolder.create(true, true, new NullProgressMonitor());
                }
                IFolder newFolder = parentFolder.getFolder(folderName);
                if (newFolder.exists()) {
                    throw new DBException("Folder '" + folderName + "' already exists in '" + resource.getFullPath().toString() + "'");
                }
                newFolder.create(true, true, new NullProgressMonitor());
            }
        } catch (CoreException e) {
            throw new DBException("Can't create new folder", e);
        }
    }

    public Collection<DBPDataSourceContainer> getAssociatedDataSources()
    {
        return handler == null ? null : handler.getAssociatedDataSources(this);
    }

    public void refreshResourceState(Object source) {
        DBPResourceHandler newHandler = getModel().getPlatform().getWorkspace().getResourceHandler(resource);
        if (newHandler != handler) {
            handler = newHandler;
        }
        if (handler != null) {
            handler.updateNavigatorNode(this, resource);
        } else {
            log.error("Can't find handler for resource " + resource.getFullPath());
        }
        getModel().fireNodeEvent(new DBNEvent(source, DBNEvent.Action.UPDATE, this));
    }

    protected void handleResourceChange(IResourceDelta delta)
    {
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
        return resource == null ? "" : resource.getLocation().toOSString();
    }

    @Property(viewable = true, order = 11)
    public long getResourceSize() {
        return resource == null ? 0 : resource.getLocation().toFile().length();
    }

    @Property(viewable = true, order = 11)
    public String getResourceLastModified() {
        return resource == null ? null : DATE_FORMAT.format(resource.getLocation().toFile().lastModified());
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
}
