/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
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
package org.jkiss.dbeaver.ui.editors.sql.syntax;

import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.TextAttribute;
import org.eclipse.jface.text.rules.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.themes.ITheme;
import org.eclipse.ui.themes.IThemeManager;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.sql.SQLConstants;
import org.jkiss.dbeaver.model.sql.SQLDialect;
import org.jkiss.dbeaver.runtime.sql.SQLRuleProvider;
import org.jkiss.dbeaver.model.sql.SQLSyntaxManager;
import org.jkiss.dbeaver.registry.sql.SQLCommandHandlerDescriptor;
import org.jkiss.dbeaver.registry.sql.SQLCommandsRegistry;
import org.jkiss.dbeaver.ui.editors.EditorUtils;
import org.jkiss.dbeaver.ui.editors.sql.syntax.rules.LineCommentRule;
import org.jkiss.dbeaver.ui.editors.sql.syntax.rules.SQLDelimiterRule;
import org.jkiss.dbeaver.ui.editors.sql.syntax.rules.SQLDelimiterSetRule;
import org.jkiss.dbeaver.ui.editors.sql.syntax.rules.SQLParameterRule;
import org.jkiss.dbeaver.ui.editors.sql.syntax.tokens.*;
import org.jkiss.dbeaver.ui.editors.text.TextWhiteSpaceDetector;
import org.jkiss.utils.ArrayUtils;
import org.jkiss.utils.CommonUtils;
import org.jkiss.utils.Pair;

import java.io.File;
import java.util.*;

/**
 * SQLSyntaxManager.
 * <p/>
 * Contains information about some concrete datasource underlying database syntax.
 * Support runtime change of datasource (reloads syntax information)
 */
public class SQLRuleManager extends RuleBasedScanner {

    private static final long MAX_FILE_LENGTH_FOR_RULES = 5000000;

    @NotNull
    private final IThemeManager themeManager;
    @NotNull
    private SQLSyntaxManager syntaxManager;
    @NotNull
    private TreeMap<Integer, SQLScriptPosition> positions = new TreeMap<>();
    private Set<SQLScriptPosition> addedPositions = new HashSet<>();
    private Set<SQLScriptPosition> removedPositions = new HashSet<>();

    private boolean evalMode;

    public SQLRuleManager(@NotNull SQLSyntaxManager syntaxManager)
    {
        this.syntaxManager = syntaxManager;
        this.themeManager = PlatformUI.getWorkbench().getThemeManager();
    }

    public boolean isEvalMode() {
        return evalMode;
    }

    public void startEval() {
        this.evalMode = true;
    }

    public void endEval() {
        this.evalMode = false;
        if (fRules != null) {
            for (IRule rule : fRules) {
                if (rule instanceof SQLDelimiterRule) {
                    ((SQLDelimiterRule) rule).changeDelimiter(null);
                }
            }
        }
    }

    public void dispose()
    {
    }

    @NotNull
    public Collection<? extends Position> getPositions(int offset, int length)
    {
        return positions.subMap(offset, offset + length).values();
    }

    @NotNull
    public synchronized Set<SQLScriptPosition> getRemovedPositions(boolean clear)
    {
        Set<SQLScriptPosition> posList = removedPositions;
        if (clear) {
            removedPositions = new HashSet<>();
        }
        return posList;
    }

    @NotNull
    public synchronized Set<SQLScriptPosition> getAddedPositions(boolean clear)
    {
        Set<SQLScriptPosition> posList = addedPositions;
        if (clear) {
            addedPositions = new HashSet<>();
        }
        return posList;
    }

