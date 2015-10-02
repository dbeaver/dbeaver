/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2015 Serge Rieder (serge@jkiss.org)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (version 2)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package org.jkiss.dbeaver.ui.editors.sql.syntax;

import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.TextAttribute;
import org.eclipse.jface.text.rules.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.themes.ITheme;
import org.eclipse.ui.themes.IThemeManager;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.sql.SQLConstants;
import org.jkiss.dbeaver.model.sql.SQLDialect;
import org.jkiss.dbeaver.model.sql.SQLSyntaxManager;
import org.jkiss.dbeaver.ui.editors.sql.syntax.tokens.*;
import org.jkiss.dbeaver.ui.editors.text.TextWhiteSpaceDetector;
import org.jkiss.utils.Pair;

import java.util.*;

/**
 * SQLSyntaxManager.
 * <p/>
 * Contains information about some concrete datasource underlying database syntax.
 * Support runtime change of datasource (reloads syntax information)
 */
public class SQLRuleManager extends RuleBasedScanner {

    @NotNull
    private final IThemeManager themeManager;
    @NotNull
    private SQLSyntaxManager syntaxManager;
    @NotNull
    private TreeMap<Integer, SQLScriptPosition> positions = new TreeMap<>();
    private Set<SQLScriptPosition> addedPositions = new HashSet<>();
    private Set<SQLScriptPosition> removedPositions = new HashSet<>();

