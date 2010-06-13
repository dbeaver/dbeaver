/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.registry.tree;

import net.sf.jkiss.utils.CommonUtils;
import org.eclipse.swt.graphics.Image;

import java.util.ArrayList;
import java.util.List;

/**
 * DBXTreeNode
 */
public abstract class DBXTreeNode
{
    private DBXTreeNode parent;
    private List<DBXTreeNode> children;
    private DBXTreeNode recursiveLink;
    private Image defaultIcon;
    private List<DBXTreeIcon> icons;

    public DBXTreeNode(DBXTreeNode parent)
    {
        this.parent = parent;
        if (parent != null) {
            parent.addChild(this);
        }
    }

    public abstract String getLabel();

    public abstract boolean isNavigable(); 

    public DBXTreeNode getParent()
    {
        return parent;
    }

    public boolean hasChildren()
    {
        return !CommonUtils.isEmpty(children);
    }

    public List<DBXTreeNode> getChildren()
    {
        return children;
    }

    private void addChild(DBXTreeNode child)
    {
        if (this.children == null) {
            this.children = new ArrayList<DBXTreeNode>();
        }
        this.children.add(child);
    }

    public DBXTreeNode getRecursiveLink()
    {
        return recursiveLink;
    }

    public Image getDefaultIcon()
    {
        return defaultIcon;
    }

    public void setDefaultIcon(Image defaultIcon)
    {
        this.defaultIcon = defaultIcon;
    }

    public List<DBXTreeIcon> getIcons()
    {
        return icons;
    }

    public void addIcon(DBXTreeIcon icon)
    {
        if (this.icons == null) {
            this.icons = new ArrayList<DBXTreeIcon>();
        }
        this.icons.add(icon);
    }

}
