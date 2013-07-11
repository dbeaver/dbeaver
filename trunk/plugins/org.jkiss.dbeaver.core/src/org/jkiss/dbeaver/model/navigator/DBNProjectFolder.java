/*
 * Copyright (C) 2010-2013 Serge Rieder
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

import org.eclipse.swt.graphics.Image;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;

import java.util.List;

/**
 * DBNProjectFolder
 */
public class DBNProjectFolder extends DBNNode implements DBNContainer
{
    private final String name;
    private final String description;
    private final Image icon;
    private final List<? extends DBNNode> children;

    public DBNProjectFolder(DBNNode parentNode, String name, String description, Image icon, List<? extends DBNNode> children)
    {
        super(parentNode);
        this.name = name;
        this.description = description;
        this.icon = icon;
        this.children = children;
    }

    @Override
    void dispose(boolean reflect)
    {
        for (DBNNode child : children) {
            child.dispose(reflect);
        }
        super.dispose(reflect);
    }

    @Override
    public Object getValueObject()
    {
        return null;
    }

    @Override
    public String getChildrenType()
    {
        return "connections";
    }

    @Override
    public String getNodeType()
    {
        return "folder";
    }

    @Override
    public String getNodeName()
    {
        return name;
    }

    @Override
    public String getNodeDescription()
    {
        return description;
    }

    @Override
    public Image getNodeIcon()
    {
        return icon;
    }

    @Override
    public boolean allowsChildren()
    {
        return true;
    }

    @Override
    public boolean allowsNavigableChildren()
    {
        return true;
    }

    @Override
    public List<? extends DBNNode> getChildren(DBRProgressMonitor monitor) throws DBException
    {
        return children;
    }

    @Override
    public Class<? extends DBSObject> getChildrenClass()
    {
        return DBSObject.class;
    }

}
