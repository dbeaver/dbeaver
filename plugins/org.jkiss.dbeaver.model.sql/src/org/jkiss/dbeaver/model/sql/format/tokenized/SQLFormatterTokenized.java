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
import org.jkiss.dbeaver.ModelPreferences;
import org.jkiss.dbeaver.model.DBPIdentifierCase;
import org.jkiss.dbeaver.model.sql.SQLConstants;
import org.jkiss.dbeaver.model.sql.format.SQLFormatter;
import org.jkiss.dbeaver.model.sql.format.SQLFormatterConfiguration;
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

    // MySQL and MariaDB comments which are executed as SQL, so they are a part of a query
    private static final String[] EXECUTABLE_COMMENT_PREFIXES = { "/*!", "/*M!" };


    private SQLFormatterConfiguration formatterCfg;

    private List<String> statementDelimiters = new ArrayList<>(2);

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

    private void convertEmptyLinesIntoDelimiters(List<FormatterToken> argList) {
        for (int i= 0; i < argList.size(); i++) {
            FormatterToken token = argList.get(i);
            if (token.getType() == TokenType.SPACE) {
                int lfCount = 0;
                for (int k = 0; k < token.getString().length(); k++) {
                    if (token.getString().charAt(k) == '\n') {
                        lfCount++;
                    }
                }
                if (lfCount > 1) {
                    if (i > 0 && statementDelimiters.contains(argList.get(i - 1).getString())) {
                        // Do nothing - there is a delimiter already
                    } else {
                        int delimiterIndex = findDelimiterIndex(argList, i);
                        if (delimiterIndex >= 0) {
                            argList.add(delimiterIndex, new FormatterToken(TokenType.SYMBOL, statementDelimiters.get(0)));
                            i++;
                        }
                    }
                }
            }
        }
    }

    /**
     * Finds the position for the delimiter which replaces an empty line at the specified index.
     *
     * @return index in the list to insert the delimiter at or -1 if the delimiter is not needed
     */
    private int findDelimiterIndex(@NotNull List<FormatterToken> argList, int emptyLineIndex) {
        int commentIndex = emptyLineIndex - 1;
        if (commentIndex < 0 || !isPlainComment(argList.get(commentIndex))) {
            return emptyLineIndex;
        }
        // The query is followed by a comment. Look for the end of the query content before the comment(s)
        int contentIndex = commentIndex - 1;
        while (contentIndex >= 0
            && (argList.get(contentIndex).getType() == TokenType.SPACE || isPlainComment(argList.get(contentIndex)))
        ) {
            contentIndex--;
        }
        if (contentIndex < 0 || statementDelimiters.contains(argList.get(contentIndex).getString())) {
            // A comment is not a query, there is nothing to terminate
            return -1;
        }
        if (argList.get(commentIndex).getString().startsWith(SQLConstants.ML_COMMENT_START)) {
            // The delimiter is valid right after a multi-line comment
            return emptyLineIndex;
        }
        // A delimiter after a single-line comment would end up inside of it, so it goes before the comment
        int delimiterIndex = commentIndex;
        while (delimiterIndex > 0 && argList.get(delimiterIndex - 1).getType() == TokenType.SPACE) {
            delimiterIndex--;
        }
        return delimiterIndex;
    }

    /**
     * Checks whether the token is a comment which is not a part of a query (executable comments are).
     */
    private static boolean isPlainComment(@NotNull FormatterToken token) {
        if (token.getType() != TokenType.COMMENT) {
            return false;
        }
        for (String prefix : EXECUTABLE_COMMENT_PREFIXES) {
            if (token.getString().startsWith(prefix)) {
                return false;
            }
        }
        return true;
    }

    private void concatenateDoublewordedKeywords(List<FormatterToken> argList) {
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
