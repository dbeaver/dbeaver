/*
 * DBeaver - Universal Database Manager
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
package org.jkiss.dbeaver.model.sql.parser;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.DBPKeywordType;
import org.jkiss.dbeaver.model.sql.SQLConstants;
import org.jkiss.dbeaver.model.sql.SQLDialect;
import org.jkiss.dbeaver.model.sql.SQLSyntaxManager;
import org.jkiss.dbeaver.model.sql.parser.rules.*;
import org.jkiss.dbeaver.model.sql.parser.tokens.*;
import org.jkiss.dbeaver.model.sql.registry.SQLCommandHandlerDescriptor;
import org.jkiss.dbeaver.model.sql.registry.SQLCommandsRegistry;
import org.jkiss.dbeaver.model.text.parser.*;
import org.jkiss.dbeaver.model.text.parser.rules.*;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.utils.ArrayUtils;
import org.jkiss.utils.CommonUtils;
import org.jkiss.utils.Pair;

import java.util.ArrayList;
import java.util.List;

/**
 * SQLRuleManager.
 *
 * Contains information about some concrete datasource underlying database syntax.
 * Support runtime change of datasource (reloads syntax information)
 */
public class SQLRuleManager {

    @NotNull
    private TPRule[] allRules = new TPRule[0];
    @NotNull
    private SQLSyntaxManager syntaxManager;

    public SQLRuleManager(@NotNull SQLSyntaxManager syntaxManager) {
        this.syntaxManager = syntaxManager;
    }

    @NotNull
    public TPRule[] getAllRules() {
        return allRules;
    }

    @NotNull
    public TPRule[] getRulesByType(@NotNull SQLTokenType requiredType) {
        final List<TPRule> rules = new ArrayList<>();
        for (TPRule rule : allRules) {
            if (rule instanceof TPPredicateRule) {
                final TPPredicateRule predicateRule = (TPPredicateRule) rule;
                if (predicateRule.getSuccessToken() instanceof TPTokenAbstract) {
                    final TPTokenAbstract<?> token = (TPTokenAbstract<?>) predicateRule.getSuccessToken();
                    final Object tokenData = token.getData();
                    if (tokenData instanceof TPTokenType && ((TPTokenType) tokenData).getTokenType() == requiredType.getTokenType()) {
                        rules.add(rule);
                    }
                }
            }
        }
        return rules.toArray(new TPRule[0]);
    }

