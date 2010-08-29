/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.navigator;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSEntity;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.registry.tree.DBXTreeItem;
import org.jkiss.dbeaver.ui.actions.OpenObjectEditorAction;

/**
 * DBNTreeItem
 */
public class DBNTreeItem extends DBNTreeNode
{
    private DBXTreeItem meta;
    private DBSObject object;

    DBNTreeItem(DBNNode parent, DBXTreeItem meta, DBSObject object)
    {
        super(parent);
        this.meta = meta;
        this.object = object;
        if (this.getModel() != null) {
            this.getModel().addNode(this);
        }

    }

    protected void dispose()
    {
        if (this.getModel() != null) {
            this.getModel().removeNode(this);
        }
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

    public DBNNode refreshNode(DBRProgressMonitor monitor)
        throws DBException
    {
        if (object instanceof DBSEntity && ((DBSEntity)object).refreshEntity(monitor)) {
            this.clearChildren();
            return this;
        } else if (this.getParentNode() != null) {
            return this.getParentNode().refreshNode(monitor);
        } else {
            return null;
        }
    }

    public Class<OpenObjectEditorAction> getDefaultAction()
    {
        return OpenObjectEditorAction.class;
    }

}
