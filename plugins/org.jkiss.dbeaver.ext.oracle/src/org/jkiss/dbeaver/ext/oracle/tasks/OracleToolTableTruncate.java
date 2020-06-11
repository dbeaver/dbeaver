package org.jkiss.dbeaver.ext.oracle.tasks;

import org.jkiss.dbeaver.ext.oracle.model.OracleTableBase;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistAction;
import org.jkiss.dbeaver.model.sql.task.SQLToolExecuteHandler;

import java.util.List;

public class OracleToolTableTruncate extends SQLToolExecuteHandler<OracleTableBase, OracleToolTableTruncateSettings> {
    @Override
    public OracleToolTableTruncateSettings createToolSettings() {
        return new OracleToolTableTruncateSettings();
    }

    @Override
    public void generateObjectQueries(DBCSession session, OracleToolTableTruncateSettings settings, List<DBEPersistAction> queries, OracleTableBase object) throws DBCException {
        String sql = "TRUNCATE TABLE " + object.getFullyQualifiedName(DBPEvaluationContext.DDL);
        if(settings.isReusable()) sql += " REUSE STORAGE";
        queries.add(new SQLDatabasePersistAction(sql));
    }
}
