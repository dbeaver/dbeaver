/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2023 DBeaver Corp and others
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jkiss.dbeaver.ext.cubrid.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.cubrid.CubridConstants;
import org.jkiss.dbeaver.ext.cubrid.model.meta.CubridMetaObject;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCConstants;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCCompositeCache;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.rdb.DBSIndexType;
import org.jkiss.utils.CommonUtils;

import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.List;
import java.util.Locale;

/**
 * Index cache implementation
 */
class IndexCache extends JDBCCompositeCache<CubridStructContainer, CubridTableBase, CubridTableIndex, CubridTableIndexColumn> {

    private final CubridMetaObject indexObject;

    IndexCache(TableCache tableCache)
    {
        super(
            tableCache,
            CubridTableBase.class,
            CubridUtils.getColumn(tableCache.getDataSource(), CubridConstants.OBJECT_INDEX, JDBCConstants.TABLE_NAME),
            CubridUtils.getColumn(tableCache.getDataSource(), CubridConstants.OBJECT_INDEX, JDBCConstants.INDEX_NAME));
        indexObject = tableCache.getDataSource().getMetaObject(CubridConstants.OBJECT_INDEX);
    }

    @NotNull
    @Override
    protected JDBCStatement prepareObjectsStatement(JDBCSession session, CubridStructContainer owner, CubridTableBase forParent)
        throws SQLException
    {
        try {
            return session.getMetaData().getIndexInfo(
                    owner.getCatalog() == null ? null : owner.getCatalog().getName(),
                    owner.getSchema() == null || DBUtils.isVirtualObject(owner.getSchema()) ? null : owner.getSchema().getName(),
                    forParent == null ? owner.getDataSource().getAllObjectsPattern() : forParent.getName(),
                    false,
                    true).getSourceStatement();
        } catch (Exception e) {
            if (forParent == null) {
                throw new SQLException("Global indexes read not supported", e);
            } else {
                if (e instanceof SQLException) {
                    throw (SQLException)e;
                }
                throw new SQLException(e);
            }
        }
    }

    @Nullable
    @Override
    protected CubridTableIndex fetchObject(JDBCSession session, CubridStructContainer owner, CubridTableBase parent, String indexName, JDBCResultSet dbResult)
        throws SQLException, DBException
    {
        boolean isNonUnique = CubridUtils.safeGetBoolean(indexObject, dbResult, JDBCConstants.NON_UNIQUE);
        String indexQualifier = CubridUtils.safeGetStringTrimmed(indexObject, dbResult, JDBCConstants.INDEX_QUALIFIER);
        long cardinality = CubridUtils.safeGetLong(indexObject, dbResult, JDBCConstants.INDEX_CARDINALITY);
        int indexTypeNum = CubridUtils.safeGetInt(indexObject, dbResult, JDBCConstants.TYPE);

        DBSIndexType indexType;
        switch (indexTypeNum) {
            case DatabaseMetaData.tableIndexStatistic:
                // Table index statistic. Not a real index.
                log.debug("Skip statistics index '" + indexName + "' in '" + DBUtils.getObjectFullName(parent, DBPEvaluationContext.DDL) + "'");
                return null;
//                indexType = DBSIndexType.STATISTIC;
//                break;
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
            indexName = parent.getName().toUpperCase(Locale.ENGLISH) + "_INDEX";
        }

        return owner.getDataSource().getMetaModel().createIndexImpl(
            parent,
            isNonUnique,
            indexQualifier,
            cardinality,
            indexName,
            indexType,
            true);
    }

    @Nullable
    @Override
    protected CubridTableIndexColumn[] fetchObjectRow(
        JDBCSession session,
        CubridTableBase parent, CubridTableIndex object, JDBCResultSet dbResult)
        throws SQLException, DBException
    {
        int ordinalPosition = CubridUtils.safeGetInt(indexObject, dbResult, JDBCConstants.ORDINAL_POSITION);
        boolean trimName = parent.getDataSource().getMetaModel().isTrimObjectNames();
        String columnName = trimName ?
            CubridUtils.safeGetStringTrimmed(indexObject, dbResult, JDBCConstants.COLUMN_NAME)
            : CubridUtils.safeGetString(indexObject, dbResult, JDBCConstants.COLUMN_NAME);
        String ascOrDesc = CubridUtils.safeGetStringTrimmed(indexObject, dbResult, JDBCConstants.ASC_OR_DESC);

        if (CommonUtils.isEmpty(columnName)) {
            // Maybe a statistics index without column
            return null;
        }
        CubridTableColumn tableColumn = parent.getAttribute(session.getProgressMonitor(), columnName);
        if (tableColumn == null) {
            log.debug("Column '" + columnName + "' not found in table '" + parent.getName() + "' for index '" + object.getName() + "'");
            return null;
        }

        return new CubridTableIndexColumn[] { new CubridTableIndexColumn(
            object,
            tableColumn,
            ordinalPosition,
            !"D".equalsIgnoreCase(ascOrDesc)) };
    }

    @Override
    protected void cacheChildren(DBRProgressMonitor monitor, CubridTableIndex index, List<CubridTableIndexColumn> rows)
    {
        index.setColumns(rows);
    }
}
