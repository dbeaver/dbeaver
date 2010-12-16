/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.edit.prop;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.edit.DBOCommand;
import org.jkiss.dbeaver.ext.IDatabasePersistAction;
import org.jkiss.dbeaver.model.impl.edit.DBOCommandImpl;
import org.jkiss.dbeaver.model.struct.DBSObject;

import java.util.Map;

/**
 * Abstract object command
 */
public class DBOCommandProperty<OBJECT_TYPE extends DBSObject> extends DBOCommandImpl<OBJECT_TYPE> {

    public static final String PROP_COMPOSITE_COMMAND = ".composite";

    private DBOPropertyHandler<OBJECT_TYPE> handler;
    private Object oldValue;
    private Object newValue;

    public DBOCommandProperty(DBOPropertyHandler<OBJECT_TYPE> handler)
    {
        super("Change property " + handler, null);
        this.handler = handler;
    }

    public DBOCommandProperty(DBOPropertyHandler<OBJECT_TYPE> handler, Object newValue)
    {
        this(handler);
        this.newValue = newValue;
    }

    public DBOPropertyHandler<OBJECT_TYPE> getHandler()
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
        if (handler instanceof DBOPropertyReflector) {
            ((DBOPropertyReflector<OBJECT_TYPE>)handler).reflectValueChange(object, prevValue, this.newValue);
        }
    }

    @Override
    public DBOCommand<OBJECT_TYPE> merge(DBOCommand<OBJECT_TYPE> prevCommand, Map<String, Object> userParams)
    {
        String compositeName = handler.getClass().getName() + PROP_COMPOSITE_COMMAND;
        DBOCommandComposite compositeCommand = (DBOCommandComposite)userParams.get(compositeName);
        if (compositeCommand == null) {
            compositeCommand = handler.createCompositeCommand();
            userParams.put(compositeName, compositeCommand);
        }
        compositeCommand.addPropertyHandler(handler, newValue);
        return compositeCommand;
    }

    @Override
    public void validateCommand(OBJECT_TYPE object) throws DBException
    {
        if (handler instanceof DBOPropertyValidator) {
            ((DBOPropertyValidator<OBJECT_TYPE>)handler).validate(object, newValue);
        }
    }

    public void updateModel(OBJECT_TYPE object)
    {
        if (handler instanceof DBOPropertyUpdater) {
            ((DBOPropertyUpdater<OBJECT_TYPE>)handler).updateModel(object, newValue);
        }
    }

    public IDatabasePersistAction[] getPersistActions(OBJECT_TYPE object)
    {
        if (handler instanceof DBOPropertyPersister) {
            return ((DBOPropertyPersister<OBJECT_TYPE>)handler).getPersistActions(object, newValue);
        }
        return null;
    }
}