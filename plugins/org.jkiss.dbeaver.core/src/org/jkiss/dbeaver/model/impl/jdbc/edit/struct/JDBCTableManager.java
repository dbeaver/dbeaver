package org.jkiss.dbeaver.model.impl.jdbc.edit.struct;

import org.eclipse.ui.IWorkbenchWindow;
import org.jkiss.dbeaver.ext.IDatabasePersistAction;
import org.jkiss.dbeaver.model.edit.DBOCreator;
import org.jkiss.dbeaver.model.impl.edit.AbstractDatabasePersistAction;
import org.jkiss.dbeaver.model.impl.edit.DBOCommandImpl;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCDataSource;
import org.jkiss.dbeaver.model.impl.jdbc.edit.DBOEditorJDBC;
import org.jkiss.dbeaver.model.impl.jdbc.struct.JDBCTable;
import org.jkiss.dbeaver.model.struct.DBSEntityContainer;

import java.util.Map;

/**
 * JDBC table manager
 */
public abstract class JDBCTableManager<OBJECT_TYPE extends JDBCTable, CONTAINER_TYPE extends DBSEntityContainer>  extends DBOEditorJDBC<OBJECT_TYPE> implements DBOCreator<OBJECT_TYPE> {

    public CreateResult createNewObject(IWorkbenchWindow workbenchWindow, Object parent, OBJECT_TYPE copyFrom)
    {
        OBJECT_TYPE newUser = createNewTable((CONTAINER_TYPE) parent, copyFrom);
        setObject(newUser);

        return CreateResult.OPEN_EDITOR;
    }

    public void deleteObject(Map<String, Object> options)
    {
        addCommand(new CommandDropTable(), null);
    }

    private class CommandCreateTable extends DBOCommandImpl<OBJECT_TYPE> {
        protected CommandCreateTable()
        {
            super("Create table");
        }
        public IDatabasePersistAction[] getPersistActions(final OBJECT_TYPE object)
        {
            return new IDatabasePersistAction[] {
                new AbstractDatabasePersistAction("Create table", "CREATE TABLE " + object.getFullQualifiedName()) {
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

    private class CommandDropTable extends DBOCommandImpl<OBJECT_TYPE> {
        protected CommandDropTable()
        {
            super("Drop table");
        }

        public IDatabasePersistAction[] getPersistActions(final OBJECT_TYPE object)
        {
            return new IDatabasePersistAction[] {
                new AbstractDatabasePersistAction("Drop schema", "DROP TABLE " + object.getFullQualifiedName()) {
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

    protected abstract OBJECT_TYPE createNewTable(CONTAINER_TYPE parent, OBJECT_TYPE copyFrom);

}

