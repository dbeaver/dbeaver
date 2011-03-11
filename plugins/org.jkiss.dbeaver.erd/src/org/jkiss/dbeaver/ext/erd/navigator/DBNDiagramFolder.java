/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.erd.navigator;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.swt.graphics.Image;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.erd.Activator;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.model.navigator.DBNResource;
import org.jkiss.dbeaver.model.project.DBPResourceHandler;
import org.jkiss.dbeaver.ui.UIUtils;

/**
 * DBNDiagramFolder
 */
public class DBNDiagramFolder extends DBNResource
{
    private Image image;

    public DBNDiagramFolder(DBNNode parentNode, IResource resource, DBPResourceHandler handler) throws DBException, CoreException
    {
        super(parentNode, resource, handler);
    }

    @Override
    protected void dispose(boolean reflect)
    {
        UIUtils.dispose(image);
        super.dispose(reflect);
    }

    public Image getNodeIcon()
    {
        if (getResource().getParent() instanceof IProject) {
            if (image == null) {
                image = Activator.getImageDescriptor("icons/erd_folder.png").createImage();
            }
            return image;
        }
        return super.getNodeIcon();
    }

    @Override
    public boolean supportsDrop(DBNNode otherNode)
    {
        return otherNode instanceof DBNDiagram || super.supportsDrop(otherNode);
    }

    @Override
    public void dropNode(DBNNode otherNode) throws DBException
    {
        if (otherNode instanceof DBNDiagram) {
            ERDResourceHandler.createDiagram(
                ((DBNDiagram) otherNode).getDiagram(),
                otherNode.getNodeName(),
                (IFolder) getResource(),
                null);
        } else {
            super.dropNode(otherNode);
        }
    }
}
