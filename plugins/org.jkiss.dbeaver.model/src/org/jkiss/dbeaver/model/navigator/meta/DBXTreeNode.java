/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jkiss.dbeaver.model.navigator.meta;

import org.apache.commons.jexl3.JexlContext;
import org.apache.commons.jexl3.JexlException;
import org.apache.commons.jexl3.JexlExpression;
import org.eclipse.core.runtime.IConfigurationElement;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPImage;
import org.jkiss.dbeaver.model.impl.AbstractDescriptor;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseNode;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.utils.CommonUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * DBXTreeNode
 */
public abstract class DBXTreeNode
{
    private static final Log log = Log.getLog(DBXTreeNode.class);

    private final AbstractDescriptor source;
    private final DBXTreeNode parent;
    private final IConfigurationElement config;
    private final String id;
    private List<DBXTreeNode> children;
    private DBPImage defaultIcon;
    private List<DBXTreeIcon> icons;
    private final boolean navigable;
    private final boolean inline;
    private final boolean virtual;
    private boolean standaloneNode;
    //private final boolean embeddable;
    private JexlExpression visibleIf;
    private DBXTreeNode recursiveLink;
    private List<DBXTreeNodeHandler> handlers = null;

    public DBXTreeNode(AbstractDescriptor source, DBXTreeNode parent, IConfigurationElement config, boolean navigable, boolean inline, boolean virtual, boolean standalone, String visibleIf, String recursive)
    {
        this.source = source;
        this.parent = parent;
        if (parent != null) {
            parent.addChild(this);
        }
        this.config = config;
        this.id = config == null ? null : config.getAttribute("id");
        this.navigable = navigable;
        this.inline = inline;
        this.virtual = virtual;
        this.standaloneNode = standalone;
        if (!CommonUtils.isEmpty(visibleIf)) {
            try {
                this.visibleIf = AbstractDescriptor.parseExpression(visibleIf);
            } catch (DBException e) {
                log.warn(e);
            }
        }
        if (recursive != null) {
            recursiveLink = this;
            for (String path : recursive.split("/")) {
                if (path.equals("..")) {
                    recursiveLink = recursiveLink.parent;
                }
            }
        }
    }

    public DBXTreeNode(AbstractDescriptor source, DBXTreeNode parent, DBXTreeNode node)
    {
        this.source = source;
        this.parent = parent;
        if (parent != null) {
            parent.addChild(this);
        }
        this.config = node.config;
        this.id = node.id;
        this.navigable = node.navigable;
        this.inline = node.inline;
        this.virtual = node.virtual;
        this.standaloneNode = node.standaloneNode;
        this.visibleIf = node.visibleIf;
        if (node.recursiveLink != null) {
/*
            recursiveLink = this;
            for (String path : recursive.split("/")) {
                if (path.equals("..")) {
                    recursiveLink = recursiveLink.parent;
                }
            }
*/
        }
        this.defaultIcon = node.defaultIcon;
        if (node.icons != null) {
            this.icons = new ArrayList<>(node.icons);
        }

        if (node.children != null) {
            this.children = new ArrayList<>(node.children.size());
            for (DBXTreeNode child : node.children) {
                if (child instanceof DBXTreeObject) new DBXTreeObject(source, this, (DBXTreeObject)child);
                else if (child instanceof DBXTreeFolder) new DBXTreeFolder(source, this, (DBXTreeFolder)child);
                else new DBXTreeItem(source, this, (DBXTreeItem)child);
            }
        }
        if (node.handlers != null) {
            this.handlers = new ArrayList<>(node.handlers);
        }
    }

    protected IConfigurationElement getConfig() {
        return config;
    }

    public AbstractDescriptor getSource()
    {
        return source;
    }

    /**
     * Human readable node type
     */
    public abstract String getNodeTypeLabel(@Nullable DBPDataSource dataSource, @Nullable String locale);

    /**
     * Human readable child nodes type
     */
    public abstract String getChildrenTypeLabel(@Nullable DBPDataSource dataSource, @Nullable String locale);

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

    public boolean isStandaloneNode() {
        return standaloneNode;
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
            return recursiveLink != null && recursiveLink.hasChildren(context, navigable);
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

    protected List<DBXTreeNode> getChildren() {
        return children;
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
        if (children == null) {
            if (recursiveLink != null) {
                return recursiveLink.getChildren(context);
            }
            return Collections.emptyList();
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

    public DBXTreeNode getRecursiveLink()
    {
        return recursiveLink;
    }

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
        if (!CommonUtils.isEmpty(extIcons) && context != null) {
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
                    log.debug("Error evaluating expression '" + icon.getExprString() + "' on node '" + context.getName() + "': " + GeneralUtils.getExpressionParseMessage(e));
                }
            }
        }
        return getDefaultIcon();
    }

    public JexlExpression getVisibleIf()
    {
        return visibleIf;
    }

    @Override
    public String toString() {
        return "Node " + id;
    }

    public void addActionHandler(DBXTreeNodeHandler.Action action, DBXTreeNodeHandler.Perform perform, String command) {
        if (handlers == null) {
            handlers = new ArrayList<>();
        }
        handlers.add(new DBXTreeNodeHandler(action, perform, command));
    }

    public DBXTreeNodeHandler getHandler(DBXTreeNodeHandler.Action action) {
        if (handlers != null) {
            for (DBXTreeNodeHandler handler : handlers) {
                if (handler.getAction() == action) {
                    return handler;
                }
            }
        }
        return null;
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

    public void moveChildAfter(DBXTreeNode child, DBXTreeItem afterItem) {
        int afterIndex = -1;
        for (int i = 0; i < children.size(); i++) {
            DBXTreeNode n = children.get(i);
            if (n == afterItem || (n instanceof DBXTreeFolder && n.getChildren().size() == 1 && n.getChildren().get(0) == afterItem)) {
                afterIndex = i;
                break;
            }
        }
        if (afterIndex >= 0) {
            children.remove(child);
            children.add(afterIndex + 1, child);
        }
    }

}
