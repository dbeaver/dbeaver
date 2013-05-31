package org.jkiss.dbeaver.ext.generic.model.meta;

import org.jkiss.dbeaver.ext.generic.model.GenericStructContainer;
import org.jkiss.dbeaver.ext.generic.model.GenericTable;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCConstants;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Metadata reader
 */
public interface GenericMetadataReader {

    String fetchCatalogName(ResultSet dbResult) throws SQLException;

    String fetchSchemaName(ResultSet dbResult, String catalog) throws SQLException;

    String fetchTableType(ResultSet dbResult) throws SQLException;

    TableInfo fetchTable(ResultSet dbResult, GenericStructContainer container) throws SQLException;

    TableColumnInfo fetchTableColumn(ResultSet dbResult, GenericTable table) throws SQLException;

    static class TableInfo {
        public String tableName;
        public String tableType;
        public String remarks;
        public boolean systemTable;
    }

    static class TableColumnInfo {
        public String columnName;
        public int valueType;
        public int sourceType;
        public String typeName;
        public long columnSize;
        public boolean isNotNull;
        public int scale;
        public int precision;
        public int radix;
        public String defaultValue;
        public String remarks;
        public long charLength;
        public int ordinalPos;
        public boolean autoIncrement;
    }

    static class ForeignKeyInfo {
        public String pkColumnName;
        public String fkTableCatalog;
        public String fkTableSchema;
        public String fkTableName;
        public String fkColumnName;
        public int keySeq;
        public int updateRuleNum;
        public int deleteRuleNum;
        public String fkName;
        public String pkName;
        public int defferabilityNum;
    }

}
