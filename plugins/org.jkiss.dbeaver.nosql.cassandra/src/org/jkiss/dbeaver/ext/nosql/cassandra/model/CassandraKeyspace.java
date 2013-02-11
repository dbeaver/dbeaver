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
package org.jkiss.dbeaver.ext.nosql.cassandra.model;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCExecutionContext;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCConstants;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCCompositeCache;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCStructCache;
import org.jkiss.dbeaver.model.meta.Association;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSEntity;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.rdb.DBSIndexType;
import org.jkiss.dbeaver.model.struct.rdb.DBSSchema;

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.List;

/**
 * CassandraKeyspace
 */
public class CassandraKeyspace implements DBSSchema
{
    private CassandraDataSource dataSource;
    private String schemaName;
    private TableCache tableCache = new TableCache();
    private IndexCache indexCache = new IndexCache(tableCache);

    public CassandraKeyspace(CassandraDataSource dataSource, String schemaName)
    {
        this.dataSource = dataSource;
        this.schemaName = schemaName;
    }

    public TableCache getTableCache()
    {
        return tableCache;
    }

    public IndexCache getIndexCache()
    {
        return indexCache;
    }

    @Override
    public CassandraDataSource getDataSource()
    {
        return dataSource;
    }

    @Override
    @Property(viewable = true, order = 1)
    public String getName()
    {
        return schemaName;
    }

    @Override
    @Property(viewable = true, order = 100)
    public String getDescription()
    {
        return null;
    }

    @Override
    public DBSObject getParentObject()
    {
        return dataSource.getContainer();
    }

    @Association
    public Collection<CassandraColumnFamily> getColumnFamilies(DBRProgressMonitor monitor) throws DBException
    {
        return getChildren(monitor);
    }

    @Override
    public Collection<CassandraColumnFamily> getChildren(DBRProgressMonitor monitor) throws DBException
    {
        return tableCache.getObjects(monitor, this);
    }

    @Override
    public CassandraColumnFamily getChild(DBRProgressMonitor monitor, String childName) throws DBException
    {
        return tableCache.getObject(monitor, this, childName);
    }

    @Override
    public Class<? extends DBSEntity> getChildType(DBRProgressMonitor monitor)
        throws DBException
    {
        return CassandraColumnFamily.class;
    }

    @Override
    public void cacheStructure(DBRProgressMonitor monitor, int scope) throws DBException
    {
        getChildren(monitor);
    }

    @Override
    public boolean isPersisted()
    {
        return true;
    }

    public class TableCache extends JDBCStructCache<CassandraKeyspace, CassandraColumnFamily, CassandraColumn> {

        TableCache()
        {
            super(JDBCConstants.TABLE_NAME);
            setListOrderComparator(DBUtils.<CassandraColumnFamily>nameComparator());
        }

        @Override
        protected JDBCStatement prepareObjectsStatement(JDBCExecutionContext context, CassandraKeyspace owner)
            throws SQLException
        {
            return context.getMetaData().getTables(
                null,
                owner.getName(),
                null,
                null).getSource();
        }

        @Override
        protected CassandraColumnFamily fetchObject(JDBCExecutionContext context, CassandraKeyspace owner, ResultSet dbResult)
            throws SQLException, DBException
        {
            String tableName = JDBCUtils.safeGetStringTrimmed(dbResult, JDBCConstants.TABLE_NAME);
            String remarks = JDBCUtils.safeGetString(dbResult, JDBCConstants.REMARKS);

            boolean isSystemTable = owner.getName().equals("system");
            if (isSystemTable && !owner.getDataSource().getContainer().isShowSystemObjects()) {
                return null;
            }
            return new CassandraColumnFamily(
                owner,
                tableName,
                remarks,
                true);
        }

        @Override
        protected JDBCStatement prepareChildrenStatement(JDBCExecutionContext context, CassandraKeyspace owner, CassandraColumnFamily forTable)
            throws SQLException
        {
            return context.getMetaData().getColumns(
                null,
                owner.getName(),
                forTable == null ? null : forTable.getName(),
                null).getSource();
        }

