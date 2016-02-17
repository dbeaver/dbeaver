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
package org.jkiss.dbeaver.model.navigator.meta;

import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.impl.AbstractDescriptor;

/**
 * DBXTreeFolder
 */
public class DBXTreeFolder extends DBXTreeNode
{
    private String type;
    private String label;
    private String description;

    public DBXTreeFolder(AbstractDescriptor source, DBXTreeNode parent, String id, String type, String label, boolean navigable, boolean virtual, String visibleIf)
    {
        super(source, parent, id, navigable, false, virtual, visibleIf);
        this.type = type;
        this.label = label;
    }

    public String getType()
    {
        return type;
    }

    @Override
    public String getNodeType(DBPDataSource dataSource)
    {
        return label;
    }

    @Override
    public String getChildrenType(DBPDataSource dataSource)
    {
        return label;
    }

    public String getDescription()
    {
        return description;
    }

    public void setDescription(String description)
    {
        this.description = description;
    }
}
