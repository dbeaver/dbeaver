/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class AltibaseSQLDialect extends JDBCSQLDialect 
            implements SQLDialectDDLExtension, SQLDialectSchemaController {

    private SQLTokenPredicateSet cachedDialectSkipTokenPredicates = null;
    
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

    public static final String[] OTHER_TYPES_FUNCTIONS = {
        //functions without parentheses
        "SYSDATE"
    };
    
    public static final String[] ALTIBASE_ONLY_KEYWORDS = new String[] {
        "ACCESS",             "AGER",               //  2
        "APPLY",              "ARCHIVE",            //  4
        "ARCHIVELOG",         "AUDIT",              //  6
        "AUTHID",             "AUTOEXTEND",         //  8
        "BACKUP",             "BODY",               // 10
        "BULK",               "CACHE",              // 12
        "CHECKPOINT",         "COMMENT",            // 14
        "COMPILE",            "COMPRESS",           // 16
        "COMPRESSED",         "CONJOIN",            // 18
        "CONNECT_BY_ROOT",    "CONSTANT",           // 20
        "CURRENT_USER",       "DATABASE",           // 22
        "DECRYPT",            "DELAUDIT",           // 24
        "DEQUEUE",            "DIRECTORY",          // 26
        "DISABLE",            "DISASTER",           // 28
        "DISJOIN",            "DUMP_CALLSTACKS",    // 30
        "ELSEIF",             "ELSIF",              // 32
        "ENABLE",             "ENQUEUE",            // 34
        "EXIT",               "EXTENT",             // 36
        "EXTENTSIZE",         "FIFO",               // 38
        "FIXED",              "FLASHBACK",          // 40
        "FLUSH",              "FLUSHER",            // 42
        "IDENTIFIED",         "INITRANS",           // 44
        "INSTEAD",            "KEEP",               // 46
        "LESS",               "LIBRARY",            // 48
        "LIFO",               "LINK",               // 50
        "LINKER",             "LOB",                // 52
        "LOCALUNIQUE",        "LOCK",               // 54
        "LOGANCHOR",          "LOGGING",            // 56
        "LOOP",               "MATERIALIZED",       // 58
        "MAXROWS",            "MAXTRANS",           // 60
        "MINUS",              "MODE",               // 62
        "MODIFY",             "MOVE",               // 64
        "MOVEMENT",           "NOARCHIVELOG",       // 66
        "NOAUDIT",            "NOCACHE",            // 68
        "NOCOPY",             "NOCYCLE",            // 70
        "NOLOGGING",          "NOPARALLEL",         // 72
        "OFF",                "OFFLINE",            // 74
        "ONLINE",             "PACKAGE",            // 76
        "PARALLEL",           "PARAMETERS",         // 78
        "PARTITIONS",         "PIVOT",              // 80
        "PURGE",              "QUEUE",              // 82
        "RAISE",              "REBUILD",            // 84
        "RECOVER",            "REMOTE_TABLE",       // 86
        "REMOTE_TABLE_STORE", "REMOVE",             // 88
        "REORGANIZE",         "REPLACE",            // 90
        "REPLICATION",        "RETURNING",          // 92
        "ROWCOUNT",           "ROWNUM",             // 94
        "ROWTYPE",            "SEGMENT",            // 96
        "SHARD",              "SHRINK_MEMPOOL",     // 98
        "SPECIFICATION",      "SPLIT",              //100
        "SQLCODE",            "SQLERRM",            //102
        "STEP",               "STORAGE",            //104
        "STORE",              "SUPPLEMENTAL",       //106
        "SYNONYM",            "TABLESPACE",         //108
        "THAN",               "TOP",                //110
        "TRUNCATE",           "TYPESET",            //112
        "UNCOMPRESSED",       "UNLOCK",             //114
        "UNPIVOT",            "UNTIL",              //116
        "VARIABLE",           "VARIABLE_LARGE",     //118
        "VC2COLL",            "VOLATILE",           //120
        "WAIT",               "WHILE",              //122
        "WRAPPED",            "_PROWID",            //124
    };
    
    public static final String[] ALTIBASE_ONLY_FUNCTIONS = new String[] {
        "ACOS",                    "ADD_MONTHS",                //  2
        "AESDECRYPT",              "AESENCRYPT",                //  4
        "ASCII",                   "ASCIISTR",                  //  6
        "ASIN",                    "ATAN",                      //  8
        "ATAN2",                   "BASE64_DECODE",             // 10
        "BASE64_DECODE_STR",       "BASE64_ENCODE",             // 12
        "BASE64_ENCODE_STR",       "BINARY_LENGTH",             // 14
        "BIN_TO_NUM",              "BITAND",                    // 16
        "BITNOT",                  "BITOR",                     // 18
        "BITXOR",                  "CASE2",                     // 20
        "CEIL",                    "CHOSUNG",                   // 22
        "CHR",                     "COALESCE",                  // 24
        "CONCAT",                  "CONVERT",                   // 26
        "CONV_TIMEZONE",           "CORR",                      // 28
        "COS",                     "COSH",                      // 30
        "COVAR_POP",               "COVAR_SAMP",                // 32
        "CUME_DIST",               "CURRENT_DATE",              // 34
        "CURRENT_TIMESTAMP",       "DATEADD",                   // 36
        "DATEDIFF",                "DATENAME",                  // 38
        "DATE_TO_UNIX",            "DB_TIMEZONE",               // 40
        "DECODE",                  "DENSE_RANK",                // 42
        "DESDECRYPT",              "DESENCRYPT",                // 44
        "DIGEST",                  "DIGITS",                    // 46
        "DUMP",                    "EMPTY_BLOB",                // 48
        "EMPTY_CLOB",              "EXP",                       // 50
        "EXTRACT",                 "FIRST",                     // 52
        "FIRST_VALUE",             "FIRST_VALUE_IGNORE_NULLS",  // 54
        "GREATEST",                "GROUPING",                  // 56
        "GROUPING_ID",             "GROUP_CONCAT",              // 58
        "HEX_DECODE",              "HEX_ENCODE",                // 60
        "HEX_TO_NUM",              "HOST_NAME",                 // 62
        "INITCAP",                 "INSTR",                     // 64
        "ISNUMERIC",               "LAG",                       // 66
        "LAG_IGNORE_NULLS",        "LAST",                      // 68
        "LAST_DAY",                "LAST_VALUE",                // 70
        "LAST_VALUE_IGNORE_NULLS", "LEAD",                      // 72
        "LEAD_IGNORE_NULLS",       "LEAST",                     // 74
        "LISTAGG",                 "LN",                        // 76
        "LNNVL",                   "LOG",                       // 78
        "LPAD",                    "LTRIM",                     // 80
        "MEDIAN",                  "MOD",                       // 82
        "MONTHS_BETWEEN",          "MSG_CREATE_QUEUE",          // 84
        "MSG_DROP_QUEUE",          "MSG_RCV_QUEUE",             // 86
        "MSG_SND_QUEUE",           "NCHR",                      // 88
        "NEXT_DAY",                "NTH_VALUE",                 // 90
        "NTH_VALUE_IGNORE_NULLS",  "NTILE",                     // 92
        "NULLIF",                  "NUMAND",                    // 94
        "NUMOR",                   "NUMSHIFT",                  // 96
        "NUMXOR",                  "NVL",                       // 98
        "NVL2",                    "OCT_TO_NUM",                //100
        "PKCS7PAD16",              "PKCS7UNPAD16",              //102
        "QUOTE_PRINTABLE_DECODE",  "QUOTE_PRINTABLE_ENCODE",    //104
        "RAND",                    "RANDOM",                    //106
        "RANDOM_STRING",           "RANK",                      //108
        "RATIO_TO_REPORT",         "RAW_CONCAT",                //110
        "RAW_SIZEOF",              "RAW_TO_FLOAT",              //112
        "RAW_TO_INTEGER",          "RAW_TO_NUMERIC",            //114
        "RAW_TO_VARCHAR",          "REGEXP_COUNT",              //116
        "REGEXP_INSTR",            "REGEXP_REPLACE",            //118
        "REGEXP_SUBSTR",           "REPLACE2",                  //120
        "REPLICATE",               "REVERSE_STR",               //122
        "ROUND",                   "ROWNUM",                    //124
        "ROW_NUMBER",              "RPAD",                      //126
        "RTRIM",                   "SENDMSG",                   //128
        "SESSION_ID",              "SESSION_TIMEZONE",          //130
        "SIGN",                    "SIN",                       //132
        "SINH",                    "SIZEOF",                    //134
        "STATS_ONE_WAY_ANOVA",     "STDDEV",                    //136
        "STDDEV_POP",              "STDDEV_SAMP",               //138
        "STUFF",                   "SUBRAW",                    //140
        "SUBSTRB",                 "SYSTIMESTAMP",              //142
        "SYS_CONNECT_BY_PATH",     "SYS_CONTEXT",               //144
        "SYS_GUID_STR",            "TAN",                       //146
        "TANH",                    "TDESDECRYPT",               //148
        "TDESENCRYPT",             "TO_BIN",                    //150
        "TO_CHAR",                 "TO_DATE",                   //152
        "TO_HEX",                  "TO_INTERVAL",               //154
        "TO_NCHAR",                "TO_NUMBER",                 //156
        "TO_OCT",                  "TO_RAW",                    //158
        "TRANSLATE",               "TRIPLE_DESDECRYPT",         //160
        "TRIPLE_DESENCRYPT",       "TRUNC",                     //162
        "UNISTR",                  "UNIX_DATE",                 //164
        "UNIX_TIMESTAMP",          "UNIX_TO_DATE",              //166
        "USER_ID",                 "USER_LOCK_RELEASE",         //168
        "USER_LOCK_REQUEST",       "USER_NAME",                 //170
        "VARIANCE",                "VAR_POP",                   //172
        "VAR_SAMP",                
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
        "ASCII", "CHAR_LENGTH", "DIGEST", "INSTR", "OCTET_LENGTH", 
        "REGEXP_INSTR", "REGEXP_SUBSTR", "SIZEOF",

        // Date
        "ADD_MONTHS", "DATEADD", "DATEDIFF", "DATENAME", "EXTRACT", 
        "LAST_DAY", "MONTHS_BETWEEN", "NEXT_DAY", "SESSION_TIMEZONE", "SYSTIMESTAMP", 
        "UNIX_DATE", "UNIX_TIMESTAMP", "CURRENT_DATE", "CURRENT_TIMESTAMP", "DB_TIMEZONE", 
        "CONV_TIMEZONE", "ROUND", "TRUNC",

        // Convert
        "ASCIISTR", "BIN_TO_NUM", "CONVERT", "DATE_TO_UNIX", "HEX_ENCODE", 
        "HEX_DECODE", "HEX_TO_NUM", "OCT_TO_NUM", "RAW_TO_FLOAT", "RAW_TO_INTEGER", 
        "RAW_TO_NUMERIC", "RAW_TO_VARCHAR", "TO_BIN", "TO_CHAR", "TO_DATE", 
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
        "WRAPPED",            "WRITE",              "_PROWID",            "CACHE",              "NOCACHE"
    };
    
    public AltibaseSQLDialect() {
        super("Altibase", "altibase");
        setUnquotedIdentCase(DBPIdentifierCase.UPPER);
    }

    @NotNull
    @Override
    public String[] getDDLKeywords() {
        return super.getDDLKeywords();
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

    /**
     * Register Altibase functions, SQL keywords, and so on.
     */
    public void initDriverSettings(JDBCSession session, JDBCDataSource dataSource, JDBCDatabaseMetaData metaData) {
        super.initDriverSettings(session, dataSource, metaData);
        
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
    public String getClobComparingPart(@NotNull String columnName) {
        return "DBMS_LOB.COMPARE(%s,?) = 0".formatted(columnName);
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
                 * 
                 * https://github.com/ALTIBASE/Documents/blob/master/Manuals/Altibase_7.3/eng/Stored%20Procedures%20Manual.md#create-typeset
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
                        tt.sequence(";")
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
        return AltibaseDataTypeDomain.DATE.getTypeName();
    }

    @Override
    public String getBigIntegerType() {
        return AltibaseDataTypeDomain.BIGINT.getTypeName();
    }

    @Override
    public String getBlobDataType() {
        return AltibaseDataTypeDomain.BLOB.getTypeName();
    }
    
    @Override
    public String getClobDataType() {
        return AltibaseDataTypeDomain.CLOB.getTypeName();
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

    @NotNull
    @Override
    public String getAlterColumnOperation() {
        return AltibaseConstants.OPERATION_MODIFY;
    }

    @Override
    public boolean supportsAlterColumnSet() {
        return false;
    }

    @Override
    public boolean supportsAlterHasColumn() {
        return false;
    }

    @NotNull
    @Override
    public String getSchemaExistQuery(@NotNull String schemaName) {
        return "SELECT 1 FROM SYSTEM_.SYS_USERS_ WHERE USER_NAME='" + schemaName + "'";
    }

    @NotNull
    @Override
    public String getCreateSchemaQuery(@NotNull String schemaName) {
        return "CREATE USER \"" + schemaName + "\" IDENTIFIED BY \"" + schemaName + "\"";
    }
    
    /**
     * Extracting and Formatting Altibase only keywords and functions 
     * for ALTIBASE_ONLY_KEYWORDS and ALTIBASE_ONLY_FUNCTIONS.
     */
    public static void main(String[] args) {
        filter(ALTIBASE_KEYWORDS, SQLConstants.SQL2003_RESERVED_KEYWORDS);
        filter(ALTIBASE_FUNCTIONS, SQLConstants.SQL2003_FUNCTIONS);
    }
    
    private static void filter(String[] dbProvideNames, String[] sqlProvidedNames) {
        
        if (!AltibaseConstants.DEBUG) {
            return;
        }
        
        String[] sqlDistinctNames = null;
        String[] dbDistinctNames = null;
        
        // Remove duplicates if any
        sqlDistinctNames = Arrays.stream(sqlProvidedNames).distinct().toArray(String[]::new);
        dbDistinctNames = Arrays.stream(dbProvideNames).distinct().toArray(String[]::new);
        
        // Find DB only words
        Set<String> defaultWordList = Set.of(sqlDistinctNames);
        final List<String> dbOnlyWordList = List.of(dbDistinctNames).stream()
                .filter(e -> !defaultWordList.contains(e))
                .sorted()
                .collect(Collectors.toList());
        
        // Print out the result
        int maxLen = dbOnlyWordList.stream()
                .mapToInt(String::length)
                .max().orElse(8);

        maxLen++;
        
        int i = 0;
        int numOfWordsLine = 2;
        String wordFormat = "%-" + (maxLen + 2) + "s";
        String numFormat = "\t//%3d";
        
        for (String dbOnlyWord : dbOnlyWordList) {
            System.out.print(String.format(wordFormat, "\"" + dbOnlyWord + "\","));
            if (++i % numOfWordsLine == 0) {
                System.out.println(String.format(numFormat, i));
            }
        }
        
        System.out.println();
    }

    @Override
    public boolean supportsNoActionIndex() {
        return false;
    }
}
