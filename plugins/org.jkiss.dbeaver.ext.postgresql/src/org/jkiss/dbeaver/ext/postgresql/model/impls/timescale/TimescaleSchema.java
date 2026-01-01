package org.jkiss.dbeaver.ext.postgresql.model.impls.timescale;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreDatabase;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreSchema;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreTableBase;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreTableReal;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

import java.sql.ResultSet;
import java.sql.SQLException;

public class TimescaleSchema extends PostgreSchema {

    private static final Log log = Log.getLog(TimescaleSchema.class);

    public TimescaleSchema(PostgreDatabase owner, String name, ResultSet dbResult) throws SQLException {
        super(owner, name, dbResult);
    }

    @Override
    public void collectObjectStatistics(
        @NotNull DBRProgressMonitor monitor,
        boolean totalSizeOnly,
        boolean forceRefresh
    ) throws DBException {
        if (!getDataSource().getServerType().supportsTableStatistics() || hasStatistics && !forceRefresh) {
            return;
        }

        for (PostgreTableBase table : getTables(monitor)) {
            if (table instanceof PostgreTableReal && table.isPersisted()) {
                ((PostgreTableReal) table).getDiskSpace(monitor);
            }
        }
        hasStatistics = true;
    }
}
