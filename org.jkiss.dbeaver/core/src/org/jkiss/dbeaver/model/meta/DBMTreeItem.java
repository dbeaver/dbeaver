/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.meta;

import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.registry.tree.DBXTreeItem;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ui.actions.OpenEntityEditorAction;
import org.eclipse.jface.action.IAction;

/**
 * DBMTreeItem
 */
public class DBMTreeItem extends DBMTreeNode
{
    private DBXTreeItem meta;
    private DBSObject object;

    DBMTreeItem(DBMNode parent, DBXTreeItem meta, DBSObject object)
    {
        super(parent);
        this.meta = meta;
        this.object = object;
        if (this.getModel() != null) {
            this.getModel().addNode(this.object, this);
        }

    }

    protected void dispose()
    {
        if (this.getModel() != null) {
            this.getModel().removeNode(this.object);
        }
        this.meta = null;
        this.object = null;
        super.dispose();
    }

    public DBXTreeItem getMeta()
    {
        return meta;
    }

    public DBSObject getObject()
    {
        return object;
    }

    public Object getValueObject()
    {
        return object;
    }

    public DBMNode refreshNode(DBRProgressMonitor monitor)
        throws DBException
    {
        if (object.refreshObject()) {
            this.clearChildren();
            return this;
        } else if (this.getParentNode() != null) {
            return this.getParentNode().refreshNode(monitor);
        } else {
            return null;
        }
    }

    public IAction getDefaultAction()
    {
        OpenEntityEditorAction action = new OpenEntityEditorAction();
        action.setText("Edit");
        return action;
    }

}
