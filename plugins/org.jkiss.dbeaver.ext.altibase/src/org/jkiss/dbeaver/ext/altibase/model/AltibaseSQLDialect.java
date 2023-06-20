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

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.ext.generic.model.GenericSQLDialect;
import org.jkiss.dbeaver.model.DBPKeywordType;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCDatabaseMetaData;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCDataSource;
import org.jkiss.dbeaver.model.sql.SQLConstants;
import org.jkiss.dbeaver.model.sql.SQLSyntaxManager;
import org.jkiss.dbeaver.model.sql.parser.SQLRuleManager;
import org.jkiss.dbeaver.model.sql.parser.SQLParserActionKind;
import org.jkiss.dbeaver.model.sql.parser.SQLTokenPredicateSet;
import org.jkiss.dbeaver.model.sql.parser.tokens.SQLTokenType;
import org.jkiss.dbeaver.model.sql.parser.tokens.predicates.TokenPredicateFactory;
import org.jkiss.dbeaver.model.sql.parser.tokens.predicates.TokenPredicateSet;
import org.jkiss.dbeaver.model.sql.parser.tokens.predicates.TokenPredicatesCondition;

import java.util.Arrays;

public class AltibaseSQLDialect extends GenericSQLDialect {

    private SQLTokenPredicateSet cachedDialectSkipTokenPredicates = null;
    
    private static final String[] ALTIBASE_BLOCK_HEADERS = new String[] {
            "EXECUTE BLOCK",
            "DECLARE",
            "IS",
    };

    private static final String[][] ALTIBASE_BEGIN_END_BLOCK = new String[][]{
        {SQLConstants.BLOCK_BEGIN, SQLConstants.BLOCK_END},
        {"LOOP", SQLConstants.BLOCK_END + " LOOP"},
        {SQLConstants.KEYWORD_CASE, SQLConstants.BLOCK_END + " " + SQLConstants.KEYWORD_CASE},
    };
    
    private static final String[] ALTIBASE_INNER_BLOCK_PREFIXES = new String[]{
            "AS",
            "IS",
        };

    private static final String[] DDL_KEYWORDS = new String[] {
            "CREATE", "ALTER", "DROP", "EXECUTE", "CACHE"
    };

    private static final String[] ALTIBASE_KEYWORDS = new String[] {
            "CURRENT_USER",
            "CURRENT_ROLE",
            "NCHAR",
            "VALUE"
    };

    public AltibaseSQLDialect() {
        super("Altibase", "altibase");
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
        turnFunctionIntoKeyword("TRUNCATE");
        addKeywords(Arrays.asList(ALTIBASE_KEYWORDS), DBPKeywordType.KEYWORD);
        
        cachedDialectSkipTokenPredicates = makeDialectSkipTokenPredicates(dataSource);
    }

    @Override
    public boolean supportsAliasInSelect() {
        return true;
    }

    @Override
    public boolean validIdentifierPart(char c, boolean quoted) {
        return super.validIdentifierPart(c, quoted) || c == '$';
    }

    @Override
    public boolean supportsInsertAllDefaultValuesStatement() {
        return false;
    }

    @Override
    public boolean supportsAliasInConditions() {
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
                                tt.alternative("FUNCTION", "PROCEDURE")
                        ),
                        tt.sequence()
                ),
                new TokenPredicatesCondition(
                    SQLParserActionKind.BEGIN_BLOCK,
                    tt.sequence(),
                    tt.sequence(tt.not("END"), "IF", tt.not("EXISTS"))
                )
        );



        if (dataSource.isServerVersionAtLeast(12, 1)) {
            // for WITH procedures and functions prepending select clause introduced in 12.1
            //     https://oracle-base.com/articles/12c/with-clause-enhancements-12cr1
            // notation presented in https://docs.oracle.com/en/database/oracle/oracle-database/18/sqlrf/SELECT.html
            // but missing in https://docs.oracle.com/cd/E11882_01/server.112/e41084/statements_10002.htm
            conditions.add(new TokenPredicatesCondition(
                    SQLParserActionKind.SKIP_SUFFIX_TERM,
                    tt.token("WITH"),
                    tt.sequence("END", ";")
            ));
        }

        return conditions;
    }
}
