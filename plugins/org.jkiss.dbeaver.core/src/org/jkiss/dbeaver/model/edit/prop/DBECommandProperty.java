/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.edit.prop;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.IDatabasePersistAction;
import org.jkiss.dbeaver.model.DBPObject;
import org.jkiss.dbeaver.model.edit.DBECommand;
import org.jkiss.dbeaver.model.impl.edit.DBECommandAbstract;

import java.util.Map;

/**
 * Abstract object command
 */
public class DBECommandProperty<OBJECT_TYPE extends DBPObject> extends DBECommandAbstract<OBJECT_TYPE> {

    //public static final String PROP_COMPOSITE_COMMAND = ".composite";

    private DBEPropertyHandler<OBJECT_TYPE> handler;
    private Object oldValue;
    private Object newValue;

    public DBECommandProperty(OBJECT_TYPE object, DBEPropertyHandler<OBJECT_TYPE> handler)
    {
        super(object, "Property '" + handler + "' change");
        this.handler = handler;
    }

    public DBECommandProperty(OBJECT_TYPE object, DBEPropertyHandler<OBJECT_TYPE> handler, Object oldValue, Object newValue)
    {
        this(object, handler);
        this.oldValue = oldValue;
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

    public Object getNewValue()
    {
        return newValue;
    }

    public void setNewValue(Object newValue)
    {
        this.newValue = newValue;
        if (handler instanceof DBEPropertyReflector) {
            ((DBEPropertyReflector<OBJECT_TYPE>)handler).reflectValueChange(getObject(), oldValue, this.newValue);
        }
    }

    public void resetValue()
    {
        this.newValue = this.oldValue;
    }

    @Override
    public DBECommand<?> merge(DBECommand<?> prevCommand, Map<Object, Object> userParams)
    {
        if (!(prevCommand instanceof DBECommandProperty) || prevCommand.getObject() != getObject()) {
            // Dunno what to do with it
            //return this;
        }
        DBECommandComposite compositeCommand = (DBECommandComposite)userParams.get(getObject());
        if (compositeCommand == null) {
            compositeCommand = handler.createCompositeCommand(getObject());
            userParams.put(getObject(), compositeCommand);
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