/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.editors.sql.syntax;

import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.TextAttribute;
import org.eclipse.jface.text.rules.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.themes.ITheme;
import org.eclipse.ui.themes.IThemeManager;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPKeywordManager;
import org.jkiss.dbeaver.model.impl.EmptyKeywordManager;
import org.jkiss.dbeaver.ui.editors.sql.SQLConstants;

import java.util.*;

/**
 * SQLSyntaxManager.
 * <p/>
 * Contains information about some concrete datasource underlying database syntax.
 * Support runtime change of datasource (reloads syntax information)
 */
public class SQLSyntaxManager extends RuleBasedScanner {

    public static final String DEFAULT_STATEMENT_DELIMITER = ";";

    public static final String CONFIG_COLOR_KEYWORD = "org.jkiss.dbeaver.sql.editor.color.keyword.foreground";
    public static final String CONFIG_COLOR_DATATYPE = "org.jkiss.dbeaver.sql.editor.color.datatype.foreground";
    public static final String CONFIG_COLOR_STRING = "org.jkiss.dbeaver.sql.editor.color.string.foreground";
    public static final String CONFIG_COLOR_NUMBER = "org.jkiss.dbeaver.sql.editor.color.number.foreground";
    public static final String CONFIG_COLOR_COMMENT = "org.jkiss.dbeaver.sql.editor.color.comment.foreground";
    public static final String CONFIG_COLOR_DELIMITER = "org.jkiss.dbeaver.sql.editor.color.delimiter.foreground";
    public static final String CONFIG_COLOR_PARAMETER = "org.jkiss.dbeaver.sql.editor.color.parameter.foreground";
    public static final String CONFIG_COLOR_TEXT = "org.jkiss.dbeaver.sql.editor.color.text.foreground";
    public static final String CONFIG_COLOR_BACKGROUND = "org.jkiss.dbeaver.sql.editor.color.text.background";

    private IThemeManager themeManager;

    private DBPKeywordManager keywordManager;
    private String quoteSymbol;
    private String structSeparator;
    private String statementDelimiter = DEFAULT_STATEMENT_DELIMITER;

    private TreeMap<Integer, SQLScriptPosition> positions = new TreeMap<Integer, SQLScriptPosition>();

    private Set<SQLScriptPosition> addedPositions = new HashSet<SQLScriptPosition>();
    private Set<SQLScriptPosition> removedPositions = new HashSet<SQLScriptPosition>();

    public SQLSyntaxManager()
    {
        themeManager = DBeaverCore.getInstance().getWorkbench().getThemeManager();
    }

    public void dispose()
    {
    }

    public DBPKeywordManager getKeywordManager()
    {
        return keywordManager;
    }

    public String getStructSeparator()
    {
        return structSeparator;
    }

    public String getStatementDelimiter()
    {
        return statementDelimiter;
    }

    public String getQuoteSymbol()
    {
        return quoteSymbol;
    }

    public Collection<? extends Position> getPositions(int offset, int length)
    {
        return positions.subMap(offset, offset + length).values();
    }

    public synchronized Set<SQLScriptPosition> getRemovedPositions(boolean clear)
    {
        Set<SQLScriptPosition> posList = removedPositions;
        if (clear) {
            removedPositions = new HashSet<SQLScriptPosition>();
        }
        return posList;
    }

    public synchronized Set<SQLScriptPosition> getAddedPositions(boolean clear)
    {
        Set<SQLScriptPosition> posList = addedPositions;
        if (clear) {
            addedPositions = new HashSet<SQLScriptPosition>();
        }
        return posList;
    }

    public void changeDataSource(DBPDataSource dataSource)
    {
        if (dataSource == null) {
            keywordManager = EmptyKeywordManager.INSTANCE;
            quoteSymbol = SQLConstants.STR_QUOTE_DOUBLE;
            structSeparator = ".";
            statementDelimiter = DEFAULT_STATEMENT_DELIMITER;
        } else {
            keywordManager = dataSource.getContainer().getKeywordManager();
            quoteSymbol = dataSource.getInfo().getIdentifierQuoteString();
            structSeparator = dataSource.getInfo().getStructSeparator();
            statementDelimiter = dataSource.getInfo().getScriptDelimiter();
            if (statementDelimiter == null) {
                statementDelimiter = DEFAULT_STATEMENT_DELIMITER;
            }
        }

        changeRules();
    }

