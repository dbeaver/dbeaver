/*
 * Copyright (C) 2010-2014 Serge Rieder
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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.generic.GenericConstants;
import org.jkiss.dbeaver.ext.generic.model.meta.GenericMetaObject;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCConstants;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCStructCache;
import org.jkiss.utils.CommonUtils;

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;

/**
 * Generic tables cache implementation
 */
public class TableCache extends JDBCStructCache<GenericStructContainer, GenericTable, GenericTableColumn> {

    static final Log log = LogFactory.getLog(TableCache.class);

    // Tables types which are not actually a table
    // This is needed for some strange JDBC drivers which returns not a table objects
    // in DatabaseMetaData.getTables method (PostgreSQL especially)
    private static final Set<String> INVALID_TABLE_TYPES = new HashSet<String>();

    static {
        // [JDBC: PostgreSQL]
        INVALID_TABLE_TYPES.add("INDEX");
        INVALID_TABLE_TYPES.add("SEQUENCE");
        INVALID_TABLE_TYPES.add("SYSTEM INDEX");
        INVALID_TABLE_TYPES.add("SYSTEM SEQUENCE");
        // [JDBC: SQLite]
        INVALID_TABLE_TYPES.add("TRIGGER");
    }

    private final GenericDataSource dataSource;
    private final GenericMetaObject tableObject;
    private final GenericMetaObject columnObject;

    TableCache(GenericDataSource dataSource)
    {
        super(GenericUtils.getColumn(dataSource, GenericConstants.OBJECT_TABLE, JDBCConstants.TABLE_NAME));
        this.dataSource = dataSource;
        this.tableObject = dataSource.getMetaObject(GenericConstants.OBJECT_TABLE);
        this.columnObject = dataSource.getMetaObject(GenericConstants.OBJECT_TABLE_COLUMN);
        setListOrderComparator(DBUtils.<GenericTable>nameComparator());
    }

    public GenericDataSource getDataSource()
    {
        return dataSource;
    }

    @Override
    protected JDBCStatement prepareObjectsStatement(JDBCSession session, GenericStructContainer owner)
        throws SQLException
    {
        return session.getMetaData().getTables(
            owner.getCatalog() == null ? null : owner.getCatalog().getName(),
            owner.getSchema() == null ? null : owner.getSchema().getName(),
            owner.getDataSource().getAllObjectsPattern(),
            null).getSourceStatement();
    }

    @Nullable
    @Override
    protected GenericTable fetchObject(JDBCSession session, GenericStructContainer owner, ResultSet dbResult)
        throws SQLException, DBException
    {
        String tableName = GenericUtils.safeGetStringTrimmed(tableObject, dbResult, JDBCConstants.TABLE_NAME);
        String tableType = GenericUtils.safeGetStringTrimmed(tableObject, dbResult, JDBCConstants.TABLE_TYPE);
        String remarks = GenericUtils.safeGetString(tableObject, dbResult, JDBCConstants.REMARKS);

        if (CommonUtils.isEmpty(tableName)) {
            log.debug("Empty table name" + (owner == null ? "" : " in container " + owner.getName()));
            return null;
        }

        if (tableType != null && INVALID_TABLE_TYPES.contains(tableType)) {
            // Bad table type. Just skip it
            return null;
        }

        boolean isSystemTable = tableType != null && tableType.toUpperCase().contains("SYSTEM");
        if (isSystemTable && !owner.getDataSource().getContainer().isShowSystemObjects()) {
            return null;
        }

        // Skip "recycled" tables (Oracle)
        if (tableName.startsWith("BIN$")) {
            return null;
        }
/*
        // Do not read table type object
        // Actually dunno what to do with it and it often throws stupid warnings in debug

        String typeName = GenericUtils.safeGetString(dbResult, JDBCConstants.TYPE_NAME);
        String typeCatalogName = GenericUtils.safeGetString(dbResult, JDBCConstants.TYPE_CAT);
        String typeSchemaName = GenericUtils.safeGetString(dbResult, JDBCConstants.TYPE_SCHEM);
        GenericCatalog typeCatalog = CommonUtils.isEmpty(typeCatalogName) ?
            null :
            getDataSourceContainer().getCatalog(context.getProgressMonitor(), typeCatalogName);
        GenericSchema typeSchema = CommonUtils.isEmpty(typeSchemaName) ?
            null :
            typeCatalog == null ?
                getDataSourceContainer().getSchema(context.getProgressMonitor(), typeSchemaName) :
                typeCatalog.getSchema(typeSchemaName);
*/
        return new GenericTable(
            owner,
            tableName,
            tableType,
            remarks,
            true);
    }

