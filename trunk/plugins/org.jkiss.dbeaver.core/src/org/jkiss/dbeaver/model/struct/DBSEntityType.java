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
package org.jkiss.dbeaver.model.struct;

import org.eclipse.swt.graphics.Image;
import org.jkiss.dbeaver.ui.DBIcon;

/**
 * Entity type
 */
public class DBSEntityType
{
    public static final DBSEntityType TABLE = new DBSEntityType("table", "Table", DBIcon.TREE_TABLE.getImage(), true); //$NON-NLS-1$
    public static final DBSEntityType VIEW = new DBSEntityType("view", "View", DBIcon.TREE_VIEW.getImage(), true); //$NON-NLS-1$
    public static final DBSEntityType TYPE = new DBSEntityType("type", "Type", DBIcon.TREE_DATA_TYPE.getImage(), true); //$NON-NLS-1$
    public static final DBSEntityType CLASS = new DBSEntityType("class", "Class", DBIcon.TREE_CLASS.getImage(), false); //$NON-NLS-1$
    public static final DBSEntityType ASSOCIATION = new DBSEntityType("association", "Association", DBIcon.TREE_ASSOCIATION.getImage(), false); //$NON-NLS-1$

    public static final DBSEntityType VIRTUAL_ENTITY = new DBSEntityType("virtual_entity", "Virtual Entity", DBIcon.TREE_TABLE.getImage(), true); //$NON-NLS-1$
    public static final DBSEntityType VIRTUAL_ASSOCIATION = new DBSEntityType("virtual_association", "Virtual Association", DBIcon.TREE_ASSOCIATION.getImage(), false); //$NON-NLS-1$

    private final String id;
    private final String name;
    private final Image icon;
    private final boolean physical;

    public DBSEntityType(String id, String name, Image icon, boolean physical)
    {
        this.id = id;
        this.name = name;
        this.icon = icon;
        this.physical = physical;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Image getIcon()
    {
        return icon;
    }

    public boolean isPhysical()
    {
        return physical;
    }

    public String toString()
    {
        return getName();
    }
}