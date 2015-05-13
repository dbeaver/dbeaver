/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2015 Serge Rieder (serge@jkiss.org)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (version 2)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package org.jkiss.dbeaver.ext.generic.model;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.generic.GenericConstants;
import org.jkiss.dbeaver.ext.generic.model.meta.GenericMetaObject;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCConstants;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCCompositeCache;
import org.jkiss.dbeaver.model.struct.rdb.DBSIndexType;
import org.jkiss.utils.CommonUtils;

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

/**
 * Index cache implementation
 */
class IndexCache extends JDBCCompositeCache<GenericStructContainer, GenericTable, GenericTableIndex, GenericTableIndexColumn> {

    private final GenericMetaObject indexObject;

    IndexCache(TableCache tableCache)
    {
        super(
            tableCache,
            GenericTable.class,
            GenericUtils.getColumn(tableCache.getDataSource(), GenericConstants.OBJECT_INDEX, JDBCConstants.TABLE_NAME),
            GenericUtils.getColumn(tableCache.getDataSource(), GenericConstants.OBJECT_INDEX, JDBCConstants.INDEX_NAME));
        indexObject = tableCache.getDataSource().getMetaObject(GenericConstants.OBJECT_INDEX);
    }

    @Override
    protected JDBCStatement prepareObjectsStatement(JDBCSession session, GenericStructContainer owner, GenericTable forParent)
        throws SQLException
    {
        try {
            return session.getMetaData().getIndexInfo(
                    owner.getCatalog() == null ? null : owner.getCatalog().getName(),
                    owner.getSchema() == null ? null : owner.getSchema().getName(),
                    // oracle fails if unquoted complex identifier specified
                    // but other DBs (and logically it's correct) do not want quote chars in this query
                    // so let's fix it in oracle plugin
                    forParent == null ? owner.getDataSource().getAllObjectsPattern() : DBUtils.getQuotedIdentifier(forParent),
                    false,
                    true).getSourceStatement();
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
    protected GenericTableIndex fetchObject(JDBCSession session, GenericStructContainer owner, GenericTable parent, String indexName, ResultSet dbResult)
        throws SQLException, DBException
    {
        boolean isNonUnique = GenericUtils.safeGetBoolean(indexObject, dbResult, JDBCConstants.NON_UNIQUE);
        String indexQualifier = GenericUtils.safeGetStringTrimmed(indexObject, dbResult, JDBCConstants.INDEX_QUALIFIER);
        long cardinality = GenericUtils.safeGetLong(indexObject, dbResult, JDBCConstants.INDEX_CARDINALITY);
        int indexTypeNum = GenericUtils.safeGetInt(indexObject, dbResult, JDBCConstants.TYPE);

        DBSIndexType indexType;
        switch (indexTypeNum) {
            case DatabaseMetaData.tableIndexStatistic:
                // Table index statistic. Not a real index.
                log.debug("Skip statistics index '" + indexName + "' in '" + DBUtils.getObjectFullName(parent) + "'");
                return null;
            // indexType = DBSIndexType.STATISTIC; break;
            case DatabaseMetaData.tableIndexClustered:
                indexType = DBSIndexType.CLUSTERED;
                break;
            case DatabaseMetaData.tableIndexHashed:
                indexType = DBSIndexType.HASHED;
                break;
            case DatabaseMetaData.tableIndexOther:
                indexType = DBSIndexType.OTHER;
                break;
            default:
                indexType = DBSIndexType.UNKNOWN;
                break;
        }
        if (CommonUtils.isEmpty(indexName)) {
            // [JDBC] Some drivers return empty index names
            indexName = parent.getName().toUpperCase() + "_INDEX";
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
        JDBCSession session,
        GenericTable parent, GenericTableIndex object, ResultSet dbResult)
        throws SQLException, DBException
    {
        int ordinalPosition = GenericUtils.safeGetInt(indexObject, dbResult, JDBCConstants.ORDINAL_POSITION);
        String columnName = GenericUtils.safeGetStringTrimmed(indexObject, dbResult, JDBCConstants.COLUMN_NAME);
        String ascOrDesc = GenericUtils.safeGetStringTrimmed(indexObject, dbResult, JDBCConstants.ASC_OR_DESC);

        if (ordinalPosition == 0 || CommonUtils.isEmpty(columnName)) {
            // Maybe a statistics index without column
            return null;
        }
        GenericTableColumn tableColumn = parent.getAttribute(session.getProgressMonitor(), columnName);
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
