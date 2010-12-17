/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.registry.tree;

import net.sf.jkiss.utils.BeanUtils;
import net.sf.jkiss.utils.CommonUtils;
import org.eclipse.swt.graphics.Image;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.registry.AbstractDescriptor;

import java.util.ArrayList;
import java.util.List;

/**
 * DBXTreeNode
 */
public abstract class DBXTreeNode
{
    private final AbstractDescriptor source;
    private DBXTreeNode parent;
    private List<DBXTreeNode> children;
    private DBXTreeNode recursiveLink;
    private Image defaultIcon;
    private List<DBXTreeIcon> icons;

    public DBXTreeNode(AbstractDescriptor source, DBXTreeNode parent)
    {
        this.source = source;
        this.parent = parent;
        if (parent != null) {
            parent.addChild(this);
        }
    }

    public AbstractDescriptor getSource()
    {
        return source;
    }

    public abstract String getLabel();
    
    public String getItemLabel()
    {
        return getLabel();
    }

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

    public Image getIcon(DBSObject forObject)
    {
        List<DBXTreeIcon> extIcons = getIcons();
        if (!CommonUtils.isEmpty(extIcons)) {
            // Try to get some icon depending on it's condition
            for (DBXTreeIcon icon : extIcons) {
                if (CommonUtils.isEmpty(icon.getExprString())) {
                    continue;
                }
                try {
                    Object propValue = BeanUtils.readObjectProperty(forObject, icon.getExprString());
                    if (Boolean.TRUE.equals(propValue) || (propValue instanceof String && Boolean.TRUE.equals(Boolean.valueOf((String)propValue)))) {
                        return icon.getIcon();
                    }
                } catch (Exception e) {
                    // do nothing
                }
            }
        }
        return getDefaultIcon();
    }
}
