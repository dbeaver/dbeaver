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
package org.jkiss.dbeaver.model.navigator;

import org.jkiss.dbeaver.model.DBPImage;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

import java.util.List;

/**
 * Empty node
 */
public class DBNEmptyNode extends DBNNode
{
    public DBNEmptyNode()
    {
        super();
    }

    @Override
    public String getNodeType()
    {
        return "empty";
    }

    @Override
    public String getNodeName()
    {
        return "#empty"; //$NON-NLS-1$
    }

    @Override
    public String getNodeDescription()
    {
        return "Empty";
    }

    @Override
    public DBPImage getNodeIcon()
    {
        return null;
    }

    @Override
    public boolean allowsChildren()
    {
        return false;
    }

    @Override
    public boolean allowsNavigableChildren()
    {
        return false;
    }

    @Override
    public List<? extends DBNNode> getChildren(DBRProgressMonitor monitor)
    {
        return null;
    }

    @Override
    public boolean allowsOpen()
    {
        return false;
    }

}
