/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.registry.tree;

import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.registry.AbstractDescriptor;

/**
 * DBXTreeFolder
 */
public class DBXTreeFolder extends DBXTreeNode
{
    private String type;
    private String label;
    private String description;

    public DBXTreeFolder(AbstractDescriptor source, DBXTreeNode parent, String id, String type, String label, boolean navigable, String visibleIf)
    {
        super(source, parent, id, navigable, false, visibleIf);
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