    private void changeRules()
    {
        final Color backgroundColor = getColor(SQLSyntaxManager.CONFIG_COLOR_BACKGROUND, SWT.COLOR_WHITE);
        final IToken keywordToken = new Token(
            new TextAttribute(getColor(SQLSyntaxManager.CONFIG_COLOR_KEYWORD), backgroundColor, SWT.BOLD));
        final IToken typeToken = new Token(
            new TextAttribute(getColor(SQLSyntaxManager.CONFIG_COLOR_DATATYPE), backgroundColor, SWT.BOLD));
        final IToken stringToken = new Token(
            new TextAttribute(getColor(SQLSyntaxManager.CONFIG_COLOR_STRING)));
        final IToken numberToken = new Token(
            new TextAttribute(getColor(SQLSyntaxManager.CONFIG_COLOR_NUMBER)));
        final IToken commentToken = new SQLCommentToken(
            new TextAttribute(getColor(SQLSyntaxManager.CONFIG_COLOR_COMMENT)));
        final SQLDelimiterToken delimiterToken = new SQLDelimiterToken(
            new TextAttribute(getColor(SQLSyntaxManager.CONFIG_COLOR_DELIMITER, SWT.COLOR_RED)));
        final SQLParameterToken parameterToken = new SQLParameterToken(
            new TextAttribute(getColor(SQLSyntaxManager.CONFIG_COLOR_PARAMETER, SWT.COLOR_DARK_BLUE), backgroundColor, SWT.BOLD));
        final IToken otherToken = new Token(
            new TextAttribute(getColor(SQLSyntaxManager.CONFIG_COLOR_TEXT)));

        setDefaultReturnToken(otherToken);
        List<IRule> rules = new ArrayList<IRule>();

        // Add rule for single-line comments.
        for (String lineComment : getKeywordManager().getSingleLineComments()) {
            rules.add(new EndOfLineRule(lineComment, commentToken)); //$NON-NLS-1$
        }

        // Add rules for delimited identifiers and string literals.
        rules.add(new NestedMultiLineRule(quoteSymbol, quoteSymbol, stringToken, '\\'));
        if (!quoteSymbol.equals(SQLConstants.STR_QUOTE_SINGLE)) {
            rules.add(new NestedMultiLineRule(SQLConstants.STR_QUOTE_SINGLE, SQLConstants.STR_QUOTE_SINGLE, stringToken, '\\')); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        }
        if (!quoteSymbol.equals(SQLConstants.STR_QUOTE_DOUBLE)) {
            rules.add(new NestedMultiLineRule(SQLConstants.STR_QUOTE_DOUBLE, SQLConstants.STR_QUOTE_DOUBLE, stringToken, '\\')); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        }

        // Add rules for multi-line comments
        rules.add(new NestedMultiLineRule(SQLConstants.ML_COMMENT_START, SQLConstants.ML_COMMENT_END, commentToken, (char) 0, true));

        // Add generic whitespace rule.
        rules.add(new WhitespaceRule(new SQLWhiteSpaceDetector()));

        // Add numeric rule
        rules.add(new NumberRule(numberToken));

        // Add word rule for keywords, types, and constants.
        WordRule wordRule = new WordRule(new SQLWordDetector(), otherToken, true);
        for (String reservedWord : keywordManager.getReservedWords()) {
            wordRule.addWord(reservedWord, keywordToken);
        }
        for (String function : keywordManager.getFunctions()) {
            wordRule.addWord(function, typeToken);
        }
        for (String type : keywordManager.getTypes()) {
            wordRule.addWord(type, typeToken);
        }
        rules.add(wordRule);

        {
            // Delimiter rule
            WordRule delimRule = new WordRule(new IWordDetector() {
                public boolean isWordStart(char c)
                {
                    return statementDelimiter.charAt(0) == c;
                }

                public boolean isWordPart(char c)
                {
                    return statementDelimiter.indexOf(c) != -1;
                }
            });
            delimRule.addWord(statementDelimiter, delimiterToken);
            rules.add(delimRule);
        }

        {
            // Parameter rule
            IRule parameterRule = new IRule() {
                private StringBuilder buffer = new StringBuilder();

                public IToken evaluate(ICharacterScanner scanner)
                {
                    int column = scanner.getColumn();
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
