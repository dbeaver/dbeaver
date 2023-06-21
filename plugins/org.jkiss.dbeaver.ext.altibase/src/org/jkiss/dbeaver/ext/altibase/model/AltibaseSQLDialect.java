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
        //{"AS", SQLConstants.BLOCK_END}, 
        /* AS ... END;
         * CREATE TYPESET type1
            AS
                TYPE rec1 IS RECORD (c1 INT, c2 INT);
                TYPE arr1 IS TABLE OF rec1 INDEX BY INT;
            END;
         */
    };
    
    private static final String[] ALTIBASE_INNER_BLOCK_PREFIXES = new String[]{
            "AS", "IS"
        };

    private static final String[] DDL_KEYWORDS = new String[] {
            "CREATE", "ALTER", "DROP", "EXECUTE"
    };

    public static final String[] OTHER_TYPES_FUNCTIONS = {
            //functions without parentheses
            "SYSDATE"
    };
    
    public static final String[] ADVANCED_KEYWORDS = {
            "REPLACE",
            "PACKAGE",
            "FUNCTION",
            "TYPE",
            "BODY",
            "RECORD",
            "TRIGGER",
            "MATERIALIZED",
            "IF",
            "EACH",
            "RETURN",
            "WRAPPED",
            "AFTER",
            "BEFORE",
            "DATABASE",
            "ANALYZE",
            "VALIDATE",
            "STRUCTURE",
            "COMPUTE",
            "STATISTICS",
            "LOOP",
            "WHILE",
            "BULK",
            "ELSIF",
            "EXIT",
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
        
        addFunctions(
                Arrays.asList(
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
                        ));
        
        for (String kw : ADVANCED_KEYWORDS) {
            addSQLKeyword(kw);
        }
        
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
        //return super.getScriptDelimiters();
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

        // Oracle SQL references could be found from https://docs.oracle.com/en/database/oracle/oracle-database/
        // by following through Get Started links till the SQL Language Reference link presented

        TokenPredicateSet conditions = TokenPredicateSet.of(
                // https://docs.oracle.com/en/database/oracle/oracle-database/12.2/lnpls/CREATE-PACKAGE-BODY-statement.html#GUID-68526FF2-96A1-4F14-A10B-4DD3E1CD80BE
                // also presented in the earliest found reference on 7.3, so considered as always supported https://docs.oracle.com/pdf/A32538_1.pdf
                new TokenPredicatesCondition(
                        SQLParserActionKind.BEGIN_BLOCK,
                        tt.sequence(
                                "CREATE",
                                tt.optional("OR", "REPLACE"),
                                tt.optional(tt.alternative("EDITIONABLE", "NONEDITIONABLE")),
                                "PACKAGE", "BODY"
                        ),
                        tt.sequence()
                ),
                // https://docs.oracle.com/en/database/oracle/oracle-database/21/sqlrf/CREATE-FUNCTION.html#GUID-156AEDAC-ADD0-4E46-AA56-6D1F7CA63306
                // https://docs.oracle.com/en/database/oracle/oracle-database/21/sqlrf/CREATE-PROCEDURE.html#GUID-771879D8-BBFD-4D87-8A6C-290102142DA3
                // not fully described, only some cases partially discovered
                new TokenPredicatesCondition(
                        SQLParserActionKind.SKIP_SUFFIX_TERM,
                        tt.sequence(
                                "CREATE",
                                tt.optional("OR", "REPLACE"),
                                tt.optional(tt.alternative("EDITIONABLE", "NONEDITIONABLE")),
                                tt.alternative("FUNCTION", "PROCEDURE")
                        ),
                        tt.sequence(tt.alternative(
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
    public String getSchemaExistQuery(String schemaName) {
        return "SELECT 1 FROM SYSTEM_.SYS_USERS_ WHERE USER_NAME='" + schemaName + "'";
    }

    @Override
    public String getCreateSchemaQuery(String schemaName) {
        return "CREATE USER \"" + schemaName + "\" IDENTIFIED BY \"" + schemaName + "\"";
    }
}