    public void refreshRules(@Nullable DBPDataSource dataSource, IEditorInput editorInput)
    {
        SQLDialect dialect = syntaxManager.getDialect();
        SQLRuleProvider ruleProvider = null;
        if (dialect instanceof SQLRuleProvider) {
            ruleProvider = (SQLRuleProvider) dialect;
        }

        boolean minimalRules = false;
        File file = EditorUtils.getLocalFileFromInput(editorInput);
        if (file != null && file.length() > MAX_FILE_LENGTH_FOR_RULES) {
            minimalRules = true;
        }

        /*final Color backgroundColor = null;unassigned || dataSource != null ?
            getColor(SQLConstants.CONFIG_COLOR_BACKGROUND, SWT.COLOR_WHITE) :
            getColor(SQLConstants.CONFIG_COLOR_DISABLED, SWT.COLOR_WIDGET_LIGHT_SHADOW);*/
        final IToken keywordToken = new Token(
            new TextAttribute(getColor(SQLConstants.CONFIG_COLOR_KEYWORD), null, SWT.BOLD));
        final IToken typeToken = new Token(
            new TextAttribute(getColor(SQLConstants.CONFIG_COLOR_DATATYPE), null, SWT.BOLD));
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
            new TextAttribute(getColor(SQLConstants.CONFIG_COLOR_PARAMETER, SWT.COLOR_DARK_BLUE), null, SWT.BOLD));
        final IToken otherToken = new Token(
            new TextAttribute(getColor(SQLConstants.CONFIG_COLOR_TEXT), null, SWT.NORMAL));
        final SQLBlockHeaderToken blockHeaderToken = new SQLBlockHeaderToken(
            new TextAttribute(getColor(SQLConstants.CONFIG_COLOR_KEYWORD), null, SWT.BOLD));
        final SQLBlockBeginToken blockBeginToken = new SQLBlockBeginToken(
            new TextAttribute(getColor(SQLConstants.CONFIG_COLOR_KEYWORD), null, SWT.BOLD));
        final SQLBlockEndToken blockEndToken = new SQLBlockEndToken(
            new TextAttribute(getColor(SQLConstants.CONFIG_COLOR_KEYWORD), null, SWT.BOLD));
        final SQLBlockToggleToken blockToggleToken = new SQLBlockToggleToken(
            new TextAttribute(getColor(SQLConstants.CONFIG_COLOR_DELIMITER), null, SWT.BOLD));

        setDefaultReturnToken(otherToken);
        List<IRule> rules = new ArrayList<>();

        if (ruleProvider != null) {
            ruleProvider.extendRules(rules, SQLRuleProvider.RulePosition.INITIAL);
        }

        // Add rule for single-line comments.
        for (String lineComment : dialect.getSingleLineComments()) {
            if (lineComment.startsWith("^")) {
                rules.add(new LineCommentRule(lineComment, commentToken)); //$NON-NLS-1$
            } else {
                rules.add(new EndOfLineRule(lineComment, commentToken)); //$NON-NLS-1$
            }
        }

        {
            final SQLControlToken controlToken = new SQLControlToken(
                    new TextAttribute(getColor(SQLConstants.CONFIG_COLOR_COMMAND), null, SWT.BOLD));

            if (ruleProvider != null) {
                ruleProvider.extendRules(rules, SQLRuleProvider.RulePosition.CONTROL);
            }

            String commandPrefix = syntaxManager.getControlCommandPrefix();

            // Control rules
            for (SQLCommandHandlerDescriptor controlCommand : SQLCommandsRegistry.getInstance().getCommandHandlers()) {
                rules.add(new EndOfLineRule(commandPrefix + controlCommand.getId(), controlToken)); //$NON-NLS-1$
            }
        }

        {
            // Add rules for delimited identifiers and string literals.
            //char escapeChar = syntaxManager.getEscapeChar();
            String[][] quoteStrings = syntaxManager.getQuoteStrings();
            boolean hasSingleQuoteRule = false, hasDoubleQuoteRule = false;
            if (!ArrayUtils.isEmpty(quoteStrings)) {
                for (int i = 0; i < quoteStrings.length; i++) {
                    rules.add(new SingleLineRule(quoteStrings[i][0], quoteStrings[i][1], quotedToken, '\\'));
                    if (quoteStrings[i][0].equals(SQLConstants.STR_QUOTE_SINGLE) && quoteStrings[i][0].equals(quoteStrings[i][1])) {
                        hasSingleQuoteRule = true;
                    } else if (quoteStrings[i][1].equals(SQLConstants.STR_QUOTE_DOUBLE) && quoteStrings[i][0].equals(quoteStrings[i][1])) {
                        hasDoubleQuoteRule = true;
                    }

                }
            }
            if (!hasSingleQuoteRule) {
                rules.add(new MultiLineRule(SQLConstants.STR_QUOTE_SINGLE, SQLConstants.STR_QUOTE_SINGLE, stringToken, '\\'));
            }
            if (!hasDoubleQuoteRule) {
                rules.add(new MultiLineRule(SQLConstants.STR_QUOTE_DOUBLE, SQLConstants.STR_QUOTE_DOUBLE, quotedToken, '\\'));
            }
        }

