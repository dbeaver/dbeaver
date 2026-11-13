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

package org.jkiss.dbeaver.model.sql.format.tokenized;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.ModelPreferences;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPIdentifierCase;
import org.jkiss.dbeaver.model.sql.SQLConstants;
import org.jkiss.dbeaver.model.sql.format.SQLFormatter;
import org.jkiss.dbeaver.model.sql.format.SQLFormatterConfiguration;
import org.jkiss.dbeaver.model.sql.parser.SQLRuleManager;
import org.jkiss.dbeaver.model.sql.parser.rules.SQLBatchDelimiterRule;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.utils.ArrayUtils;
import org.jkiss.utils.CommonUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * SQL formatter
 */
public class SQLFormatterTokenized implements SQLFormatter {

    public static final String FORMATTER_ID = "DEFAULT";

    private static final String[] DML_KEYWORD = { "SELECT", "UPDATE", "INSERT", "DELETE" };



    private SQLFormatterConfiguration formatterCfg;

    private List<String> statementDelimiters = new ArrayList<>(2);
    // The same batch delimiter rules (e.g. GO) which the script parser uses
    private List<SQLBatchDelimiterRule> batchDelimiterRules = List.of();

    private boolean isCompact;


    @Override
    public String format(final String argSql, SQLFormatterConfiguration configuration) {
        formatterCfg = configuration;

        for (String delim : formatterCfg.getSyntaxManager().getStatementDelimiters()) {
            if (!CommonUtils.isEmptyTrimmed(delim)) {
                statementDelimiters.add(delim.toUpperCase(Locale.ENGLISH));
            }
        }
        SQLTokensParser fParser = new SQLTokensParser(formatterCfg);

        boolean isSqlEndsWithNewLine = false;
        if (argSql.endsWith("\n")) { //$NON-NLS-1$
            isSqlEndsWithNewLine = true;
        }

        List<FormatterToken> list = fParser.parse(argSql);
        list = format(list);

        StringBuilder after = new StringBuilder(argSql.length() + 20);
        for (FormatterToken token : list) {
            after.append(token.getString());
        }

        if (isSqlEndsWithNewLine) {
            after.append(GeneralUtils.getDefaultLineSeparator());
        }

        return after.toString();
    }

    public boolean isCompact() {
        return isCompact;
    }

    public void setCompact(boolean compact) {
        isCompact = compact;
    }

    private List<FormatterToken> format(@NotNull List<FormatterToken> argList) {
        if (argList.isEmpty()) {
            return argList;
        }

        if (isEmptyAfterSpaceRemoving(argList, 0) ||
                isEmptyAfterSpaceRemoving(argList, argList.size() - 1)){
            return argList;
        }

        transformCase(argList);

        if (formatterCfg.getPreferenceStore().getBoolean(ModelPreferences.SQL_FORMAT_INSERT_DELIMITERS_IN_EMPTY_LINES)) {
            convertEmptyLinesIntoDelimiters(argList);
        }
        removeSpacesAroundCommentToken(argList);

        concatenateDoublewordedKeywords(argList);

        IndentFormatter indentFormatter = new IndentFormatter(formatterCfg, isCompact);
        indentFormatter.format(argList);

        trimSpacesBetweenBraces(argList);

        insertSpaces(argList);

        return argList;
    }

    private void insertSpaces(List<FormatterToken> argList) {
        FormatterToken token;
        for (int index = 1; index < argList.size(); index++) {
            FormatterToken prev = argList.get(index - 1);
            token = argList.get(index);

            String prevString = prev.getString();
            String curString = token.getString();
            if (prev.getType() != TokenType.SPACE &&
                    token.getType() != TokenType.SPACE &&
                    !prevString.equals("(") &&
                    !curString.startsWith("(") &&
                    !prevString.equals(")") &&
                    !curString.equals(")")) {
                if (curString.equals(",") || statementDelimiters.contains(curString)) { //$NON-NLS-1$
                    continue;
                }
                if (formatterCfg.isFunction(prevString) && curString.equals("(")) { //$NON-NLS-1$
                    continue;
                }
                if (token.getType() == TokenType.VALUE && prev.getType() == TokenType.NAME) {
                    // Do not add space between name and value [JDBC:MSSQL]
                    continue;
                }
                if (token.getType() == TokenType.SYMBOL && isEmbeddedToken(token) ||
                        prev.getType() == TokenType.SYMBOL && isEmbeddedToken(prev)) {
                    // Do not insert spaces around colons
                    continue;
                }
                if (token.getType() == TokenType.SYMBOL && prev.getType() == TokenType.SYMBOL) {
                    // Do not add space between symbols
                    continue;
                }
                if ("+".equals(curString) &&
                    prevString.length() > 1 && Character.isDigit(prevString.charAt(0)) &&
                    (prevString.charAt(prevString.length() - 1) == 'E' || prevString.charAt(prevString.length() - 1) == 'e'))
                {
                    // Possible exponential numbers notation (5E+6)
                    index++;
                    continue;
                }
                argList.add(index, new FormatterToken(TokenType.SPACE, " ")); //$NON-NLS-1$
            }
        }
    }

