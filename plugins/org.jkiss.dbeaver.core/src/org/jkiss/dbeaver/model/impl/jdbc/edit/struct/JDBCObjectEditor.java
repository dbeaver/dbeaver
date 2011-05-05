/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.impl.jdbc.edit.struct;

import org.eclipse.ui.views.properties.IPropertyDescriptor;
import org.jkiss.dbeaver.ext.IDatabasePersistAction;
import org.jkiss.dbeaver.model.DBPObject;
import org.jkiss.dbeaver.model.edit.DBECommand;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.DBECommandReflector;
import org.jkiss.dbeaver.model.edit.DBEObjectEditor;
import org.jkiss.dbeaver.model.edit.prop.DBECommandComposite;
import org.jkiss.dbeaver.model.edit.prop.DBECommandProperty;
import org.jkiss.dbeaver.model.edit.prop.DBEPropertyHandler;
import org.jkiss.dbeaver.model.edit.prop.DBEPropertyReflector;
import org.jkiss.dbeaver.model.impl.jdbc.edit.JDBCObjectManager;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.ui.properties.ObjectPropertyDescriptor;
import org.jkiss.dbeaver.ui.properties.PropertyCollector;
import org.jkiss.dbeaver.ui.properties.ProxyPropertyDescriptor;

import java.util.ArrayList;
import java.util.List;
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
        return new PropertyHandler<OBJECT_TYPE>(this, property);
    }

    protected void makeInitialCommands(
        OBJECT_TYPE object,
        DBECommandContext context,
        DBECommand<OBJECT_TYPE> createCommand)
    {
        List<DBECommand> commands = new ArrayList<DBECommand>();
        commands.add(createCommand);

        PropertyCollector propertyCollector = new PropertyCollector(object, false);
        propertyCollector.collectProperties();
        for (IPropertyDescriptor prop : propertyCollector.getPropertyDescriptors()) {
            if (prop instanceof ObjectPropertyDescriptor && ((ObjectPropertyDescriptor) prop).isEditPossible()) {
                final Object propertyValue = propertyCollector.getPropertyValue(prop.getId());
                if (propertyValue != null) {
                    commands.add(new NewObjectPropertyCommand(object, new PropertyHandler<OBJECT_TYPE>(this, prop), propertyValue));
                }
            }
        }

        context.addCommandBatch(commands, new NewObjectReflector());
    }

    protected abstract IDatabasePersistAction[] makePersistActions(ObjectChangeCommand<OBJECT_TYPE> command);

    protected static class PropertyHandler<OBJECT_TYPE extends DBSObject> extends ProxyPropertyDescriptor implements DBEPropertyHandler<OBJECT_TYPE>, DBEPropertyReflector<OBJECT_TYPE> {
        private final JDBCObjectEditor<OBJECT_TYPE> editor;
        private PropertyHandler(JDBCObjectEditor<OBJECT_TYPE> editor, IPropertyDescriptor property)
        {
            super(property);
            this.editor = editor;
        }

        public DBECommandComposite<OBJECT_TYPE, ? extends DBEPropertyHandler<OBJECT_TYPE>> createCompositeCommand(OBJECT_TYPE object)
        {
            return new ObjectChangeCommand<OBJECT_TYPE>(editor, object);
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
            return original.getId().hashCode();
        }

        @Override
        public boolean equals(Object obj)
        {
            return obj != null &&
                obj.getClass() == PropertyHandler.class &&
                editor == ((PropertyHandler)obj).editor &&
                getId().equals(((PropertyHandler) obj).getId());
        }
    }

    protected static class ObjectChangeCommand<OBJECT_TYPE extends DBSObject> extends DBECommandComposite<OBJECT_TYPE, PropertyHandler<OBJECT_TYPE>> {
        private final JDBCObjectEditor<OBJECT_TYPE> editor;
        private ObjectChangeCommand(JDBCObjectEditor<OBJECT_TYPE> editor, OBJECT_TYPE object)
        {
            super(object, "JDBC Composite");
            this.editor = editor;
        }

        public Object getProperty(String id)
        {
            for (Map.Entry<PropertyHandler<OBJECT_TYPE>,Object> entry : getProperties().entrySet()) {
                if (id.equals(entry.getKey().getId())) {
                    return entry.getValue();
                }
            }
            return null;
        }

        public IDatabasePersistAction[] getPersistActions()
        {
            return editor.makePersistActions(this);
        }
    }

    protected class NewObjectPropertyCommand extends DBECommandProperty<OBJECT_TYPE> {
        public NewObjectPropertyCommand(OBJECT_TYPE object, PropertyHandler<OBJECT_TYPE> handler, Object value)
        {
            super(object, handler, value);
        }

        @Override
        public boolean isUndoable()
        {
            return true;
        }
    }

    protected static class NewObjectReflector<OBJECT_TYPE extends DBPObject> implements DBECommandReflector<OBJECT_TYPE, DBECommand<OBJECT_TYPE>> {

        public void redoCommand(DBECommand<OBJECT_TYPE> object)
        {
            //To change body of implemented methods use File | Settings | File Templates.
        }

        public void undoCommand(DBECommand<OBJECT_TYPE> object)
        {
            //To change body of implemented methods use File | Settings | File Templates.
        }
    }

}

