/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2013-2017 Denis Forveille (titou10.titou10@gmail.com)
 * Copyright (C) 2010-2021 DBeaver Corp and others
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jkiss.dbeaver.ext.db2;

import org.jkiss.dbeaver.model.DBConstants;
import org.jkiss.dbeaver.model.data.DBDPseudoAttribute;
import org.jkiss.dbeaver.model.data.DBDPseudoAttributeType;

/**
 * DB2 constants
 * 
 * @author Denis Forveille
 */
public class DB2Constants {

    // Connection properties
    public static final int                DEFAULT_PORT                = 50000;

    public static final String             PROP_TRACE_ENABLED          = DBConstants.INTERNAL_PROP_PREFIX + "trace.enabled";
    public static final String             PROP_TRACE_FOLDER           = DBConstants.INTERNAL_PROP_PREFIX + "trace.folder";
    public static final String             PROP_TRACE_FILE             = DBConstants.INTERNAL_PROP_PREFIX + "trace.file";
    public static final String             PROP_TRACE_APPEND           = DBConstants.INTERNAL_PROP_PREFIX + "trace.append";
    public static final String             PROP_TRACE_LEVEL            = DBConstants.INTERNAL_PROP_PREFIX + "trace.level";
    public static final String             PROP_READ_ONLY              = "readOnly";

    // Explain Tables
    public static final String             EXPLAIN_SCHEMA_NAME_DEFAULT = "SYSTOOLS";

    // DB2 Versions
    public static final Double             DB2v9_1                     = 9.1;                                                      // Lowest
                                                                                                                                   // supported
                                                                                                                                   // version
    public static final Double             DB2v9_5                     = 9.5;
    public static final Double             DB2v9_7                     = 9.7;
    public static final Double             DB2v10_1                    = 10.1;
    public static final Double             DB2v10_5                    = 10.5;
    public static final Double             DB2v11_1                    = 11.1;

    public static final String             TYPE_NAME_DECFLOAT          = "DECFLOAT";
    public static final int                EXT_TYPE_DECFLOAT           = -100001;                                                  // DB2Types.DECFLOAT
    public static final int                EXT_TYPE_CURSOR             = -100008;                                                  // DB2Types.CURSOR

    // DB2BaseDataSource constants
    public static final int                TRACE_NONE                  = 0;
    public static final int                TRACE_CONNECTION_CALLS      = 1;
    public static final int                TRACE_STATEMENT_CALLS       = 2;
    public static final int                TRACE_RESULT_SET_CALLS      = 4;
    public static final int                TRACE_DRIVER_CONFIGURATION  = 16;
    public static final int                TRACE_CONNECTS              = 32;
    public static final int                TRACE_DRDA_FLOWS            = 64;
    public static final int                TRACE_RESULT_SET_META_DATA  = 128;
    public static final int                TRACE_PARAMETER_META_DATA   = 256;
    public static final int                TRACE_DIAGNOSTICS           = 512;
    public static final int                TRACE_SQLJ                  = 1024;
    public static final int                TRACE_XA_CALLS              = 2048;
    public static final int                TRACE_META_CALLS            = 8192;
    public static final int                TRACE_DATASOURCE_CALLS      = 16384;
    public static final int                TRACE_LARGE_OBJECT_CALLS    = 32768;
    public static final int                TRACE_T2ZOS                 = 65536;
    public static final int                TRACE_SYSTEM_MONITOR        = 131072;
    public static final int                TRACE_TRACEPOINTS           = 262144;
    public static final int                TRACE_ALL_EXTERNAL_CALLS    = 59399;
    public static final int                TRACE_ALL                   = -1;
    public static final int                TRACE_NOTSET                = 2147483647;

    // Display Categories
    public static final String             CAT_AUDIT                   = "Audit";
    public static final String             CAT_AUTH                    = "Authorities";
    public static final String             CAT_BASEBJECT               = "Base Object";
    public static final String             CAT_CLIENT                  = "Client";
    public static final String             CAT_CODE                    = "Code";
    public static final String             CAT_COLLATION               = "Collation";
    public static final String             CAT_COMPILER                = "Compiler";
    public static final String             CAT_DATETIME                = "Date & Time";
    public static final String             CAT_OWNER                   = "Owner";
    public static final String             CAT_SOURCE                  = "Source";
    public static final String             CAT_PERFORMANCE             = "Performance";
    public static final String             CAT_REMOTE                  = "Remote";
    public static final String             CAT_TEMPORAL                = "Temporal";
    public static final String             CAT_TABLESPACE              = "Tablespace";

