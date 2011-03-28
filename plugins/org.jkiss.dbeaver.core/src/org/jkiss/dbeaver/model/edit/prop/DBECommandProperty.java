/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.edit.prop;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.IDatabasePersistAction;
import org.jkiss.dbeaver.model.edit.DBECommand;
import org.jkiss.dbeaver.model.impl.edit.DBECommandImpl;
import org.jkiss.dbeaver.model.struct.DBSObject;

import java.util.Map;

/**
 * Abstract object command
 */
public class DBECommandProperty<OBJECT_TYPE extends DBSObject> extends DBECommandImpl<OBJECT_TYPE> {

    public static final String PROP_COMPOSITE_COMMAND = ".composite";

    private DBEPropertyHandler<OBJECT_TYPE> handler;
    private Object oldValue;
    private Object newValue;

    public DBECommandProperty(OBJECT_TYPE object, DBEPropertyHandler<OBJECT_TYPE> handler)
    {
        super(object, "Change property " + handler);
        this.handler = handler;
    }

    public DBECommandProperty(OBJECT_TYPE object, DBEPropertyHandler<OBJECT_TYPE> handler, Object newValue)
    {
        this(object, handler);
        this.newValue = newValue;
    }

    public DBEPropertyHandler<OBJECT_TYPE> getHandler()
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

    void setNewValue(OBJECT_TYPE object, Object newValue)
    {
        Object prevValue = this.newValue;
        if (prevValue == null) {
            prevValue = this.oldValue;
        }
        this.newValue = newValue;
        if (handler instanceof DBEPropertyReflector) {
            ((DBEPropertyReflector<OBJECT_TYPE>)handler).reflectValueChange(object, prevValue, this.newValue);
        }
    }

    @Override
    public DBECommand<?> merge(DBECommand<?> prevCommand, Map<String, Object> userParams)
    {
        String compositeName = handler.getClass().getName() + PROP_COMPOSITE_COMMAND;
        DBECommandComposite compositeCommand = (DBECommandComposite)userParams.get(compositeName);
        if (compositeCommand == null) {
            compositeCommand = handler.createCompositeCommand(getObject());
            userParams.put(compositeName, compositeCommand);
        }
        compositeCommand.addPropertyHandler(handler, newValue);
        return compositeCommand;
    }

    @Override
    public void validateCommand() throws DBException
    {
        if (handler instanceof DBEPropertyValidator) {
            ((DBEPropertyValidator<OBJECT_TYPE>)handler).validate(getObject(), newValue);
        }
    }

    public void updateModel()
    {
        if (handler instanceof DBEPropertyUpdater) {
            ((DBEPropertyUpdater<OBJECT_TYPE>)handler).updateModel(getObject(), newValue);
        }
    }

    public IDatabasePersistAction[] getPersistActions()
    {
        if (handler instanceof DBEPropertyPersister) {
            return ((DBEPropertyPersister<OBJECT_TYPE>)handler).getPersistActions(getObject(), newValue);
        }
        return null;
    }
}