package org.jkiss.dbeaver.ext.mssql.edit;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.mssql.model.*;
import org.jkiss.dbeaver.model.DBPScriptObject;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

import java.util.Map;

public class SQLServerTableTypeManager extends SQLServerBaseTableManager<SQLServerTableType> {

    private static final Class<?>[] CHILD_TYPES = {
            SQLServerTableColumn.class,
            SQLServerTableUniqueKey.class,
            SQLServerTableForeignKey.class,
            SQLServerTableIndex.class,
            SQLServerTableCheckConstraint.class,
    };

    @Override
    public void renameObject(@NotNull DBECommandContext commandContext, @NotNull SQLServerTableType object, @NotNull Map<String, Object> options, @NotNull String newName) throws DBException {
        throw new DBException("SQL Server data table types rename not supported");
    }

    @NotNull
    @Override
    public Class<?>[] getChildTypes() {
        return CHILD_TYPES;
    }

    @Override
    protected SQLServerTableType createDatabaseObject(DBRProgressMonitor monitor, DBECommandContext context, Object container, Object copyFrom, Map<String, Object> options) throws DBException {
        throw new IllegalStateException("Not implemented");
    }

    @Override
    protected String beginCreateTableStatement(DBRProgressMonitor monitor, SQLServerTableType table, String tableName, Map<String, Object> options) throws DBException {
        if (!options.isEmpty() && options.containsKey(DBPScriptObject.OPTION_USE_SPECIAL_NAME)) {
            return "CREATE TYPE " + options.get(DBPScriptObject.OPTION_USE_SPECIAL_NAME) + " AS TABLE (\n";
        }
        return "CREATE TYPE " + tableName + " AS TABLE\n (";
    }

    @Override
    protected boolean isIncludeDropInDDL() {
        return false;
    }
}
