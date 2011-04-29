/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.impl.jdbc.edit.struct;

import org.eclipse.ui.views.properties.IPropertyDescriptor;
import org.jkiss.dbeaver.ext.IDatabasePersistAction;
import org.jkiss.dbeaver.model.edit.DBEObjectEditor;
import org.jkiss.dbeaver.model.edit.prop.DBECommandComposite;
import org.jkiss.dbeaver.model.edit.prop.DBEPropertyHandler;
import org.jkiss.dbeaver.model.edit.prop.DBEPropertyReflector;
import org.jkiss.dbeaver.model.impl.jdbc.edit.JDBCObjectManager;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.ui.views.properties.ProxyPropertyDescriptor;

import java.util.Map;

/**
 * JDBC object editor
 */
public abstract class JDBCObjectEditor<OBJECT_TYPE extends DBSObject>
    extends JDBCObjectManager<OBJECT_TYPE>
    implements DBEObjectEditor<OBJECT_TYPE>
{
    public DBEPropertyHandler<OBJECT_TYPE> makePropertyHandler(OBJECT_TYPE object, IPropertyDescriptor property)
    {
        return new PropertyHandler(property);
    }

    protected abstract IDatabasePersistAction[] makePersistActions(ObjectChangeCommand command);

    protected class PropertyHandler extends ProxyPropertyDescriptor implements DBEPropertyHandler<OBJECT_TYPE>, DBEPropertyReflector<OBJECT_TYPE> {

        public PropertyHandler(IPropertyDescriptor property)
        {
            super(property);
        }

        public DBECommandComposite<OBJECT_TYPE, ? extends DBEPropertyHandler<OBJECT_TYPE>> createCompositeCommand(OBJECT_TYPE object)
        {
            return new ObjectChangeCommand(object);
        }

        public void reflectValueChange(OBJECT_TYPE object, Object oldValue, Object newValue)
        {
        }

        @Override
        public String toString()
        {
            return original.toString();
        }

        @Override
        public int hashCode()
        {
            return original.hashCode();
        }

        @Override
        public boolean equals(Object obj)
        {
            return obj != null &&
                obj.getClass() == PropertyHandler.class &&
                original.equals(((PropertyHandler) obj).original);
        }
    }

    protected class ObjectChangeCommand extends DBECommandComposite<OBJECT_TYPE, PropertyHandler> {

        protected ObjectChangeCommand(OBJECT_TYPE object)
        {
            super(object, "JDBC Composite");
        }

        public Object getProperty(String id)
        {
            for (Map.Entry<PropertyHandler,Object> entry : getProperties().entrySet()) {
                if (id.equals(entry.getKey().getId())) {
                    return entry.getValue();
                }
            }
            return null;
        }

        public IDatabasePersistAction[] getPersistActions()
        {
            return makePersistActions(this);
        }
    }

}

