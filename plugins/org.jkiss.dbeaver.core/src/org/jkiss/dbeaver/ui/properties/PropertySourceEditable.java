/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.properties;

import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.model.DBPObject;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.DBECommandReflector;
import org.jkiss.dbeaver.model.edit.DBEObjectEditor;
import org.jkiss.dbeaver.model.edit.prop.DBECommandComposite;
import org.jkiss.dbeaver.model.edit.prop.DBECommandProperty;
import org.jkiss.dbeaver.model.edit.prop.DBEPropertyHandler;

import java.lang.reflect.InvocationTargetException;

/**
 * PropertySourceEditable
 */
public class PropertySourceEditable extends PropertySourceAbstract implements DBPObject, IPropertySourceEditable
{
    private DBECommandContext commandContext;
    private PropertyChangeCommand lastCommand = null;

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

    @Override
    public Object getPropertyValue(Object editableValue, ObjectPropertyDescriptor prop)
    {
        final DBECommandComposite compositeCommand = (DBECommandComposite)getCommandContext().getUserParams().get(editableValue);
        if (compositeCommand != null) {
            final Object value = compositeCommand.getProperty(prop.getId());
            if (value != null) {
                return value;
            }
        }
        return super.getPropertyValue(editableValue, prop);
    }

    @Override
    public void setPropertyValue(Object editableValue, ObjectPropertyDescriptor prop, Object value)
    {
        if (prop.getValueTransformer() != null) {
            value = prop.getValueTransformer().transform(editableValue, value);
        }
        if (lastCommand == null || lastCommand.property != prop) {
            final DBEObjectEditor<DBPObject> objectEditor = getObjectEditor();
            if (objectEditor == null) {
                log.error("Can't obtain object editor for " + getEditableValue());
                return;
            }
            final DBEPropertyHandler<DBPObject> propertyHandler = objectEditor.makePropertyHandler(
                (DBPObject)editableValue,
                prop);
            PropertyChangeCommand curCommand = new PropertyChangeCommand((DBPObject) editableValue, prop, propertyHandler, value);
            final CommandReflector reflector = new CommandReflector();
            getCommandContext().addCommand(curCommand, reflector);
            lastCommand = curCommand;
        } else {
            lastCommand.setNewValue(value);
            getCommandContext().updateCommand(lastCommand);
        }

        handlePropertyChange(editableValue, prop, value);
    }

    @Override
    public void resetPropertyValue(Object object, ObjectPropertyDescriptor prop)
    {
//        final ObjectProps objectProps = getObjectProps(object);
//        DBECommandProperty curCommand = objectProps.propValues.get(prop);
//        if (curCommand != null) {
//            curCommand.resetValue();
//        }
        log.warn("Property reset not implemented");
    }

    protected void handlePropertyChange(Object editableValue, ObjectPropertyDescriptor prop, Object value)
    {
    }

    private class PropertyChangeCommand extends DBECommandProperty<DBPObject> {
        ObjectPropertyDescriptor property;
        public PropertyChangeCommand(DBPObject editableValue, ObjectPropertyDescriptor property, DBEPropertyHandler<DBPObject> propertyHandler, Object value)
        {
            super(editableValue, propertyHandler, value);
            this.property = property;
        }

        @Override
        public void updateModel()
        {
            super.updateModel();
            try {
                property.writeValue(getObject(), getNewValue());
            } catch (Throwable e) {
                if (e instanceof InvocationTargetException) {
                    e = ((InvocationTargetException) e).getTargetException();
                }
                log.error("Can't write property '" + property.getDisplayName() + "' value", e);
            }
        }
    }

    private class CommandReflector implements DBECommandReflector<DBPObject, PropertyChangeCommand> {

        public void redoCommand(PropertyChangeCommand propertyChangeCommand)
        {
            //propertyChangeCommand.
        }

        public void undoCommand(PropertyChangeCommand propertyChangeCommand)
        {

        }
    }

}