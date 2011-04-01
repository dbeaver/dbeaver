/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.views.properties;

import net.sf.jkiss.utils.CommonUtils;
import org.eclipse.ui.views.properties.IPropertyDescriptor;
import org.jkiss.dbeaver.ext.IDatabaseNodeEditorInput;
import org.jkiss.dbeaver.model.DBConstants;
import org.jkiss.dbeaver.model.DBPObject;
import org.jkiss.dbeaver.model.edit.DBECommand;
import org.jkiss.dbeaver.model.edit.DBECommandReflector;
import org.jkiss.dbeaver.model.edit.DBEObjectCommander;
import org.jkiss.dbeaver.model.edit.DBEObjectEditor;
import org.jkiss.dbeaver.model.edit.prop.DBECommandProperty;
import org.jkiss.dbeaver.model.edit.prop.DBEPropertyHandler;

import java.util.HashMap;
import java.util.Map;

/**
 * PropertySourceEditable
 */
public class PropertySourceEditable extends PropertySourceAbstract implements DBPObject, IPropertySourceEditable
{
    private IDatabaseNodeEditorInput editorInput;
    private DBEObjectEditor objectManager;
    private Map<Object, Object> updatedValues = new HashMap<Object, Object>();

    public PropertySourceEditable(IDatabaseNodeEditorInput editorInput)
    {
        super(editorInput.getDatabaseObject(), editorInput.getDatabaseObject(), true);
        this.editorInput = editorInput;
        this.objectManager = editorInput.getObjectManager(DBEObjectEditor.class);
    }

    public boolean isEditable()
    {
        return editorInput.getObjectCommander() != null && this.objectManager != null;
    }

    public DBEObjectCommander getObjectCommander()
    {
        return editorInput.getObjectCommander();
    }

    @Override
    public void resetPropertyValue(Object id)
    {

    }

    @Override
    public Object getPropertyValue(Object id)
    {
        final Object value = updatedValues.get(id);
        if (value != null) {
            return value;
        } else {
            return super.getPropertyValue(id);
        }
    }

    @Override
    public void setPropertyValue(Object id, Object value)
    {
        final Object oldValue = updatedValues.put(id, value);
        if (CommonUtils.equalObjects(oldValue, value)) {
            return;
        }
        final IPropertyDescriptor propertyDescriptor = getPropertyDescriptor(id);
        if (propertyDescriptor == null) {
            log.warn("Can't detect property meta info for property '" + id + "'");
            return;
        }
        final DBEPropertyHandler<DBPObject> propertyHandler = objectManager.makePropertyHandler((DBPObject) getEditableValue(), propertyDescriptor);
        final DBECommandProperty<? extends DBPObject> command = new DBECommandProperty<DBPObject>(
            (DBPObject) getEditableValue(),
            propertyHandler,
            value);
        final CommandReflector reflector = new CommandReflector();
        getObjectCommander().addCommand(command, reflector);

        handlePropertyChange(id, value);
    }

    private void handlePropertyChange(Object id, Object value)
    {
        if (DBConstants.PROP_ID_NAME.equals(id)) {
            // Update object in navigator
            editorInput.getTreeNode().setNodeName(CommonUtils.toString(value));
        }
    }

    private class CommandReflector <T extends DBPObject>  implements DBECommandReflector<T, DBECommand<T>> {
        public void redoCommand(DBECommand<T> tdbeCommand)
        {

        }

        public void undoCommand(DBECommand<T> tdbeCommand)
        {

        }
    }
}