        // Add rules for multi-line comments
        Pair<String, String> multiLineComments = dialect.getMultiLineComments();
        if (multiLineComments != null) {
            rules.add(new MultiLineRule(multiLineComments.getFirst(), multiLineComments.getSecond(), commentToken, (char) 0, true));
        }

        // Add generic whitespace rule.
        rules.add(new WhitespaceRule(new TextWhiteSpaceDetector()));

        if (!minimalRules) {
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
                    new TextAttribute(getColor(SQLConstants.CONFIG_COLOR_COMMAND), null, SWT.BOLD));

                rules.add(new SQLDelimiterSetRule(delimRedefine, setDelimiterToken, delimRule));
            }
        }

        final String blockToggleString = dialect.getBlockToggleString();
        if (!CommonUtils.isEmpty(blockToggleString)) {
            int divPos = blockToggleString.indexOf(SQLConstants.KEYWORD_PATTERN_CHARS);
            if (divPos != -1) {
                String prefix = blockToggleString.substring(0, divPos);
                String postfix = blockToggleString.substring(divPos + SQLConstants.KEYWORD_PATTERN_CHARS.length());
                WordPatternRule blockToggleRule = new WordPatternRule(new SQLWordDetector(), prefix, postfix, blockToggleToken);
                rules.add(blockToggleRule);
            } else {
                WordRule blockToggleRule = new WordRule(getWordOrSymbolDetector(blockToggleString), Token.UNDEFINED, true);
                blockToggleRule.addWord(blockToggleString, blockToggleToken);
                rules.add(blockToggleRule);
            }
        }

        if (!minimalRules) {
            if (ruleProvider != null) {
                ruleProvider.extendRules(rules, SQLRuleProvider.RulePosition.KEYWORDS);
            }

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
            final String blockHeaderString = dialect.getBlockHeaderString();
            if (!CommonUtils.isEmpty(blockHeaderString)) {
                wordRule.addWord(blockHeaderString, blockHeaderToken);
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
        }

        if (!minimalRules) {
            // Parameter rule
            rules.add(new SQLParameterRule(syntaxManager, parameterToken));
        }

        IRule[] result = new IRule[rules.size()];
        rules.toArray(result);
        setRules(result);
    }

    public Color getColor(String colorKey)
    {
        return getColor(colorKey, SWT.COLOR_BLACK);
    }

    public Color getColor(String colorKey, int colorDefault)
    {
        ITheme currentTheme = themeManager.getCurrentTheme();
        Color color = currentTheme.getColorRegistry().get(colorKey);
        if (color == null) {
            color = Display.getDefault().getSystemColor(colorDefault);
        }
        return color;
    }

    private static IWordDetector getWordOrSymbolDetector(String word) {
        if (Character.isLetterOrDigit(word.charAt(0))) {
            return new SQLWordDetector();
        } else {
            // Default delim rule
            return new SymbolSequenceDetector(word);
        }
    }

    private static class SymbolSequenceDetector implements IWordDetector {
        private final String delimiter;

        public SymbolSequenceDetector(String delimiter) {
            this.delimiter = delimiter;
        }

        @Override
        public boolean isWordStart(char c) {
            return delimiter.charAt(0) == c;
        }

        @Override
        public boolean isWordPart(char c) {
            return delimiter.indexOf(c) != -1;
        }
    }

}
