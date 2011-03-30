/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.views.properties;

import net.sf.jkiss.utils.CommonUtils;
import org.eclipse.ui.views.properties.IPropertyDescriptor;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.model.DBPObject;
import org.jkiss.dbeaver.model.edit.DBEObjectCommander;
import org.jkiss.dbeaver.model.edit.DBEObjectEditor;
import org.jkiss.dbeaver.model.edit.DBEObjectManager;
import org.jkiss.dbeaver.model.edit.prop.DBECommandComposite;
import org.jkiss.dbeaver.model.edit.prop.DBECommandProperty;
import org.jkiss.dbeaver.model.edit.prop.DBEPropertyHandler;
import org.jkiss.dbeaver.model.meta.Property;

import java.util.HashMap;
import java.util.Map;

/**
 * PropertySourceEditable
 */
public class PropertySourceEditable extends PropertySourceAbstract implements DBPObject, IPropertySourceEditable
{
    private DBEObjectCommander objectCommander;
    private DBEObjectEditor objectManager;
    private Map<Object, Object> updatedValues = new HashMap<Object, Object>();

    public PropertySourceEditable(DBPObject object, DBEObjectCommander objectCommander)
    {
        super(object, object, true);
        this.objectCommander = objectCommander;
        this.objectManager = DBeaverCore.getInstance().getEditorsRegistry().getObjectManager(object.getClass(), DBEObjectEditor.class);
    }

    public boolean isEditable()
    {
        return objectCommander != null && this.objectManager != null;
    }

    public DBEObjectCommander getObjectCommander()
    {
        return objectCommander;
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
        final DBECommandProperty<DBPObject> command = new DBECommandProperty<DBPObject>(
            (DBPObject) getEditableValue(),
            propertyHandler,
            value);
        getObjectCommander().addCommand(command, null);
    }

}