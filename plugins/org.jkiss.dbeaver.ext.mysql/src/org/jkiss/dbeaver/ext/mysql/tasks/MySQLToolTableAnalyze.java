package org.jkiss.dbeaver.ext.mysql.tasks;

import org.jkiss.dbeaver.ext.mysql.model.MySQLTableBase;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistAction;
import org.jkiss.dbeaver.model.sql.task.SQLToolExecuteHandler;

import java.util.List;

public class MySQLToolTableAnalyze extends SQLToolExecuteHandler<MySQLTableBase, MySQLToolTableAnalyzeSettings> {
    @Override
    public MySQLToolTableAnalyzeSettings createToolSettings() {
        return new MySQLToolTableAnalyzeSettings();
    }

    @Override
    public void generateObjectQueries(DBCSession session, MySQLToolTableAnalyzeSettings settings, List<DBEPersistAction> queries, MySQLTableBase object) throws DBCException {
        String sql = "ANALYZE TABLE " + object.getFullyQualifiedName(DBPEvaluationContext.DDL);
        queries.add(new SQLDatabasePersistAction(sql));
    }
}
