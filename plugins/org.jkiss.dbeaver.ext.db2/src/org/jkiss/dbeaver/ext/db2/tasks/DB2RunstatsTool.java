package org.jkiss.dbeaver.ext.db2.tasks;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.ext.db2.model.DB2TableBase;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistAction;

import java.util.List;

import static org.jkiss.utils.CommonUtils.getLineSeparator;

public class DB2RunstatsTool extends DB2ToolWithStatus<DB2TableBase, DB2RunstatsToolSettings>{

    @NotNull
    @Override
    public DB2RunstatsToolSettings createToolSettings() {
        return new DB2RunstatsToolSettings();
    }

    @Override
    public void generateObjectQueries(DBCSession session, DB2RunstatsToolSettings settings, List<DBEPersistAction> queries, DB2TableBase object) throws DBCException {
        String sql = "CALL SYSPROC.ADMIN_CMD('";
        sql += "RUNSTATS ON TABLE ";
        sql += object.getFullyQualifiedName(DBPEvaluationContext.DDL);
        sql += getLineSeparator();
        sql += settings.getColumnStat();
        sql += settings.getIndexStat();

        if (settings.isTableSampling()) {
            sql += String.format(" TABLESAMPLE SYSTEM(%d)", settings.getSamplePercent());
        }
        sql += "')";
        queries.add(new SQLDatabasePersistAction(sql));
    }
}
