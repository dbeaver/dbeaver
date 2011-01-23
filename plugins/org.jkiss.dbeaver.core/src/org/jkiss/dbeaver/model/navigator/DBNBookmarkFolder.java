/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.navigator;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.swt.graphics.Image;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.impl.project.BookmarksHandlerImpl;
import org.jkiss.dbeaver.model.project.DBPResourceHandler;
import org.jkiss.dbeaver.ui.DBIcon;

/**
 * DBNBookmarkFolder
 */
public class DBNBookmarkFolder extends DBNResource
{
    public DBNBookmarkFolder(DBNNode parentNode, IResource resource, DBPResourceHandler handler) throws DBException, CoreException
    {
        super(parentNode, resource, handler);
    }

    public Image getNodeIcon()
    {
        if (getResource().getParent() instanceof IProject) {
            return DBIcon.BOOKMARK_FOLDER.getImage();
        }
        return super.getNodeIcon();
    }

    @Override
    public boolean supportsDrop(DBNNode otherNode)
    {
        if (otherNode instanceof DBNDatabaseNode || otherNode instanceof DBNBookmark) {
            return true;
        } else {
            return super.supportsDrop(otherNode);
        }
    }

    @Override
    public void dropNode(DBNNode otherNode) throws DBException
    {
        if (otherNode instanceof DBNDatabaseNode) {
            BookmarksHandlerImpl.createBookmark((DBNDatabaseNode) otherNode, otherNode.getNodeName(), (IFolder) getResource());
        } else {
            super.dropNode(otherNode);
        }
    }
}
