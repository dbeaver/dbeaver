/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.registry.tree;

import net.sf.jkiss.utils.CommonUtils;
import org.apache.commons.jexl2.JexlContext;
import org.eclipse.swt.graphics.Image;
import org.jkiss.dbeaver.model.struct.DBSObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
            final Map<String, Object> vars = new HashMap<String, Object>();
            JexlContext exprContext = new JexlContext() {
                public Object get(String s) {
                    return vars.get(s);
                }

                public void set(String s, Object o) {
                    vars.put(s, o);
                }

                public boolean has(String s) {
                    return vars.containsKey(s);
                }
            };
            vars.put("object", forObject);
            // Try to get some icon depending on it's condition
            for (DBXTreeIcon icon : extIcons) {
                if (icon.getExpr() == null) {
                    continue;
                }
                try {
                    Object result = icon.getExpr().evaluate(exprContext);
                    if (Boolean.TRUE.equals(result)) {
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
