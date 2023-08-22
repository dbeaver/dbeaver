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
 * @Description:
 * @Author dengqh
 * @Date 2023/6/30 17:57
 */
public class YashanDBToolViewUpdate extends SQLToolExecuteHandler<YashanDBTableBase, YashanDBToolViewUpdateSettings> {
    @Override
    public YashanDBToolViewUpdateSettings createToolSettings() {
        return new YashanDBToolViewUpdateSettings();
    }

    @Override
    public void generateObjectQueries(DBCSession session, YashanDBToolViewUpdateSettings settings, List<DBEPersistAction> queries, YashanDBTableBase object) throws DBCException {
        String sql = "UPDATE VIEW " + object.getFullyQualifiedName(DBPEvaluationContext.DDL);
//        if (settings.isReusable()) sql += " REUSE STORAGE";
        queries.add(new SQLDatabasePersistAction(sql));
    }

    public boolean needsRefreshOnFinish() {
        return true;
    }
}
