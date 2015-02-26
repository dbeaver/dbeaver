/*
 * Copyright (C) 2010-2015 Serge Rieder
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
package org.jkiss.dbeaver.registry.tree;

import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.registry.AbstractDescriptor;

/**
 * DBXTreeObject
 */
public class DBXTreeObject extends DBXTreeNode
{
    private String label;
    private String description;
    private String editorId;

    public DBXTreeObject(AbstractDescriptor source, DBXTreeNode parent, String id, String visibleIf, String label, String description, String editorId)
    {
        super(source, parent, id, true, false, false, visibleIf);
        this.label = label;
        this.description = description;
        this.editorId = editorId;
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

    public String getEditorId()
    {
        return editorId;
    }
}