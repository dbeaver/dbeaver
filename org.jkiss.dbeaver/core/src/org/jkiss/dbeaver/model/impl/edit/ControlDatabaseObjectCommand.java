/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.impl.edit;

import org.eclipse.swt.graphics.Image;
import org.jkiss.dbeaver.model.struct.DBSObject;

/**
 * Abstract object command
 */
public abstract class ControlDatabaseObjectCommand<OBJECT_TYPE extends DBSObject> extends AbstractDatabaseObjectCommand<OBJECT_TYPE> {
    private Object oldValue;
    private Object newValue;

    protected ControlDatabaseObjectCommand(String title, Image icon)
    {
        super(title, icon);
    }

    protected ControlDatabaseObjectCommand(String title)
    {
        super(title);
    }

    protected Object getOldValue()
    {
        return oldValue;
    }

    void setOldValue(Object oldValue)
    {
        this.oldValue = oldValue;
    }

    protected Object getNewValue()
    {
        return newValue;
    }

    void setNewValue(Object newValue)
    {
        this.newValue = newValue;
    }

}