/*
 * Copyright (C) 2010-2012 Serge Rieder
 * serge@jkiss.org
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
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

    @Override
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
