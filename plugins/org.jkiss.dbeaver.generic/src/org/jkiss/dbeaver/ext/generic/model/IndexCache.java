/*
 * Copyright (C) 2010-2012 Serge Rieder
 * serge@jkiss.org
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package org.jkiss.dbeaver.ext.generic.model;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCExecutionContext;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCConstants;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCCompositeCache;
import org.jkiss.dbeaver.model.struct.rdb.DBSIndexType;

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

/**
 * Index cache implementation
 */
class IndexCache extends JDBCCompositeCache<GenericStructContainer, GenericTable, GenericTableIndex, GenericTableIndexColumn> {

    IndexCache(TableCache tableCache)
    {
        super(tableCache, GenericTable.class, JDBCConstants.TABLE_NAME, JDBCConstants.INDEX_NAME);
    }

    @Override
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
                    forParent == null ? null : DBUtils.getQuotedIdentifier(forParent),
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

    @Override
    protected GenericTableIndex fetchObject(JDBCExecutionContext context, GenericStructContainer owner, GenericTable parent, String indexName, ResultSet dbResult)
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

        return new GenericTableIndex(
            parent,
            isNonUnique,
            indexQualifier,
            cardinality,
            indexName,
            indexType,
            true);
    }

    @Override
    protected GenericTableIndexColumn fetchObjectRow(
        JDBCExecutionContext context,
        GenericTable parent, GenericTableIndex object, ResultSet dbResult)
        throws SQLException, DBException
    {
        int ordinalPosition = JDBCUtils.safeGetInt(dbResult, JDBCConstants.ORDINAL_POSITION);
        String columnName = JDBCUtils.safeGetStringTrimmed(dbResult, JDBCConstants.COLUMN_NAME);
        String ascOrDesc = JDBCUtils.safeGetStringTrimmed(dbResult, JDBCConstants.ASC_OR_DESC);

        GenericTableColumn tableColumn = parent.getAttribute(context.getProgressMonitor(), columnName);
        if (tableColumn == null) {
            log.debug("Column '" + columnName + "' not found in table '" + parent.getName() + "' for index '" + object.getName() + "'");
            return null;
        }

        return new GenericTableIndexColumn(
            object,
            tableColumn,
            ordinalPosition,
            !"D".equalsIgnoreCase(ascOrDesc));
    }

    @Override
    protected void cacheChildren(GenericTableIndex index, List<GenericTableIndexColumn> rows)
    {
        index.setColumns(rows);
    }
}