    public SQLRuleManager(SQLSyntaxManager syntaxManager)
    {
        this.syntaxManager = syntaxManager;
        this.themeManager = PlatformUI.getWorkbench().getThemeManager();
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

    public void refreshRules()
    {
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
        final SQLBlockBeginToken blockBeginToken = new SQLBlockBeginToken(
            new TextAttribute(getColor(SQLConstants.CONFIG_COLOR_KEYWORD), null, SWT.BOLD));
        final SQLBlockEndToken blockEndToken = new SQLBlockEndToken(
            new TextAttribute(getColor(SQLConstants.CONFIG_COLOR_KEYWORD), null, SWT.BOLD));

        setDefaultReturnToken(otherToken);
        List<IRule> rules = new ArrayList<>();

        SQLDialect dialect = syntaxManager.getDialect();
        // Add rule for single-line comments.
        for (String lineComment : dialect.getSingleLineComments()) {
            if (lineComment.startsWith("^")) {
                rules.add(new LineCommentRule(lineComment, commentToken)); //$NON-NLS-1$
            } else {
                rules.add(new EndOfLineRule(lineComment, commentToken)); //$NON-NLS-1$
            }
        }

        // Add rules for delimited identifiers and string literals.
        char escapeChar = syntaxManager.getEscapeChar();
        String quoteSymbol = syntaxManager.getQuoteSymbol();
        if (quoteSymbol != null) {
            rules.add(new SingleLineRule(quoteSymbol, quoteSymbol, quotedToken, escapeChar));
        }
        if (quoteSymbol == null || !quoteSymbol.equals(SQLConstants.STR_QUOTE_SINGLE)) {
            rules.add(new NestedMultiLineRule(SQLConstants.STR_QUOTE_SINGLE, SQLConstants.STR_QUOTE_SINGLE, stringToken, escapeChar));
        }
        if (quoteSymbol == null || !quoteSymbol.equals(SQLConstants.STR_QUOTE_DOUBLE)) {
            rules.add(new SingleLineRule(SQLConstants.STR_QUOTE_DOUBLE, SQLConstants.STR_QUOTE_DOUBLE, quotedToken, escapeChar));
        }

        Pair<String, String> multiLineComments = dialect.getMultiLineComments();
        if (multiLineComments != null) {
            // Add rules for multi-line comments
            rules.add(new MultiLineRule(multiLineComments.getFirst(), multiLineComments.getSecond(), commentToken, (char) 0, true));
        }

        // Add generic whitespace rule.
        rules.add(new WhitespaceRule(new TextWhiteSpaceDetector()));

        // Add numeric rule
        rules.add(new NumberRule(numberToken));

        for (final String delimiter : syntaxManager.getStatementDelimiters()) {
            WordRule delimRule;
            if (Character.isLetterOrDigit(delimiter.charAt(0))) {
                delimRule = new WordRule(new SQLWordDetector(), Token.UNDEFINED, true);
                delimRule.addWord(delimiter, delimiterToken);
            } else {
                // Default delim rule
                delimRule = new WordRule(new IWordDetector() {
                    @Override
                    public boolean isWordStart(char c) {
                        return delimiter.charAt(0) == c;
                    }

                    @Override
                    public boolean isWordPart(char c) {
                        return delimiter.indexOf(c) != -1;
                    }
                }, Token.UNDEFINED, false);
                delimRule.addWord(delimiter, delimiterToken);
            }
            rules.add(delimRule);
        }

        // Add word rule for keywords, types, and constants.
        WordRule wordRule = new WordRule(new SQLWordDetector(), otherToken, true);
        for (String reservedWord : dialect.getReservedWords()) {
            wordRule.addWord(reservedWord, keywordToken);
        }
        for (String function : dialect.getFunctions()) {
            wordRule.addWord(function, typeToken);
        }
        for (String type : dialect.getTypes()) {
            wordRule.addWord(type, typeToken);
        }
        wordRule.addWord(SQLConstants.BLOCK_BEGIN, blockBeginToken);
        wordRule.addWord(SQLConstants.BLOCK_END, blockEndToken);
        rules.add(wordRule);

        // Parameter rule
        rules.add(new ParametersRule(parameterToken));

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

    private class ParametersRule implements IRule {
        private final SQLParameterToken parameterToken;
        private final StringBuilder buffer;
        private final char anonymousParameterMark;

        public ParametersRule(SQLParameterToken parameterToken) {
            this.parameterToken = parameterToken;
            buffer = new StringBuilder();
            anonymousParameterMark = syntaxManager.getAnonymousParameterMark();
        }

        @Override
        public IToken evaluate(ICharacterScanner scanner)
        {
            int column = scanner.getColumn();
            if (column  <= 0) {
                return Token.UNDEFINED;
            }
            scanner.unread();
            int prevChar = scanner.read();
            if (Character.isJavaIdentifierPart(prevChar) ||
                prevChar == SQLConstants.PARAMETER_PREFIX || prevChar == anonymousParameterMark || prevChar == '\\' || prevChar == '/')
            {
                return Token.UNDEFINED;
            }
            int c = scanner.read();
            if (c != ICharacterScanner.EOF && (c == anonymousParameterMark || c == SQLConstants.PARAMETER_PREFIX)) {
                buffer.setLength(0);
                do {
                    buffer.append((char) c);
                    c = scanner.read();
                } while (c != ICharacterScanner.EOF && Character.isJavaIdentifierPart(c));
                scanner.unread();

                // Check for parameters
                if (syntaxManager.isAnonymousParametersEnabled()) {
                    if (buffer.length() == 1 && buffer.charAt(0) == anonymousParameterMark) {
                        return parameterToken;
                    }
                }
                if (syntaxManager.isParametersEnabled()) {
                    if (buffer.charAt(0) == SQLConstants.PARAMETER_PREFIX && buffer.length() > 1) {
                        boolean validChars = true;
                        for (int i = 1; i < buffer.length(); i++) {
                            if (!Character.isLetterOrDigit(buffer.charAt(i))) {
                                validChars = false;
                                break;
                            }
                        }
                        if (validChars) {
                            return parameterToken;
                        }
                    }
                }

                for (int i = buffer.length() - 1; i >= 0; i--) {
                    scanner.unread();
                }
            } else {
                scanner.unread();
            }
            return Token.UNDEFINED;
        }
    }
}
