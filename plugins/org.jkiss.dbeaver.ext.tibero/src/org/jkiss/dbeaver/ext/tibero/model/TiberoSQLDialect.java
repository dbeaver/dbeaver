/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2026 DBeaver Corp and others
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

package org.jkiss.dbeaver.ext.tibero.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.DBPDataKind;
import org.jkiss.dbeaver.model.DBPIdentifierCase;
import org.jkiss.dbeaver.model.DBPKeywordType;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCDatabaseMetaData;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCDataSource;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCSQLDialect;
import org.jkiss.dbeaver.model.impl.sql.BasicSQLDialect;
import org.jkiss.dbeaver.model.sql.SQLConstants;
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

public class TiberoSQLDialect extends JDBCSQLDialect {

    private static final String[] EXEC_KEYWORDS = {"EXEC", "EXECUTE", "CALL"};

    private static final String[] NON_TRANSACTIONAL_KEYWORDS = ArrayUtils.concatArrays(
        BasicSQLDialect.NON_TRANSACTIONAL_KEYWORDS,
        new String[]{"CREATE", "ALTER", "DROP", "ANALYZE", "VALIDATE"}
    );

    private static final String[] BLOCK_HEADERS = {"DECLARE", "PACKAGE"};

    private static final String[] INNER_BLOCK_PREFIXES = {"AS", "IS"};

    private static final String[][] BEGIN_END_BLOCK = {
        {SQLConstants.BLOCK_BEGIN, SQLConstants.BLOCK_END},
        {"LOOP", SQLConstants.BLOCK_END + " LOOP"},
        {SQLConstants.KEYWORD_CASE, SQLConstants.BLOCK_END + " " + SQLConstants.KEYWORD_CASE}
    };

    private static final String[] ADVANCED_KEYWORDS = {
        "AFTER", "ANALYZE", "BEFORE", "BODY", "BULK", "DATABASE", "EACH", "ELSIF",
        "EXCEPTION", "EXIT", "FUNCTION", "IF", "LOOP", "MATERIALIZED", "PACKAGE",
        "PROCEDURE", "RAISE", "REPLACE", "RETURN", "RETURNING", "ROWNUM", "ROWTYPE",
        "SEQUENCE", "SYNONYM", "TABLESPACE", "TRIGGER", "TYPE", "VALIDATE", "WHILE", "WRAPPED"
    };

    private static final List<String> FUNCTIONS = Arrays.asList(
        "ABS", "ADD_MONTHS", "ASCII", "BITAND", "CEIL", "CHR", "COALESCE", "CONCAT",
        "CURRENT_DATE", "CURRENT_TIMESTAMP", "DECODE", "DUMP", "EMPTY_BLOB", "EMPTY_CLOB",
        "EXTRACT", "FLOOR", "GREATEST", "INITCAP", "INSTR", "LAST_DAY", "LEAST", "LENGTH",
        "LISTAGG", "LOWER", "LPAD", "LTRIM", "MOD", "MONTHS_BETWEEN", "NEXT_DAY", "NULLIF",
        "NVL", "NVL2", "POWER", "RAWTOHEX", "REGEXP_COUNT", "REGEXP_INSTR", "REGEXP_REPLACE",
        "REGEXP_SUBSTR", "REPLACE", "ROUND", "RPAD", "RTRIM", "SIGN", "STDDEV", "SUBSTR",
        "SYS_CONTEXT", "SYS_GUID", "TO_CHAR", "TO_DATE", "TO_NUMBER", "TO_TIMESTAMP",
        "TRANSLATE", "TRIM", "TRUNC", "UPPER", "VARIANCE"
    );

    private static final List<String> DATA_TYPES = Arrays.asList(
        "BFILE", "BINARY_DOUBLE", "BINARY_FLOAT", "BLOB", "CHAR", "CLOB", "DATE", "FLOAT",
        "LONG", "LONG RAW", "NCHAR", "NCLOB", "NUMBER", "NVARCHAR2", "RAW", "ROWID",
        "TIMESTAMP", "TIMESTAMP WITH LOCAL TIME ZONE", "TIMESTAMP WITH TIME ZONE", "VARCHAR2"
    );

