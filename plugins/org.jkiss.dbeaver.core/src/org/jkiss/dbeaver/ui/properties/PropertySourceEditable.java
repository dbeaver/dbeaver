/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.properties;

import net.sf.jkiss.utils.CommonUtils;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.model.DBPObject;
import org.jkiss.dbeaver.model.DBPPersistedObject;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.DBECommandReflector;
import org.jkiss.dbeaver.model.edit.DBEObjectEditor;
import org.jkiss.dbeaver.model.edit.prop.DBECommandComposite;
import org.jkiss.dbeaver.model.edit.prop.DBECommandProperty;
import org.jkiss.dbeaver.model.edit.prop.DBEPropertyHandler;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

/**
 * PropertySourceEditable
 */
public class PropertySourceEditable extends PropertySourceAbstract implements DBPObject, IPropertySourceEditable
{
    private DBECommandContext commandContext;
    private PropertyChangeCommand lastCommand = null;
    private final List<IPropertySourceListener> listeners = new ArrayList<IPropertySourceListener>();

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

    public void addPropertySourceListener(IPropertySourceListener listener)
    {
        synchronized (listeners) {
            listeners.add(listener);
        }
    }

    public void removePropertySourceListener(IPropertySourceListener listener)
    {
        synchronized (listeners) {
            listeners.add(listener);
        }
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
        final Object oldValue = getPropertyValue(editableValue, prop);
        if (CommonUtils.equalObjects(oldValue, value)) {
            return;
        }
        updatePropertyValue(editableValue, prop, value, false);
        if (lastCommand == null || lastCommand.getObject() != editableValue || lastCommand.property != prop) {
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
        // Notify listeners
        for (IPropertySourceListener listener : listeners) {
            listener.handlePropertyChange(editableValue, prop, value);
        }
    }

    private void updatePropertyValue(Object editableValue, ObjectPropertyDescriptor prop, Object value, boolean force)
    {
        if (force || !(editableValue instanceof DBPPersistedObject) || !((DBPPersistedObject)editableValue).isPersisted()) {
            // Write property value only for non-persisted objects
            try {
                prop.writeValue(editableValue, value);
            } catch (Throwable e) {
                if (e instanceof InvocationTargetException) {
                    e = ((InvocationTargetException) e).getTargetException();
                }
                log.error("Can't write property '" + prop.getDisplayName() + "' value", e);
            }
        }
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
            updatePropertyValue(getObject(), property, getNewValue(), true);
        }
    }

    private class CommandReflector implements DBECommandReflector<DBPObject, PropertyChangeCommand> {

        public void redoCommand(PropertyChangeCommand command)
        {
            final Object propertyValue = getPropertyValue(command.getObject(), command.property);
            updatePropertyValue(command.getObject(), command.property, propertyValue, false);
            // Notify listeners
            for (IPropertySourceListener listener : listeners) {
                listener.handlePropertyChange(command.getObject(), command.property, propertyValue);
            }
        }

        public void undoCommand(PropertyChangeCommand command)
        {
            final Object propertyValue = getPropertyValue(command.getObject(), command.property);
            updatePropertyValue(command.getObject(), command.property, propertyValue, false);
            // Notify listeners
            for (IPropertySourceListener listener : listeners) {
                listener.handlePropertyChange(command.getObject(), command.property, propertyValue);
            }
        }
    }

}