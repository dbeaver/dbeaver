/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.navigator;

import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSFolder;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.registry.tree.DBXTreeFolder;
import org.jkiss.dbeaver.ui.ICommandIds;

/**
 * DBNTreeFolder
 */
public class DBNTreeFolder extends DBNTreeNode implements DBSFolder
{
    private DBXTreeFolder meta;

    DBNTreeFolder(DBNNode parent, DBXTreeFolder meta)
    {
        super(parent);
        this.meta = meta;
        if (this.getModel() != null) {
            this.getModel().addNode(this);
        }
    }

    protected void dispose()
    {
        if (this.getModel() != null) {
            this.getModel().removeNode(this);
        }
        super.dispose();
    }

    public DBXTreeFolder getMeta()
    {
        return meta;
    }

    @Override
    protected void reloadObject(DBRProgressMonitor monitor, DBSObject object) {
        // do nothing
    }

    public DBSObject getObject()
    {
        return this;
    }

    public Object getValueObject()
    {
        return getParentNode() == null ? null : getParentNode().getValueObject();
    }

    public String getName()
    {
        return meta.getLabel();
    }

    public String getObjectId() {
        return getParentObject().getObjectId() + "." + meta.getLabel();
    }

    public String getDescription()
    {
        return meta.getDescription();
    }

    public DBSObject getParentObject()
    {
        return getParentNode() == null ? null : getParentNode().getObject();
    }

    public DBPDataSource getDataSource()
    {
        return getParentObject() == null ? null : getParentObject().getDataSource();
    }

    public String getDefaultCommandId()
    {
        return ICommandIds.CMD_OPEN_OBJECT;
    }

    public String getItemsType()
    {
        return meta.getType();
    }

    public Class<?> getItemsClass()
    {
        try {
            return Class.forName(getItemsType());
        } catch (ClassNotFoundException e) {
            return null;
        }
    }

}
