package org.jkiss.dbeaver.ext.mssql.tasks;

import org.jkiss.dbeaver.ext.mssql.model.SQLServerTableBase;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistAction;

import java.util.List;

public class SQLServerToolTableRebuild extends SQLServerToolWithStatus<SQLServerTableBase, SQLServerToolTableRebuildSettings> {
    @Override
    public SQLServerToolTableRebuildSettings createToolSettings() {
        return new SQLServerToolTableRebuildSettings();
    }

    @Override
    public void generateObjectQueries(DBCSession session, SQLServerToolTableRebuildSettings settings, List<DBEPersistAction> queries, SQLServerTableBase object) throws DBCException {
        queries.add(new SQLDatabasePersistAction("ALTER INDEX ALL ON " + object.getFullyQualifiedName(DBPEvaluationContext.DDL) + " REBUILD "));
    }
}
