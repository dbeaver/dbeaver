/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jkiss.dbeaver.runtime.properties;

import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPNamedObject;
import org.jkiss.dbeaver.model.DBPObject;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.DBWorkbench;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.DBECommandReflector;
import org.jkiss.dbeaver.model.edit.DBEObjectEditor;
import org.jkiss.dbeaver.model.edit.DBEObjectMaker;
import org.jkiss.dbeaver.model.edit.prop.DBECommandProperty;
import org.jkiss.dbeaver.model.edit.prop.DBEPropertyHandler;
import org.jkiss.dbeaver.model.impl.DBSObjectCache;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.utils.CommonUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

/**
 * PropertySourceEditable
 */
public class PropertySourceEditable extends PropertySourceAbstract implements DBPObject, IPropertySourceEditable
{
    private static final Log log = Log.getLog(PropertySourceEditable.class);

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

    @Override
    public boolean isEditable(Object object)
    {
        DBEObjectEditor objectEditor = getObjectEditor(DBEObjectEditor.class);
        return objectEditor != null &&
            object instanceof DBPObject && objectEditor.canEditObject((DBPObject) object);
    }

    private <T> T getObjectEditor(Class<T> managerType)
    {
        final Object editableValue = getEditableValue();
        if (editableValue == null) {
            return null;
        }
        return DBWorkbench.getPlatform().getEditorsRegistry().getObjectManager(
            editableValue.getClass(),
            managerType);
    }

    @Override
    public DBECommandContext getCommandContext()
    {
        return commandContext;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void setPropertyValue(@Nullable DBRProgressMonitor monitor, Object editableValue, ObjectPropertyDescriptor prop, Object newValue)
        throws IllegalArgumentException
    {
        if (prop.getValueTransformer() != null) {
            newValue = prop.getValueTransformer().transform(editableValue, newValue);
        }
        final Object oldValue = getPropertyValue(monitor, editableValue, prop);
        if (!updatePropertyValue(monitor, editableValue, prop, newValue, false)) {
            return;
        }
        if (commandContext != null) {
            if (lastCommand == null || lastCommand.getObject() != editableValue || lastCommand.property != prop || !commandContext.isDirty()) {
                // Last command is not applicable (check for isDirty because command queue might be reverted)
                final DBEObjectEditor<DBPObject> objectEditor = getObjectEditor(DBEObjectEditor.class);
                if (objectEditor == null) {
                    log.error("Can't obtain object editor for " + getEditableValue());
                    return;
                }
                final DBEPropertyHandler<DBPObject> propertyHandler = objectEditor.makePropertyHandler(
                    (DBPObject) editableValue,
                    prop);
                PropertyChangeCommand curCommand = new PropertyChangeCommand((DBPObject) editableValue, prop, propertyHandler, oldValue, newValue);
                commandContext.addCommand(curCommand, commandReflector);
                lastCommand = curCommand;
            } else {
                lastCommand.setNewValue(newValue);
                commandContext.updateCommand(lastCommand, commandReflector);
            }
        }

        // If we perform rename then we should refresh object cache
        // To update name-based cache
        if (prop.isNameProperty() && editableValue instanceof DBSObject) {
            DBEObjectMaker objectManager = getObjectEditor(DBEObjectMaker.class);
            if (objectManager != null) {
                DBSObjectCache cache = objectManager.getObjectsCache((DBSObject) editableValue);
                if (cache != null && cache.isFullyCached()) {
                    List<? extends DBSObject> cachedObjects = CommonUtils.copyList(cache.getCachedObjects());
                    cache.setCache(cachedObjects);
                }
            }
        }

/*
        // Notify listeners
        for (IPropertySourceListener listener : listeners) {
            listener.handlePropertyChange(editableValue, prop, newValue);
        }
*/
    }

    private boolean updatePropertyValue(@Nullable DBRProgressMonitor monitor, Object editableValue, ObjectPropertyDescriptor prop, Object value, boolean force)
        throws IllegalArgumentException
    {
        // Write property value
        try {
            // Check for complex object
            // If value should be a named object then try to obtain it from list provider
            if (value != null && value.getClass() == String.class) {
                final Object[] items = prop.getPossibleValues(editableValue);
                if (items != null) {
                    boolean found = false;
                    if (items.length > 0) {
                        for (int i = 0, itemsLength = items.length; i < itemsLength; i++) {
                            if ((items[i] instanceof DBPNamedObject && value.equals(((DBPNamedObject) items[i]).getName())) ||
                                (items[i] instanceof Enum && value.equals(((Enum) items[i]).name()))
                                ) {
                                value = items[i];
                                found = true;
                                break;
                            }
                        }
                    }
                    if (!found) {
                        if (value.getClass() != prop.getDataType()){
                            value = null;
                        }
                    }
                }
            }
            final Object oldValue = getPropertyValue(monitor, editableValue, prop);
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
    public void resetPropertyValue(@Nullable DBRProgressMonitor monitor, Object object, ObjectPropertyDescriptor prop)
    {
//        final DBECommandComposite compositeCommand = (DBECommandComposite)getCommandContext().getUserParams().get(obj);
//        if (compositeCommand != null) {
//            final Object value = compositeCommand.getProperty(prop.getId());
//        }

        if (lastCommand != null && lastCommand.property == prop) {
            setPropertyValue(monitor, object, prop, lastCommand.getOldValue());
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
            updatePropertyValue(null, getObject(), property, getNewValue(), true);
        }
    }

    private class CommandReflector implements DBECommandReflector<DBPObject, PropertyChangeCommand> {

        @Override
        public void redoCommand(PropertyChangeCommand command)
        {
            updatePropertyValue(null, command.getObject(), command.property, command.getNewValue(), false);
/*
            // Notify listeners
            for (IPropertySourceListener listener : listeners) {
                listener.handlePropertyChange(command.getObject(), command.property, command.getNewValue());
            }
*/
        }

        @Override
        public void undoCommand(PropertyChangeCommand command)
        {
            updatePropertyValue(null, command.getObject(), command.property, command.getOldValue(), false);
/*
            // Notify listeners
            for (IPropertySourceListener listener : listeners) {
                listener.handlePropertyChange(command.getObject(), command.property, command.getOldValue());
            }
*/
        }
    }

}