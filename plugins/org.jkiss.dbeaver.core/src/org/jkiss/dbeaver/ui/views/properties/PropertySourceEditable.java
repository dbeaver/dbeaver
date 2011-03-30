/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.views.properties;

import net.sf.jkiss.utils.CommonUtils;
import org.jkiss.dbeaver.model.DBPObject;
import org.jkiss.dbeaver.model.edit.DBEObjectCommander;
import org.jkiss.dbeaver.model.edit.prop.DBECommandComposite;
import org.jkiss.dbeaver.model.edit.prop.DBECommandProperty;
import org.jkiss.dbeaver.model.edit.prop.DBEPropertyHandler;

import java.util.HashMap;
import java.util.Map;

/**
 * PropertySourceEditable
 */
public class PropertySourceEditable extends PropertySourceAbstract implements DBPObject, IPropertySourceEditable
{
    private DBEObjectCommander objectCommander;
    private Map<Object, Object> updatedValues = new HashMap<Object, Object>();

    public PropertySourceEditable(DBPObject object, DBEObjectCommander objectCommander)
    {
        super(object, object, true);
        this.objectCommander = objectCommander;
    }

    public boolean isEditable()
    {
        return objectCommander != null;
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
        final DBECommandProperty<DBPObject> command = new DBECommandProperty<DBPObject>(
            (DBPObject) getEditableValue(),
            new DBEPropertyHandler<DBPObject>() {
                public DBECommandComposite<DBPObject, ? extends DBEPropertyHandler<DBPObject>> createCompositeCommand(DBPObject object)
                {
                    return new DBECommandComposite<DBPObject, DBEPropertyHandler<DBPObject>>(object, "Change table") { };
                }
            },
            value);
        getObjectCommander().addCommand(command, null);
    }

}