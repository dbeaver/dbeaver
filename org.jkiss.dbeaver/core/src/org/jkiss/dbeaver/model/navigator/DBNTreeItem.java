/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.navigator;

import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.registry.tree.DBXTreeItem;
import org.jkiss.dbeaver.ui.ICommandIds;

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

    @Override
    protected void reloadObject(DBRProgressMonitor monitor, DBSObject object) {
        getModel().removeNode(this, false);
        this.object = object;
        getModel().addNode(this, false);
    }

    public DBSObject getObject()
    {
        return object;
    }

    public Object getValueObject()
    {
        return object;
    }

    public String getDefaultCommandId()
    {
        return ICommandIds.CMD_OPEN_OBJECT;
    }

}
