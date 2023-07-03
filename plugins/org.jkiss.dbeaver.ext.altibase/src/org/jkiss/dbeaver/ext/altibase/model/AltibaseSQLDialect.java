/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2022 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.altibase.model;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.ext.altibase.AltibaseConstants;
import org.jkiss.dbeaver.model.DBPIdentifierCase;
import org.jkiss.dbeaver.model.DBPKeywordType;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCDatabaseMetaData;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCDataSource;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCSQLDialect;
import org.jkiss.dbeaver.model.impl.sql.BasicSQLDialect;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.model.sql.SQLConstants;
import org.jkiss.dbeaver.model.sql.SQLDialectDDLExtension;
import org.jkiss.dbeaver.model.sql.SQLDialectSchemaController;
import org.jkiss.dbeaver.model.sql.SQLSyntaxManager;
import org.jkiss.dbeaver.model.sql.parser.SQLParserActionKind;
import org.jkiss.dbeaver.model.sql.parser.SQLRuleManager;
import org.jkiss.dbeaver.model.sql.parser.SQLTokenPredicateSet;
import org.jkiss.dbeaver.model.sql.parser.tokens.SQLTokenType;
import org.jkiss.dbeaver.model.sql.parser.tokens.predicates.TokenPredicateFactory;
import org.jkiss.dbeaver.model.sql.parser.tokens.predicates.TokenPredicateSet;
import org.jkiss.dbeaver.model.sql.parser.tokens.predicates.TokenPredicatesCondition;
import org.jkiss.utils.ArrayUtils;

