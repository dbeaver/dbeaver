/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.generic.model.*;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCConstants;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCCompositeCache;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.rdb.DBSIndexType;
import org.jkiss.utils.CommonUtils;

import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.List;
import java.util.Locale;

public class CubridObjectContainer extends GenericObjectContainer
{

    private final CubridDataSource dataSource;
    private final CubridIndexCache cubridIndexCache;

    protected CubridObjectContainer(CubridDataSource dataSource)
    {
        super(dataSource);
        this.dataSource = dataSource;
        this.cubridIndexCache = new CubridIndexCache(this.getTableCache());
    }

    @NotNull
    @Override
    public CubridDataSource getDataSource()
    {
        return dataSource;
    }

    public CubridIndexCache getCubridIndexCache()
    {
        return this.cubridIndexCache;
    }

    @Override
    public GenericStructContainer getObject()
    {
        return this;
    }

    @Override
    public GenericCatalog getCatalog()
    {
        return null;
    }

    @Override
    public GenericSchema getSchema()
    {
        return null;
    }

    @NotNull
    @Override
    public Class<? extends DBSObject> getPrimaryChildType(DBRProgressMonitor monitor)
            throws DBException
    {
        return CubridTable.class;
    }

    @Override
    public DBSObject getParentObject()
    {
        return this.getDataSource().getParentObject();
    }

    @Override
    public String getName()
    {
        return this.getDataSource().getName();
    }

    @Override
    public String getDescription()
    {
        return this.getDataSource().getDescription();
    }

    public class CubridIndexCache extends JDBCCompositeCache<GenericStructContainer, CubridTable, GenericTableIndex, GenericTableIndexColumn>
    {

        CubridIndexCache(TableCache tableCache)
        {
            super(tableCache, CubridTable.class, JDBCConstants.TABLE_NAME, JDBCConstants.INDEX_NAME);
        }

        @Override
        protected JDBCStatement prepareObjectsStatement(JDBCSession session, GenericStructContainer owner, CubridTable forParent)
                throws SQLException
        {
            return session.getMetaData().getIndexInfo(null, null, forParent.getUniqueName(), false, true).getSourceStatement();
        }

        @Override
        protected GenericTableIndex fetchObject(
                JDBCSession session,
                GenericStructContainer owner,
                CubridTable parent,
                String indexName,
                JDBCResultSet dbResult)
                throws SQLException, DBException
        {
            boolean isNonUnique = JDBCUtils.safeGetBoolean(dbResult, JDBCConstants.NON_UNIQUE);
            String indexQualifier =
                    JDBCUtils.safeGetStringTrimmed(dbResult, JDBCConstants.INDEX_QUALIFIER);
            long cardinality = JDBCUtils.safeGetLong(dbResult, JDBCConstants.INDEX_CARDINALITY);
            int indexTypeNum = JDBCUtils.safeGetInt(dbResult, JDBCConstants.TYPE);
            String name = indexName;

            DBSIndexType indexType;
            switch (indexTypeNum) {
                case DatabaseMetaData.tableIndexStatistic:
                    return null;
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
            if (CommonUtils.isEmpty(name)) {
                name = parent.getName().toUpperCase(Locale.ENGLISH) + "_INDEX";
            }
            return new GenericTableIndex(
                    parent, isNonUnique, indexQualifier, cardinality, name, indexType, true);
        }

        @Override
        protected GenericTableIndexColumn[] fetchObjectRow(
                JDBCSession session,
                CubridTable parent,
                GenericTableIndex object,
                JDBCResultSet dbResult)
                throws SQLException, DBException
        {
            int ordinalPosition = JDBCUtils.safeGetInt(dbResult, JDBCConstants.ORDINAL_POSITION);
            String columnName = JDBCUtils.safeGetString(dbResult, JDBCConstants.COLUMN_NAME);
            String ascOrDesc = JDBCUtils.safeGetStringTrimmed(dbResult, JDBCConstants.ASC_OR_DESC);

            if (CommonUtils.isEmpty(columnName)) {
                // Maybe a statistics index without column
                return null;
            }
            GenericTableColumn tableColumn = parent.getAttribute(session.getProgressMonitor(), columnName);
            if (tableColumn == null) {
                return null;
            }
            return new GenericTableIndexColumn[]{new GenericTableIndexColumn(
                    object, tableColumn, ordinalPosition, !"D".equalsIgnoreCase(ascOrDesc))
            };
        }

        @Override
        protected void cacheChildren(
                DBRProgressMonitor monitor,
                GenericTableIndex object,
                List<GenericTableIndexColumn> children)
        {
            object.setColumns(children);
        }
    }
}
