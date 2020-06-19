package org.jkiss.dbeaver.ext.postgresql.tasks;

import org.jkiss.dbeaver.ext.postgresql.model.PostgreTableBase;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistAction;
import org.jkiss.dbeaver.model.struct.DBSObject;

import java.util.List;

public class PostgreToolBaseVacuum extends PostgreToolWithStatus<DBSObject, PostgreToolBaseVacuumSettings> {
    @Override
    public PostgreToolBaseVacuumSettings createToolSettings() {
        return new PostgreToolBaseVacuumSettings();
    }

    @Override
    public void generateObjectQueries(DBCSession session, PostgreToolBaseVacuumSettings settings, List<DBEPersistAction> queries, DBSObject object) throws DBCException {
        String sql = "VACUUM (";
        if(settings.isFull()) sql += "FULL, ";
        if(settings.isFreeze()) sql += "FREEZE, ";
        sql += "VERBOSE";
        if(settings.isAnalyzed()) sql += ", ANALYZE";
        if(settings.isDisableSkipping()) sql += ", DISABLE_PAGE_SKIPPING";
        if(settings.isSkipLocked()) sql += ", SKIP_LOCKED";
        if(settings.isIndexCleaning()) sql += ", INDEX_CLEANUP";
        if(settings.isTruncated()) sql += ", TRUNCATE";
        sql += ")";
        if(object instanceof PostgreTableBase){
            PostgreTableBase postObject = (PostgreTableBase) object;
            sql += " " + postObject.getFullyQualifiedName(DBPEvaluationContext.DDL);
        }
        queries.add(new SQLDatabasePersistAction(sql));
    }

    @Override
    public boolean isRunInAutoCommit() {
        return true;
    }
}
