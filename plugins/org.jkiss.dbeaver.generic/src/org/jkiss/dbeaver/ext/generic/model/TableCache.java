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
import org.jkiss.dbeaver.ext.generic.GenericConstants;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCExecutionContext;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCConstants;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCStructCache;
import org.jkiss.dbeaver.model.impl.jdbc.struct.JDBCDataType;
import org.jkiss.utils.CommonUtils;

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;

/**
 * Tables cache implementation
 */
public class TableCache extends JDBCStructCache<GenericStructContainer, GenericTable, GenericTableColumn> {

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

    TableCache()
    {
        super(JDBCConstants.TABLE_NAME);
        setListOrderComparator(DBUtils.<GenericTable>nameComparator());
    }

    @Override
    protected JDBCStatement prepareObjectsStatement(JDBCExecutionContext context, GenericStructContainer owner)
        throws SQLException
    {
        return context.getMetaData().getTables(
            owner.getCatalog() == null ? null : owner.getCatalog().getName(),
            owner.getSchema() == null ? null : owner.getSchema().getName(),
            null,
            null).getSource();
    }

    @Override
    protected GenericTable fetchObject(JDBCExecutionContext context, GenericStructContainer owner, ResultSet dbResult)
        throws SQLException, DBException
    {
        String tableName = JDBCUtils.safeGetStringTrimmed(dbResult, JDBCConstants.TABLE_NAME);
        String tableType = JDBCUtils.safeGetStringTrimmed(dbResult, JDBCConstants.TABLE_TYPE);
        String remarks = JDBCUtils.safeGetString(dbResult, JDBCConstants.REMARKS);

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

        String typeName = JDBCUtils.safeGetString(dbResult, JDBCConstants.TYPE_NAME);
        String typeCatalogName = JDBCUtils.safeGetString(dbResult, JDBCConstants.TYPE_CAT);
        String typeSchemaName = JDBCUtils.safeGetString(dbResult, JDBCConstants.TYPE_SCHEM);
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
    protected JDBCStatement prepareChildrenStatement(JDBCExecutionContext context, GenericStructContainer owner, GenericTable forTable)
        throws SQLException
    {
        return context.getMetaData().getColumns(
            owner.getCatalog() == null ? null : owner.getCatalog().getName(),
            owner.getSchema() == null ? null : owner.getSchema().getName(),
            forTable == null ? null : forTable.getName(),
            null).getSource();
    }

    @Override
    protected GenericTableColumn fetchChild(JDBCExecutionContext context, GenericStructContainer owner, GenericTable table, ResultSet dbResult)
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
        String defaultValue = JDBCUtils.safeGetString(dbResult, JDBCConstants.COLUMN_DEF);
        String remarks = JDBCUtils.safeGetString(dbResult, JDBCConstants.REMARKS);
        long charLength = JDBCUtils.safeGetLong(dbResult, JDBCConstants.CHAR_OCTET_LENGTH);
        int ordinalPos = JDBCUtils.safeGetInt(dbResult, JDBCConstants.ORDINAL_POSITION);
        boolean autoIncrement = "YES".equals(JDBCUtils.safeGetStringTrimmed(dbResult, JDBCConstants.IS_AUTOINCREMENT));
        if (!CommonUtils.isEmpty(typeName)) {
            // Try to determine value type from type name
            valueType = JDBCDataType.getValueTypeByTypeName(typeName, valueType);
        }
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
            remarks, defaultValue, autoIncrement
        );
    }
}
