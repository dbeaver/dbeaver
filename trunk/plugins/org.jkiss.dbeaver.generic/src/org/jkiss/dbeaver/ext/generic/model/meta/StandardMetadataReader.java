package org.jkiss.dbeaver.ext.generic.model.meta;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jkiss.dbeaver.ext.generic.GenericConstants;
import org.jkiss.dbeaver.ext.generic.model.GenericStructContainer;
import org.jkiss.dbeaver.ext.generic.model.GenericTable;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCConstants;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.utils.CommonUtils;

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Standard metadata reader, uses column names from JDBC API spec
 */
public class StandardMetadataReader implements GenericMetadataReader {

    static final Log log = LogFactory.getLog(StandardMetadataReader.class);

    @Override
    public String fetchCatalogName(ResultSet dbResult) throws SQLException
    {
        String catalog = JDBCUtils.safeGetString(dbResult, JDBCConstants.TABLE_CAT);
        if (CommonUtils.isEmpty(catalog)) {
            // Some drivers uses TABLE_QUALIFIER instead of catalog
            catalog = JDBCUtils.safeGetStringTrimmed(dbResult, JDBCConstants.TABLE_QUALIFIER);
        }
        return catalog;
    }

    @Override
    public String fetchSchemaName(ResultSet dbResult, String catalog) throws SQLException
    {
        String schemaName = JDBCUtils.safeGetString(dbResult, JDBCConstants.TABLE_SCHEM);
        if (CommonUtils.isEmpty(schemaName)) {
            // some drivers uses TABLE_OWNER column instead of TABLE_SCHEM
            schemaName = JDBCUtils.safeGetString(dbResult, JDBCConstants.TABLE_OWNER);
        }
        return schemaName;
    }

    @Override
    public String fetchTableType(ResultSet dbResult) throws SQLException
    {
        return JDBCUtils.safeGetString(dbResult, JDBCConstants.TABLE_TYPE);
    }

    @Override
    public TableInfo fetchTable(ResultSet dbResult, GenericStructContainer container) throws SQLException
    {
        TableInfo tableInfo = new TableInfo();
        tableInfo.tableName = JDBCUtils.safeGetStringTrimmed(dbResult, JDBCConstants.TABLE_NAME);
        tableInfo.tableType = JDBCUtils.safeGetStringTrimmed(dbResult, JDBCConstants.TABLE_TYPE);
        tableInfo.remarks = JDBCUtils.safeGetString(dbResult, JDBCConstants.REMARKS);

        if (!CommonUtils.isEmpty(tableInfo.tableName)) {
            tableInfo.systemTable = tableInfo.tableType != null &&
                (tableInfo.tableType.toUpperCase().contains("SYSTEM") ||
                    tableInfo.tableName.contains("RDB$"));    // [JDBC: Firebird]
        }
        return tableInfo;
    }

    @Override
    public TableColumnInfo fetchTableColumn(ResultSet dbResult, GenericTable table) throws SQLException
    {
        TableColumnInfo column = new TableColumnInfo();
        column.columnName = JDBCUtils.safeGetStringTrimmed(dbResult, JDBCConstants.COLUMN_NAME);
        column.valueType = JDBCUtils.safeGetInt(dbResult, JDBCConstants.DATA_TYPE);
        column.sourceType = JDBCUtils.safeGetInt(dbResult, JDBCConstants.SOURCE_DATA_TYPE);
        column.typeName = JDBCUtils.safeGetStringTrimmed(dbResult, JDBCConstants.TYPE_NAME);
        column.columnSize = JDBCUtils.safeGetLong(dbResult, JDBCConstants.COLUMN_SIZE);
        column.isNotNull = JDBCUtils.safeGetInt(dbResult, JDBCConstants.NULLABLE) == DatabaseMetaData.columnNoNulls;
        column.scale = JDBCUtils.safeGetInt(dbResult, JDBCConstants.DECIMAL_DIGITS);
        column.precision = 0;//GenericUtils.safeGetInt(dbResult, JDBCConstants.COLUMN_);
        column.radix = JDBCUtils.safeGetInt(dbResult, JDBCConstants.NUM_PREC_RADIX);
        column.defaultValue = JDBCUtils.safeGetString(dbResult, JDBCConstants.COLUMN_DEF);
        column.remarks = JDBCUtils.safeGetString(dbResult, JDBCConstants.REMARKS);
        column.charLength = JDBCUtils.safeGetLong(dbResult, JDBCConstants.CHAR_OCTET_LENGTH);
        column.ordinalPos = JDBCUtils.safeGetInt(dbResult, JDBCConstants.ORDINAL_POSITION);
        column.autoIncrement = "YES".equals(JDBCUtils.safeGetStringTrimmed(dbResult, JDBCConstants.IS_AUTOINCREMENT));
        // Check for identity modifier [DBSPEC: MS SQL]
        if (column.typeName.toUpperCase().endsWith(GenericConstants.TYPE_MODIFIER_IDENTITY)) {
            column.autoIncrement = true;
            column.typeName = column.typeName.substring(0, column.typeName.length() - GenericConstants.TYPE_MODIFIER_IDENTITY.length());
        }
        return column;
    }

}
