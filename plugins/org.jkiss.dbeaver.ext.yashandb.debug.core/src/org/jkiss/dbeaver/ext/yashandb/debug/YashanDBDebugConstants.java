
package org.jkiss.dbeaver.ext.yashandb.debug;

import org.jkiss.dbeaver.debug.DBGConstants;

import java.util.ArrayList;
import java.util.List;

public class YashanDBDebugConstants {
    public static final String ATTR_ATTACH_KIND = "YashanDB.ATTR_ATTACH_KIND"; //$NON-NLS-1$
    public static final String ATTR_ATTACH_PROCESS = "YashanDB.ATTACH_PROCESS"; //$NON-NLS-1$
    public static final String ATTR_DATABASE_NAME = "YashanDB.ATTR_DATABASE_NAME"; //$NON-NLS-1$
    public static final String ATTR_SCHEMA_NAME = "YashanDB.ATTR_SCHEMA_NAME"; //$NON-NLS-1$
    public static final String ATTR_FUNCTION_OID = "YashanDB.ATTR_FUNCTION_OID"; //$NON-NLS-1$
    public static final String ATTR_FUNCTION_NAME = "YashanDB.ATTR_FUNCTION_ONAME";//$NON-NLS-1$
    public static final String ATTR_FUNCTION_PARAMETERS = "YashanDB.ATTR_FUNCTION_PARAMETERS"; //$NON-NLS-1$
    public static final String ATTR_FUNCTION_PARAMETERS_TYPE="YASHANDB.ATTR_FUNCTION_PARAMETERS_TYPE";


    public static final String ATTACH_KIND_LOCAL = "LOCAL"; //$NON-NLS-1$
    public static final String ATTACH_KIND_GLOBAL = "GLOBAL"; //$NON-NLS-1$

    public static final String DEBUG_TYPE_FUNCTION = "org.jkiss.dbeaver.ext.yashandb.debug.function"; //$NON-NLS-1$
    public static final String LINE_NUMBER = "lineNumber"; //$NON-NLS-1$

    public static final String attrFunctionOid = YashanDBDebugConstants.ATTR_FUNCTION_OID;//$NON-NLS-1$
    public static final String breakpointAttributeObjectName = DBGConstants.BREAKPOINT_ATTRIBUTE_OBJECT_NAME;//$NON-NLS-1$
    public static final String path = DBGConstants.BREAKPOINT_ATTRIBUTE_NODE_PATH;//$NON-NLS-1$
    public static final String YASHANDB_DRIVER = "com.yashandb.jdbc.Driver";//$NON-NLS-1$

    public static final String YASHANDB_TINYINT = "TINYINT";
    public static final String YASHANDB_SMALLINT = "SMALLINT";
    public static final String YASHANDB_INT = "INT";
    public static final String YASHANDB_INTEGER = "INTEGER";
    public static final String YASHANDB_BIGINT = "BIGINT";
    public static final String YASHANDB_FLOAT = "FLOAT";
    public static final String YASHANDB_DOUBLE = "DOUBLE";
    public static final String YASHANDB_NUMBER = "NUMBER";
    public static final String YASHANDB_CHAR = "CHAR";
    public static final String YASHANDB_VARCHAR = "VARCHAR";
    public static final String YASHANDB_NCHAR = "NCHAR";
    public static final String YASHANDB_NVARCHAR = "NVARCHAR";
    public static final String YASHANDB_BOOLEAN = "BOOLEAN";
    public static final String YASHANDB_DATE = "DATE";
    public static final String YASHANDB_TIME = "TIME";
    public static final String YASHANDB_TIMESTAMP = "TIMESTAMP";
    public static final String YASHANDB_INTERVAL_YEAR_TO_MONTH = "INTERVAL YEAR TO MONTH";
    public static final String YASHANDB_INTERVAL_DAY_TO_SECOND = "INTERVAL DAY TO SECOND";
    public static final String YASHANDB_BLOB = "BLOB";
    public static final String YASHANDB_CLOB = "CLOB";
    public static final String YASHANDB_NCLOB = "NCLOB";
    public static final String YASHANDB_RAW = "RAW";
    public static final String YASHANDB_JSON = "JSON";
    public static final String YASHANDB_ROWID = "ROWID";
    public static final String YASHANDB_UROWID = "UROWID";
    public static final String YASHANDB_GEOMETRY = "GEOMETRY";


}
