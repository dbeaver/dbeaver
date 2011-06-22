/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.oracle;

import com.sun.xml.internal.bind.v2.schemagen.Util;
import org.jkiss.dbeaver.model.DBConstants;
import org.jkiss.dbeaver.model.struct.DBSIndexType;

/**
 * Oracle constants
 */
public class OracleConstants {

    public static final int DEFAULT_PORT = 1521;

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

    public static final String PROP_CONNECTION_TYPE = DBConstants.INTERNAL_PROP_PREFIX + "connection-type@";
    public static final String PROP_DRIVER_TYPE = DBConstants.INTERNAL_PROP_PREFIX + "driver-type@";
    public static final String PROP_INTERNAL_LOGON = "internal_logon";
    public static final String OS_AUTH_USER_NAME = "@OS_AUTH@";

    public static final String DRIVER_TYPE_THIN = "THIN";
    public static final String DRIVER_TYPE_OCI = "OCI";

    public static final String USER_PUBLIC = "PUBLIC";

    public static final String YES = "YES";

    public static final String TYPE_NAME_XML = "XMLTYPE";
    public static final String TYPE_FQ_XML = "SYS.XMLTYPE";

    public static final String COL_DATA_LENGTH = "DATA_LENGTH";
    public static final String COL_NULLABLE = "NULLABLE";
    public static final String COL_DATA_PRECISION = "DATA_PRECISION";
    public static final String COL_DATA_SCALE = "DATA_SCALE";
    public static final String COL_DATA_DEFAULT = "DATA_DEFAULT";
    public static final String COL_COMMENTS = "COMMENTS";

    public static final String COL_TABLE_ROWS = "ROWS";
    public static final String COL_CREATE_TIME = "CREATE_TIME";
    public static final String COL_UPDATE_TIME = "UPDATE_TIME";
    public static final String COL_CHECK_TIME = "CHECK_TIME";
    public static final String COL_AVG_ROW_LENGTH = "AVG_ROW_LENGTH";

    public static final DBSIndexType INDEX_TYPE_NORMAL = new DBSIndexType("NORMAL", "Normal");
    public static final DBSIndexType INDEX_TYPE_BITMAP = new DBSIndexType("BITMAP", "Bitmap");
    public static final DBSIndexType INDEX_TYPE_FUNCTION_BASED_NORMAL = new DBSIndexType("FUNCTION-BASED NORMAL", "Function-based Normal");
    public static final DBSIndexType INDEX_TYPE_FUNCTION_BASED_BITMAP = new DBSIndexType("FUNCTION-BASED BITMAP", "Function-based Bitmap");
    public static final DBSIndexType INDEX_TYPE_DOMAIN = new DBSIndexType("DOMAIN", "Domain");

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
    public static final String PROP_SOURCE_DEFINITION = "sourceDefinition";
    public static final String PROP_SOURCE_DECLARATION = "sourceDeclaration";


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

}
