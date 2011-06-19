/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.registry.tree;

import org.jkiss.dbeaver.ext.IDatabaseTermProvider;
import org.jkiss.dbeaver.model.DBPDataSource;
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

    public DBXTreeItem(
        AbstractDescriptor source,
        DBXTreeNode parent,
        String id,
        String label,
        String itemLabel,
        String path,
        String propertyName,
        boolean optional,
        boolean virtual,
        boolean navigable,
        boolean inline,
        String visibleIf)
    {
        super(source, parent, id, navigable, inline, visibleIf);
        this.label = label;
        this.itemLabel = itemLabel == null ? label : itemLabel;
        this.path = path;
        this.propertyName = propertyName;
        this.optional = optional;
        this.virtual = virtual;
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

    public String getChildrenType(DBPDataSource dataSource)
    {
        final String term = getNodeTerm(dataSource, label, true);
        if (term != null) {
            return term;
        }
        return label;
    }

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
        if (termId.startsWith("#") && dataSource instanceof IDatabaseTermProvider) {
            final String term = ((IDatabaseTermProvider) dataSource).getObjectTypeTerm(getPath(), termId.substring(1), multiple);
            if (term != null) {
                return term;
            }
        }
        return null;
    }

}
