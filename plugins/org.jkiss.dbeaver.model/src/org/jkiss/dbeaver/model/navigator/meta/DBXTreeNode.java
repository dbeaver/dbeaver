/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2015 Serge Rieder (serge@jkiss.org)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (version 2)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package org.jkiss.dbeaver.model.navigator.meta;

import org.apache.commons.jexl2.Expression;
import org.apache.commons.jexl2.JexlContext;
import org.apache.commons.jexl2.JexlException;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPImage;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseNode;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.model.impl.AbstractDescriptor;
import org.jkiss.utils.CommonUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * DBXTreeNode
 */
public abstract class DBXTreeNode
{
    static final Log log = Log.getLog(DBXTreeNode.class);

    private final AbstractDescriptor source;
    private final DBXTreeNode parent;
    private final String id;
    private List<DBXTreeNode> children;
    private DBPImage defaultIcon;
    private List<DBXTreeIcon> icons;
    private final boolean navigable;
    private final boolean inline;
    private final boolean virtual;
    //private final boolean embeddable;
    private Expression visibleIf;

    public DBXTreeNode(AbstractDescriptor source, DBXTreeNode parent, String id, boolean navigable, boolean inline, boolean virtual, String visibleIf)
    {
        this.source = source;
        this.parent = parent;
        if (parent != null) {
            parent.addChild(this);
        }
        this.id = id;
        this.navigable = navigable;
        this.inline = inline;
        this.virtual = virtual;
        if (!CommonUtils.isEmpty(visibleIf)) {
            try {
                this.visibleIf = AbstractDescriptor.parseExpression(visibleIf);
            } catch (DBException e) {
                log.warn(e);
            }
        }
    }

    public AbstractDescriptor getSource()
    {
        return source;
    }

    public abstract String getNodeType(DBPDataSource dataSource);

    public abstract String getChildrenType(DBPDataSource dataSource);

    public boolean isNavigable()
    {
        return navigable;
    }

    public boolean isInline()
    {
        return inline;
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

    public DBXTreeNode getParent()
    {
        return parent;
    }

    public String getId()
    {
        return id;
    }

    public boolean hasChildren(DBNNode context)
    {
        return hasChildren(context, false);
    }

    public boolean hasChildren(DBNNode context, boolean navigable)
    {
        if (CommonUtils.isEmpty(children)) {
            return false;
        }
        if (context == null) {
            return true;
        }
        for (DBXTreeNode child : children) {
            if ((!navigable || child.isNavigable()) && child.isVisible(context)) {
                return true;
            }
        }
        return false;
    }

    public List<DBXTreeNode> getChildren(DBNNode context)
    {
        if (context != null && !CommonUtils.isEmpty(children)) {
            boolean hasExpr = false;
            for (DBXTreeNode child : children) {
                if (child.getVisibleIf() != null) {
                    hasExpr = true;
                    break;
                }
            }
            if (hasExpr) {
                List<DBXTreeNode> filteredChildren = new ArrayList<>(children.size());
                for (DBXTreeNode child : children) {
                    if (child.isVisible(context)) {
                        filteredChildren.add(child);
                    }
                }
                return filteredChildren;
            }
        }
        return children;
    }

    private boolean isVisible(DBNNode context)
    {
        try {
            return visibleIf == null || Boolean.TRUE.equals(visibleIf.evaluate(makeContext(context)));
        } catch (JexlException e) {
            log.warn(e);
            return false;
        }
    }

    public void addChild(DBXTreeNode child)
    {
        if (this.children == null) {
            this.children = new ArrayList<>();
        }
        this.children.add(child);
    }

//    public DBXTreeNode getRecursiveLink()
//    {
//        return recursiveLink;
//    }

    public DBPImage getDefaultIcon()
    {
        return defaultIcon;
    }

    public void setDefaultIcon(DBPImage defaultIcon)
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
            this.icons = new ArrayList<>();
        }
        this.icons.add(icon);
    }

    public DBPImage getIcon(DBNNode context)
    {
        List<DBXTreeIcon> extIcons = getIcons();
        if (!CommonUtils.isEmpty(extIcons)) {
            // Try to get some icon depending on it's condition
            for (DBXTreeIcon icon : extIcons) {
                if (icon.getExpression() == null) {
                    continue;
                }
                try {
                    Object result = icon.getExpression().evaluate(makeContext(context));
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

    public Expression getVisibleIf()
    {
        return visibleIf;
    }

    private static JexlContext makeContext(final DBNNode node)
    {
        return new JexlContext() {

            @Override
            public Object get(String name)
            {
                if (node instanceof DBNDatabaseNode && name.equals("object")) {
                    return ((DBNDatabaseNode) node).getValueObject();
                }
                return null;
            }

            @Override
            public void set(String name, Object value)
            {
                log.warn("Set is not implemented in DBX model");
            }

            @Override
            public boolean has(String name)
            {
                return node instanceof DBNDatabaseNode && name.equals("object")
                    && ((DBNDatabaseNode) node).getValueObject() != null;
            }
        };
    }

}
