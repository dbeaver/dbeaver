/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
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
package org.jkiss.dbeaver.ext.generic.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.generic.GenericConstants;
import org.jkiss.dbeaver.ext.generic.model.meta.GenericMetaObject;
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

    @NotNull
    @Override
    protected JDBCStatement prepareObjectsStatement(JDBCSession session, GenericStructContainer owner, GenericTable forParent)
        throws SQLException
    {
        try {
            return session.getMetaData().getIndexInfo(
                    owner.getCatalog() == null ? null : owner.getCatalog().getName(),
                    owner.getSchema() == null ? null : owner.getSchema().getName(),
                    forParent == null ? owner.getDataSource().getAllObjectsPattern() : forParent.getName(),
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

    @Nullable
    @Override
    protected GenericTableIndex fetchObject(JDBCSession session, GenericStructContainer owner, GenericTable parent, String indexName, JDBCResultSet dbResult)
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
    protected GenericTableIndexColumn[] fetchObjectRow(
        JDBCSession session,
        GenericTable parent, GenericTableIndex object, JDBCResultSet dbResult)
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

        return new GenericTableIndexColumn[] { new GenericTableIndexColumn(
            object,
            tableColumn,
            ordinalPosition,
            !"D".equalsIgnoreCase(ascOrDesc)) };
    }

    @Override
    protected void cacheChildren(DBRProgressMonitor monitor, GenericTableIndex index, List<GenericTableIndexColumn> rows)
    {
        index.setColumns(rows);
    }
}