    private static final GlobalVariableInfo[] GLOBAL_VARIABLES = {
        new GlobalVariableInfo("SYSDATE", "Current database date and time", DBPDataKind.DATETIME),
        new GlobalVariableInfo("SYSTIMESTAMP", "Current database timestamp", DBPDataKind.DATETIME),
        new GlobalVariableInfo("CURRENT_DATE", "Current session date", DBPDataKind.DATETIME),
        new GlobalVariableInfo("CURRENT_TIMESTAMP", "Current session timestamp", DBPDataKind.DATETIME),
        new GlobalVariableInfo("DBTIMEZONE", "Database time zone", DBPDataKind.DATETIME),
        new GlobalVariableInfo("SESSIONTIMEZONE", "Session time zone", DBPDataKind.DATETIME),
        new GlobalVariableInfo("USER", "Current user name", DBPDataKind.STRING),
        new GlobalVariableInfo("UID", "Current user id", DBPDataKind.NUMERIC),
        new GlobalVariableInfo("ROWNUM", "Current row number", DBPDataKind.NUMERIC)
    };

    private SQLTokenPredicateSet cachedDialectSkipTokenPredicates;

    public TiberoSQLDialect() {
        super("Tibero", "tibero");
        setUnquotedIdentCase(DBPIdentifierCase.UPPER);
    }

    @Override
    public void initDriverSettings(JDBCSession session, JDBCDataSource dataSource, JDBCDatabaseMetaData metaData) {
        super.initDriverSettings(session, dataSource, metaData);
        addFunctions(FUNCTIONS);
        for (String keyword : ADVANCED_KEYWORDS) {
            addSQLKeyword(keyword);
        }
        addKeywords(Arrays.stream(GLOBAL_VARIABLES).map(GlobalVariableInfo::name).toList(), DBPKeywordType.OTHER);
        turnFunctionIntoKeyword("TRUNCATE");
        cachedDialectSkipTokenPredicates = makeDialectSkipTokenPredicates(dataSource);
    }

    @Override
    protected void loadDataTypesFromDatabase(JDBCDataSource dataSource) {
        super.loadDataTypesFromDatabase(dataSource);
        addDataTypes(DATA_TYPES);
    }

    @NotNull
    @Override
    public String[] getExecuteKeywords() {
        return EXEC_KEYWORDS;
    }

    @Override
    public String[] getBlockHeaderStrings() {
        return BLOCK_HEADERS;
    }

    @Nullable
    @Override
    public String[] getInnerBlockPrefixes() {
        return INNER_BLOCK_PREFIXES;
    }

    @Override
    public String[][] getBlockBoundStrings() {
        return BEGIN_END_BLOCK;
    }

    @NotNull
    @Override
    public GlobalVariableInfo[] getGlobalVariables() {
        return GLOBAL_VARIABLES;
    }

    @NotNull
    @Override
    public String[] getNonTransactionKeywords() {
        return NON_TRANSACTIONAL_KEYWORDS;
    }

    @NotNull
    @Override
    public String[] getScriptDelimiters() {
        return new String[]{";", "/"};
    }

    @Override
    public boolean isDelimiterAfterBlock() {
        return true;
    }

    @Override
    public String getDualTableName() {
        return "DUAL";
    }

    @Override
    public boolean supportsAliasInSelect() {
        return true;
    }

    @Override
    public boolean supportsAliasInUpdate() {
        return true;
    }

    @Override
    public boolean supportsAsKeywordBeforeAliasInFromClause() {
        return false;
    }

    @Override
    public boolean supportsAliasInConditions() {
        return false;
    }

    @Override
    public boolean supportsTableDropCascade() {
        return true;
    }

    @NotNull
    @Override
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

        return TokenPredicateSet.of(
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
                        "deterministic", "pipelined", "parallel_enable", "result_cache",
                        ")",
                        tt.sequence("procedure", SQLTokenType.T_OTHER),
                        tt.sequence(SQLTokenType.T_OTHER, SQLTokenType.T_TYPE)
                    ),
                    ";"
                )
            ),
            new TokenPredicatesCondition(
                SQLParserActionKind.BEGIN_BLOCK,
                tt.sequence(),
                tt.sequence(tt.not("END"), "IF", tt.not("EXISTS"))
            ),
            new TokenPredicatesCondition(
                SQLParserActionKind.BLOCK_HEADER,
                tt.sequence(
                    "CREATE",
                    tt.optional("OR", "REPLACE"),
                    tt.alternative("FUNCTION", "PROCEDURE", "PACKAGE")
                ),
                tt.alternative("AS", "IS")
            )
        );
    }
}
