package org.jkiss.dbeaver.ext.yashandb.tasks;

import org.jkiss.dbeaver.ext.yashandb.model.YashanDBTableBase;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistAction;
import org.jkiss.dbeaver.model.sql.task.SQLToolExecuteHandler;

import java.util.List;

/**
 * @Author: donghy
 * @Date: 2022/09
 * @Description:
 */
public class YashanDBToolTableTruncate extends SQLToolExecuteHandler<YashanDBTableBase, YashanDBToolTableTruncateSettings> {
    @Override
    public YashanDBToolTableTruncateSettings createToolSettings() {
        return new YashanDBToolTableTruncateSettings();
    }

    @Override
    public void generateObjectQueries(DBCSession session, YashanDBToolTableTruncateSettings settings, List<DBEPersistAction> queries, YashanDBTableBase object) throws DBCException {
        String sql = "TRUNCATE TABLE " + object.getFullyQualifiedName(DBPEvaluationContext.DDL);
        if (settings.isReusable()) sql += " REUSE STORAGE";
        queries.add(new SQLDatabasePersistAction(sql));
    }

    public boolean needsRefreshOnFinish() {
        return true;
    }
}
