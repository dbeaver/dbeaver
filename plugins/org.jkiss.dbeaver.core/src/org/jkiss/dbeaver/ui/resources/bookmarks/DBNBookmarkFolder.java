/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2016 Serge Rieder (serge@jkiss.org)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (version 2)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package org.jkiss.dbeaver.ui.resources.bookmarks;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPImage;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseNode;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.model.navigator.DBNResource;
import org.jkiss.dbeaver.model.app.DBPResourceHandler;
import org.jkiss.dbeaver.ui.UIIcon;

import java.util.Collection;
import java.util.Collections;

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
    public DBPImage getNodeIcon()
    {
        IResource resource = getResource();
        if (resource != null && resource.getParent() instanceof IProject) {
            return UIIcon.BOOKMARK_FOLDER;
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
    public void dropNodes(Collection<DBNNode> nodes) throws DBException
    {
        for (DBNNode node : nodes) {
            if (node instanceof DBNDatabaseNode) {
                BookmarksHandlerImpl.createBookmark((DBNDatabaseNode) node, node.getNodeName(), (IFolder) getResource());
            } else if (node instanceof DBNBookmark) {
                super.dropNodes(Collections.singleton(node));
            }
        }
    }
}
