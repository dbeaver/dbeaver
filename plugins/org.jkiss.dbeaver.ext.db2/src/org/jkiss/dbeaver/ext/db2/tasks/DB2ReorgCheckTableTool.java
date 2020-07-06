package org.jkiss.dbeaver.ext.db2.tasks;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.db2.model.DB2TableBase;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistAction;

import java.util.List;

public class DB2ReorgCheckTableTool extends DB2ToolWithStatus<DB2TableBase, DB2ReorgCheckTableToolSettings>{

    @NotNull
    @Override
    public DB2ReorgCheckTableToolSettings createToolSettings() {
        return new DB2ReorgCheckTableToolSettings();
    }

    @Override
    public void generateObjectQueries(DBCSession session, DB2ReorgCheckTableToolSettings settings, List<DBEPersistAction> queries, DB2TableBase object) throws DBCException {
        String sql = "CALL SYSPROC.REORGCHK_TB_STATS('T','";
        sql += object.getFullyQualifiedName(DBPEvaluationContext.DDL);
        sql += "')";
        queries.add(new SQLDatabasePersistAction(sql));
    }
}
