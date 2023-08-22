package org.jkiss.dbeaver.ext.yashandb.tasks;

import org.jkiss.dbeaver.ext.yashandb.model.YashanDBTableBase;
import org.jkiss.dbeaver.ext.yashandb.model.YashanDBTableIndex;
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
 * @Date 2023/6/30 19:16
 */
public class YashanDBToolIndexRebuild extends SQLToolExecuteHandler<YashanDBTableIndex, YashanDBToolIndexRebuildSettings> {
    @Override
    public YashanDBToolIndexRebuildSettings createToolSettings() {
        return new YashanDBToolIndexRebuildSettings();
    }

    @Override
    public void generateObjectQueries(DBCSession session, YashanDBToolIndexRebuildSettings settings, List<DBEPersistAction> queries, YashanDBTableIndex object) throws DBCException {
        String sql = "alter index " + object.getFullyQualifiedName(DBPEvaluationContext.DDL)+" rebuild";
        if (settings.getPartition()!=null) sql += " PARTITION "+settings.getPartition();
        if(settings.getTablespace()!=null) sql+=" TABLESPACE "+settings.getTablespace();
        if(settings.getInitrans()!=null&&settings.getInitrans()!=0) sql+=" INITRANS "+settings.getInitrans();
        if(settings.getPctfree()!=null) sql+=" PCTFREE "+settings.getPctfree();
        if(settings.isOnline()) sql+=" ONLINE";
        queries.add(new SQLDatabasePersistAction(sql));
    }

    public boolean needsRefreshOnFinish() {
        return true;
    }

}