public class AltibaseSQLDialect extends JDBCSQLDialect 
            implements SQLDialectDDLExtension, SQLDialectSchemaController {

    private SQLTokenPredicateSet cachedDialectSkipTokenPredicates = null;
    private DBPPreferenceStore preferenceStore;
    
    private static final String[] ALTIBASE_NON_TRANSACTIONAL_KEYWORDS = ArrayUtils.concatArrays(
            BasicSQLDialect.NON_TRANSACTIONAL_KEYWORDS,
            new String[]{
                "CREATE", "ALTER", "DROP", "ANALYZE", "VALIDATE",
            }
        );
    
    private static final String[] ALTIBASE_BLOCK_HEADERS = new String[] {
            "DECLARE", "PACKAGE"
    };

    private static final String[][] ALTIBASE_BEGIN_END_BLOCK = new String[][]{
        {SQLConstants.BLOCK_BEGIN, SQLConstants.BLOCK_END},
        {"LOOP", SQLConstants.BLOCK_END + " LOOP"},
        {SQLConstants.KEYWORD_CASE, SQLConstants.BLOCK_END + " " + SQLConstants.KEYWORD_CASE},
    };
    
    private static final String[] ALTIBASE_INNER_BLOCK_PREFIXES = new String[]{
            "AS", "IS"
    };

    private static final String[] DDL_KEYWORDS = new String[] {
            "CREATE", "ALTER", "DROP"
    };

    public static final String[] OTHER_TYPES_FUNCTIONS = {
            //functions without parentheses
            "SYSDATE"
    };
    
    public static final String[] ALTIBASE_ONLY_KEYWORDS = new String[] {
            "ACCESS",           "AGER",             "APPLY",            "ARCHIVE",          "ARCHIVELOG",           //  5
            "AUDIT",            "AUTHID",           "AUTOEXTEND",       "BACKUP",           "BODY",                 // 10
            "BULK",             "CHECKPOINT",       "COMMENT",          "COMPILE",          "COMPRESS",             // 15
            "COMPRESSED",       "CONJOIN",          "CONNECT_BY_ROOT",  "CONSTANT",         "CURRENT_USER",         // 20
            "DATABASE",         "DECRYPT",          "DELAUDIT",         "DEQUEUE",          "DIRECTORY",            // 25
            "DISABLE",          "DISASTER",         "DISJOIN",          "DUMP_CALLSTACKS",  "ELSEIF",               // 30
            "ELSIF",            "ENABLE",           "ENQUEUE",          "EXIT",             "EXTENT",               // 35
            "EXTENTSIZE",       "FIFO",             "FIXED",            "FLASHBACK",        "FLUSH",                // 40
            "FLUSHER",          "IDENTIFIED",       "INITRANS",         "INSTEAD",          "KEEP",                 // 45
            "LESS",             "LIBRARY",          "LIFO",             "LINK",             "LINKER",               // 50
            "LOB",              "LOCALUNIQUE",      "LOCK",             "LOGANCHOR",        "LOGGING",              // 55
            "LOOP",             "MATERIALIZED",     "MAXROWS",          "MAXTRANS",         "MINUS",                // 60
            "MODE",             "MODIFY",           "MOVE",             "MOVEMENT",         "NOARCHIVELOG",         // 65
            "NOAUDIT",          "NOCOPY",           "NOCYCLE",          "NOLOGGING",        "NOPARALLEL",           // 70
            "OFF",              "OFFLINE",          "ONLINE",           "PACKAGE",          "PARALLEL",             // 75
            "PARAMETERS",       "PARTITIONS",       "PIVOT",            "PURGE",            "QUEUE",                // 80
            "RAISE",            "REBUILD",          "RECOVER",          "REMOTE_TABLE",     "REMOTE_TABLE_STORE",   // 85
            "REMOVE",           "REORGANIZE",       "REPLACE",          "REPLICATION",      "RETURNING",            // 90
            "ROWCOUNT",         "ROWNUM",           "ROWTYPE",          "SEGMENT",          "SHARD",                // 95
            "SHRINK_MEMPOOL",   "SPECIFICATION",    "SPLIT",            "SQLCODE",          "SQLERRM",              //100
            "STEP",             "STORAGE",          "STORE",            "SUPPLEMENTAL",     "SYNONYM",              //105
            "TABLESPACE",       "THAN",             "TOP",              "TRUNCATE",         "TYPESET",              //110
            "UNCOMPRESSED",     "UNLOCK",           "UNPIVOT",          "UNTIL",            "VARIABLE",             //115
            "VARIABLE_LARGE",   "VC2COLL",          "VOLATILE",         "WAIT",             "WHILE",                //120
            "WRAPPED",          "_PROWID",          
            "CACHE",            "NOCACHE" 
    };
    
    public static final String[] ALTIBASE_ONLY_FUNCTIONS = new String[] {
            "ACOS",                   "ADD_MONTHS",             "AESDECRYPT",             "AESENCRYPT",             "ASCII",                    //  5
            "ASCIISTR",               "ASIN",                   "ATAN",                   "ATAN2",                  "BASE64_DECODE",            // 10
            "BASE64_DECODE_STR",      "BASE64_ENCODE",          "BASE64_ENCODE_STR",      "BINARY_LENGTH",          "BIN_TO_NUM",               // 15
            "BITAND",                 "BITNOT",                 "BITOR",                  "BITXOR",                 "CASE2",                    // 20
            "CEIL",                   "CHOSUNG",                "CHR",                    "COALESCE",               "CONCAT",                   // 25
            "CONVERT",                "CONV_TIMEZONE",          "CORR",                   "COS",                    "COSH",                     // 30
            "COVAR_POP",              "COVAR_SAMP",             "CUME_DIST",              "CURRENT_DATE",           "CURRENT_TIMESTAMP",        // 35
            "DATEADD",                "DATEDIFF",               "DATENAME",               "DATE_TO_UNIX",           "DB_TIMEZONE",              // 40
            "DECODE",                 "DENSE_RANK",             "DESDECRYPT",             "DESENCRYPT",             "DIGEST",                   // 45
            "DIGITS",                 "DUMP",                   "EMPTY_BLOB",             "EMPTY_CLOB",             "EXP",                      // 50
            "EXTRACT",                "FIRST",                  "FIRST_VALUE",            "FIRST_VALUE_IGNORE_NULLS","GREATEST",                // 55
            "GROUPING",               "GROUPING_ID",            "GROUP_CONCAT",           "HEX_DECODE",             "HEX_ENCODE",               // 60
            "HEX_TO_NUM",             "HOST_NAME",              "INITCAP",                "INSTR",                  "ISNUMERIC",                // 65
            "LAG",                    "LAG_IGNORE_NULLS",       "LAST",                   "LAST_DAY",               "LAST_VALUE",               // 70
            "LAST_VALUE_IGNORE_NULLS","LEAD",                   "LEAD_IGNORE_NULLS",      "LEAST",                  "LISTAGG",                  // 75
            "LN",                     "LNNVL",                  "LOG",                    "LPAD",                   "LTRIM",                    // 80
            "MEDIAN",                 "MOD",                    "MONTHS_BETWEEN",         "MSG_CREATE_QUEUE",       "MSG_DROP_QUEUE",           // 85
            "MSG_RCV_QUEUE",          "MSG_SND_QUEUE",          "NCHR",                   "NEXT_DAY",               "NTH_VALUE",                // 90
            "NTH_VALUE_IGNORE_NULLS", "NTILE",                  "NULLIF",                 "NUMAND",                 "NUMOR",                    // 95
            "NUMSHIFT",               "NUMXOR",                 "NVL",                    "NVL2",                   "OCT_TO_NUM",               //100
            "PKCS7PAD16",             "PKCS7UNPAD16",           "QUOTE_PRINTABLE_DECODE", "QUOTE_PRINTABLE_ENCODE", "RAND",                     //105
            "RANDOM",                 "RANDOM_STRING",          "RANK",                   "RATIO_TO_REPORT",        "RAW_CONCAT",               //110
            "RAW_SIZEOF",             "RAW_TO_FLOAT",           "RAW_TO_INTEGER",         "RAW_TO_NUMERIC",         "RAW_TO_VARCHAR",           //115
            "REGEXP_COUNT",           "REGEXP_INSTR",           "REGEXP_REPLACE",         "REGEXP_SUBSTR",          "REPLACE2",                 //120
            "REPLICATE",              "REVERSE_STR",            "ROUND",                  "ROWNUM",                 "ROW_NUMBER",               //125
            "RPAD",                   "RTRIM",                  "SENDMSG",                "SESSION_ID",             "SESSION_TIMEZONE",         //130
            "SIGN",                   "SIN",                    "SINH",                   "SIZEOF",                 "STATS_ONE_WAY_ANOVA",      //135
            "STDDEV",                 "STDDEV_POP",             "STDDEV_SAMP",            "STUFF",                  "SUBRAW",                   //140
            "SUBSTRB",                "SYSTIMESTAMP",           "SYS_CONNECT_BY_PATH",    "SYS_CONTEXT",            "SYS_GUID_STR",             //145
            "TAN",                    "TANH",                   "TDESDECRYPT",            "TDESENCRYPT",            "TO_BIN",                   //150
            "TO_CHAR",                "TO_DATE",                "TO_HEX",                 "TO_INTERVAL",            "TO_NCHAR",                 //155
            "TO_NUMBER",              "TO_OCT",                 "TO_RAW",                 "TRANSLATE",              "TRIPLE_DESDECRYPT",        //160
            "TRIPLE_DESENCRYPT",      "TRUNC",                  "UNISTR",                 "UNIX_DATE",              "UNIX_TIMESTAMP",           //165
            "UNIX_TO_DATE",           "USER_ID",                "USER_LOCK_RELEASE",      "USER_LOCK_REQUEST",      "USER_NAME",                //170
            "VARIANCE",               "VAR_POP",                "VAR_SAMP",      
    };
    
    public AltibaseSQLDialect() {
        super("Altibase", "altibase");
        setUnquotedIdentCase(DBPIdentifierCase.UPPER);
    }

    @NotNull
    @Override
    public String[] getDDLKeywords() {
        return DDL_KEYWORDS;
    }

    @Override
    public String[] getBlockHeaderStrings() {
        return ALTIBASE_BLOCK_HEADERS;
    }
    

    @Nullable
    @Override
    public String[] getInnerBlockPrefixes() {
        return ALTIBASE_INNER_BLOCK_PREFIXES;
    }
    
    @Override
    public String[][] getBlockBoundStrings() {
        return ALTIBASE_BEGIN_END_BLOCK;
    }

    public void initDriverSettings(JDBCSession session, JDBCDataSource dataSource, JDBCDatabaseMetaData metaData) {
        super.initDriverSettings(session, dataSource, metaData);
        preferenceStore = dataSource.getContainer().getPreferenceStore();
        
        addFunctions(Arrays.asList(ALTIBASE_ONLY_FUNCTIONS));
        addSQLKeywords(Arrays.asList(ALTIBASE_ONLY_KEYWORDS));
        addKeywords(Arrays.asList(OTHER_TYPES_FUNCTIONS), DBPKeywordType.OTHER);
        turnFunctionIntoKeyword("TRUNCATE");
        
        cachedDialectSkipTokenPredicates = makeDialectSkipTokenPredicates(dataSource);
    }
    
    @Override
    protected void loadDataTypesFromDatabase(JDBCDataSource dataSource) {
        super.loadDataTypesFromDatabase(dataSource);
        addDataTypes(Stream.of(AltibaseDataTypeDomain.values())
                .map(AltibaseDataTypeDomain::name)
                .collect(Collectors.toList()));
    }
    
    @Nullable
    @Override
    public String getDualTableName() {
        return "DUAL";
    }

    @NotNull
    @Override
    public String[] getNonTransactionKeywords() {
        return ALTIBASE_NON_TRANSACTIONAL_KEYWORDS;
    }
    
    @NotNull
    @Override
    public String[] getScriptDelimiters() {
        return new String[]{";", "/"};
    }

    @Override
    public boolean supportsInsertAllDefaultValuesStatement() {
        return false;
    }

    @Override
    public boolean supportsAliasInConditions() {
        return false;
    }
    
    @Override
    public boolean supportsAliasInSelect() {
        return true;
    }
    
    @Override
    public boolean supportsTableDropCascade() {
        return true;
    }
    
    @Override
    @NotNull
    public SQLTokenPredicateSet getSkipTokenPredicates() {
        return cachedDialectSkipTokenPredicates == null ? super.getSkipTokenPredicates() : cachedDialectSkipTokenPredicates;
    }

    @NotNull
    private SQLTokenPredicateSet makeDialectSkipTokenPredicates(JDBCDataSource dataSource) {
        SQLSyntaxManager syntaxManager = new SQLSyntaxManager();
        syntaxManager.init(this, dataSource.getContainer().getPreferenceStore());
        SQLRuleManager ruleManager = new SQLRuleManager(syntaxManager);
        ruleManager.loadRules(dataSource, false);
        TokenPredicateFactory tt = TokenPredicateFactory.makeDialectSpecificFactory(ruleManager);

        TokenPredicateSet conditions = TokenPredicateSet.of(
                /* Oracle grammar */
                new TokenPredicatesCondition(
                        SQLParserActionKind.BEGIN_BLOCK,
                        tt.sequence(
                                "CREATE",
                                tt.optional("OR", "REPLACE"),
                                "PACKAGE", "BODY"
                        ),
                        tt.sequence()
                ),
                new TokenPredicatesCondition(
                        SQLParserActionKind.SKIP_SUFFIX_TERM,
                        tt.sequence(
                                "CREATE",
                                tt.optional("OR", "REPLACE"),
                                tt.alternative("FUNCTION", "PROCEDURE")
                        ),
                        tt.sequence(
                                tt.alternative(
                                        tt.sequence("RETURN", SQLTokenType.T_TYPE),
                                        "deterministor", "pipelined", "parallel_enable", "result_cache",
                                        ")",
                                        tt.sequence("procedure", SQLTokenType.T_OTHER),
                                        tt.sequence(SQLTokenType.T_OTHER, SQLTokenType.T_TYPE)
                                        ), ";")
                ),
                new TokenPredicatesCondition(
                    SQLParserActionKind.BEGIN_BLOCK,
                    tt.sequence(),
                    tt.sequence(tt.not("END"), "IF", tt.not("EXISTS"))
                ),
                /*
                 * Altibase only grammar
                 * Grammar conflict between PSM and Typeset
                 * PSM: CREATE...[AS|IS] ... BEGIN ... END
                 * TYPESET: CREATE...[AS|IS] ... END
                 */
                new TokenPredicatesCondition(
                        SQLParserActionKind.BEGIN_BLOCK,
                        tt.sequence(
                                "CREATE",
                                tt.optional("OR", "REPLACE"),
                                "TYPESET"
                        ),
                        tt.sequence()
                ),
                new TokenPredicatesCondition(
                        SQLParserActionKind.SKIP_SUFFIX_TERM,
                        tt.token("END"),
                        tt.sequence(";", "/")
                )
        );

        return conditions;
    }
    
    @Override
    public boolean isDisableScriptEscapeProcessing() {
        return false;
    }
    
    /*
     * Implements SQLDialectDDLExtension
     */
    @Override
    public String getAutoIncrementKeyword() {
        return null;
    }

    @Override
    public boolean supportsCreateIfExists() {
        return false;
    }

    @Override
    public String getTimestampDataType() {
        return AltibaseConstants.TYPE_NAME_DATE;
    }

    @Override
    public String getBigIntegerType() {
        return SQLConstants.DATA_TYPE_BIGINT;
    }

    @Override
    public String getClobDataType() {
        return AltibaseConstants.TYPE_NAME_CLOB;
    }

    @Override
    public String getUuidDataType() {
        // Not supported data type
        return "";
    }

    @Override
    public String getBooleanDataType() {
        // Not supported data type
        return "";
    }
    
    @Override
    public String getSchemaExistQuery(String schemaName) {
        return "SELECT 1 FROM SYSTEM_.SYS_USERS_ WHERE USER_NAME='" + schemaName + "'";
    }

    @Override
    public String getCreateSchemaQuery(String schemaName) {
        return "CREATE USER \"" + schemaName + "\" IDENTIFIED BY \"" + schemaName + "\"";
    }
    
    /******************************************************************************
     * Base data to generate Altibase only keyword and function list
     ******************************************************************************/
    private static final String[] ALTIBASE_KEYWORDS = {
            "ACCESS",             "ADD",                "AFTER",              "AGER",               "ALL",                  //5
            "ALTER",              "AND",                "ANY",                "APPLY",              "ARCHIVE",              //10
            "ARCHIVELOG",         "AS",                 "ASC",                "AT",                 "AUDIT",                //15
            "AUTHID",             "AUTOEXTEND",         "BACKUP",             "BEFORE",             "BEGIN",                //20
            "BETWEEN",            "BODY",               "BULK",               "BY",                 "CASCADE",              //25
            "CASE",               "CAST",               "CHECK",              "CHECKPOINT",         "CLOSE",                //30
            "COALESCE",           "COLUMN",             "COMMENT",            "COMMIT",             "COMPILE",              //35
            "COMPRESS",           "COMPRESSED",         "CONJOIN",            "CONNECT",            "CONNECT_BY_ROOT",      //40
            "CONSTANT",           "CONSTRAINT",         "CONSTRAINTS",        "CONTINUE",           "CREATE",               //45
            "CROSS",              "CUBE",               "CURRENT_USER",       "CURSOR",             "CYCLE",                //50
            "DATABASE",           "DECLARE",            "DECRYPT",            "DEFAULT",            "DEFINER",              //55
            "DELAUDIT",           "DELETE",             "DEQUEUE",            "DESC",               "DETERMINISTIC",        //60
            "DIRECTORY",          "DISABLE",            "DISASTER",           "DISCONNECT",         "DISJOIN",              //65
            "DISTINCT",           "DROP",               "DUMP_CALLSTACKS",    "EACH",               "ELSE",                 //70
            "ELSEIF",             "ELSIF",              "ENABLE",             "END",                "ENQUEUE",              //75
            "ESCAPE",             "EXCEPTION",          "EXEC",               "EXECUTE",            "EXISTS",               //80
            "EXIT",               "EXTENT",             "EXTENTSIZE",         "FALSE",              "FETCH",                //85
            "FIFO",               "FIXED",              "FLASHBACK",          "FLUSH",              "FLUSHER",              //90
            "FOLLOWING",          "FOR",                "FOREIGN",            "FROM",               "FULL",                 //95
            "FUNCTION",           "GOTO",               "GRANT",              "GROUP",              "HAVING",               //100
            "IDENTIFIED",         "IF",                 "IN",                 "INDEX",              "INITRANS",             //105
            "INNER",              "INSERT",             "INSTEAD",            "INTERSECT",          "INTO",                 //110
            "IS",                 "ISOLATION",          "JOIN",               "KEEP",               "KEY",                  //115
            "LANGUAGE",           "LATERAL",            "LEFT",               "LESS",               "LEVEL",                //120
            "LIBRARY",            "LIFO",               "LIKE",               "LIMIT",              "LINK",                 //125
            "LINKER",             "LOB",                "LOCAL",              "LOCALUNIQUE",        "LOCK",                 //130
            "LOGANCHOR",          "LOGGING",            "LOOP",               "MATERIALIZED",       "MAXROWS",              //135
            "MAXTRANS",           "MERGE",              "MINUS",              "MODE",               "MODIFY",               //140
            "MOVE",               "MOVEMENT",           "NEW",                "NOARCHIVELOG",       "NOAUDIT",              //145
            "NOCOPY",             "NOCYCLE",            "NOLOGGING",          "NOPARALLEL",         "NOT",                  //150
            "NULL",               "NULLS",              "OF",                 "OFF",                "OFFLINE",              //155
            "OLD",                "ON",                 "ONLINE",             "OPEN",               "OR",                   //160
            "ORDER",              "OTHERS",             "OUT",                "OUTER",              "OVER",                 //165
            "PACKAGE",            "PARALLEL",           "PARAMETERS",         "PARTITION",          "PARTITIONS",           //170
            "PIVOT",              "PRECEDING",          "PRIMARY",            "PRIOR",              "PRIVILEGES",           //175
            "PROCEDURE",          "PURGE",              "QUEUE",              "RAISE",              "READ",                 //180
            "REBUILD",            "RECOVER",            "REFERENCES",         "REFERENCING",        "REMOTE_TABLE",         //185
            "REMOTE_TABLE_STORE", "REMOVE",             "RENAME",             "REORGANIZE",         "REPLACE",              //190
            "REPLICATION",        "RETURN",             "RETURNING",          "REVOKE",             "RIGHT",                //195
            "ROLLBACK",           "ROLLUP",             "ROW",                "ROWCOUNT",           "ROWNUM",               //200
            "ROWTYPE",            "SAVEPOINT",          "SEGMENT",            "SELECT",             "SEQUENCE",             //205
            "SESSION",            "SET",                "SHARD",              "SHRINK_MEMPOOL",     "SOME",                 //210
            "SPECIFICATION",      "SPLIT",              "SQLCODE",            "SQLERRM",            "START",                //215
            "STATEMENT",          "STEP",               "STORAGE",            "STORE",              "SUPPLEMENTAL",         //220
            "SYNONYM",            "TABLE",              "TABLESPACE",         "TEMPORARY",          "THAN",                 //225
            "THEN",               "TO",                 "TOP",                "TRIGGER",            "TRUE",                 //230
            "TRUNCATE",           "TYPE",               "TYPESET",            "UNCOMPRESSED",       "UNION",                //235
            "UNIQUE",             "UNLOCK",             "UNPIVOT",            "UNTIL",              "UPDATE",               //240
            "USING",              "VALUES",             "VARIABLE",           "VARIABLE_LARGE",     "VC2COLL",              //245
            "VIEW",               "VOLATILE",           "WAIT",               "WHEN",               "WHENEVER",             //250
            "WHERE",              "WHILE",              "WITH",               "WITHIN",             "WORK",                 //255
            "WRAPPED",            "WRITE",              "_PROWID"
    };
    
    private static final String[] ALTIBASE_FUNCTIONS = new String[] {
            // Aggregation functions
            "AVG", "CORR", "COUNT", "COVAR_POP", "COVAR_SAMP", 
            "CUME_DIST", "FIRST", "GROUP_CONCAT", "LAST", "LISTAGG", 
            "MAX", "MIN", "PERCENTILE_CONT", "PERCENTILE_DISC", "PERCENT_RANK", 
            "RANK", "STATS_ONE_WAY_ANOVA", "STDDEV", "STDDEV_POP", "STDDEV_SAMP", 
            "SUM", "VARIANCE", "VAR_POP", "VAR_SAMP", "MEDIAN",
            // Window functions
            "LISTAGG", "RATIO_TO_REPORT", "GROUP_CONCAT", 
            "RANK", "DENSE_RANK", "ROW_NUMBER", "LAG", "LAG_IGNORE_NULLS", 
            "LEAD", "LEAD_IGNORE_NULLS", "NTILE", "FIRST", "LAST",
            "FIRST_VALUE", "FIRST_VALUE_IGNORE_NULLS", "LAST_VALUE", "LAST_VALUE_IGNORE_NULLS", "NTH_VALUE",
            "NTH_VALUE_IGNORE_NULLS",
            
            // Number
            "ABS", "ACOS", "ASIN", "ATAN", "ATAN2", 
            "CEIL", "COS", "COSH", "EXP", "FLOOR", 
            "ISNUMERIC", "LN", "LOG", "MOD", "NUMAND", 
            "NUMOR", "NUMSHIFT", "NUMXOR", "POWER", "RAND", 
            "RANDOM", "ROUND", "SIGN", "SIN", "SINH", 
            "SQRT", "TAN", "TANH", "TRUNC", "BITAND", 
            "BITOR", "BITXOR", "BITNOT",
            
            // Convert string
            "CHR", "CHOSUNG", "CONCAT", "DIGITS", "INITCAP", 
            "LOWER", "LPAD", "LTRIM", "NCHR", "PKCS7PAD16", 
            "PKCS7UNPAD16", "RANDOM_STRING", "REGEXP_COUNT", "REGEXP_REPLACE", "REPLICATE", 
            "REPLACE2", "REVERSE_STR", "RPAD", "RTRIM", "STUFF", "SUBSTRB", 
            "TRANSLATE", "TRIM", "UPPER", 
            
            // Convert number
            "ASCII", "CHAR_LENGTH", "DIGEST" ,"INSTR", "OCTET_LENGTH", 
            "REGEXP_INSTR", "REGEXP_SUBSTR", "SIZEOF",
            
            // Date
            "ADD_MONTHS", "DATEADD", "DATEDIFF", "DATENAME", "EXTRACT", 
            "LAST_DAY", "MONTHS_BETWEEN", "NEXT_DAY", "SESSION_TIMEZONE", "SYSTIMESTAMP", 
            "UNIX_DATE", "UNIX_TIMESTAMP", "CURRENT_DATE", "CURRENT_TIMESTAMP", "DB_TIMEZONE", 
            "CONV_TIMEZONE", "ROUND", "TRUNC",
            
            // Convert
            "ASCIISTR", "BIN_TO_NUM", "CONVERT", "DATE_TO_UNIX", "HEX_ENCODE", 
            "HEX_DECODE", "HEX_TO_NUM", "OCT_TO_NUM", "RAW_TO_FLOAT", "RAW_TO_INTEGER", 
            "RAW_TO_NUMERIC", "RAW_TO_VARCHAR", "TO_BIN", "TO_CHAR",  "TO_DATE", 
            "TO_HEX", "TO_INTERVAL", "TO_NCHAR", "TO_NUMBER", "TO_OCT", 
            "TO_RAW", "UNISTR", "UNIX_TO_DATE",
            
            // Encryption
            "AESDECRYPT", "AESENCRYPT", "DESENCRYPT", "DESDECRYPT", "TDESDECRYPT", "TRIPLE_DESDECRYPT", 
            "TDESENCRYPT", "TRIPLE_DESENCRYPT",
            
            // Etc.
            "BASE64_DECODE", "BASE64_DECODE_STR", "BASE64_ENCODE", "BASE64_ENCODE_STR", "BINARY_LENGTH", 
            "CASE2", "COALESCE", "DECODE", "DIGEST", 
            "DUMP", "EMPTY_BLOB", "EMPTY_CLOB", "GREATEST", "GROUPING", 
            "GROUPING_ID", "HOST_NAME", "LEAST", "LNNVL", "MSG_CREATE_QUEUE", 
            "MSG_DROP_QUEUE", "MSG_SND_QUEUE", "MSG_RCV_QUEUE", "NULLIF", "NVL", 
            "NVL2", "QUOTE_PRINTABLE_DECODE", "QUOTE_PRINTABLE_ENCODE", "RAW_CONCAT", "RAW_SIZEOF", 
            "ROWNUM", "SENDMSG", "USER_ID", "USER_NAME", "SESSION_ID", 
            "SUBRAW", "SYS_CONNECT_BY_PATH", "SYS_GUID_STR", "USER_LOCK_REQUEST", "USER_LOCK_RELEASE", 
            "SYS_CONTEXT"      
    };
    
    public static void main(String[] args) {
        filter(ALTIBASE_KEYWORDS, SQLConstants.SQL2003_RESERVED_KEYWORDS);
        filter(ALTIBASE_FUNCTIONS, SQLConstants.SQL2003_FUNCTIONS);
    }
    
    private static void filter(String[] dbProvideWords, String[] defaultWords) {

        // Remove duplicates if any
        defaultWords = Arrays.stream(defaultWords).distinct().toArray(String[]::new);
        dbProvideWords = Arrays.stream(dbProvideWords).distinct().toArray(String[]::new);
        
        // Find DB only words
        Set<String> defaultWordList = Set.of(defaultWords);
        final List<String> dbOnlyWordList = List.of(dbProvideWords).stream()
                .filter(e -> !defaultWordList.contains(e))
                .sorted()
                .collect(Collectors.toList());
        
        // Print out the result
        final int maxLen = dbOnlyWordList.stream()
                .mapToInt(String::length)
                .max().orElse(8);

        int i = 0;
        int numOfWordsLine = 5;
        String wordFormat = "%-" + (maxLen + 2) + "s";
        String numFormat = "\t//%3d";
        
        for(String dbOnlyWord:dbOnlyWordList) {
            System.out.print(String.format(wordFormat, "\"" + dbOnlyWord + "\","));
            if (++i % numOfWordsLine == 0)
                System.out.println(String.format(numFormat, i));
        }
        
        System.out.println();
    }


}
