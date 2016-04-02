/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2016 Serge Rieder (serge@jkiss.org)
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
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.sql.SQLConstants;
import org.jkiss.dbeaver.model.sql.SQLDialect;
import org.jkiss.dbeaver.model.sql.SQLSyntaxManager;
import org.jkiss.dbeaver.ui.editors.sql.syntax.tokens.*;
import org.jkiss.dbeaver.ui.editors.text.TextWhiteSpaceDetector;
import org.jkiss.utils.CommonUtils;
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
        for (IRule rule : fRules) {
            if (rule instanceof DelimiterRule) {
                ((DelimiterRule) rule).changeDelimiter(null);
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

    public void refreshRules(DBPDataSource dataSource)
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
        final SQLBlockToggleToken blockToggleToken = new SQLBlockToggleToken(
            new TextAttribute(getColor(SQLConstants.CONFIG_COLOR_DELIMITER), null, SWT.BOLD));

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

        DelimiterRule delimRule = new DelimiterRule(syntaxManager.getStatementDelimiters(), delimiterToken);
        rules.add(delimRule);

        {
            // Delimiter redefine
            String delimRedefine = dialect.getScriptDelimiterRedefiner();
            if (!CommonUtils.isEmpty(delimRedefine)) {
                final SQLSetDelimiterToken setDelimiterToken = new SQLSetDelimiterToken(
                    new TextAttribute(getColor(SQLConstants.CONFIG_COLOR_COMMAND), null, SWT.BOLD));

                rules.add(new SetDelimiterRule(delimRedefine, setDelimiterToken, delimRule));
            }
        }

        // Add word rule for keywords, types, and constants.
        WordRule wordRule = new WordRule(new SQLWordDetector(), otherToken, true);
        for (String reservedWord : dialect.getReservedWords()) {
            wordRule.addWord(reservedWord, keywordToken);
        }
        for (String function : dialect.getFunctions(dataSource)) {
            wordRule.addWord(function, typeToken);
        }
        for (String type : dialect.getDataTypes(dataSource)) {
            wordRule.addWord(type, typeToken);
        }
        wordRule.addWord(SQLConstants.BLOCK_BEGIN, blockBeginToken);
        wordRule.addWord(SQLConstants.BLOCK_END, blockEndToken);
        rules.add(wordRule);

        final String blockToggleString = dialect.getBlockToggleString();
        if (!CommonUtils.isEmpty(blockToggleString)) {
            WordRule blockToggleRule = new WordRule(getWordOrSymbolDetector(blockToggleString), Token.UNDEFINED, true);
            blockToggleRule.addWord(blockToggleString, blockToggleToken);
            rules.add(blockToggleRule);
        }

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

    private static class DelimiterRule implements IRule {
        private final IToken token;
        private char[][] delimiters, origDelimiters;
        private char[] buffer, origBuffer;
        public DelimiterRule(Collection<String> delimiters, IToken token) {
            this.token = token;
            this.origDelimiters = this.delimiters = new char[delimiters.size()][];
            int index = 0, maxLength = 0;
            for (Iterator<String> iter = delimiters.iterator(); iter.hasNext(); ) {
                this.delimiters[index] = iter.next().toCharArray();
                for (int i = 0; i < this.delimiters[index].length; i++) {
                    this.delimiters[index][i] = Character.toUpperCase(this.delimiters[index][i]);
                }
                maxLength = Math.max(maxLength, this.delimiters[index].length);
                index++;
            }
            this.origBuffer = this.buffer = new char[maxLength];
        }

        @Override
        public IToken evaluate(ICharacterScanner scanner) {
            for (int i = 0; ; i++) {
                int c = scanner.read();
                boolean matches = false;
                if (c != ICharacterScanner.EOF) {
                    c = Character.toUpperCase(c);
                    for (int k = 0; k < delimiters.length; k++) {
                        if (i < delimiters[k].length && delimiters[k][i] == c) {
                            buffer[i] = (char)c;
                            if (i == delimiters[k].length - 1 && equalsBegin(delimiters[k])) {
                                // Matched. Check next character
                                if (Character.isLetterOrDigit(c)) {
                                    int cn = scanner.read();
                                    scanner.unread();
                                    if (Character.isLetterOrDigit(cn)) {
                                        matches = false;
                                        continue;
                                    }
                                }
                                return token;
                            }
                            matches = true;
                            break;
                        }
                    }
                }
                if (!matches) {
                    for (int k = 0; k <= i; k++) {
                        scanner.unread();
                    }
                    return Token.UNDEFINED;
                }
            }
        }

        private boolean equalsBegin(char[] delimiter) {
            for (int i = 0; i < delimiter.length; i++) {
                if (buffer[i] != delimiter[i]) {
                    return false;
                }
            }
            return true;
        }

        public void changeDelimiter(String newDelimiter) {
            if (CommonUtils.isEmpty(newDelimiter)) {
                this.delimiters = this.origDelimiters;
                this.buffer = this.origBuffer;
            } else {
                this.delimiters = new char[1][];
                this.delimiters[0] = newDelimiter.toUpperCase(Locale.ENGLISH).toCharArray();
                this.buffer = new char[newDelimiter.length()];
            }
        }
    }

    private class ParametersRule implements IRule {
        private final SQLParameterToken parameterToken;
        private final StringBuilder buffer;
        private final char anonymousParameterMark;
        private final char namedParameterPrefix;

        public ParametersRule(SQLParameterToken parameterToken) {
            this.parameterToken = parameterToken;
            buffer = new StringBuilder();
            anonymousParameterMark = syntaxManager.getAnonymousParameterMark();
            namedParameterPrefix = syntaxManager.getNamedParameterPrefix();
        }

        @Override
        public IToken evaluate(ICharacterScanner scanner)
        {
            scanner.unread();
            int prevChar = scanner.read();
            if (Character.isJavaIdentifierPart(prevChar) ||
                prevChar == namedParameterPrefix || prevChar == anonymousParameterMark || prevChar == '\\' || prevChar == '/')
            {
                return Token.UNDEFINED;
            }
            int c = scanner.read();
            if (c != ICharacterScanner.EOF && (c == anonymousParameterMark || c == namedParameterPrefix)) {
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
                    if (buffer.charAt(0) == namedParameterPrefix && buffer.length() > 1) {
                        boolean validChars = true;
                        for (int i = 1; i < buffer.length(); i++) {
                            if (!Character.isJavaIdentifierPart(buffer.charAt(i))) {
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

    private static class SetDelimiterRule implements IRule {

        private final String setDelimiterWord;
        private final SQLSetDelimiterToken setDelimiterToken;
        private final DelimiterRule delimiterRule;

        public SetDelimiterRule(String setDelimiterWord, SQLSetDelimiterToken setDelimiterToken, DelimiterRule delimiterRule) {
            this.setDelimiterWord = setDelimiterWord;
            this.setDelimiterToken = setDelimiterToken;
            this.delimiterRule = delimiterRule;
        }

        @Override
        public IToken evaluate(ICharacterScanner scanner) {
            // Must be in the beginning of line
            {
                scanner.unread();
                int prevChar = scanner.read();
                if (prevChar != ICharacterScanner.EOF && prevChar != '\r' && prevChar != '\n') {
                    return Token.UNDEFINED;
                }
            }

            for (int i = 0; i < setDelimiterWord.length(); i++) {
                char c = setDelimiterWord.charAt(i);
                final int nextChar = scanner.read();
                if (Character.toUpperCase(nextChar) != c) {
                    // Doesn't match
                    for (int k = 0; k <= i; k++) {
                        scanner.unread();
                    }
                    return Token.UNDEFINED;
                }
            }
            StringBuilder delimBuffer = new StringBuilder();

            int next = scanner.read();
            if (next == ICharacterScanner.EOF || next == '\n' || next == '\r') {
                // Empty delimiter
                scanner.unread();
            } else {
                if (!Character.isWhitespace(next)) {
                    for (int k = 0; k < setDelimiterWord.length() + 1; k++) {
                        scanner.unread();
                    }
                    return Token.UNDEFINED;
                }
                // Get everything till the end of line
                for (; ; ) {
                    next = scanner.read();
                    if (next == ICharacterScanner.EOF || next == '\n' || next == '\r') {
                        break;
                    }
                    delimBuffer.append((char) next);
                }
                scanner.unread();
            }
            if (scanner instanceof SQLRuleManager && ((SQLRuleManager) scanner).isEvalMode()) {
                final String newDelimiter = delimBuffer.toString().trim();
                delimiterRule.changeDelimiter(newDelimiter);
            }

            return setDelimiterToken;
        }
    }

}
