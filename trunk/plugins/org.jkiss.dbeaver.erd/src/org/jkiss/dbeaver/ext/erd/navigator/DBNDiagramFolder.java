/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2015 Serge Rieder (serge@jkiss.org)
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

import java.util.Collection;

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

    @Override
    public Image getNodeIcon()
    {
        IResource resource = getResource();
        if (resource != null && resource.getParent() instanceof IProject) {
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

}
