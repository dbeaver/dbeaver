/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.oracle;

import org.jkiss.dbeaver.model.struct.DBSIndexType;

/**
 * Oracle constants
 */
public class OracleConstants {

    public static final int DEFAULT_PORT = 1521;

    public static final String[] TABLE_TYPES = new String[]{"TABLE", "VIEW", "TEMPORARY"};

    public static final String INFO_SCHEMA_NAME = "SYS";

    public static final String[] SYSTEM_SCHEMAS = {
        "CTXSYS",
        "DBSNMP",
        "DMSYS",
        "EXFSYS",
        "IX",
        "MDSYS",
        "MGMT_VIEW",
        "OLAPSYS",
        "ORDPLUGINS",
        "ORDSYS",
        "SI_INFORMTN_SCHEMA",
        "SYS",
        "SYSMAN",
        "SYSTEM",
        "TSMSYS",
        "WMSYS",
        "XDB",
    };

    public static final String PROP_CONNECTION_TYPE = "@dbeaver-connection-type@";
    public static final String PROP_DRIVER_TYPE = "@dbeaver-driver-type@";
    public static final String PROP_INTERNAL_LOGON = "internal_logon";
    public static final String OS_AUTH_USER_NAME = "@OS_AUTH@";

    public static final String DRIVER_TYPE_THIN = "THIN";
    public static final String DRIVER_TYPE_OCI = "OCI";

    public static final String META_TABLE_USERS = INFO_SCHEMA_NAME + ".ALL_USERS";
    public static final String META_TABLE_TABLES = INFO_SCHEMA_NAME + ".ALL_TABLES";
    public static final String META_TABLE_COLUMNS = INFO_SCHEMA_NAME + ".ALL_TAB_COLS";
    public static final String META_TABLE_CONSTRAINTS = INFO_SCHEMA_NAME + ".ALL_CONSTRAINTS";
    public static final String META_TABLE_CONSTRAINT_COLUMNS = INFO_SCHEMA_NAME + ".ALL_CONS_COLUMNS";

    public static final String META_TABLE_ROUTINES = INFO_SCHEMA_NAME + ".ROUTINES";
    public static final String META_TABLE_TRIGGERS = INFO_SCHEMA_NAME + ".TRIGGERS";
    public static final String META_TABLE_TABLE_CONSTRAINTS = INFO_SCHEMA_NAME + ".TABLE_CONSTRAINTS";
    public static final String META_TABLE_KEY_COLUMN_USAGE = INFO_SCHEMA_NAME + ".KEY_COLUMN_USAGE";
    public static final String META_TABLE_STATISTICS = INFO_SCHEMA_NAME + ".STATISTICS";
    public static final String META_TABLE_PARTITIONS = INFO_SCHEMA_NAME + ".PARTITIONS";
    public static final String META_TABLE_VIEWS = INFO_SCHEMA_NAME + ".VIEWS";


    public static final String COL_USER_ID = "USER_ID";
    public static final String COL_USER_NAME = "USERNAME";
    public static final String COL_OWNER = "OWNER";
    public static final String COL_TABLE_NAME = "TABLE_NAME";
    public static final String COL_COLUMN_ID = "COLUMN_ID";
    public static final String COL_COLUMN_NAME = "COLUMN_NAME";
    public static final String COL_COLUMN_POSITION = "COLUMN_POSITION";
    public static final String COL_DATA_TYPE = "DATA_TYPE";
    public static final String COL_DATA_LENGTH = "DATA_LENGTH";
    public static final String COL_NULLABLE = "NULLABLE";
    public static final String COL_DATA_PRECISION = "DATA_PRECISION";
    public static final String COL_DATA_SCALE = "DATA_SCALE";
    public static final String COL_DATA_DEFAULT = "DATA_DEFAULT";
    public static final String COL_COMMENTS = "COMMENTS";

    public static final String COL_CONSTRAINT_NAME = "CONSTRAINT_NAME";
    public static final String COL_CONSTRAINT_TYPE = "CONSTRAINT_TYPE";
    public static final String COL_SEARCH_CONDITION = "SEARCH_CONDITION";
    public static final String COL_POSITION = "POSITION";
    public static final String COL_STATUS = "STATUS";

    public static final String COL_INDEX_NAME = "INDEX_NAME";
    public static final String COL_INDEX_TYPE = "INDEX_TYPE";
    public static final String COL_UNIQUENESS = "UNIQUENESS";
    public static final String COL_DESCEND = "DESCEND";

    public static final String COL_TABLE_SCHEMA = "TABLE_SCHEMA";
    public static final String COL_TABLE_TYPE = "TABLE_TYPE";
    public static final String COL_ENGINE = "ENGINE";
    public static final String COL_VERSION = "VERSION";
    public static final String COL_TABLE_ROWS = "ROWS";
    public static final String COL_AUTO_INCREMENT = "AUTO_INCREMENT";
    public static final String COL_TABLE_COMMENT = "COMMENT";
    public static final String COL_COLUMNS_NAME = "COLUMNS_NAME";
    public static final String COL_ORDINAL_POSITION = "ORDINAL_POSITION";
    public static final String COL_CREATE_TIME = "CREATE_TIME";
    public static final String COL_UPDATE_TIME = "UPDATE_TIME";
    public static final String COL_CHECK_TIME = "CHECK_TIME";
    public static final String COL_COLLATION = "COLLATION";
    public static final String COL_COLLATION_NAME = "COLLATION_NAME";
    public static final String COL_AVG_ROW_LENGTH = "AVG_ROW_LENGTH";
    public static final String COL_SEQ_IN_INDEX = "SEQ_IN_INDEX";
    public static final String COL_NON_UNIQUE = "NON_UNIQUE";

