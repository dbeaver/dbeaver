package org.jkiss.dbeaver.ext.postgresql.tasks;

import org.jkiss.dbeaver.ext.postgresql.model.PostgreDataSource;
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
        boolean isFull = settings.isFull();
        boolean isFreeze = settings.isFreeze();
        if(isFull) sql += "FULL, ";
        if(isFreeze) sql += "FREEZE, ";
        sql += "VERBOSE";
        boolean isAnalyzed = settings.isAnalyzed();
        boolean isDisabled = settings.isDisableSkipping();
        if(isAnalyzed) sql += ", ANALYZE";
        PostgreDataSource postgreDataSource = (PostgreDataSource) session.getExecutionContext().getDataSource();
        if(isDisabled && (postgreDataSource.isServerVersionAtLeast(9, 6))){
            sql += ", DISABLE_PAGE_SKIPPING";
        }
        if(postgreDataSource.isServerVersionAtLeast(12, 0)){
            boolean isSkipLocked = settings.isSkipLocked();
            boolean isIndexCleaning = settings.isIndexCleaning();
            boolean isTruncated = settings.isTruncated();
            if(isSkipLocked) sql += ", SKIP_LOCKED";
            if(isIndexCleaning) sql += ", INDEX_CLEANUP";
            if(isTruncated) sql += ", TRUNCATE";
        }
        sql += ")";
        if(object instanceof PostgreTableBase){
            PostgreTableBase postObject = (PostgreTableBase) object;
            sql += " " + postObject.getFullyQualifiedName(DBPEvaluationContext.DDL);
        }
        queries.add(new SQLDatabasePersistAction(sql));
    }
}
