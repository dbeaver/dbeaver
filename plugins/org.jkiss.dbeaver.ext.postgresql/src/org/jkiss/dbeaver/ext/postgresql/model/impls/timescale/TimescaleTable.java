package org.jkiss.dbeaver.ext.postgresql.model.impls.timescale;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreSchema;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreTableRegular;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;

import java.sql.ResultSet;
import java.sql.SQLException;

public class TimescaleTable extends PostgreTableRegular {

    public TimescaleTable(PostgreSchema schema, ResultSet dbResult) {
        super(schema, dbResult);
    }

    @Override
    protected void readTableStatistics(JDBCSession session) throws DBException, SQLException {
        if (!getDataSource().getServerType().supportsTableStatistics()) {
            return;
        }

        String sql;
        if (isHypertable(session)) {
            sql = "SELECT hypertable_size(?) as total_rel_size," +
                  "hypertable_size(?) as rel_size";
        } else {
            sql = "SELECT pg_catalog.pg_total_relation_size(?) as total_rel_size," +
                  "pg_catalog.pg_relation_size(?) as rel_size";
        }

        try (JDBCPreparedStatement dbStat = session.prepareStatement(sql)) {
            dbStat.setLong(1, getObjectId());
            dbStat.setLong(2, getObjectId());
            try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                if (dbResult.next()) {
                    fetchStatistics(dbResult);
                }
            }
        }
    }
}