    private void trimSpacesBetweenBraces(List<FormatterToken> argList) {
        for (int index = argList.size() - 1; index >= 4; index--) {
            if (index >= argList.size()) {
                continue;
            }

            FormatterToken t0 = argList.get(index);
            FormatterToken t1 = argList.get(index - 1);
            FormatterToken t2 = argList.get(index - 2);
            FormatterToken t3 = argList.get(index - 3);
            FormatterToken t4 = argList.get(index - 4);

            if (t4.getString().equals("(") //$NON-NLS-1$
                    && t3.getString().trim().isEmpty()
                    && t1.getString().trim().isEmpty()
                    && t0.getString().equalsIgnoreCase(")")) //$NON-NLS-1$
            {
                // "( TOKEN )"
                t4.setString(t4.getString() + t2.getString() + t0.getString());
                argList.remove(index);
                argList.remove(index - 1);
                argList.remove(index - 2);
                argList.remove(index - 3);
            } else if (t3.getString().equals("(") &&
                    t2.getString().trim().isEmpty() &&
                    t0.getString().equalsIgnoreCase(")")) {
                // "( TOKEN)"
                t3.setString(t3.getString() + t1.getString() + t0.getString());
                argList.remove(index);
                argList.remove(index - 1);
                argList.remove(index - 2);
            } else if (t3.getString().equals("(") &&
                    t1.getString().trim().isEmpty() &&
                    t0.getString().equalsIgnoreCase(")")) {
                // "(TOKEN )"
                t3.setString(t3.getString() + t2.getString() + t0.getString());
                argList.remove(index);
                argList.remove(index - 1);
                argList.remove(index - 2);
            }
        }
    }

    private void transformCase(List<FormatterToken> argList) {
        final DBPIdentifierCase keywordCase = formatterCfg.getKeywordCase();
        for (FormatterToken token : argList) {
            if (token.getType() == TokenType.KEYWORD) {
                token.setString(keywordCase.transform(token.getString()));
            }
        }
    }

    private boolean isEmptyAfterSpaceRemoving(List<FormatterToken> argList, int tokenPosition) {
        FormatterToken token = argList.get(tokenPosition);
        if (token.getType() == TokenType.SPACE) {
            argList.remove(tokenPosition);
            return argList.isEmpty();
        }
        return false;
    }

    private static void removeSpacesAroundCommentToken(List<? extends FormatterToken> argList) {
        // Remove extra tokens (spaces, etc)
        // We do not, however, remove spaces between comments which occupy the entire string.
        // That means, we do not delete spaces around comments if spaces contain line separator.
        CharSequence separator = "\n";
        for (int index = argList.size() - 1; index >= 1; index--) {
            FormatterToken token = argList.get(index);
            FormatterToken prevToken = argList.get(index - 1);
            boolean isTokenNotContainsSeparator = !token.getString().contains(separator);
            if (prevToken.getType() == TokenType.COMMENT && token.getType() == TokenType.SPACE && isTokenNotContainsSeparator) {
                argList.remove(index);
            } else if (prevToken.getType() == TokenType.SPACE && !prevToken.getString().contains(separator) && token.getType() == TokenType.COMMENT) {
                argList.remove(index - 1);
            } else if (token.getType() == TokenType.SPACE && isTokenNotContainsSeparator) {
                token.setString(" "); //$NON-NLS-1$
            }
        }
    }