        @Override
        protected CassandraColumn fetchChild(JDBCExecutionContext context, CassandraKeyspace owner, CassandraColumnFamily table, ResultSet dbResult)
            throws SQLException, DBException
        {
            String columnName = JDBCUtils.safeGetStringTrimmed(dbResult, JDBCConstants.COLUMN_NAME);
            int valueType = JDBCUtils.safeGetInt(dbResult, JDBCConstants.DATA_TYPE);
            int sourceType = JDBCUtils.safeGetInt(dbResult, JDBCConstants.SOURCE_DATA_TYPE);
            String typeName = JDBCUtils.safeGetStringTrimmed(dbResult, JDBCConstants.TYPE_NAME);
            long columnSize = JDBCUtils.safeGetLong(dbResult, JDBCConstants.COLUMN_SIZE);
            boolean isNotNull = JDBCUtils.safeGetInt(dbResult, JDBCConstants.NULLABLE) == DatabaseMetaData.columnNoNulls;
            int scale = JDBCUtils.safeGetInt(dbResult, JDBCConstants.DECIMAL_DIGITS);
            int precision = 0;//GenericUtils.safeGetInt(dbResult, JDBCConstants.COLUMN_);
            int radix = JDBCUtils.safeGetInt(dbResult, JDBCConstants.NUM_PREC_RADIX);
            String remarks = JDBCUtils.safeGetString(dbResult, JDBCConstants.REMARKS);
            long charLength = JDBCUtils.safeGetLong(dbResult, JDBCConstants.CHAR_OCTET_LENGTH);
            int ordinalPos = JDBCUtils.safeGetInt(dbResult, JDBCConstants.ORDINAL_POSITION);

            return new CassandraColumn(
                table,
                columnName,
                typeName, valueType, sourceType, ordinalPos,
                columnSize,
                charLength, scale, precision, radix, isNotNull,
                remarks
            );
        }
    }

    class IndexCache extends JDBCCompositeCache<CassandraKeyspace, CassandraColumnFamily, CassandraIndex, CassandraIndexColumn> {

        IndexCache(TableCache tableCache)
        {
            super(tableCache, CassandraColumnFamily.class, JDBCConstants.TABLE_NAME, JDBCConstants.INDEX_NAME);
        }

        @Override
        protected JDBCStatement prepareObjectsStatement(JDBCExecutionContext context, CassandraKeyspace owner, CassandraColumnFamily forParent)
            throws SQLException
        {
            return context.getMetaData().getIndexInfo(
                null,
                owner.getName(),
                forParent.getName(),
                false,
                true).getSource();
        }

        @Override
        protected CassandraIndex fetchObject(JDBCExecutionContext context, CassandraKeyspace owner, CassandraColumnFamily parent, String indexName, ResultSet dbResult)
            throws SQLException, DBException
        {
            boolean isNonUnique = JDBCUtils.safeGetBoolean(dbResult, JDBCConstants.NON_UNIQUE);
            String indexQualifier = JDBCUtils.safeGetStringTrimmed(dbResult, JDBCConstants.INDEX_QUALIFIER);
            long cardinality = JDBCUtils.safeGetLong(dbResult, JDBCConstants.INDEX_CARDINALITY);

            DBSIndexType indexType = DBSIndexType.CLUSTERED;

            return new CassandraIndex(
                parent,
                isNonUnique,
                indexQualifier,
                cardinality,
                indexName,
                indexType,
                true);
        }

        @Override
        protected CassandraIndexColumn fetchObjectRow(
            JDBCExecutionContext context,
            CassandraColumnFamily parent, CassandraIndex object, ResultSet dbResult)
            throws SQLException, DBException
        {
            int ordinalPosition = JDBCUtils.safeGetInt(dbResult, JDBCConstants.ORDINAL_POSITION);
            String columnName = JDBCUtils.safeGetStringTrimmed(dbResult, JDBCConstants.COLUMN_NAME);
            String ascOrDesc = JDBCUtils.safeGetStringTrimmed(dbResult, JDBCConstants.ASC_OR_DESC);

            CassandraColumn tableColumn = parent.getAttribute(context.getProgressMonitor(), columnName);
            if (tableColumn == null) {
                log.debug("Column '" + columnName + "' not found in table '" + parent.getName() + "' for index '" + object.getName() + "'");
                return null;
            }

            return new CassandraIndexColumn(
                object,
                tableColumn,
                ordinalPosition,
                !"D".equalsIgnoreCase(ascOrDesc));
        }

        @Override
        protected void cacheChildren(CassandraIndex index, List<CassandraIndexColumn> rows)
        {
            index.setColumns(rows);
        }
    }
}
