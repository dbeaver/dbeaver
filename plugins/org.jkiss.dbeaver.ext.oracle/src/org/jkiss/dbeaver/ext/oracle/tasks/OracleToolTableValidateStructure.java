package org.jkiss.dbeaver.ext.oracle.tasks;

import org.jkiss.dbeaver.ext.oracle.model.OracleTableBase;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistAction;
import org.jkiss.dbeaver.model.sql.task.SQLToolExecuteHandler;
import org.jkiss.utils.CommonUtils;

import java.util.List;

public class OracleToolTableValidateStructure extends SQLToolExecuteHandler<OracleTableBase, OracleToolTableValidateStructureSettings> {
    @Override
    public OracleToolTableValidateStructureSettings createToolSettings() {
        return new OracleToolTableValidateStructureSettings();
    }

    @Override
    public void generateObjectQueries(DBCSession session, OracleToolTableValidateStructureSettings settings, List<DBEPersistAction> queries, OracleTableBase object) throws DBCException {
        String sql = "ANALYZE TABLE " + object.getFullyQualifiedName(DBPEvaluationContext.DDL) + " VALIDATE STRUCTURE";
        String option = settings.getOption();
        if (!CommonUtils.isEmpty(option)) sql += " " + option;
        queries.add(new SQLDatabasePersistAction(sql));
    }
}
