/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.impl.edit;

import org.eclipse.swt.graphics.Image;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.IDatabaseObjectCommand;
import org.jkiss.dbeaver.ext.IDatabasePersistAction;
import org.jkiss.dbeaver.model.struct.DBSObject;

/**
 * Abstract object command
 */
public class DatabaseObjectPropertyCommand<OBJECT_TYPE extends DBSObject> extends AbstractDatabaseObjectCommand<OBJECT_TYPE> {
    private DatabaseObjectPropertyHandler<OBJECT_TYPE> handler;
    private Object oldValue;
    private Object newValue;

    protected DatabaseObjectPropertyCommand(Image icon, DatabaseObjectPropertyHandler<OBJECT_TYPE> handler)
    {
        super("Change property " + handler, icon);
        this.handler = handler;
    }

    protected DatabaseObjectPropertyCommand(DatabaseObjectPropertyHandler<OBJECT_TYPE> handler)
    {
        this(null, handler);
    }

    public DatabaseObjectPropertyHandler<OBJECT_TYPE> getHandler()
    {
        return handler;
    }

    public Object getOldValue()
    {
        return oldValue;
    }

    void setOldValue(Object oldValue)
    {
        this.oldValue = oldValue;
    }

    public Object getNewValue()
    {
        return newValue;
    }

    void setNewValue(Object newValue)
    {
        this.newValue = newValue;
    }

    @Override
    public MergeResult merge(IDatabaseObjectCommand<OBJECT_TYPE> prevCommand)
    {
        if (prevCommand instanceof DatabaseObjectPropertyCommand) {
            DatabaseObjectPropertyCommand prevPropCommand = (DatabaseObjectPropertyCommand) prevCommand;
            if (prevPropCommand.getHandler() == this.getHandler()) {
                // Double change of the same property
                return MergeResult.CANCEL_PREVIOUS;
            }
        }
        return MergeResult.NONE;
    }

    @Override
    public void validateCommand(OBJECT_TYPE object) throws DBException
    {
        if (handler instanceof DatabaseObjectPropertyValidator) {
            ((DatabaseObjectPropertyValidator<OBJECT_TYPE>)handler).validate(object, newValue);
        }
    }

    public void updateModel(OBJECT_TYPE object)
    {
        if (handler instanceof DatabaseObjectPropertyUpdater) {
            ((DatabaseObjectPropertyUpdater<OBJECT_TYPE>)handler).updateModel(object, newValue);
        }
    }

    public IDatabasePersistAction[] getPersistActions(OBJECT_TYPE object)
    {
        if (handler instanceof DatabaseObjectPropertyPersister) {
            return ((DatabaseObjectPropertyPersister<OBJECT_TYPE>)handler).getPersistActions(object, newValue);
        }
        return null;
    }
}