/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.registry.tree;

import org.jkiss.dbeaver.registry.AbstractDescriptor;

/**
 * DBXTreeItem
 */
public class DBXTreeItem extends DBXTreeNode
{
    private String label;
    private String itemLabel;
    private String path;
    private String propertyName;
    private boolean optional;
    private boolean virtual;
    private boolean navigable;

    public DBXTreeItem(
        AbstractDescriptor source,
        DBXTreeNode parent,
        String label,
        String itemLabel,
        String path,
        String propertyName,
        boolean optional,
        boolean virtual,
        boolean navigable)
    {
        super(source, parent);
        this.label = label;
        this.itemLabel = itemLabel == null ? label : itemLabel;
        this.path = path;
        this.propertyName = propertyName;
        this.optional = optional;
        this.virtual = virtual;
        this.navigable = navigable;
    }

    public String getPath()
    {
        return path;
    }

    public String getPropertyName()
    {
        return propertyName;
    }

    public boolean isOptional()
    {
        return optional;
    }

    /**
     * Virtual items. Such items are not added to global meta model and couldn't
     * be found in tree by object
     * @return true or false
     */
    public boolean isVirtual()
    {
        return virtual;
    }

    public String getLabel()
    {
        return label;
    }

    public String getItemLabel()
    {
        return itemLabel;
    }

    @Override
    public boolean isNavigable()
    {
        return navigable;
    }
}
