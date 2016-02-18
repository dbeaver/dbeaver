/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2016 Serge Rieder (serge@jkiss.org)
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

import org.jkiss.dbeaver.model.DBPTermProvider;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.impl.AbstractDescriptor;

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
        String visibleIf)
    {
        super(source, parent, id, navigable, inline, virtual, visibleIf);
        this.label = label;
        this.itemLabel = itemLabel == null ? label : itemLabel;
        this.path = path;
        this.propertyName = propertyName;
        this.optional = optional;
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

}
