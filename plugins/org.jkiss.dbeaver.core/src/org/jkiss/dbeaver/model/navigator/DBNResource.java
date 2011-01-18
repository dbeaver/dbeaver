/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.navigator;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.ui.ICommandIds;

import java.util.ArrayList;
import java.util.List;

/**
 * DBNResource
 */
public class DBNResource extends DBNNode
{
    private IResource resource;
    protected List<DBNNode> children;

    public DBNResource(DBNNode parentNode, IResource resource)
    {
        super(parentNode);
        this.resource = resource;
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
        String imageKey = ISharedImages.IMG_OBJ_ELEMENT;
        switch (resource.getType()) {
            case IResource.FOLDER: imageKey = ISharedImages.IMG_OBJ_FOLDER;
            case IResource.FILE: imageKey = ISharedImages.IMG_OBJ_FILE;
            case IResource.PROJECT: imageKey = ISharedImages.IMG_OBJ_PROJECT;
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
                List<DBNNode> result = new ArrayList<DBNNode>();
                try {
                    IResource[] members = ((IContainer) resource).members();
                    for (IResource member : members) {
                        if (!member.isAccessible() || member.isHidden() || member.isPhantom()) {
                            // Skip not accessible hidden and phantom resources
                            continue;
                        }
                        if (resource instanceof IProject && member.getName().startsWith(".")) {
                            // Skip all root resources which name starts with dot (system resources)
                            continue;
                        }

                        DBNResource newChild = new DBNResource(this, member);
                        result.add(newChild);
                    }
                } catch (CoreException e) {
                    throw new DBException(e);
                }
                addCustomChildren(result);
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

    protected void addCustomChildren(List<DBNNode> list)
    {

    }

}