    public static final String             PLAN_COST_FORMAT            = "###,###,###,##0.000";

    // Schema for system datatypes
    public static final String             SYSTEM_DATATYPE_SCHEMA      = "SYSIBM";
    public static final String             SYSTEM_CATALOG_SCHEMA       = "SYSCAT";

    // Preferences
    public static final String             PREF_KEY_DDL_FORMAT         = "db2.ddl.format";

    // Keywords

    public static final String[]           ADVANCED_KEYWORDS           = { "ALIAS", "ALLOW", "APPLICATION", "ASSOCIATE", "ASUTIME",
        "AUDIT", "AUTONOMOUS", "AUX", "AUXILIARY", "BEFORE", "BINARY", "BUFFERPOOL", "CACHE", "CALL", "CALLED", "CAPTURE",
        "CARDINALITY", "CCSID", "CLUSTER", "COLLECTION", "COLLID", "COMMENT", "COMPRESS", "CONCAT", "CONDITION", "CONTAINS",
        "COUNT_BIG", "CURRENT_LC_CTYPE", "CURRENT_PATH", "CURRENT_SERVER", "CURRENT_TIMEZONE", "CYCLE", "DATA", "DATABASE", "DAY",
        "DAYS", "DB2GENERAL", "DB2GENRL", "DB2SQL", "DBINFO", "DEFAULTS", "DEFINITION", "DETERMINISTIC", "DISALLOW", "DO",
        "DSNHATTR", "DSSIZE", "DYNAMIC", "EACH", "EDITPROC", "ELSEIF", "ENCODING", "END-EXEC1", "ENDING", "ERASE", "EVERY",
        "EXCLUDING", "EXCLUSIVE", "EXIT", "FENCED", "FIELDPROC", "FILE", "FINAL", "FREE", "FUNCTION", "GENERAL", "GENERATED",
        "GRAPHIC", "HANDLER", "HOLD", "HOUR", "HOURS", "IF", "INCLUDING", "INCLUSIVE", "INCREMENT", "INDEX", "INHERIT", "INOUT",
        "INTEGRITY", "ISOBID", "ITERATE", "JAR", "JAVA", "LABEL", "LC_CTYPE", "LEAVE", "LINKTYPE", "LOCALE", "LOCATOR", "LOCATORS",
        "LOCK", "LOCKMAX", "LOCKSIZE", "LONG", "LOOP", "MAXVALUE", "MICROSECOND", "MICROSECONDS", "MINUTE", "MINUTES", "MINVALUE",
        "MODE", "MODIFIES", "MONTH", "MONTHS", "NEW", "NEW_TABLE", "NOCACHE", "NOCYCLE", "NODENAME", "NODENUMBER", "NOMAXVALUE",
        "NOMINVALUE", "NOORDER", "NULLS", "NUMPARTS", "OBID", "OLD", "OLD_TABLE", "OPTIMIZATION", "OPTIMIZE", "ORGANIZE", "OUT",
        "OVERRIDING", "PACKAGE", "PARAMETER", "PART", "PARTITION", "PATH", "PIECESIZE", "PLAN", "PRIQTY", "PROGRAM", "PSID",
        "QUERYNO", "RANGE", "READS", "RECOVERY", "REFERENCING", "RELEASE", "RENAME", "REPEAT", "RESET", "RESIGNAL", "RESTART",
        "RESULT", "RESULT_SET_LOCATOR", "RETURN", "RETURNS", "ROUTINE", "ROW", "RRN", "RUN", "SAVEPOINT", "SCRATCHPAD", "SECOND",
        "SECONDS", "SECQTY", "SECURITY", "SENSITIVE", "SIGNAL", "SIMPLE", "SOURCE", "SPECIFIC", "SQLID", "STANDARD", "START",
        "STARTING", "STATIC", "STAY", "STOGROUP", "STORES", "STYLE", "SUBPAGES", "SYNONYM", "SYSTEM", "TABLESPACE",
        "TRIGGER", "TYPE", "UNDO", "UNTIL", "VALIDPROC", "VARIABLE", "VARIANT", "VCAT", "VOLATILE", "VOLUMES", "WHILE", "WLM",
        "IMPLICITLY", "HIDDEN" };

