/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.registry.tree;

import net.sf.jkiss.utils.CommonUtils;
import org.apache.commons.jexl2.JexlContext;
import org.apache.commons.jexl2.JexlException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.swt.graphics.Image;
import org.jkiss.dbeaver.registry.AbstractDescriptor;

import java.util.ArrayList;
import java.util.List;

/**
 * DBXTreeNode
 */
public abstract class DBXTreeNode
{
    static final Log log = LogFactory.getLog(DBXTreeNode.class);

    private final AbstractDescriptor source;
    private final DBXTreeNode parent;
    private List<DBXTreeNode> children;
    private DBXTreeNode recursiveLink;
    private Image defaultIcon;
    private List<DBXTreeIcon> icons;
    private final boolean navigable;
    private final boolean inline;

    public DBXTreeNode(AbstractDescriptor source, DBXTreeNode parent, boolean navigable, boolean inline)
    {
        this.source = source;
        this.parent = parent;
        if (parent != null) {
            parent.addChild(this);
        }
        this.navigable = navigable;
        this.inline = inline;
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

    public boolean isNavigable()
    {
        return navigable;
    }

    public boolean isInline()
    {
        return inline;
    }

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

    public Image getIcon(JexlContext context)
    {
        List<DBXTreeIcon> extIcons = getIcons();
/*
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
*/
        if (!CommonUtils.isEmpty(extIcons)) {
            // Try to get some icon depending on it's condition
            for (DBXTreeIcon icon : extIcons) {
                if (icon.getExpression() == null) {
                    continue;
                }
                try {
                    Object result = icon.getExpression().evaluate(context);
                    if (Boolean.TRUE.equals(result)) {
                        return icon.getIcon();
                    }
                } catch (JexlException e) {
                    // do nothing
                    log.debug("Error evaluating expression '" + icon.getExprString() + "'", e);
                }
            }
        }
        return getDefaultIcon();
    }
}
