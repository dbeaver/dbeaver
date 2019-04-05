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

package org.jkiss.dbeaver.model.sql.format.tokenized;

import org.jkiss.dbeaver.ModelPreferences;
import org.jkiss.dbeaver.model.DBPIdentifierCase;
import org.jkiss.dbeaver.model.sql.format.SQLFormatter;
import org.jkiss.dbeaver.model.sql.format.SQLFormatterConfiguration;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.utils.ArrayUtils;

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

    private boolean isCompact;


    @Override
    public String format(final String argSql, SQLFormatterConfiguration configuration) {
        formatterCfg = configuration;

        for (String delim : formatterCfg.getSyntaxManager().getStatementDelimiters()) {
            statementDelimiters.add(delim.toUpperCase(Locale.ENGLISH));
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

    private List<FormatterToken> format(final List<FormatterToken> argList) {
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

            if (prev.getType() != TokenType.SPACE &&
                    token.getType() != TokenType.SPACE &&
                    !prev.getString().equals("(") &&
                    !token.getString().startsWith("(") &&
                    !prev.getString().equals(")") &&
                    !token.getString().equals(")")) {
                if (token.getString().equals(",") || statementDelimiters.contains(token.getString())) { //$NON-NLS-1$
                    continue;
                }
                if (formatterCfg.isFunction(prev.getString()) && token.getString().equals("(")) { //$NON-NLS-1$
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

    private void removeSpacesAroundCommentToken(List<FormatterToken> argList) {
        FormatterToken token;// Remove extra tokens (spaces, etc)
        for (int index = argList.size() - 1; index >= 1; index--) {
            token = argList.get(index);
            FormatterToken prevToken = argList.get(index - 1);
            if (token.getType() == TokenType.SPACE && prevToken.getType() == TokenType.COMMENT) {
                argList.remove(index);
            } else if (token.getType() == TokenType.COMMENT && prevToken.getType() == TokenType.SPACE) {
                argList.remove(index - 1);
            } else if (token.getType() == TokenType.SPACE) {
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
                        argList.add(i, new FormatterToken(TokenType.SYMBOL, statementDelimiters.get(0)));
                        i++;
                    }
                }
            }
        }
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
            case "\"":
            case "`":
                return true;
            default:
                return false;
        }
    }





}