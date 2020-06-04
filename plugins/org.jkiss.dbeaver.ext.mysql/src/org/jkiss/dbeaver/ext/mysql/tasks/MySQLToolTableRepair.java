package org.jkiss.dbeaver.ext.mysql.tasks;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.ext.mysql.model.MySQLTableBase;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistAction;

import java.util.List;

public class MySQLToolTableRepair extends MySQLToolWithStatus<MySQLTableBase, MySQLToolTableRepairSettings> {
    @NotNull
    @Override
    public MySQLToolTableRepairSettings createToolSettings() {
        return new MySQLToolTableRepairSettings();
    }

    @Override
    public void generateObjectQueries(DBCSession session, MySQLToolTableRepairSettings settings, List<DBEPersistAction> queries, MySQLTableBase object) throws DBCException {
        String sql = "REPAIR TABLE " + object.getFullyQualifiedName(DBPEvaluationContext.DDL);
        boolean isQuick = settings.isQuick();
        boolean isExtended = settings.isExtended();
        boolean useFRM = settings.isUseFRM();
        if (isQuick) sql += " QUICK";
        if (isExtended) sql += " EXTENDED";
        if (useFRM) sql += " USE_FRM";
        queries.add(new SQLDatabasePersistAction(sql));
    }
}
