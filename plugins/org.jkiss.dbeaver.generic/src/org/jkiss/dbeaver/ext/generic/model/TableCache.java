package org.jkiss.dbeaver.ext.generic.model;

import net.sf.jkiss.utils.CommonUtils;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.generic.GenericConstants;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCExecutionContext;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCConstants;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCStructCache;

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Tables cache implementation
 */
class TableCache extends JDBCStructCache<GenericTable, GenericTableColumn> {

    // Tables types which are not actually a table
    // This is needed for some strange JDBC drivers which returns not a table objects
    // in DatabaseMetaData.getTables method (PostgreSQL especially)
    private static final Set<String> INVALID_TABLE_TYPES = new HashSet<String>();

    static {
        INVALID_TABLE_TYPES.add("INDEX");
        INVALID_TABLE_TYPES.add("SEQUENCE");
        INVALID_TABLE_TYPES.add("SYSTEM INDEX");
        INVALID_TABLE_TYPES.add("SYSTEM SEQUENCE");
    }

    private GenericStructContainer structContainer;

    TableCache(GenericStructContainer structContainer, GenericDataSource dataSource)
    {
        super(dataSource, JDBCConstants.TABLE_NAME);
        this.structContainer = structContainer;
        setListOrderComparator(DBUtils.<GenericTable>nameComparator());
    }

    protected JDBCPreparedStatement prepareObjectsStatement(JDBCExecutionContext context)
        throws SQLException, DBException
    {
        return context.getMetaData().getTables(
            structContainer.getCatalog() == null ? null : structContainer.getCatalog().getName(),
            structContainer.getSchema() == null ? null : structContainer.getSchema().getName(),
            null,
            null).getSource();
    }

    protected GenericTable fetchObject(JDBCExecutionContext context, ResultSet dbResult)
        throws SQLException, DBException
    {
        String tableName = JDBCUtils.safeGetStringTrimmed(dbResult, JDBCConstants.TABLE_NAME);
        String tableType = JDBCUtils.safeGetStringTrimmed(dbResult, JDBCConstants.TABLE_TYPE);
        String remarks = JDBCUtils.safeGetString(dbResult, JDBCConstants.REMARKS);

        if (tableType != null && INVALID_TABLE_TYPES.contains(tableType)) {
            // Bad table type. Just skip it
            return null;
        }

        boolean isSystemTable = tableType != null && tableType.toUpperCase().indexOf("SYSTEM") != -1;
        if (isSystemTable && !structContainer.getDataSource().getContainer().isShowSystemObjects()) {
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
                structContainer,
            tableName,
            tableType,
            remarks,
            true);
    }

    protected boolean isChildrenCached(GenericTable table)
    {
        return table.isColumnsCached();
    }

    protected void cacheChildren(GenericTable table, List<GenericTableColumn> columns)
    {
        table.setColumns(columns);
    }

    protected JDBCPreparedStatement prepareChildrenStatement(JDBCExecutionContext context, GenericTable forTable)
        throws SQLException, DBException
    {
        return context.getMetaData().getColumns(
            structContainer.getCatalog() == null ? null : structContainer.getCatalog().getName(),
            structContainer.getSchema() == null ? null : structContainer.getSchema().getName(),
            forTable == null ? null : forTable.getName(),
            null).getSource();
    }

    protected GenericTableColumn fetchChild(JDBCExecutionContext context, GenericTable table, ResultSet dbResult)
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
            valueType = JDBCUtils.getDataTypeByName(valueType, typeName);
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
