package org.jkiss.dbeaver.ext.mssql.edit;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.mssql.model.SQLServerDataSource;
import org.jkiss.dbeaver.ext.mssql.model.SQLServerDatabase;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.DBEObjectRenamer;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistAction;
import org.jkiss.dbeaver.model.impl.sql.edit.SQLObjectEditor;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.cache.DBSObjectCache;

import java.util.List;
import java.util.Map;

public class SQLServerDatabaseManager extends SQLObjectEditor<SQLServerDatabase, SQLServerDataSource> implements DBEObjectRenamer<SQLServerDatabase> {

    @Override
    public boolean canCreateObject(Object container) {
        return false;
    }

    @Override
    public void renameObject(DBECommandContext commandContext, SQLServerDatabase object, String newName) throws DBException {
        processObjectRename(commandContext, object, newName);
    }

    @Override
    protected SQLServerDatabase createDatabaseObject(DBRProgressMonitor monitor, DBECommandContext context, Object container, Object copyFrom, Map<String, Object> options) throws DBException {
        return null;
    }

    @Override
    protected void addObjectCreateActions(DBRProgressMonitor monitor, DBCExecutionContext executionContext, List<DBEPersistAction> actions, ObjectCreateCommand command, Map<String, Object> options) throws DBException {
        actions.add(new SQLDatabasePersistAction(
                "Create database",
                "CREATE DATABASE " + DBUtils.getQuotedIdentifier(command.getObject()) + ";"
        ));
    }

    @Override
    protected void addObjectDeleteActions(DBRProgressMonitor monitor, DBCExecutionContext executionContext, List<DBEPersistAction> actions, ObjectDeleteCommand command, Map<String, Object> options) {
        actions.add(new SQLDatabasePersistAction(
                "Drop database",
                "DROP DATABASE " + DBUtils.getQuotedIdentifier(command.getObject()) + ";"
        ));
    }

    @Override
    protected void addObjectRenameActions(DBRProgressMonitor monitor, DBCExecutionContext executionContext, List<DBEPersistAction> actions, ObjectRenameCommand command, Map<String, Object> options) {
        final SQLServerDataSource source = command.getObject().getDataSource();
        final String oldName = DBUtils.getQuotedIdentifier(source, command.getOldName());
        final String newName = DBUtils.getQuotedIdentifier(source, command.getNewName());

        actions.add(new SQLDatabasePersistAction(
                "Rename database",
                "ALTER DATABASE " + oldName + " MODIFY NAME = " + newName + ";"
        ));
    }

    @Override
    public long getMakerOptions(DBPDataSource dataSource) {
        return 0;
    }

    @Override
    public DBSObjectCache<? extends DBSObject, SQLServerDatabase> getObjectsCache(SQLServerDatabase object) {
        return null;
    }
}
