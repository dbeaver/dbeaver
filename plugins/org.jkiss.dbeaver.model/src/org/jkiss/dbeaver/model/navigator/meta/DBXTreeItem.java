/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
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

import org.jkiss.dbeaver.model.DBPTermProvider;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.impl.AbstractDescriptor;
import org.jkiss.utils.CommonUtils;

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

    public DBXTreeItem(
        AbstractDescriptor source,
        DBXTreeNode parent,
        String id,
        String label,
        String itemLabel,
        String path,
        String propertyName,
        boolean optional,
        boolean navigable,
        boolean inline,
        boolean virtual,
        boolean standalone,
        String visibleIf,
        String recursiveLink)
    {
        super(source, parent, id, navigable, inline, virtual, standalone, visibleIf, recursiveLink);
        this.label = label;
        this.itemLabel = itemLabel == null ? label : itemLabel;
        this.path = path;
        this.propertyName = propertyName;
        this.optional = optional;
    }

    public DBXTreeItem(AbstractDescriptor source, DBXTreeNode parent, DBXTreeItem item) {
        super(source, parent, item);
        this.label = item.label;
        this.itemLabel = item.itemLabel;
        this.path = item.path;
        this.propertyName = item.propertyName;
        this.optional = item.optional;
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

    @Override
    public String getChildrenType(DBPDataSource dataSource)
    {
        final String term = getNodeTerm(dataSource, label, true);
        if (term != null) {
            return term;
        }
        return label;
    }

    @Override
    public String toString() {
        return "Item " + label;
    }

    @Override
    public String getNodeType(DBPDataSource dataSource)
    {
        final String term = getNodeTerm(dataSource, itemLabel, false);
        if (term != null) {
            return term;
        }
        return itemLabel;
    }

    private String getNodeTerm(DBPDataSource dataSource, String termId, boolean multiple)
    {
        if (termId.startsWith("#") && dataSource instanceof DBPTermProvider) {
            final String term = ((DBPTermProvider) dataSource).getObjectTypeTerm(getPath(), termId.substring(1), multiple);
            if (term != null) {
                return term;
            }
        }
        return null;
    }

    public DBXTreeItem findChildItemByPath(String path) {
        for (DBXTreeNode node : getChildren()) {
            DBXTreeItem subItem = findChildItemByPath(node, path);
            if (subItem != null) {
                return subItem;
            }
        }
        return null;
    }

    private DBXTreeItem findChildItemByPath(DBXTreeNode node, String path) {
        if (node instanceof DBXTreeItem && CommonUtils.equalObjects(((DBXTreeItem) node).getPath(), path)) {
            return (DBXTreeItem) node;
        }
        if (node instanceof DBXTreeFolder) {
            for (DBXTreeNode subFolder : node.getChildren()) {
                DBXTreeItem subItem = findChildItemByPath(subFolder, path);
                if (subItem != null) {
                    return subItem;
                }
            }
        }
        return null;
    }
}
