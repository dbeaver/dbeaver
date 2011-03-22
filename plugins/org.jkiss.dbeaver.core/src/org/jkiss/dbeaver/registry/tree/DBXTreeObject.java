/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.registry.tree;

import org.jkiss.dbeaver.registry.AbstractDescriptor;

/**
 * DBXTreeItem
 */
public class DBXTreeObject extends DBXTreeNode
{
    private String label;
    private String description;
    private String editorId;

    public DBXTreeObject(AbstractDescriptor source, DBXTreeNode parent, String label, String description, String editorId)
    {
        super(source, parent, true, false);
        this.label = label;
        this.description = description;
        this.editorId = editorId;
    }

    public String getLabel()
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