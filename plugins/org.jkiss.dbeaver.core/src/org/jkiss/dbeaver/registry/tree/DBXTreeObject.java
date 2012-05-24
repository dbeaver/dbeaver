/*
 * Copyright (c) 2012, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.registry.tree;

import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.registry.AbstractDescriptor;

/**
 * DBXTreeItem
 */
public class DBXTreeObject extends DBXTreeNode
{
    private String label;
    private String description;
    private String editorId;

    public DBXTreeObject(AbstractDescriptor source, DBXTreeNode parent, String id, String visibleIf, String label, String description, String editorId)
    {
        super(source, parent, id, true, false, visibleIf);
        this.label = label;
        this.description = description;
        this.editorId = editorId;
    }

    @Override
    public String getNodeType(DBPDataSource dataSource)
    {
        return label;
    }

    @Override
    public String getChildrenType(DBPDataSource dataSource)
    {
        return label;
    }

    public String getDescription()
    {
        return description;
    }

    public String getEditorId()
    {
        return editorId;
    }
}