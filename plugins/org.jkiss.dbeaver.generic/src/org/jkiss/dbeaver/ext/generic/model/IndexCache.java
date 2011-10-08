/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.generic.model;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCExecutionContext;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCConstants;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCCompositeCache;
import org.jkiss.dbeaver.model.struct.DBSIndexType;

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.List;

/**
 * Index cache implementation
 */
class IndexCache extends JDBCCompositeCache<GenericStructContainer, GenericTable, GenericIndex, GenericIndexColumn> {

    IndexCache(TableCache tableCache)
    {
        super(tableCache, GenericTable.class, JDBCConstants.TABLE_NAME, JDBCConstants.INDEX_NAME);
    }

    protected JDBCStatement prepareObjectsStatement(JDBCExecutionContext context, GenericStructContainer owner, GenericTable forParent)
        throws SQLException
    {
        try {
            return context.getMetaData().getIndexInfo(
                    owner.getCatalog() == null ? null : owner.getCatalog().getName(),
                    owner.getSchema() == null ? null : owner.getSchema().getName(),
                    // oracle fails if unquoted complex identifier specified
                    // but other DBs (and logically it's correct) do not want quote chars in this query
                    // so let's fix it in oracle plugin
                    forParent == null ? null : DBUtils.getQuotedIdentifier(owner.getDataSource(), forParent.getName()),
                    false,
                    true).getSource();
        } catch (SQLException e) {
            throw e;
        } catch (Exception e) {
            if (forParent == null) {
                throw new SQLException("Global indexes read not supported", e);
            } else {
                throw new SQLException(e);
            }
        }
    }

    protected GenericIndex fetchObject(JDBCExecutionContext context, GenericStructContainer owner, GenericTable parent, String indexName, ResultSet dbResult)
        throws SQLException, DBException
    {
        boolean isNonUnique = JDBCUtils.safeGetBoolean(dbResult, JDBCConstants.NON_UNIQUE);
        String indexQualifier = JDBCUtils.safeGetStringTrimmed(dbResult, JDBCConstants.INDEX_QUALIFIER);
        long cardinality = JDBCUtils.safeGetLong(dbResult, JDBCConstants.INDEX_CARDINALITY);
        int indexTypeNum = JDBCUtils.safeGetInt(dbResult, JDBCConstants.TYPE);

        DBSIndexType indexType;
        switch (indexTypeNum) {
            case DatabaseMetaData.tableIndexStatistic: indexType = DBSIndexType.STATISTIC; break;
            case DatabaseMetaData.tableIndexClustered: indexType = DBSIndexType.CLUSTERED; break;
            case DatabaseMetaData.tableIndexHashed: indexType = DBSIndexType.HASHED; break;
            case DatabaseMetaData.tableIndexOther: indexType = DBSIndexType.OTHER; break;
            default: indexType = DBSIndexType.UNKNOWN; break;
        }

        return new GenericIndex(
            parent,
            isNonUnique,
            indexQualifier,
            cardinality,
            indexName,
            indexType,
            true);
    }

    protected GenericIndexColumn fetchObjectRow(
        JDBCExecutionContext context,
        GenericTable parent, GenericIndex object, ResultSet dbResult)
        throws SQLException, DBException
    {
        int ordinalPosition = JDBCUtils.safeGetInt(dbResult, JDBCConstants.ORDINAL_POSITION);
        String columnName = JDBCUtils.safeGetStringTrimmed(dbResult, JDBCConstants.COLUMN_NAME);
        String ascOrDesc = JDBCUtils.safeGetStringTrimmed(dbResult, JDBCConstants.ASC_OR_DESC);

        GenericTableColumn tableColumn = parent.getColumn(context.getProgressMonitor(), columnName);
        if (tableColumn == null) {
            log.debug("Column '" + columnName + "' not found in table '" + parent.getName() + "' for index '" + object.getName() + "'");
            return null;
        }

        return new GenericIndexColumn(
            object,
            tableColumn,
            ordinalPosition,
            !"D".equalsIgnoreCase(ascOrDesc));
    }

    protected Collection<GenericIndex> getObjectsCache(GenericTable parent)
    {
        return parent.getIndexesCache();
    }

    protected void cacheObjects(GenericTable parent, List<GenericIndex> indexes)
    {
        parent.setIndexes(indexes);
    }

    protected void cacheChildren(GenericIndex index, List<GenericIndexColumn> rows)
    {
        index.setColumns(rows);
    }
}
