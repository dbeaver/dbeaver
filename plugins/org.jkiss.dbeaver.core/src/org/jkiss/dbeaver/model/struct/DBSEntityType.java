/*
 * Copyright (c) 2012, Serge Rieder and others. All Rights Reserved.
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