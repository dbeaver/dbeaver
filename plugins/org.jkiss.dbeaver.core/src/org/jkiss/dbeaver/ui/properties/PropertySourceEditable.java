/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.properties;

import org.jkiss.utils.CommonUtils;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.model.DBPNamedObject;
import org.jkiss.dbeaver.model.DBPObject;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.DBECommandReflector;
import org.jkiss.dbeaver.model.edit.DBEObjectEditor;
import org.jkiss.dbeaver.model.edit.prop.DBECommandProperty;
import org.jkiss.dbeaver.model.edit.prop.DBEPropertyHandler;
import org.jkiss.dbeaver.model.struct.DBSObject;

import java.lang.reflect.InvocationTargetException;

/**
 * PropertySourceEditable
 */
public class PropertySourceEditable extends PropertySourceAbstract implements DBPObject, IPropertySourceEditable
{
    private DBECommandContext commandContext;
    private PropertyChangeCommand lastCommand = null;
    //private final List<IPropertySourceListener> listeners = new ArrayList<IPropertySourceListener>();
    private final CommandReflector commandReflector = new CommandReflector();

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
    public void setPropertyValue(Object editableValue, ObjectPropertyDescriptor prop, Object newValue)
    {
        if (prop.getValueTransformer() != null) {
            newValue = prop.getValueTransformer().transform(editableValue, newValue);
        }
        if (!updatePropertyValue(editableValue, prop, newValue, false)) {
            return;
        }
        if (lastCommand == null || lastCommand.getObject() != editableValue || lastCommand.property != prop) {
            final DBEObjectEditor<DBPObject> objectEditor = getObjectEditor();
            if (objectEditor == null) {
                log.error("Can't obtain object editor for " + getEditableValue());
                return;
            }
            final DBEPropertyHandler<DBPObject> propertyHandler = objectEditor.makePropertyHandler(
                (DBPObject)editableValue,
                prop);
            final Object oldValue = getPropertyValue(editableValue, prop);
            PropertyChangeCommand curCommand = new PropertyChangeCommand((DBPObject) editableValue, prop, propertyHandler, oldValue, newValue);
            getCommandContext().addCommand(curCommand, commandReflector);
            lastCommand = curCommand;
        } else {
            lastCommand.setNewValue(newValue);
            getCommandContext().updateCommand(lastCommand, commandReflector);
        }
/*
        // Notify listeners
        for (IPropertySourceListener listener : listeners) {
            listener.handlePropertyChange(editableValue, prop, newValue);
        }
*/
    }

    private boolean updatePropertyValue(Object editableValue, ObjectPropertyDescriptor prop, Object value, boolean force)
    {
        // Write property value
        try {
            // Check for complex object
            // If value should be a named object then try to obtain it from list provider
            if (value != null && value.getClass() == String.class) {
                final Object[] items = prop.getPossibleValues(editableValue);
                if (!CommonUtils.isEmpty(items)) {
                    for (int i = 0, itemsLength = items.length; i < itemsLength; i++) {
                        if ((items[i] instanceof DBPNamedObject && value.equals(((DBPNamedObject)items[i]).getName())) ||
                            (items[i] instanceof Enum && value.equals(((Enum)items[i]).name()))
                            )
                        {
                            value = items[i];
                            break;
                        }
                    }
                }
            }
            final Object oldValue = getPropertyValue(editableValue, prop);
            if (CommonUtils.equalObjects(oldValue, value)) {
                return false;
            }

            prop.writeValue(editableValue, value);
            // Fire object update event
            if (editableValue instanceof DBSObject) {
                DBUtils.fireObjectUpdate((DBSObject) editableValue, prop);
            }
            return true;
        } catch (Throwable e) {
            if (e instanceof InvocationTargetException) {
                e = ((InvocationTargetException) e).getTargetException();
            }
            log.error("Can't write property '" + prop.getDisplayName() + "' value", e);
            return false;
        }
    }

    @Override
    public boolean isPropertyResettable(Object object, ObjectPropertyDescriptor prop)
    {
        return (lastCommand != null && lastCommand.property == prop && lastCommand.getObject() == object);
    }

    @Override
    public void resetPropertyValue(Object object, ObjectPropertyDescriptor prop)
    {
//        final DBECommandComposite compositeCommand = (DBECommandComposite)getCommandContext().getUserParams().get(obj);
//        if (compositeCommand != null) {
//            final Object value = compositeCommand.getProperty(prop.getId());
//        }

        if (lastCommand != null && lastCommand.property == prop) {
            setPropertyValue(object, prop, lastCommand.getOldValue());
        }
//        final ObjectProps objectProps = getObjectProps(object);
//        DBECommandProperty curCommand = objectProps.propValues.get(prop);
//        if (curCommand != null) {
//            curCommand.resetValue();
//        }
        log.warn("Property reset not implemented");
    }

    private class PropertyChangeCommand extends DBECommandProperty<DBPObject> {
        ObjectPropertyDescriptor property;
        public PropertyChangeCommand(DBPObject editableValue, ObjectPropertyDescriptor property, DBEPropertyHandler<DBPObject> propertyHandler, Object oldValue, Object newValue)
        {
            super(editableValue, propertyHandler, oldValue, newValue);
            this.property = property;
        }

        @Override
        public void updateModel()
        {
            super.updateModel();
            updatePropertyValue(getObject(), property, getNewValue(), true);
        }
    }

    private class CommandReflector implements DBECommandReflector<DBPObject, PropertyChangeCommand> {

        public void redoCommand(PropertyChangeCommand command)
        {
            updatePropertyValue(command.getObject(), command.property, command.getNewValue(), false);
/*
            // Notify listeners
            for (IPropertySourceListener listener : listeners) {
                listener.handlePropertyChange(command.getObject(), command.property, command.getNewValue());
            }
*/
        }

        public void undoCommand(PropertyChangeCommand command)
        {
            updatePropertyValue(command.getObject(), command.property, command.getOldValue(), false);
/*
            // Notify listeners
            for (IPropertySourceListener listener : listeners) {
                listener.handlePropertyChange(command.getObject(), command.property, command.getOldValue());
            }
*/
        }
    }

}