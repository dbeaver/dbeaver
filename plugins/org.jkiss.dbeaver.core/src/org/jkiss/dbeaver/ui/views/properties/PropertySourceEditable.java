/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.views.properties;

import net.sf.jkiss.utils.CommonUtils;
import org.eclipse.ui.views.properties.IPropertyDescriptor;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.model.DBConstants;
import org.jkiss.dbeaver.model.DBPObject;
import org.jkiss.dbeaver.model.edit.DBECommand;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.DBECommandReflector;
import org.jkiss.dbeaver.model.edit.DBEObjectEditor;
import org.jkiss.dbeaver.model.edit.prop.DBECommandProperty;
import org.jkiss.dbeaver.model.edit.prop.DBEPropertyHandler;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseNode;
import org.jkiss.dbeaver.model.navigator.DBNEvent;

import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;

/**
 * PropertySourceEditable
 */
public class PropertySourceEditable extends PropertySourceAbstract implements DBPObject, IPropertySourceEditable
{
    private DBECommandContext commandContext;
    private Map<DBPObject, Map<Object, Object>> updatedValues = new IdentityHashMap<DBPObject, Map<Object, Object>>();
    private DBECommandProperty<? extends DBPObject> curCommand = null;

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

    private Map<Object, Object> getObjectProps(Object object)
    {
        Map<Object, Object> props = updatedValues.get((DBPObject)object);
        if (props == null) {
            props = new HashMap<Object, Object>();
            updatedValues.put((DBPObject)object, props);
        }
        return props;
    }

    @Override
    public Object getPropertyValue(Object editableValue, Object id)
    {
        final Object value = getObjectProps(editableValue).get(id);
        if (value != null) {
            return value;
        } else {
            return super.getPropertyValue(editableValue, id);
        }
    }

    @Override
    public void setPropertyValue(Object editableValue, Object id, Object value)
    {
        final Object oldValue = getObjectProps(editableValue).put(id, value);
        if (CommonUtils.equalObjects(oldValue, value)) {
            return;
        }
        final IPropertyDescriptor propertyDescriptor = getPropertyDescriptor(id);
        if (propertyDescriptor == null) {
            log.warn("Can't detect property meta info for property '" + id + "'");
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
                propertyDescriptor);
            curCommand = new DBECommandProperty<DBPObject>(
                (DBPObject)editableValue,
                propertyHandler,
                value);
            final CommandReflector reflector = new CommandReflector();
            getCommandContext().addCommand(curCommand, reflector);
        } else {
            curCommand.setNewValue(value);
            getCommandContext().updateCommand(curCommand);
        }

        handlePropertyChange(editableValue, id, value);
    }

    protected void handlePropertyChange(Object editableValue, Object id, Object value)
    {
        if (getSourceObject() instanceof DBNDatabaseNode) {
            final DBNDatabaseNode sourceNode = (DBNDatabaseNode) getSourceObject();
            if (DBConstants.PROP_ID_NAME.equals(id) && sourceNode.getObject() == editableValue) {
                // Update object in navigator
                sourceNode.setNodeName(CommonUtils.toString(value));
            }
        }
    }

    private class CommandReflector <T extends DBPObject>  implements DBECommandReflector<T, DBECommand<T>> {
        public void redoCommand(DBECommand<T> command)
        {

        }

        public void undoCommand(DBECommand<T> command)
        {

        }
    }
}