    @Override
    protected JDBCStatement prepareChildrenStatement(JDBCSession session, GenericStructContainer owner, GenericTable forTable)
        throws SQLException
    {
        return session.getMetaData().getColumns(
            owner.getCatalog() == null ? null : owner.getCatalog().getName(),
            owner.getSchema() == null ? null : owner.getSchema().getName(),
            forTable == null ? owner.getDataSource().getAllObjectsPattern() : forTable.getName(),
            owner.getDataSource().getAllObjectsPattern()).getSourceStatement();
    }

    @Override
    protected GenericTableColumn fetchChild(JDBCSession session, GenericStructContainer owner, GenericTable table, ResultSet dbResult)
        throws SQLException, DBException
    {
        String columnName = GenericUtils.safeGetStringTrimmed(columnObject, dbResult, JDBCConstants.COLUMN_NAME);
        int valueType = GenericUtils.safeGetInt(columnObject, dbResult, JDBCConstants.DATA_TYPE);
        int sourceType = GenericUtils.safeGetInt(columnObject, dbResult, JDBCConstants.SOURCE_DATA_TYPE);
        String typeName = GenericUtils.safeGetStringTrimmed(columnObject, dbResult, JDBCConstants.TYPE_NAME);
        long columnSize = GenericUtils.safeGetLong(columnObject, dbResult, JDBCConstants.COLUMN_SIZE);
        boolean isNotNull = GenericUtils.safeGetInt(columnObject, dbResult, JDBCConstants.NULLABLE) == DatabaseMetaData.columnNoNulls;
        int scale = GenericUtils.safeGetInt(columnObject, dbResult, JDBCConstants.DECIMAL_DIGITS);
        int precision = 0;//GenericUtils.safeGetInt(dbResult, JDBCConstants.COLUMN_);
        int radix = GenericUtils.safeGetInt(columnObject, dbResult, JDBCConstants.NUM_PREC_RADIX);
        String defaultValue = GenericUtils.safeGetString(columnObject, dbResult, JDBCConstants.COLUMN_DEF);
        String remarks = GenericUtils.safeGetString(columnObject, dbResult, JDBCConstants.REMARKS);
        long charLength = GenericUtils.safeGetLong(columnObject, dbResult, JDBCConstants.CHAR_OCTET_LENGTH);
        int ordinalPos = GenericUtils.safeGetInt(columnObject, dbResult, JDBCConstants.ORDINAL_POSITION);
        boolean autoIncrement = "YES".equals(GenericUtils.safeGetStringTrimmed(columnObject, dbResult, JDBCConstants.IS_AUTOINCREMENT));
        boolean autoGenerated = "YES".equals(GenericUtils.safeGetStringTrimmed(columnObject, dbResult, JDBCConstants.IS_AUTOINCREMENT));
        // Check for identity modifier [DBSPEC: MS SQL]
        if (typeName.toUpperCase().endsWith(GenericConstants.TYPE_MODIFIER_IDENTITY)) {
            autoIncrement = true;
            typeName = typeName.substring(0, typeName.length() - GenericConstants.TYPE_MODIFIER_IDENTITY.length());
        }

        return new GenericTableColumn(
            table,
            columnName,
            typeName, valueType, sourceType, ordinalPos,
            columnSize,
            charLength, scale, precision, radix, isNotNull,
            remarks, defaultValue, autoIncrement, autoGenerated
        );
    }

}
