/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.navigator;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.model.project.DBPResourceHandler;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.registry.ProjectRegistry;
import org.jkiss.dbeaver.ui.ICommandIds;

import java.util.ArrayList;
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
                        if (!member.isAccessible() || member.isHidden() || member.isPhantom()) {
                            // Skip not accessible hidden and phantom resources
                            continue;
                        }
                        DBPResourceHandler resourceHandler = projectRegistry.getResourceHandler(member);
                        if (resourceHandler == null) {
                            log.debug("Skip resource '" + member.getName() + "'");
                            continue;
                        }

                        DBNNode newChild = resourceHandler.makeNavigatorNode(this, member);
                        result.add(newChild);
                    }
                } catch (CoreException e) {
                    throw new DBException(e);
                }
                filterChildren(result);
                this.children = result;
            }
        }
        return children;
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

    public IResource getResource()
    {
        return resource;
    }

    protected void filterChildren(List<DBNNode> list)
    {

    }

    public void setResourceImage(Image resourceImage)
    {
        this.resourceImage = resourceImage;
    }

}