    private void convertEmptyLinesIntoDelimiters(@NotNull List<FormatterToken> argList) {
        String[] executableCommentPrefixes = formatterCfg.getSyntaxManager().getDialect().getExecutableCommentPrefixes();
        DBPDataSource dataSource = formatterCfg.getDataSource();
        batchDelimiterRules = SQLRuleManager.createBatchDelimiterRules(
            formatterCfg.getSyntaxManager(),
            dataSource == null ? null : dataSource.getContainer());
        boolean hasQueryContent = false;
        // Position right after the last token of the current statement which is not a comment
        int contentEndIndex = 0;
        for (int i = 0; i < argList.size(); i++) {
            FormatterToken token = argList.get(i);
            if (token.getType() == TokenType.SPACE) {
                int lfCount = 0;
                for (int k = 0; k < token.getString().length(); k++) {
                    if (token.getString().charAt(k) == '\n') {
                        lfCount++;
                    }
                }
                if (lfCount > 1) {
                    if (i > 0 && isStatementDelimiter(argList, i - 1)) {
                        // Do nothing - there is a delimiter already
                    } else {
                        int delimiterIndex = findDelimiterIndex(argList, i, hasQueryContent, contentEndIndex, executableCommentPrefixes);
                        if (delimiterIndex >= 0) {
                            argList.add(delimiterIndex, new FormatterToken(TokenType.SYMBOL, statementDelimiters.get(0)));
                            hasQueryContent = false;
                            i++;
                        }
                    }
                }
            } else if (isStatementDelimiter(argList, i)) {
                hasQueryContent = false;
                // The delimiter suffix (e.g. the repeat count in GO 5) is not a part of a query
                i = skipBatchDelimiterSuffix(argList, i);
            } else if (!isPlainComment(token, executableCommentPrefixes)) {
                hasQueryContent = true;
                contentEndIndex = i + 1;
            }
        }
    }

    private int findDelimiterIndex(
        @NotNull List<FormatterToken> argList,
        int emptyLineIndex,
        boolean hasQueryContent,
        int contentEndIndex,
        @NotNull String[] executableCommentPrefixes
    ) {
        if (!hasQueryContent) {
            // There is nothing to terminate: the statement is empty or has only comments
            return -1;
        }
        int commentIndex = emptyLineIndex - 1;
        if (commentIndex < 0 || !isPlainComment(argList.get(commentIndex), executableCommentPrefixes)) {
            return emptyLineIndex;
        }
        if (argList.get(commentIndex).getString().startsWith(SQLConstants.ML_COMMENT_START)) {
            // The delimiter is valid right after a multi-line comment
            return emptyLineIndex;
        }
        // A delimiter after a single-line comment would end up inside of it,
        // so it goes right after the query, before all the trailing comments
        return contentEndIndex;
    }

    private boolean isStatementDelimiter(@NotNull List<FormatterToken> argList, int index) {
        String text = argList.get(index).getString();
        // Delimiters are stored in upper case, but a word delimiter (e.g. GO) may be written in any case
        if (!statementDelimiters.contains(text.toUpperCase(Locale.ENGLISH))) {
            return false;
        }
        SQLBatchDelimiterRule rule = findBatchDelimiterRule(text);
        // A batch delimiter is valid only where the script parser accepts it (e.g. on a line of its own),
        // otherwise it is an identifier (e.g. an alias)
        return rule == null || getBatchDelimiterSuffixLength(argList, index, rule) >= 0;
    }

    @Nullable
    private SQLBatchDelimiterRule findBatchDelimiterRule(@NotNull String text) {
        for (SQLBatchDelimiterRule rule : batchDelimiterRules) {
            if (rule.matches(text)) {
                return rule;
            }
        }
        return null;
    }

    /**
     * Returns the index of the last token which belongs to the batch delimiter at the given index
     * or the given index if it is not a batch delimiter or it has no suffix.
     */
    private int skipBatchDelimiterSuffix(@NotNull List<FormatterToken> argList, int delimiterIndex) {
        SQLBatchDelimiterRule rule = findBatchDelimiterRule(argList.get(delimiterIndex).getString());
        int suffixLength = rule == null ? -1 : getBatchDelimiterSuffixLength(argList, delimiterIndex, rule);
        int lastIndex = delimiterIndex;
        for (int length = 0; length < suffixLength && lastIndex + 1 < argList.size(); ) {
            length += argList.get(++lastIndex).getString().length();
        }
        return lastIndex;
    }

