package org.jkiss.dbeaver.ext.hive.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.generic.model.GenericStructContainer;
import org.jkiss.dbeaver.ext.generic.model.GenericTable;
import org.jkiss.dbeaver.ext.generic.model.GenericTableColumn;
import org.jkiss.dbeaver.ext.generic.model.GenericTableIndexColumn;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCCompositeCache;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.utils.ArrayUtils;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class HiveTable extends GenericTable {
    final public IndexCache indexCache = new IndexCache();

    public HiveTable(GenericStructContainer container, @Nullable String tableName, @Nullable String tableType, @Nullable JDBCResultSet dbResult) {
        super(container, tableName, tableType, dbResult);
    }

    /**
     * Index cache implementation
     */
    class IndexCache extends JDBCCompositeCache<GenericStructContainer, HiveTable, HiveIndex, GenericTableIndexColumn> {
        IndexCache()
        {
            super(getCache(), HiveTable.class, "tab_name", "idx_name");
        }

        @NotNull
        @Override
        protected JDBCStatement prepareObjectsStatement(JDBCSession session, GenericStructContainer owner, HiveTable forParent)
                throws SQLException
        {
            JDBCPreparedStatement dbStat;
            dbStat = session.prepareStatement("SHOW INDEX ON ?");
            if (forParent != null) {
                dbStat.setString(1, forParent.getName());
            }
            return dbStat;
        }

        @Nullable
        @Override
        protected HiveIndex fetchObject(JDBCSession session, GenericStructContainer owner, HiveTable parent, String indexName, JDBCResultSet dbResult)
        {
            String hiveIndexName = JDBCUtils.safeGetString(dbResult, "idx_name");
            String comment = JDBCUtils.safeGetString(dbResult, "comment");
            String indexType = JDBCUtils.safeGetString(dbResult, "idx_type");
            String indexTableName = JDBCUtils.safeGetString(dbResult, "idx_tab_name");
            try {
                HiveTable table = (HiveTable) owner.getTable(dbResult.getSession().getProgressMonitor(), indexTableName.trim());
                return new HiveIndex(parent, hiveIndexName, comment, indexType, table);
            } catch (DBException e) {
                log.debug("Can't read table from index" + indexName, e);
            }
            return new HiveIndex(parent, hiveIndexName, comment, indexType, null);
        }

        @Nullable
        @Override
        protected GenericTableIndexColumn[] fetchObjectRow(
                JDBCSession session,
                HiveTable parent, HiveIndex index, JDBCResultSet dbResult)
                throws DBException
        {
            String columnNames = JDBCUtils.safeGetString(dbResult, "col_names");
            ArrayList<GenericTableIndexColumn> indexColumns = new ArrayList<>();
            if (columnNames != null) {
                if (columnNames.contains(",")) {
                    String[] indexColumnNames = columnNames.split(",");
                    for (String column : indexColumnNames) {
                        GenericTableColumn attribute = parent.getAttribute(session.getProgressMonitor(), column.trim());
                        if (attribute != null) {
                            indexColumns.add(new GenericTableIndexColumn(index, attribute, attribute.getOrdinalPosition(), false));
                        }
                    }
                } else {
                    GenericTableColumn attribute = parent.getAttribute(session.getProgressMonitor(), columnNames.trim());
                    if (attribute != null) {
                        indexColumns.add(new GenericTableIndexColumn(index, attribute, attribute.getOrdinalPosition(), false));
                    }
                }
            }
            return ArrayUtils.toArray(GenericTableIndexColumn.class, indexColumns);
        }

        @Override
        protected void cacheChildren(DBRProgressMonitor monitor, HiveIndex index, List<GenericTableIndexColumn> rows)
        {
            index.setColumns(rows);
        }
    }
}
