/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.impl.jdbc.edit.struct;

import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.views.properties.IPropertyDescriptor;
import org.jkiss.dbeaver.ext.IDatabasePersistAction;
import org.jkiss.dbeaver.model.edit.DBEObjectCommander;
import org.jkiss.dbeaver.model.edit.DBEObjectEditor;
import org.jkiss.dbeaver.model.edit.DBEObjectMaker;
import org.jkiss.dbeaver.model.edit.prop.DBECommandComposite;
import org.jkiss.dbeaver.model.edit.prop.DBEPropertyHandler;
import org.jkiss.dbeaver.model.edit.prop.DBEPropertyReflector;
import org.jkiss.dbeaver.model.impl.edit.AbstractDatabasePersistAction;
import org.jkiss.dbeaver.model.impl.edit.DBECommandImpl;
import org.jkiss.dbeaver.model.impl.jdbc.edit.JDBCObjectManager;
import org.jkiss.dbeaver.model.impl.jdbc.struct.JDBCTable;
import org.jkiss.dbeaver.model.struct.DBSEntityContainer;

import java.util.IdentityHashMap;
import java.util.Map;

/**
 * JDBC table manager
 */
public abstract class JDBCTableManager<OBJECT_TYPE extends JDBCTable, CONTAINER_TYPE extends DBSEntityContainer>
    extends JDBCObjectManager<OBJECT_TYPE>
    implements DBEObjectMaker<OBJECT_TYPE>, DBEObjectEditor<OBJECT_TYPE>
{
    private final Map<IPropertyDescriptor, TablePropertyHandler> handlerMap = new IdentityHashMap<IPropertyDescriptor, TablePropertyHandler>();

    public long getMakerOptions()
    {
        return FEATURE_EDITOR_ON_CREATE;
    }

    public OBJECT_TYPE createNewObject(IWorkbenchWindow workbenchWindow, DBEObjectCommander commander, Object parent, Object copyFrom)
    {
        OBJECT_TYPE newTable = createNewTable((CONTAINER_TYPE) parent, copyFrom);

        commander.addCommand(new CommandCreateTable(newTable), null);

        return newTable;
    }

    public void deleteObject(DBEObjectCommander commander, OBJECT_TYPE object, Map<String, Object> options)
    {
        commander.addCommand(new CommandDropTable(object), null);
    }

    public DBEPropertyHandler<OBJECT_TYPE> makePropertyHandler(OBJECT_TYPE object, IPropertyDescriptor property)
    {
        TablePropertyHandler handler = handlerMap.get(property);
        if (handler == null) {
            handler = new TablePropertyHandler(property);
            handlerMap.put(property, handler);
        }
//        if (DBConstants.PROP_ID_NAME.equals(property.getId())) {
//            // Update object in navigator
//            object.getDataSource().getContainer().fireEvent(
//                new DBPEvent(DBPEvent.Action.OBJECT_UPDATE, object));
//        }
        return handler;
    }

    private class CommandCreateTable extends DBECommandImpl<OBJECT_TYPE> {
        protected CommandCreateTable(OBJECT_TYPE table)
        {
            super(table, "Create table");
        }
        public IDatabasePersistAction[] getPersistActions()
        {
            return new IDatabasePersistAction[] {
                new AbstractDatabasePersistAction("Create table", "CREATE TABLE " + getObject().getFullQualifiedName()) {
                    @Override
                    public void handleExecute(Throwable error)
                    {
                        if (error == null) {
                            //object.setPersisted(true);
                        }
                    }
                }};
        }
    }

    private class CommandDropTable extends DBECommandImpl<OBJECT_TYPE> {
        protected CommandDropTable(OBJECT_TYPE table)
        {
            super(table, "Drop table");
        }

        public IDatabasePersistAction[] getPersistActions()
        {
            return new IDatabasePersistAction[] {
                new AbstractDatabasePersistAction("Drop schema", "DROP TABLE " + getObject().getFullQualifiedName()) {
                    @Override
                    public void handleExecute(Throwable error)
                    {
                        if (error == null) {
                            //object.setPersisted(false);
                        }
                    }
                }};
        }
    }

    protected abstract OBJECT_TYPE createNewTable(CONTAINER_TYPE parent, Object copyFrom);

    protected abstract TableCompositeCommand createTableCommand(OBJECT_TYPE table);

    protected class TablePropertyHandler implements DBEPropertyHandler<OBJECT_TYPE>, DBEPropertyReflector<OBJECT_TYPE> {

        private final IPropertyDescriptor property;

        public TablePropertyHandler(IPropertyDescriptor property)
        {
            this.property = property;
        }

        public IPropertyDescriptor getProperty()
        {
            return property;
        }

        public DBECommandComposite<OBJECT_TYPE, ? extends DBEPropertyHandler<OBJECT_TYPE>> createCompositeCommand(OBJECT_TYPE object)
        {
            return createTableCommand(object);
        }

        public void reflectValueChange(OBJECT_TYPE object, Object oldValue, Object newValue)
        {
        }
    }

    protected abstract class TableCompositeCommand extends DBECommandComposite<OBJECT_TYPE, TablePropertyHandler> {

        protected TableCompositeCommand(OBJECT_TYPE object)
        {
            super(object, "Alter table");
        }

        protected Object getProperty(String id)
        {
            for (Map.Entry<TablePropertyHandler,Object> entry : getProperties().entrySet()) {
                if (id.equals(entry.getKey().getProperty().getId())) {
                    return entry.getValue();
                }
            }
            return null;
        }

    }

}