    /**
     * Asks the batch delimiter rule if the token starts a batch delimiter in the line which contains it.
     * The tokens keep the original text, so the line is restored from them.
     *
     * @return the length of the delimiter suffix which follows the token (e.g. " 5" in GO 5)
     * or -1 if the line is not a batch delimiter line
     */
    private static int getBatchDelimiterSuffixLength(
        @NotNull List<FormatterToken> argList,
        int delimiterIndex,
        @NotNull SQLBatchDelimiterRule rule
    ) {
        String delimiter = argList.get(delimiterIndex).getString();
        StringBuilder prefix = new StringBuilder();
        for (int i = delimiterIndex - 1; i >= 0; i--) {
            FormatterToken token = argList.get(i);
            int lineBreak = token.getType() == TokenType.SPACE ? token.getString().lastIndexOf('\n') : -1;
            if (lineBreak >= 0) {
                prefix.insert(0, token.getString().substring(lineBreak + 1));
                break;
            }
            prefix.insert(0, token.getString());
        }
        StringBuilder rest = new StringBuilder();
        for (int i = delimiterIndex + 1; i < argList.size(); i++) {
            FormatterToken token = argList.get(i);
            int lineBreak = token.getType() == TokenType.SPACE ? indexOfLineBreak(token.getString()) : -1;
            if (lineBreak >= 0) {
                rest.append(token.getString(), 0, lineBreak);
                break;
            }
            rest.append(token.getString());
        }
        int matchEnd = rule.findMatchEnd(prefix + delimiter + rest, prefix.length());
        return matchEnd < 0 ? -1 : matchEnd - prefix.length() - delimiter.length();
    }

    private static int indexOfLineBreak(@NotNull String text) {
        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) == '\n' || text.charAt(i) == '\r') {
                return i;
            }
        }
        return -1;
    }

    private static boolean isPlainComment(@NotNull FormatterToken token, @NotNull String[] executableCommentPrefixes) {
        if (token.getType() != TokenType.COMMENT) {
            return false;
        }
        for (String prefix : executableCommentPrefixes) {
            if (token.getString().startsWith(prefix)) {
                return false;
            }
        }
        return true;
    }

    private void concatenateDoublewordedKeywords(@NotNull List<FormatterToken> argList) {
        for (int index = 0; index < argList.size() - 2; index++) {
            FormatterToken t0 = argList.get(index);
            FormatterToken t1 = argList.get(index + 1);
            FormatterToken t2 = argList.get(index + 2);

            String tokenString = t0.getString().toUpperCase(Locale.ENGLISH);
            String token2String = t2.getString().toUpperCase(Locale.ENGLISH);
            // Concatenate tokens
            if (t0.getType() == TokenType.KEYWORD && t1.getType() == TokenType.SPACE && t2.getType() == TokenType.KEYWORD) {
                if (((tokenString.equals("ORDER") || tokenString.equals("GROUP") || tokenString.equals("CONNECT")) && token2String.equals("BY")) ||
                        ((tokenString.equals("START")) && token2String.equals("WITH")))
                {
                    t0.setString(t0.getString() + " " + t2.getString());
                    argList.remove(index + 1);
                    argList.remove(index + 1);
                }
            }

            // Oracle style joins
            if (tokenString.equals("(") && t1.getString().equals("+") && token2String.equals(")")) { //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                t0.setString("(+)"); //$NON-NLS-1$
                argList.remove(index + 1);
                argList.remove(index + 1);
            }
        }
    }

    private static String getPrevDMLKeyword(List<FormatterToken> argList, int index) {
        for (int i = index - 1; i >= 0; i--) {
            FormatterToken token = argList.get(i);
            if (token.getType() == TokenType.KEYWORD) {
                if (ArrayUtils.contains(DML_KEYWORD, token.getString())) {
                    return token.getString();
                }
            }
        }
        return null;
    }

    private static  boolean isEmbeddedToken(FormatterToken token) {
        switch (token.getString()) {
            case ":":
            case ".":
            case ">":
            case "<":
            case "[":
            case "]":
            case "#":
            case "-":
            case "'":
            case "%":
            case "\"":
            case "`":
                return true;
            default:
                return false;
        }
    }
}
