/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.navigator;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.model.project.DBPResourceHandler;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.registry.ProjectRegistry;
import org.jkiss.dbeaver.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.ui.ICommandIds;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * DBNResource
 */
public class DBNResource extends DBNNode
{
    private IResource resource;
    private DBPResourceHandler handler;
    private List<DBNNode> children;
    private Image resourceImage;

    public DBNResource(DBNNode parentNode, IResource resource, DBPResourceHandler handler)
    {
        super(parentNode);
        this.resource = resource;
        this.handler = handler;
    }

    protected void dispose(boolean reflect)
    {
        this.resource = null;
        if (children != null) {
            for (DBNNode child : children) {
                child.dispose(reflect);
            }
            children = null;
        }
        super.dispose(reflect);
    }

    public int getFeatures()
    {
        return handler.getFeatures(resource);
    }

    @Override
    public String getNodeType()
    {
        return handler.getTypeName(resource);
    }

    public String getNodeName()
    {
        return resource.getName();
    }

    public String getNodeDescription()
    {
        return getNodeName();
    }

    @Override
    public Image getNodeIcon()
    {
        if (resourceImage != null) {
            return resourceImage;
        }
        String imageKey = ISharedImages.IMG_OBJ_ELEMENT;
        switch (resource.getType()) {
            case IResource.FOLDER: imageKey = ISharedImages.IMG_OBJ_FOLDER; break;
            case IResource.FILE: imageKey = ISharedImages.IMG_OBJ_FILE; break;
            case IResource.PROJECT: imageKey = ISharedImages.IMG_OBJ_PROJECT; break;
        }
        return PlatformUI.getWorkbench().getSharedImages().getImage(imageKey);
        //return DBIcon.PROJECT.getImage();
    }

    @Override
    public boolean hasChildren()
    {
        return resource instanceof IContainer;
    }

    @Override
    public boolean hasNavigableChildren()
    {
        return hasChildren();
    }

    @Override
    public List<? extends DBNNode> getChildren(DBRProgressMonitor monitor) throws DBException
    {
        if (children == null) {
            if (resource instanceof IContainer) {
                final ProjectRegistry projectRegistry = DBeaverCore.getInstance().getProjectRegistry();
                List<DBNNode> result = new ArrayList<DBNNode>();
                try {
                    IResource[] members = ((IContainer) resource).members();
                    for (IResource member : members) {
                        DBNNode newChild = makeNode(member);
                        if (newChild != null) {
                            result.add(newChild);
                        }
                    }
                } catch (CoreException e) {
                    throw new DBException(e);
                }
                filterChildren(result);
                sortChildren(result);
                this.children = result;
            }
        }
        return children;
    }

    DBNResource getChild(IResource resource)
    {
        if (children == null) {
            return null;
        }
        for (DBNNode child : children) {
            if (child instanceof DBNResource && ((DBNResource)child).getResource().equals(resource)) {
                return (DBNResource) child;
            }
        }
        return null;
    }

    private DBNNode makeNode(IResource resource)
    {
        if (!resource.isAccessible() || resource.isHidden() || resource.isPhantom()) {
            // Skip not accessible hidden and phantom resources
            return null;
        }
        try {
            if (resource instanceof IFolder && resource.getParent() instanceof IFolder) {
                // Sub folder
                return handler.makeNavigatorNode(this, resource);
            }
            DBPResourceHandler resourceHandler = DBeaverCore.getInstance().getProjectRegistry().getResourceHandler(resource);
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

    public String getDefaultCommandId()
    {
        return ICommandIds.CMD_OBJECT_OPEN;
    }

    public boolean isManagable()
    {
        return true;
    }

    @Override
    public DBNNode refreshNode(DBRProgressMonitor monitor) throws DBException
    {
        try {
            resource.refreshLocal(IResource.DEPTH_INFINITE, monitor.getNestedMonitor());
        } catch (CoreException e) {
            throw new DBException(e);
        }
        return this;
    }

    public boolean supportsDrop(DBNNode otherNode)
    {
        if (!(resource instanceof IFolder) || (getFeatures() & DBPResourceHandler.FEATURE_DROP) == 0) {
            return false;
        }
        if (otherNode == null) {
            // Potentially any other node could be dropped in the folder
            return true;
        }

        // Drop supported only if both nodes are resource with the same handler and DROP feature is supported
        return otherNode instanceof DBNResource
            && otherNode.getParentNode() != this
            && !this.isChildOf(otherNode)
            && ((DBNResource)otherNode).handler == this.handler;
    }

    public void dropNode(DBNNode otherNode) throws DBException
    {
        DBNResource resourceNode = (DBNResource)otherNode;
        try {
            IResource otherResource = resourceNode.getResource();
            otherResource.move(
                resource.getFullPath().append(otherResource.getName()),
                true,
                VoidProgressMonitor.INSTANCE.getNestedMonitor());
        } catch (CoreException e) {
            throw new DBException(e);
        }
    }

    public IResource getResource()
    {
        return resource;
    }

    protected void filterChildren(List<DBNNode> list)
    {

    }

    protected void sortChildren(List<DBNNode> list)
    {
        Collections.sort(list, new Comparator<DBNNode>() {
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
                    return o1.getNodeName().compareTo(o2.getNodeName());
                }
            }
        });
    }

    public void setResourceImage(Image resourceImage)
    {
        this.resourceImage = resourceImage;
    }

    void handleResourceChange(IResourceDelta delta)
    {
        if (delta.getKind() == IResourceDelta.CHANGED) {
            // Update this node in navigator
            getModel().fireNodeEvent(new DBNEvent(delta, DBNEvent.Action.UPDATE, this));
        }
        if (children == null) {
            // Child nodes are not yet read so nothing to change here - just return
            return;
        }
        for (IResourceDelta childDelta : delta.getAffectedChildren()) {
            DBNResource childResource = getChild(childDelta.getResource());
            if (childResource == null) {
                if (childDelta.getKind() == IResourceDelta.ADDED) {
                    // New child
                    DBNNode newChild = makeNode(childDelta.getResource());
                    if (newChild != null) {
                        children.add(newChild);
                        getModel().fireNodeEvent(new DBNEvent(childDelta, DBNEvent.Action.ADD, newChild));
                    }
                } else {
                    //log.warn("Can't find resource '" + childDelta.getResource().getName() + "' in navigator model");
                }
            } else {
                if (childDelta.getKind() == IResourceDelta.REMOVED) {
                    // Node deleted
                    children.remove(childResource);
                    getModel().fireNodeEvent(new DBNEvent(childDelta, DBNEvent.Action.REMOVE, childResource));
                } else {
                    // Node changed - handle it recursive
                    childResource.handleResourceChange(childDelta);
                }
            }
        }
    }

}
