package org.jkiss.dbeaver.ext.postgresql.model.impls.greenplum;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreDatabase;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreSchema;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreTableBase;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;
import org.jkiss.dbeaver.model.impl.jdbc.struct.JDBCTable;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.stream.Collectors;

public class GreenplumSchema extends PostgreSchema {
    private static final Log log = Log.getLog(GreenplumSchema.class);
    private GreenplumTableCache greenplumTableCache = new GreenplumTableCache();


    public GreenplumSchema(PostgreDatabase owner, String name, JDBCResultSet resultSet) throws SQLException {
        super(owner, name, resultSet);
    }

    @Override
    public Collection<? extends JDBCTable> getTables(DBRProgressMonitor monitor) throws DBException {
        return greenplumTableCache.getTypedObjects(monitor, this, GreenplumTable.class)
                .stream()
                .filter(table -> !table.isPartition())
                .collect(Collectors.toCollection(ArrayList::new));
    }

    public class GreenplumTableCache extends TableCache {
        protected GreenplumTableCache() {
            super();
        }

        @NotNull
        @Override
        public JDBCStatement prepareLookupStatement(@NotNull JDBCSession session, @NotNull PostgreSchema postgreSchema, @Nullable PostgreTableBase object, @Nullable String objectName) throws SQLException {
            String sqlQuery = "SELECT c.oid,d.description, c.*\n" +
                    "FROM pg_catalog.pg_class c\n" +
                    "inner join pg_catalog.pg_namespace ns\n" +
                    "\ton ns.oid = c.relnamespace\n" +
                    "LEFT OUTER JOIN pg_catalog.pg_description d\n" +
                    "\tON d.objoid=c.oid AND d.objsubid=0\n" +
                    "left outer join pg_catalog.pg_partitions p\n" +
                    "\ton c.relname = p.partitiontablename and ns.nspname = p.schemaname\n" +
                    "WHERE c.relnamespace= ? AND c.relkind not in ('i','c') AND p.partitiontablename is null "
                    + (object == null && objectName == null ? "" : " AND relname=?");
            final JDBCPreparedStatement dbStat = session.prepareStatement(sqlQuery);
            dbStat.setLong(1, getObjectId());
            if (object != null || objectName != null)
                dbStat.setString(2, object != null ? object.getName() : objectName);
            return dbStat;
        }
    }
}
