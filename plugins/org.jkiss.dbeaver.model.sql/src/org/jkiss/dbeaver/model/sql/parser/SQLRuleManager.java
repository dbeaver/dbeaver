/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2019 Serge Rider (serge@jkiss.org)
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

import org.eclipse.jface.text.TextAttribute;
import org.eclipse.jface.text.rules.*;
import org.eclipse.swt.SWT;
import org.eclipse.ui.IEditorInput;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.sql.SQLConstants;
import org.jkiss.dbeaver.model.sql.SQLDialect;
import org.jkiss.dbeaver.model.sql.SQLSyntaxManager;
import org.jkiss.dbeaver.model.sql.parser.rules.SQLDelimiterRule;
import org.jkiss.dbeaver.model.sql.parser.rules.SQLWordRule;
import org.jkiss.dbeaver.model.sql.registry.SQLCommandHandlerDescriptor;
import org.jkiss.dbeaver.model.sql.registry.SQLCommandsRegistry;
import org.jkiss.dbeaver.model.text.parser.TPRule;
import org.jkiss.dbeaver.runtime.sql.SQLRuleProvider;
import org.jkiss.dbeaver.ui.editors.text.TextWhiteSpaceDetector;
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
    private boolean evalMode;

    public SQLRuleManager(@NotNull SQLSyntaxManager syntaxManager) {
        this.syntaxManager = syntaxManager;
    }

    @NotNull
    public TPRule[] getAllRules() {
        return allRules;
    }

    public boolean isEvalMode() {
        return evalMode;
    }

    public void startEval() {
        this.evalMode = true;
    }

    public void endEval() {
        this.evalMode = false;
        {
            for (TPRule rule : allRules) {
                if (rule instanceof SQLDelimiterRule) {
                    ((SQLDelimiterRule) rule).changeDelimiter(null);
                }
            }
        }
    }

    public void dispose() {
    }

    public void refreshRules(@Nullable DBPDataSource dataSource, @Nullable IEditorInput editorInput, boolean minimalRules) {
        SQLDialect dialect = syntaxManager.getDialect();
        SQLRuleProvider ruleProvider = GeneralUtils.adapt(dialect, SQLRuleProvider.class);
        DBPDataSourceContainer dataSourceContainer = dataSource == null ? null : dataSource.getContainer();

        final IToken keywordToken = new Token(
                new TextAttribute(getColor(SQLConstants.CONFIG_COLOR_KEYWORD), null, keywordStyle));
        final IToken typeToken = new Token(
                new TextAttribute(getColor(SQLConstants.CONFIG_COLOR_DATATYPE), null, keywordStyle));
        final IToken stringToken = new Token(
                new TextAttribute(getColor(SQLConstants.CONFIG_COLOR_STRING), null, SWT.NORMAL));
        final IToken quotedToken = new Token(
                new TextAttribute(getColor(SQLConstants.CONFIG_COLOR_DATATYPE), null, SWT.NORMAL));
        final IToken numberToken = new Token(
                new TextAttribute(getColor(SQLConstants.CONFIG_COLOR_NUMBER), null, SWT.NORMAL));
        final IToken commentToken = new SQLCommentToken(
                new TextAttribute(getColor(SQLConstants.CONFIG_COLOR_COMMENT), null, SWT.NORMAL));
        final SQLDelimiterToken delimiterToken = new SQLDelimiterToken(
                new TextAttribute(getColor(SQLConstants.CONFIG_COLOR_DELIMITER, SWT.COLOR_RED), null, SWT.NORMAL));
        final SQLParameterToken parameterToken = new SQLParameterToken(
                new TextAttribute(getColor(SQLConstants.CONFIG_COLOR_PARAMETER, SWT.COLOR_DARK_BLUE), null, keywordStyle));
        final SQLVariableToken variableToken = new SQLVariableToken(
                new TextAttribute(getColor(SQLConstants.CONFIG_COLOR_PARAMETER, SWT.COLOR_DARK_BLUE), null, keywordStyle));
        final IToken otherToken = new Token(
                new TextAttribute(getColor(SQLConstants.CONFIG_COLOR_TEXT), null, SWT.NORMAL));
        final SQLBlockHeaderToken blockHeaderToken = new SQLBlockHeaderToken(
                new TextAttribute(getColor(SQLConstants.CONFIG_COLOR_KEYWORD), null, keywordStyle));
        final SQLBlockBeginToken blockBeginToken = new SQLBlockBeginToken(
                new TextAttribute(getColor(SQLConstants.CONFIG_COLOR_KEYWORD), null, keywordStyle));
        final SQLBlockEndToken blockEndToken = new SQLBlockEndToken(
                new TextAttribute(getColor(SQLConstants.CONFIG_COLOR_KEYWORD), null, keywordStyle));

        setDefaultReturnToken(otherToken);
        List<TPRule> rules = new ArrayList<>();

        // Add rule for single-line comments.
        for (String lineComment : dialect.getSingleLineComments()) {
            if (lineComment.startsWith("^")) {
                rules.add(new LineCommentRule(lineComment, commentToken)); //$NON-NLS-1$
            } else {
                rules.add(new EndOfLineRule(lineComment, commentToken)); //$NON-NLS-1$
            }
        }

        if (ruleProvider != null) {
            ruleProvider.extendRules(dataSourceContainer, rules, SQLRuleProvider.RulePosition.CONTROL);
        }

        if (!minimalRules) {
            final SQLControlToken controlToken = new SQLControlToken(
                    new TextAttribute(getColor(SQLConstants.CONFIG_COLOR_COMMAND), null, keywordStyle));

            String commandPrefix = syntaxManager.getControlCommandPrefix();

            // Control rules
            for (SQLCommandHandlerDescriptor controlCommand : SQLCommandsRegistry.getInstance().getCommandHandlers()) {
                rules.add(new SQLCommandRule(commandPrefix, controlCommand, controlToken)); //$NON-NLS-1$
            }
        }
        {
            if (!minimalRules && syntaxManager.isVariablesEnabled()) {
                // Variable rule
                rules.add(new SQLVariableRule(parameterToken));
            }
        }

        if (!minimalRules) {
            // Add rules for delimited identifiers and string literals.
            char escapeChar = syntaxManager.getEscapeChar();
            String[][] identifierQuoteStrings = syntaxManager.getIdentifierQuoteStrings();
            String[][] stringQuoteStrings = syntaxManager.getStringQuoteStrings();

            boolean hasDoubleQuoteRule = false;
            if (!ArrayUtils.isEmpty(identifierQuoteStrings)) {
                for (String[] quotes : identifierQuoteStrings) {
                    rules.add(new SingleLineRule(quotes[0], quotes[1], quotedToken, escapeChar));
                    if (quotes[1].equals(SQLConstants.STR_QUOTE_DOUBLE) && quotes[0].equals(quotes[1])) {
                        hasDoubleQuoteRule = true;
                    }
                }
            }
            if (!ArrayUtils.isEmpty(stringQuoteStrings)) {
                for (String[] quotes : stringQuoteStrings) {
                    rules.add(new MultiLineRule(quotes[0], quotes[1], stringToken, escapeChar));
                }
            }
            if (!hasDoubleQuoteRule) {
                rules.add(new MultiLineRule(SQLConstants.STR_QUOTE_DOUBLE, SQLConstants.STR_QUOTE_DOUBLE, quotedToken, escapeChar));
            }
        }
        if (ruleProvider != null) {
            ruleProvider.extendRules(dataSourceContainer, rules, SQLRuleProvider.RulePosition.QUOTES);
        }

        // Add rules for multi-line comments
        Pair<String, String> multiLineComments = dialect.getMultiLineComments();
        if (multiLineComments != null) {
            rules.add(new MultiLineRule(multiLineComments.getFirst(), multiLineComments.getSecond(), commentToken, (char) 0, true));
        }

        if (!minimalRules) {
            // Add generic whitespace rule.
            rules.add(new WhitespaceRule(new TextWhiteSpaceDetector()));

            // Add numeric rule
            rules.add(new NumberRule(numberToken));
        }

        SQLDelimiterRule delimRule = new SQLDelimiterRule(syntaxManager.getStatementDelimiters(), delimiterToken);
        rules.add(delimRule);

        {
            // Delimiter redefine
            String delimRedefine = dialect.getScriptDelimiterRedefiner();
            if (!CommonUtils.isEmpty(delimRedefine)) {
                final SQLSetDelimiterToken setDelimiterToken = new SQLSetDelimiterToken(
                        new TextAttribute(getColor(SQLConstants.CONFIG_COLOR_COMMAND), null, keywordStyle));

                rules.add(new SQLDelimiterSetRule(delimRedefine, setDelimiterToken, delimRule));
            }
        }

        if (ruleProvider != null) {
            ruleProvider.extendRules(dataSourceContainer, rules, SQLRuleProvider.RulePosition.KEYWORDS);
        }

        if (!minimalRules) {

            // Add word rule for keywords, types, and constants.
            SQLWordRule wordRule = new SQLWordRule(delimRule, otherToken);
            for (String reservedWord : dialect.getReservedWords()) {
                wordRule.addWord(reservedWord, keywordToken);
            }
            if (dataSource != null) {
                for (String function : dialect.getFunctions(dataSource)) {
                    wordRule.addWord(function, typeToken);
                }
                for (String type : dialect.getDataTypes(dataSource)) {
                    wordRule.addWord(type, typeToken);
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
                rules.add(new SQLParameterRule(syntaxManager, parameterToken, npPrefix));
            }
        }

        allRules = rules.toArray(new TPRule[0]);
    }

}
