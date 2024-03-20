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
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.generic.model.GenericDataSource;
import org.jkiss.dbeaver.ext.generic.model.GenericSchema;
import org.jkiss.dbeaver.ext.generic.model.GenericStructContainer;
import org.jkiss.dbeaver.ext.generic.model.GenericTable;
import org.jkiss.dbeaver.ext.generic.model.GenericTableColumn;
import org.jkiss.dbeaver.ext.generic.model.GenericTableIndex;
import org.jkiss.dbeaver.ext.generic.model.GenericTableIndexColumn;
import org.jkiss.dbeaver.ext.generic.model.GenericView;
import org.jkiss.dbeaver.ext.generic.model.TableCache;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCConstants;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCCompositeCache;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.rdb.DBSIndexType;
import org.jkiss.utils.CommonUtils;

import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class CubridUser extends GenericSchema
{
    private String name;
    private String comment;
    private final CubridIndexCache cubridIndexCache;

    public CubridUser(GenericDataSource dataSource, String schemaName, String comment) {
        super(dataSource, null, schemaName);
        this.name = schemaName;
        this.comment = comment;
        this.cubridIndexCache = new CubridIndexCache(this.getTableCache());
    }

    @Property(viewable = true, order = 1)
    public String getName() {
        return name;
    }

    @Nullable
    @Property(viewable = true, order = 2)
    public String getComment() {
        return comment;
    }

    public boolean supportsSystemTable() {
        return name.equals("DBA");
    }

    public boolean supportsSystemView() {
        return name.equals("DBA");
    }

    public boolean showSystemTableFolder() {
        return this.getDataSource().getContainer().getNavigatorSettings().isShowSystemObjects();
    }

    @NotNull
    public boolean supportsSynonym() {
        return ((CubridDataSource) this.getDataSource()).getSupportMultiSchema();
    }

    public CubridIndexCache getCubridIndexCache() {
        return cubridIndexCache;
    }

    @Override
    public List<CubridTable> getPhysicalTables(DBRProgressMonitor monitor) throws DBException {
        List<CubridTable> tables = new ArrayList<>();
        for (GenericTable table : super.getPhysicalTables(monitor)) {
            if (!table.isSystem()) {
                tables.add((CubridTable) table);
            }
        }
        return tables;
    }

    public List<? extends CubridTable> getPhysicalSystemTables(DBRProgressMonitor monitor)
            throws DBException {
        List<CubridTable> tables = new ArrayList<>();
        for (GenericTable table : super.getPhysicalTables(monitor)) {
            if (table.isSystem()) {
                tables.add((CubridTable) table);
            }
        }
        return tables;
    }

    @Override
    public List<CubridView> getViews(DBRProgressMonitor monitor) throws DBException {
        List<CubridView> views = new ArrayList<>();
        for (GenericView view : super.getViews(monitor)) {
            if (!view.isSystem()) {
                views.add((CubridView) view);
            }
        }
        return views;
    }

    public List<CubridView> getSystemViews(DBRProgressMonitor monitor) throws DBException {
        List<CubridView> views = new ArrayList<>();
        for (GenericView view : super.getViews(monitor)) {
            if (view.isSystem()) {
                views.add((CubridView) view);
            }
        }
        return views;
    }

    @Nullable
    @Override
    public List<GenericTableIndex> getIndexes(@NotNull DBRProgressMonitor monitor) throws DBException {
        cacheIndexes(monitor);
        return cubridIndexCache.getAllObjects(monitor, this);
    }

    private void cacheIndexes(@NotNull DBRProgressMonitor monitor) throws DBException {
        synchronized (cubridIndexCache) {
            if(!cubridIndexCache.isFullyCached()) {
                List<GenericTableIndex> newIndexCache = new ArrayList<>();
                for(CubridTable table : getPhysicalTables(monitor)) {
                    newIndexCache.addAll(table.getIndexes(monitor));
                }
                cubridIndexCache.setCache(newIndexCache);
            }
        }
    }

    public class CubridIndexCache extends JDBCCompositeCache<GenericStructContainer, CubridTable, GenericTableIndex, GenericTableIndexColumn>
    {
        CubridIndexCache(TableCache tableCache) {
            super(tableCache, CubridTable.class, JDBCConstants.TABLE_NAME, JDBCConstants.INDEX_NAME);
        }

        @Override
        protected JDBCStatement prepareObjectsStatement(JDBCSession session, GenericStructContainer owner, CubridTable forParent)
                throws SQLException {
            return session.getMetaData().getIndexInfo(null, null, forParent.getUniqueName(), false, true).getSourceStatement();
        }

        @Override
        protected GenericTableIndex fetchObject(
                JDBCSession session,
                GenericStructContainer owner,
                CubridTable parent,
                String indexName,
                JDBCResultSet dbResult)
                throws SQLException, DBException {
            boolean isNonUnique = JDBCUtils.safeGetBoolean(dbResult, JDBCConstants.NON_UNIQUE);
            String indexQualifier = JDBCUtils.safeGetStringTrimmed(dbResult, JDBCConstants.INDEX_QUALIFIER);
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
                throws SQLException, DBException {
            int ordinalPosition = JDBCUtils.safeGetInt(dbResult, JDBCConstants.ORDINAL_POSITION);
            String columnName = JDBCUtils.safeGetString(dbResult, JDBCConstants.COLUMN_NAME);
            String ascOrDesc = JDBCUtils.safeGetStringTrimmed(dbResult, JDBCConstants.ASC_OR_DESC);

            if (CommonUtils.isEmpty(columnName)) {
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
                List<GenericTableIndexColumn> children) {
            object.setColumns(children);
        }
    }
}