    public void loadRules(@Nullable DBPDataSource dataSource, boolean minimalRules) {
        SQLDialect dialect = syntaxManager.getDialect();
        TPRuleProvider ruleProvider = GeneralUtils.adapt(dialect, TPRuleProvider.class);
        DBPDataSourceContainer dataSourceContainer = dataSource == null ? null : dataSource.getContainer();

        final TPToken keywordToken = new TPTokenDefault(SQLTokenType.T_KEYWORD);
        final TPToken typeToken = new TPTokenDefault(SQLTokenType.T_TYPE);
        final TPToken stringToken = new TPTokenDefault(SQLTokenType.T_STRING);
        final TPToken quotedToken = new TPTokenDefault(SQLTokenType.T_QUOTED);
        final TPToken numberToken = new TPTokenDefault(SQLTokenType.T_NUMBER);
        final TPToken commentToken = new SQLCommentToken();
        final TPToken multilineCommentToken = new SQLMultilineCommentToken();
        final SQLDelimiterToken delimiterToken = new SQLDelimiterToken();
        final SQLParameterToken parameterToken = new SQLParameterToken();
        final SQLVariableToken variableToken = new SQLVariableToken();
        final TPToken otherToken = new TPTokenDefault(SQLTokenType.T_OTHER);
        final SQLBlockHeaderToken blockHeaderToken = new SQLBlockHeaderToken();
        final SQLBlockBeginToken blockBeginToken = new SQLBlockBeginToken();
        final SQLBlockEndToken blockEndToken = new SQLBlockEndToken();

        List<TPRule> rules = new ArrayList<>();

        if (ruleProvider != null) {
            ruleProvider.extendRules(dataSourceContainer, rules, TPRuleProvider.RulePosition.INITIAL);
        }

        // Add rule for single-line comments.
        for (String lineComment : dialect.getSingleLineComments()) {
            if (lineComment.startsWith("^")) {
                rules.add(new LineCommentRule(lineComment, commentToken, (char) 0, false, true));
            } else {
                rules.add(new EndOfLineRule(lineComment, commentToken, (char) 0, false, true));
            }
        }

        if (ruleProvider != null) {
            ruleProvider.extendRules(dataSourceContainer, rules, TPRuleProvider.RulePosition.CONTROL);
        }

        if (!minimalRules) {
            final SQLControlToken controlToken = new SQLControlToken();

            String commandPrefix = syntaxManager.getControlCommandPrefix();

            // Control rules
            for (SQLCommandHandlerDescriptor controlCommand : SQLCommandsRegistry.getInstance().getCommandHandlers()) {
                rules.add(new SQLCommandRule(commandPrefix, controlCommand, controlToken)); //$NON-NLS-1$
            }
        }
        {
            if (!minimalRules && syntaxManager.isVariablesEnabled()) {
                // Variable rule
                rules.add(new ScriptVariableRule(parameterToken));
            }
        }

        // Decides whether the pattern can be accepted by hitting EOF instead of the end sequence.
        // We enable it for all "paired" literals like quoted strings, identifiers and comments, as proposed in #11773.
        final boolean breaksOnEOF = true;

        {
            // Add rules for delimited identifiers and string literals.
            char escapeChar = syntaxManager.getEscapeChar();
            String[][] identifierQuoteStrings = syntaxManager.getIdentifierQuoteStrings();
            String[][] stringQuoteStrings = syntaxManager.getStringQuoteStrings();

            boolean hasDoubleQuoteRule = false;
            if (!ArrayUtils.isEmpty(identifierQuoteStrings)) {
                for (String[] quotes : identifierQuoteStrings) {
                    rules.add(new MultiLineRule(quotes[0], quotes[1], quotedToken, escapeChar, breaksOnEOF));
                    if (quotes[1].equals(SQLConstants.STR_QUOTE_DOUBLE) && quotes[0].equals(quotes[1])) {
                        hasDoubleQuoteRule = true;
                    }
                }
            }
            if (!ArrayUtils.isEmpty(stringQuoteStrings)) {
                for (String[] quotes : stringQuoteStrings) {
                    rules.add(new MultiLineRule(quotes[0], quotes[1], stringToken, escapeChar, breaksOnEOF));
                }
            }
            if (!hasDoubleQuoteRule) {
                rules.add(new MultiLineRule(SQLConstants.STR_QUOTE_DOUBLE, SQLConstants.STR_QUOTE_DOUBLE, quotedToken, escapeChar, breaksOnEOF));
            }
        }
        if (ruleProvider != null) {
            ruleProvider.extendRules(dataSourceContainer, rules, TPRuleProvider.RulePosition.QUOTES);
        }

        // Add rules for multi-line comments
        Pair<String, String> multiLineComments = dialect.getMultiLineComments();
        if (multiLineComments != null) {
            rules.add(dialect.supportsNestedComments()
                ? new NestedMultiLineRule(multiLineComments.getFirst(), multiLineComments.getSecond(), multilineCommentToken, (char) 0, breaksOnEOF)
                : new MultiLineRule(multiLineComments.getFirst(), multiLineComments.getSecond(), multilineCommentToken, (char) 0, breaksOnEOF
            ));
        }

        if (!minimalRules) {
            // Add generic whitespace rule.
            rules.add(new WhitespaceRule(TPTokenAbstract.WHITESPACE));

            // Add numeric rule
            rules.add(new NumberRule(numberToken));
        }

        SQLDelimiterRule delimRule = new SQLDelimiterRule(syntaxManager.getStatementDelimiters(), delimiterToken);
        rules.add(delimRule);

        {
            // Delimiter redefine
            String delimRedefine = dialect.getScriptDelimiterRedefiner();
            if (!CommonUtils.isEmpty(delimRedefine)) {
                final SQLSetDelimiterToken setDelimiterToken = new SQLSetDelimiterToken();

                rules.add(new SQLDelimiterSetRule(delimRedefine, setDelimiterToken, delimRule));
            }
        }

        if (ruleProvider != null) {
            ruleProvider.extendRules(dataSourceContainer, rules, TPRuleProvider.RulePosition.KEYWORDS);
        }

        if (!minimalRules) {
            // Add word rule for keywords, functions, types, and constants.
            SQLWordRule wordRule = new SQLWordRule(delimRule, typeToken, otherToken, dialect);
            for (String reservedWord : dialect.getReservedWords()) {
                DBPKeywordType keywordType = dialect.getKeywordType(reservedWord);
                // Functions without parentheses has type 'DBPKeywordType.OTHER' (#8710)
                if (keywordType == DBPKeywordType.FUNCTION || keywordType == DBPKeywordType.OTHER) {
                    wordRule.addFunction(reservedWord);
                } else {
                    wordRule.addWord(reservedWord, keywordToken);
                }
            }
            if (dataSource != null) {
                for (String type : dialect.getDataTypes(dataSource)) {
                    wordRule.addWord(type, typeToken);
                }
                for (String function : dialect.getFunctions(dataSource)) {
                    wordRule.addFunction(function);
                }
            }
            final String[] blockHeaderStrings = dialect.getBlockHeaderStrings();
            if (!ArrayUtils.isEmpty(blockHeaderStrings)) {
                for (String bhs : blockHeaderStrings) {
                    wordRule.addWord(bhs, blockHeaderToken);
                }
            }
            String[][] blockBounds = dialect.getBlockBoundStrings();
            if (blockBounds != null) {
                for (String[] block : blockBounds) {
                    if (block.length != 2) {
                        continue;
                    }
                    wordRule.addWord(block[0], blockBeginToken);
                    wordRule.addWord(block[1], blockEndToken);
                }
            }
            rules.add(wordRule);

            // Parameter rule
            for (String npPrefix : syntaxManager.getNamedParameterPrefixes()) {
                rules.add(new ScriptParameterRule(syntaxManager, parameterToken, npPrefix));
            }
        }

        if (ruleProvider != null) {
            ruleProvider.extendRules(dataSourceContainer, rules, TPRuleProvider.RulePosition.FINAL);
        }

        allRules = rules.toArray(new TPRule[0]);
    }

}
