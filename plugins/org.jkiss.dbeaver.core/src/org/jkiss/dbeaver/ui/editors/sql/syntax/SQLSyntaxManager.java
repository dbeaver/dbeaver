/*
 * Copyright (C) 2010-2015 Serge Rieder
 * serge@jkiss.org
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
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
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.impl.sql.BasicSQLDialect;
import org.jkiss.dbeaver.model.sql.SQLDataSource;
import org.jkiss.dbeaver.model.sql.SQLDialect;
import org.jkiss.dbeaver.ui.editors.sql.SQLConstants;
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
public class SQLSyntaxManager extends RuleBasedScanner {

    @NotNull
    private final IThemeManager themeManager;
    @Nullable
    private SQLDataSource dataSource;
    @NotNull
    private SQLDialect sqlDialect;
    @Nullable
    private String quoteSymbol;
    private char structSeparator;
    @NotNull
    private String catalogSeparator;
    @NotNull
    private String statementDelimiter = SQLConstants.DEFAULT_STATEMENT_DELIMITER;
    @NotNull
    private TreeMap<Integer, SQLScriptPosition> positions = new TreeMap<Integer, SQLScriptPosition>();

    private Set<SQLScriptPosition> addedPositions = new HashSet<SQLScriptPosition>();
    private Set<SQLScriptPosition> removedPositions = new HashSet<SQLScriptPosition>();
    private char escapeChar;
    private boolean unassigned;

    public SQLSyntaxManager()
    {
        themeManager = PlatformUI.getWorkbench().getThemeManager();
    }

    public void dispose()
    {
    }

    /**
     * Returns true if this syntax manager wasn't assigned to a some particular data source container/ SQL dialect
     */
    public boolean isUnassigned() {
        return unassigned;
    }

    @NotNull
    public SQLDialect getDialect() {
        return sqlDialect;
    }

    public char getStructSeparator()
    {
        return structSeparator;
    }

    @NotNull
    public String getCatalogSeparator()
    {
        return catalogSeparator;
    }

    @NotNull
    public String getStatementDelimiter()
    {
        return statementDelimiter;
    }

    @Nullable
    public String getQuoteSymbol()
    {
        return quoteSymbol;
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
            removedPositions = new HashSet<SQLScriptPosition>();
        }
        return posList;
    }

    @NotNull
    public synchronized Set<SQLScriptPosition> getAddedPositions(boolean clear)
    {
        Set<SQLScriptPosition> posList = addedPositions;
        if (clear) {
            addedPositions = new HashSet<SQLScriptPosition>();
        }
        return posList;
    }

    public void setDataSource(@Nullable SQLDataSource dataSource)
    {
        this.unassigned = dataSource == null;
        this.dataSource = dataSource;
        if (this.dataSource == null) {
            sqlDialect = new BasicSQLDialect();
            quoteSymbol = null;
            structSeparator = SQLConstants.STRUCT_SEPARATOR;
            catalogSeparator = String.valueOf(SQLConstants.STRUCT_SEPARATOR);
            escapeChar = '\\';
            statementDelimiter = SQLConstants.DEFAULT_STATEMENT_DELIMITER;
        } else {
            sqlDialect = this.dataSource.getSQLDialect();
            quoteSymbol = sqlDialect.getIdentifierQuoteString();
            structSeparator = sqlDialect.getStructSeparator();
            catalogSeparator = sqlDialect.getCatalogSeparator();
            sqlDialect.getSearchStringEscape();
            escapeChar = '\\';
            statementDelimiter = sqlDialect.getScriptDelimiter().toLowerCase();
        }
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
        List<IRule> rules = new ArrayList<IRule>();

        // Add rule for single-line comments.
        for (String lineComment : sqlDialect.getSingleLineComments()) {
            if (lineComment.startsWith("^")) {
                rules.add(new LineCommentRule(lineComment, commentToken)); //$NON-NLS-1$
            } else {
                rules.add(new EndOfLineRule(lineComment, commentToken)); //$NON-NLS-1$
            }
        }

        // Add rules for delimited identifiers and string literals.
        if (quoteSymbol != null) {
            rules.add(new SingleLineRule(quoteSymbol, quoteSymbol, quotedToken, escapeChar));
        }
        if (quoteSymbol == null || !quoteSymbol.equals(SQLConstants.STR_QUOTE_SINGLE)) {
            rules.add(new NestedMultiLineRule(SQLConstants.STR_QUOTE_SINGLE, SQLConstants.STR_QUOTE_SINGLE, stringToken, escapeChar));
        }
        if (quoteSymbol == null || !quoteSymbol.equals(SQLConstants.STR_QUOTE_DOUBLE)) {
            rules.add(new SingleLineRule(SQLConstants.STR_QUOTE_DOUBLE, SQLConstants.STR_QUOTE_DOUBLE, quotedToken, escapeChar));
        }

        Pair<String, String> multiLineComments = sqlDialect.getMultiLineComments();
        if (multiLineComments != null) {
            // Add rules for multi-line comments
            rules.add(new MultiLineRule(multiLineComments.getFirst(), multiLineComments.getSecond(), commentToken, (char) 0, true));
        }

        // Add generic whitespace rule.
        rules.add(new WhitespaceRule(new TextWhiteSpaceDetector()));

        // Add numeric rule
        rules.add(new NumberRule(numberToken));

        {
            // Default delim rule
            WordRule delimRule = new WordRule(new IWordDetector() {
                @Override
                public boolean isWordStart(char c)
                {
                    return SQLConstants.DEFAULT_STATEMENT_DELIMITER.charAt(0) == c;
                }

                @Override
                public boolean isWordPart(char c)
                {
                    return SQLConstants.DEFAULT_STATEMENT_DELIMITER.indexOf(c) != -1;
                }
            }, Token.UNDEFINED, false);
            delimRule.addWord(SQLConstants.DEFAULT_STATEMENT_DELIMITER, delimiterToken);
            rules.add(delimRule);
        }
        if (!statementDelimiter.equals(SQLConstants.DEFAULT_STATEMENT_DELIMITER)) {
            WordRule delimRule = new WordRule(new SQLWordDetector(), Token.UNDEFINED, true);
            delimRule.addWord(statementDelimiter, delimiterToken);
            rules.add(delimRule);
        }

        // Add word rule for keywords, types, and constants.
        WordRule wordRule = new WordRule(new SQLWordDetector(), otherToken, true);
        for (String reservedWord : sqlDialect.getReservedWords()) {
            wordRule.addWord(reservedWord, keywordToken);
        }
        for (String function : sqlDialect.getFunctions()) {
            wordRule.addWord(function, typeToken);
        }
        for (String type : sqlDialect.getTypes()) {
            wordRule.addWord(type, typeToken);
        }
        wordRule.addWord(SQLConstants.BLOCK_BEGIN, blockBeginToken);
        wordRule.addWord(SQLConstants.BLOCK_END, blockEndToken);
        rules.add(wordRule);

        {
            // Parameter rule
            IRule parameterRule = new IRule() {
                private StringBuilder buffer = new StringBuilder();

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
                        prevChar == ':' || prevChar == '?' || prevChar == '\\' || prevChar == '/')
                    {
                        return Token.UNDEFINED;
                    }
                    int c = scanner.read();
                    if (c != ICharacterScanner.EOF && (c == '?' || c == ':')) {
                        buffer.setLength(0);
                        do {
                            buffer.append((char) c);
                            c = scanner.read();
                        } while (c != ICharacterScanner.EOF && Character.isJavaIdentifierPart(c));
                        scanner.unread();

                        if ((buffer.charAt(0) == '?' && buffer.length() == 1) || (buffer.charAt(0) == ':' && buffer.length() > 1)) {
                            return parameterToken;
                        }

                        for (int i = buffer.length() - 1; i >= 0; i--) {
                            scanner.unread();
                        }
                    } else {
                        scanner.unread();
                    }
                    return Token.UNDEFINED;
                }
            };
            rules.add(parameterRule);
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

}
