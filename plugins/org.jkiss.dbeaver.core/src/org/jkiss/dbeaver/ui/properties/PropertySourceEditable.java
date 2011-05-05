/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.properties;

import net.sf.jkiss.utils.CommonUtils;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.model.DBPObject;
import org.jkiss.dbeaver.model.edit.DBECommand;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.DBECommandReflector;
import org.jkiss.dbeaver.model.edit.DBEObjectEditor;
import org.jkiss.dbeaver.model.edit.prop.DBECommandProperty;
import org.jkiss.dbeaver.model.edit.prop.DBEPropertyHandler;

import java.util.IdentityHashMap;
import java.util.Map;

/**
 * PropertySourceEditable
 */
public class PropertySourceEditable extends PropertySourceAbstract implements DBPObject, IPropertySourceEditable
{
    private DBECommandContext commandContext;
    private Map<DBPObject, ObjectProps> updatedValues = new IdentityHashMap<DBPObject, ObjectProps>();

    private static class ObjectProps {
        Map<ObjectPropertyDescriptor, DBECommandProperty> propValues = new IdentityHashMap<ObjectPropertyDescriptor, DBECommandProperty>();
    }

    public PropertySourceEditable(DBECommandContext commandContext, Object sourceObject, Object object)
    {
        super(sourceObject, object, true);
        this.commandContext = commandContext;
        //this.objectManager = editorInput.getObjectManager(DBEObjectEditor.class);
    }

    public boolean isEditable(Object object)
    {
        return commandContext != null && getObjectEditor() != null;
    }

    private DBEObjectEditor getObjectEditor()
    {
        final Object editableValue = getEditableValue();
        if (editableValue == null) {
            return null;
        }
        return DBeaverCore.getInstance().getEditorsRegistry().getObjectManager(
            editableValue.getClass(),
            DBEObjectEditor.class);
    }

    public DBECommandContext getCommandContext()
    {
        return commandContext;
    }

    private ObjectProps getObjectProps(Object object)
    {
        ObjectProps props = updatedValues.get((DBPObject)object);
        if (props == null) {
            props = new ObjectProps();
            updatedValues.put((DBPObject)object, props);
        }
        return props;
    }

    @Override
    public Object getPropertyValue(Object editableValue, ObjectPropertyDescriptor prop)
    {
        final ObjectProps objectProps = getObjectProps(editableValue);
        final DBECommandProperty value = objectProps.propValues.get(prop);
        if (value != null && value.getNewValue() != null) {
            return value.getNewValue();
        } else {
            return super.getPropertyValue(editableValue, prop);
        }
    }

    @Override
    public void setPropertyValue(Object editableValue, ObjectPropertyDescriptor prop, Object value)
    {
        final ObjectProps objectProps = getObjectProps(editableValue);
        DBECommandProperty curCommand = objectProps.propValues.get(prop);
        if (curCommand != null && CommonUtils.equalObjects(curCommand.getNewValue(), value)) {
            return;
        }
        final DBEObjectEditor<DBPObject> objectEditor = getObjectEditor();
        if (objectEditor == null) {
            log.error("Can't obtain object editor for " + getEditableValue());
            return;
        }
        if (curCommand == null) {
            final DBEPropertyHandler<DBPObject> propertyHandler = objectEditor.makePropertyHandler(
                (DBPObject)editableValue,
                prop);
            curCommand = new DBECommandProperty<DBPObject>(
                (DBPObject)editableValue,
                propertyHandler,
                value);
            objectProps.propValues.put(prop, curCommand);
            final CommandReflector reflector = new CommandReflector();
            getCommandContext().addCommand(curCommand, reflector);
        } else {
            curCommand.setNewValue(value);
            getCommandContext().updateCommand(curCommand);
        }

        handlePropertyChange(editableValue, prop, value);
    }

    protected void handlePropertyChange(Object editableValue, ObjectPropertyDescriptor prop, Object value)
    {
    }

    private class CommandReflector <T extends DBPObject> implements DBECommandReflector<T, DBECommand<T>> {
        public void redoCommand(DBECommand<T> command)
        {

        }

        public void undoCommand(DBECommand<T> command)
        {

        }
    }

}