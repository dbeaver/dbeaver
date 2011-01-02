/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.registry.tree;

import org.jkiss.dbeaver.registry.AbstractDescriptor;

/**
 * DBXTreeFolder
 */
public class DBXTreeFolder extends DBXTreeNode
{
    private String type;
    private String label;
    private String description;
    private boolean navigable;

    public DBXTreeFolder(AbstractDescriptor source, DBXTreeNode parent, String type, String label, boolean navigable)
    {
        super(source, parent);
        this.type = type;
        this.label = label;
        this.navigable = navigable;
    }

    public String getType()
    {
        return type;
    }

    public String getLabel()
    {
        return label;
    }

    @Override
    public boolean isNavigable()
    {
        return navigable;
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
