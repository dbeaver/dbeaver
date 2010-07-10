package org.jkiss.dbeaver.ext.mysql;

/**
 * MySQL constants
 */
public class MySQLConstants {

    public static final int DEFAULT_PORT = 3306;

    public static final String[] TABLE_TYPES = new String[]{"TABLE", "VIEW", "LOCAL TEMPORARY"};

    public static final String INFO_SCHEMA_NAME = "information_schema";
    public static final String META_TABLE_SCHEMATA = INFO_SCHEMA_NAME + ".SCHEMATA";
    public static final String META_TABLE_TABLES = INFO_SCHEMA_NAME + ".TABLES";
    public static final String META_TABLE_ROUTINES = INFO_SCHEMA_NAME + ".ROUTINES";
    public static final String META_TABLE_COLUMNS = INFO_SCHEMA_NAME + ".COLUMNS";

    public static final String COL_CATALOG_NAME = "CATALOG_NAME";
    public static final String COL_SCHEMA_NAME = "SCHEMA_NAME";
    public static final String COL_DEFAULT_CHARACTER_SET_NAME = "DEFAULT_CHARACTER_SET_NAME";
    public static final String COL_DEFAULT_COLLATION_NAME = "DEFAULT_COLLATION_NAME";
    public static final String COL_SQL_PATH = "SQL_PATH";

    public static final String COL_TABLE_SCHEMA = "TABLE_SCHEMA";
    public static final String COL_TABLE_NAME = "TABLE_NAME";
    public static final String COL_TABLE_TYPE = "TABLE_TYPE";
    public static final String COL_ENGINE = "ENGINE";
    public static final String COL_VERSION = "VERSION";
    public static final String COL_TABLE_ROWS = "TABLE_ROWS";
    public static final String COL_AUTO_INCREMENT = "AUTO_INCREMENT";
    public static final String COL_TABLE_COMMENT = "TABLE_COMMENT";
    public static final String COL_COLUMNS_NAME = "COLUMNS_NAME";
    public static final String COL_ORDINAL_POSITION = "ORDINAL_POSITION";
    
    public static final String COL_COLUMN_NAME = "COLUMN_NAME";
    public static final String COL_DATA_TYPE = "DATA_TYPE";
    public static final String COL_CHARACTER_MAXIMUM_LENGTH = "CHARACTER_MAXIMUM_LENGTH";
    public static final String COL_CHARACTER_OCTET_LENGTH = "CHARACTER_OCTET_LENGTH";
    public static final String COL_NUMERIC_PRECISION = "NUMERIC_PRECISION";
    public static final String COL_NUMERIC_SCALE = "NUMERIC_SCALE";
    public static final String COL_COLUMN_DEFAULT = "COLUMN_DEFAULT";
    public static final String COL_IS_NULLABLE = "IS_NULLABLE";
    public static final String COL_COLUMN_COMMENT = "COLUMN_COMMENT";
    public static final String COL_COLUMN_EXTRA = "EXTRA";

    public static final String COL_ROUTINE_SCHEMA = "ROUTINE_SCHEMA";
    public static final String COL_ROUTINE_NAME = "ROUTINE_NAME";
    public static final String COL_ROUTINE_TYPE = "ROUTINE_TYPE";
    public static final String COL_DTD_IDENTIFIER = "DTD_IDENTIFIER";
    public static final String COL_ROUTINE_BODY = "ROUTINE_BODY";
    public static final String COL_ROUTINE_DEFINITION = "ROUTINE_DEFINITION";
    public static final String COL_EXTERNAL_NAME = "EXTERNAL_NAME";
    public static final String COL_EXTERNAL_LANGUAGE = "EXTERNAL_LANGUAGE";
    public static final String COL_PARAMETER_STYLE = "PARAMETER_STYLE";
    public static final String COL_IS_DETERMINISTIC = "IS_DETERMINISTIC";
    public static final String COL_SQL_DATA_ACCESS = "SQL_DATA_ACCESS";
    public static final String COL_SECURITY_TYPE = "SECURITY_TYPE";
    public static final String COL_ROUTINE_COMMENT = "ROUTINE_COMMENT";
    public static final String COL_DEFINER = "DEFINER";
    public static final String COL_CHARACTER_SET_CLIENT = "CHARACTER_SET_CLIENT";

    public static final String EXTRA_AUTO_INCREMENT = "auto_increment";

    public static final String QUERY_SELECT_TABLES =
        "SELECT * FROM " + META_TABLE_TABLES +
        " WHERE " + COL_TABLE_SCHEMA + "=?" +
        " ORDER BY " + COL_TABLE_NAME;

    public static final String QUERY_SELECT_TABLE_COLUMNS =
        "SELECT * FROM " + META_TABLE_COLUMNS +
        " WHERE " + COL_TABLE_SCHEMA + "=? AND " + COL_TABLE_NAME + "=?" +
        " ORDER BY " + COL_ORDINAL_POSITION;

    public static final String QUERY_SELECT_ROUTINES =
        "SELECT * FROM " + META_TABLE_ROUTINES +
        " WHERE " + COL_ROUTINE_SCHEMA + "=?" +
        " ORDER BY " + COL_ROUTINE_NAME;

    public static final String TYPE_NAME_ENUM = "ENUM";
    public static final String TYPE_NAME_SET = "SET";
}