    public static final DBDPseudoAttribute PSEUDO_ATTR_RID_BIT         = new DBDPseudoAttribute(DBDPseudoAttributeType.ROWID,
        "RID_BIT()", "RID_BIT($alias)", "RID_BIT", "Unique physical row identifier", false);

    static final String[] ROUTINES = {
        "ABS",
        "ABSVAL",
        "ACOS",
        "ADD_DAYS",
        "ADD_HOURS",
        "ADD_MINUTES",
        "ADD_MONTHS",
        "ADD_SECONDS",
        "ADD_YEARS",
        "AGE",
        "ARRAY_DELETE",
        "ARRAY_FIRST",
        "ARRAY_LAST",
        "ARRAY_NEXT",
        "ARRAY_PRIOR",
        "ASCII",
        "ASCII_STR",
        "ASIN",
        "ATAN",
        "ATAN2",
        "ATANH",
        "BINARY",
        "BITAND",
        "BITANDNOT",
        "BITOR",
        "BITXOR",
        "BITNOT",
        "BSON_TO_JSON",
        "BTRIM",
        "CARDINALITY",
        "CEILING",
        "CEIL",
        "CHARACTER_LENGTH",
        "CHR",
        "COALESCE",
        "COLLATION_KEY",
        "COLLATION_KEY_BIT",
        "COMPARE_DECFLOAT",
        "CONCAT",
        "COS",
        "COSH",
        "COT",
        "CURSOR_ROWCOUNT",
        "DATAPARTITIONNUM",
        "DATE_PART",
        "DATE_TRUNC",
        "DAY",
        "DAYNAME",
        "DAYOFMONTH",
        "DAYOFWEEK",
        "DAYOFWEEK_ISO",
        "DAYOFYEAR",
        "DAYS",
        "DAYS_BETWEEN",
        "DAYS_TO_END_OF_MONTH",
        "DBPARTITIONNUM",
        "DECFLOAT_FORMAT",
        "DECODE",
        "DECRYPT_BIN and DECRYPT_CHAR",
        "DEGREES",
        "DEREF",
        "DIFFERENCE",
        "DIGITS",
        "EMPTY_BLOB",
        "EMPTY_CLOB",
        "EMPTY_DBCLOB",
        "EMPTY_NCLOB",
        "ENCRYPT",
        "EVENT_MON_STATE",
        "EXP",
        "EXTRACT",
        "FIRST_DAY",
        "FLOAT",
        "FLOAT4",
        "FLOAT8",
        "FLOOR",
        "FROM_UTC_TIMESTAMP",
        "GENERATE_UNIQUE",
        "GETHINT",
        "GRAPHIC",
        "GREATEST",
        "HASH",
        "HASH4",
        "HASH8",
        "HASHEDVALUE",
        "HEX",
        "HEXTORAW",
        "HOUR",
        "HOURS_BETWEEN",
        "IDENTITY_VAL_LOCAL",
        "IFNULL",
        "INITCAP",
        "INSERT",
        "INSTR",
        "INSTR2",
        "INSTR4",
        "INSTRB",
        "INTNAND",
        "INTNOR",
        "INTNXOR",
        "INTNNOT",
        "ISNULL",
        "JSON_ARRAY",
        "JSON_OBJECT",
        "JSON_QUERY",
        "JSON_TO_BSON",
        "JSON_VALUE",
        "JULIAN_DAY",
        "LAST_DAY",
        "LCASE",
        "LEAST",
        "LEFT",
        "LENGTH",
        "LENGTH2",
        "LENGTH4",
        "LENGTHB",
        "LN",
        "LOCATE",
        "LOCATE_IN_STRING",
        "LOG10",
        "LOWER",
        "LPAD",
        "LTRIM",
        "LTRIM",
        "MAX",
        "MAX_CARDINALITY",
        "MICROSECOND",
        "MIDNIGHT_SECONDS",
        "MIN",
        "MINUTE",
        "MINUTES_BETWEEN",
        "MOD",
        "MONTH",
        "MONTHNAME",
        "MONTHS_BETWEEN",
        "MULTIPLY_ALT",
        "NEXT_DAY",
        "NEXT_MONTH",
        "NEXT_QUARTER",
        "NEXT_WEEK",
        "NEXT_YEAR",
        "NORMALIZE_DECFLOAT",
        "NOW",
        "NULLIF",
        "NVL",
        "NVL2",
        "OCTET_LENGTH",
        "OVERLAY",
        "PARAMETER",
        "POSITION",
        "POSSTR",
        "POW",
        "POWER",
        "QUANTIZE",
        "QUARTER",
        "QUOTE_IDENT",
        "QUOTE_LITERAL",
        "RADIANS",
        "RAISE_ERROR",
        "RAND",
        "RANDOM",
        "RAWTOHEX",
        "REC2XML",
        "REGEXP_COUNT",
        "REGEXP_EXTRACT",
        "REGEXP_INSTR",
        "REGEXP_LIKE",
        "REGEXP_MATCH_COUNT",
        "REGEXP_REPLACE",
        "REGEXP_SUBSTR",
        "REPEAT",
        "REPLACE",
        "RIGHT",
        "ROUND",
        "ROUND_TIMESTAMP",
        "RPAD",
        "RTRIM",
        "SECLABEL",
        "SECLABEL_BY_NAME",
        "SECLABEL_TO_CHAR",
        "SECOND",
        "SECONDS_BETWEEN",
        "SIGN",
        "SIN",
        "SINH",
        "SOUNDEX",
        "SPACE",
        "SQRT",
        "STRIP",
        "STRLEFT",
        "STRPOS",
        "STRRIGHT",
        "SUBSTR",
        "SUBSTR2",
        "SUBSTR4",
        "SUBSTRB",
        "SUBSTRING",
        "TABLE_NAME",
        "TABLE_SCHEMA",
        "TAN",
        "TANH",
        "THIS_MONTH",
        "THIS_QUARTER",
        "THIS_WEEK",
        "THIS_YEAR",
        "TIMESTAMP_FORMAT",
        "TIMESTAMP_ISO",
        "TIMESTAMPDIFF",
        "TIMEZONE",
        "TO_CHAR",
        "TO_CLOB",
        "TO_DATE",
        "TO_HEX",
        "TO_MULTI_BYTE",
        "TO_NCHAR",
        "TO_NCLOB",
        "TO_NUMBER",
        "TO_SINGLE_BYTE",
        "TO_TIMESTAMP",
        "TO_UTC_TIMESTAMP",
        "TOTALORDER",
        "TRANSLATE",
        "TRIM",
        "TRIM_ARRAY",
        "TRUNC_TIMESTAMP",
        "TRUNC",
        "TYPE_ID",
        "TYPE_NAME",
        "TYPE_SCHEMA",
        "UCASE",
        "UNICODE_STR",
        "UPPER",
        "VALUE",
        "VARCHAR_BIT_FORMAT",
        "VARCHAR_FORMAT",
        "VARCHAR_FORMAT_BIT",
        "VERIFY_GROUP_FOR_USER",
        "VERIFY_ROLE_FOR_USER",
        "VERIFY_TRUSTED_CONTEXT_ROLE_FOR_USER",
        "WEEK",
        "WEEK_ISO",
        "WEEKS_BETWEEN",
        "WIDTH_BUCKET",
        "XMLATTRIBUTES",
        "XMLCOMMENT",
        "XMLCONCAT",
        "XMLDOCUMENT",
        "XMLELEMENT",
        "XMLFOREST",
        "XMLNAMESPACES",
        "XMLPARSE",
        "XMLPI",
        "XMLQUERY",
        "XMLROW",
        "XMLSERIALIZE",
        "XMLTEXT",
        "XMLVALIDATE",
        "XMLXSROBJECTID",
        "XSLTRANSFORM",
        "YEAR",
        "YEARS_BETWEEN",
        "YMD_BETWEEN",

    };
}
