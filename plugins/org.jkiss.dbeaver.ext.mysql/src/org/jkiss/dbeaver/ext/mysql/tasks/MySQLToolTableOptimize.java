package org.jkiss.dbeaver.ext.mysql.tasks;

import org.jkiss.dbeaver.ext.mysql.model.MySQLTableBase;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistAction;
import org.jkiss.dbeaver.model.sql.task.SQLToolExecuteHandler;

import java.util.List;

public class MySQLToolTableOptimize extends SQLToolExecuteHandler<MySQLTableBase, MySQLToolTableOptimizeSettings> {
    @Override
    public MySQLToolTableOptimizeSettings createToolSettings() {
        return new MySQLToolTableOptimizeSettings();
    }

    @Override
    public void generateObjectQueries(DBCSession session, MySQLToolTableOptimizeSettings settings, List<DBEPersistAction> queries, MySQLTableBase object) throws DBCException {
        String sql = "OPTIMIZE TABLE " + object.getFullyQualifiedName(DBPEvaluationContext.DDL);
        queries.add(new SQLDatabasePersistAction(sql));
    }
}