    public static final String COL_COLUMN_KEY = "COLUMN_KEY";
    public static final String COL_CHARACTER_MAXIMUM_LENGTH = "CHARACTER_MAXIMUM_LENGTH";
    public static final String COL_CHARACTER_OCTET_LENGTH = "CHARACTER_OCTET_LENGTH";
    public static final String COL_IS_NULLABLE = "IS_NULLABLE";
    public static final String COL_IS_UPDATABLE = "IS_UPDATABLE";
    public static final String COL_COLUMN_COMMENT = "COLUMN_COMMENT";
    public static final String COL_COLUMN_EXTRA = "EXTRA";
    public static final String COL_COLUMN_TYPE = "COLUMN_TYPE";

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

    public static final String COL_TRIGGER_SCHEMA = "TRIGGER_SCHEMA";
	public static final String COL_TRIGGER_NAME = "TRIGGER_NAME";
	public static final String COL_TRIGGER_EVENT_MANIPULATION = "EVENT_MANIPULATION"; 	 
	public static final String COL_TRIGGER_EVENT_OBJECT_SCHEMA = "EVENT_OBJECT_SCHEMA";
	public static final String COL_TRIGGER_EVENT_OBJECT_TABLE = "EVENT_OBJECT_TABLE"; 	 
	public static final String COL_TRIGGER_ACTION_ORDER = "ACTION_ORDER";
	public static final String COL_TRIGGER_ACTION_CONDITION = "ACTION_CONDITION";
	public static final String COL_TRIGGER_ACTION_STATEMENT = "ACTION_STATEMENT"; 	 
	public static final String COL_TRIGGER_ACTION_ORIENTATION = "ACTION_ORIENTATION";
	public static final String COL_TRIGGER_ACTION_TIMING = "ACTION_TIMING"; 	 
	public static final String COL_TRIGGER_SQL_MODE = "SQL_MODE";
	public static final String COL_TRIGGER_DEFINER = "DEFINER";
	public static final String COL_TRIGGER_CHARACTER_SET_CLIENT = "CHARACTER_SET_CLIENT";
	public static final String COL_TRIGGER_COLLATION_CONNECTION = "COLLATION_CONNECTION";
	public static final String COL_TRIGGER_DATABASE_COLLATION = "DATABASE_COLLATION";
    
    public static final String CONSTRAINT_FOREIGN_KEY = "FOREIGN KEY";

    public static final DBSIndexType INDEX_TYPE_NORMAL = new DBSIndexType("NORMAL", "Normal");
    public static final DBSIndexType INDEX_TYPE_BITMAP = new DBSIndexType("BITMAP", "Bitmap");
    public static final DBSIndexType INDEX_TYPE_FUNCTION_BASED_NORMAL = new DBSIndexType("FUNCTION-BASED NORMAL", "Function-based Normal");
    public static final DBSIndexType INDEX_TYPE_FUNCTION_BASED_BITMAP = new DBSIndexType("FUNCTION-BASED BITMAP", "Function-based Bitmap");
    public static final DBSIndexType INDEX_TYPE_DOMAIN = new DBSIndexType("DOMAIN", "Domain");

    public static final String COL_PARTITION_NAME = "PARTITION_NAME";
    public static final String COL_SUBPARTITION_NAME = "SUBPARTITION_NAME";
    public static final String COL_PARTITION_ORDINAL_POSITION = "PARTITION_ORDINAL_POSITION";
    public static final String COL_SUBPARTITION_ORDINAL_POSITION = "SUBPARTITION_ORDINAL_POSITION";
    public static final String COL_PARTITION_METHOD = "PARTITION_METHOD";
    public static final String COL_SUBPARTITION_METHOD = "SUBPARTITION_METHOD";
    public static final String COL_PARTITION_EXPRESSION = "PARTITION_EXPRESSION";
    public static final String COL_SUBPARTITION_EXPRESSION = "SUBPARTITION_EXPRESSION";
    public static final String COL_PARTITION_DESCRIPTION = "PARTITION_DESCRIPTION";
    public static final String COL_PARTITION_COMMENT = "PARTITION_COMMENT";

    public static final String COL_MAX_DATA_LENGTH = "MAX_DATA_LENGTH";
    public static final String COL_INDEX_LENGTH = "INDEX_LENGTH";
    public static final String COL_NODEGROUP = "NODEGROUP";
    public static final String COL_DATA_FREE = "DATA_FREE";
    public static final String COL_CHECKSUM = "CHECKSUM";
    public static final String COL_CHECK_OPTION = "CHECK_OPTION";
    public static final String COL_VIEW_DEFINITION = "VIEW_DEFINITION";

    /**
     * Connection type
     */
    public static enum ConnectionType {
        BASIC,
        TNS,
        CUSTOM
    }

    public static enum ConnectionRole {
        NORMAL("Normal"),
        SYSDBA("SYSDBA"),
        SYSOPER("SYSOPER");
        private final String title;

        ConnectionRole(String title)
        {
            this.title = title;
        }

        public String getTitle()
        {
            return title;
        }
    }

    public static enum ObjectStatus {
        ENABLED,
        DISABLED,
    }

}
