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

package org.jkiss.dbeaver.model.sql.format.tokenized;

import org.jkiss.dbeaver.model.DBPIdentifierCase;
import org.jkiss.dbeaver.model.sql.SQLUtils;
import org.jkiss.dbeaver.model.sql.format.SQLFormatter;
import org.jkiss.dbeaver.model.sql.format.SQLFormatterConfiguration;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.utils.ArrayUtils;
import org.jkiss.utils.CommonUtils;
import org.jkiss.utils.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * SQL formatter
 */
public class SQLTokenizedFormatter implements SQLFormatter {

    public static final String FORMATTER_ID = "DEFAULT";

    private static final String[] JOIN_BEGIN = { "LEFT", "RIGHT", "INNER", "OUTER", "JOIN" };

    private SQLFormatterConfiguration formatterCfg;
    private List<Boolean> functionBracket = new ArrayList<>();
    private List<String> statementDelimiters = new ArrayList<>(2);
    private String delimiterRedefiner;

    @Override
    public String format(final String argSql, SQLFormatterConfiguration configuration)
    {
        formatterCfg = configuration;

        for (String delim : formatterCfg.getSyntaxManager().getStatementDelimiters()) {
            statementDelimiters.add(delim.toUpperCase(Locale.ENGLISH));
        }
        delimiterRedefiner = formatterCfg.getSyntaxManager().getDialect().getScriptDelimiterRedefiner();
        if (delimiterRedefiner != null) {
            delimiterRedefiner = delimiterRedefiner.toUpperCase(Locale.ENGLISH);
        }
        SQLTokensParser fParser = new SQLTokensParser(formatterCfg);

        functionBracket.clear();

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

    private List<FormatterToken> format(final List<FormatterToken> argList) {
        if (argList.isEmpty()) {
            return argList;
        }

        FormatterToken token = argList.get(0);
        if (token.getType() == TokenType.SPACE) {
            argList.remove(0);
            if (argList.isEmpty()) {
                return argList;
            }
        }

        token = argList.get(argList.size() - 1);
        if (token.getType() == TokenType.SPACE) {
            argList.remove(argList.size() - 1);
            if (argList.isEmpty()) {
                return argList;
            }
        }

        final DBPIdentifierCase keywordCase = formatterCfg.getKeywordCase();
        for (int index = 0; index < argList.size(); index++) {
            token = argList.get(index);
            if (token.getType() == TokenType.KEYWORD) {
                token.setString(keywordCase.transform(token.getString()));
            }
        }

        // Remove extra tokens (spaces, etc)
        for (int index = argList.size() - 1; index >= 1; index--) {
            token = argList.get(index);
            FormatterToken prevToken = argList.get(index - 1);
            if (token.getType() == TokenType.SPACE && (prevToken.getType() == TokenType.SYMBOL || prevToken.getType() == TokenType.COMMENT)) {
                argList.remove(index);
            } else if ((token.getType() == TokenType.SYMBOL || token.getType() == TokenType.COMMENT) && prevToken.getType() == TokenType.SPACE) {
                argList.remove(index - 1);
            } else if (token.getType() == TokenType.SPACE) {
                token.setString(" "); //$NON-NLS-1$
            }
        }

        for (int index = 0; index < argList.size() - 2; index++) {
            FormatterToken t0 = argList.get(index);
            FormatterToken t1 = argList.get(index + 1);
            FormatterToken t2 = argList.get(index + 2);

            if (t0.getType() == TokenType.KEYWORD
                    && t1.getType() == TokenType.SPACE
                    && t2.getType() == TokenType.KEYWORD) {
                if (((t0.getString().equalsIgnoreCase("ORDER") || t0 //$NON-NLS-1$
                        .getString().equalsIgnoreCase("GROUP")) && t2 //$NON-NLS-1$
                        .getString().equalsIgnoreCase("BY"))) { //$NON-NLS-1$
                    t0.setString(t0.getString() + " " + t2.getString()); //$NON-NLS-1$
                    argList.remove(index + 1);
                    argList.remove(index + 1);
                }
            }

            // Oracle style joins
            if (t0.getString().equals("(") && t1.getString().equals("+") && t2.getString().equals(")")) { //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                t0.setString("(+)"); //$NON-NLS-1$
                argList.remove(index + 1);
                argList.remove(index + 1);
            }
        }

        int indent = 0;
        final List<Integer> bracketIndent = new ArrayList<>();
        FormatterToken prev = new FormatterToken(TokenType.SPACE, " "); //$NON-NLS-1$
        boolean encounterBetween = false;
        for (int index = 0; index < argList.size(); index++) {
            token = argList.get(index);
            String tokenString = token.getString().toUpperCase(Locale.ENGLISH);
            if (token.getType() == TokenType.SYMBOL) {
                if (tokenString.equals("(")) { //$NON-NLS-1$
                    functionBracket.add(formatterCfg.isFunction(prev.getString()) ? Boolean.TRUE : Boolean.FALSE);
                    bracketIndent.add(indent);
                    indent++;
                    index += insertReturnAndIndent(argList, index + 1, indent);
                } else if (tokenString.equals(")") && !bracketIndent.isEmpty() && !functionBracket.isEmpty()) { //$NON-NLS-1$
                    indent = bracketIndent.remove(bracketIndent.size() - 1);
                    index += insertReturnAndIndent(argList, index, indent);
                    functionBracket.remove(functionBracket.size() - 1);
                } else if (tokenString.equals(",")) { //$NON-NLS-1$
                    index += insertReturnAndIndent(argList, index + 1, indent);
                } else if (statementDelimiters.contains(tokenString)) { //$NON-NLS-1$
                    indent = 0;
                    index += insertReturnAndIndent(argList, index, indent);
                }
            } else if (token.getType() == TokenType.KEYWORD) {
                switch (tokenString) {
                    case "DELETE":
                    case "SELECT":
                    case "UPDATE": //$NON-NLS-1$
                    case "INSERT":
                    case "INTO":
                    case "CREATE":
                    case "DROP":
                    case "TRUNCATE":
                    case "TABLE":
                    case "CASE":  //$NON-NLS-1$
                        indent++;
                        index += insertReturnAndIndent(argList, index + 1, indent);
                        break;
                    case "FROM":
                    case "WHERE":
                    case "SET":
                    case "ORDER BY":
                    case "GROUP BY":
                    case "HAVING":  //$NON-NLS-1$
                        index += insertReturnAndIndent(argList, index, indent - 1);
                        index += insertReturnAndIndent(argList, index + 1, indent);
                        break;
                    case "LEFT":
                    case "RIGHT":
                    case "INNER":
                    case "OUTER":
                    case "JOIN":
                        if (isJoinStart(argList, index)) {
                            index += insertReturnAndIndent(argList, index, indent - 1);
                        }
                        if (tokenString.equals("JOIN")) {
                            //index += insertReturnAndIndent(argList, index + 1, indent);
                        }
                        break;
                    case "VALUES":  //$NON-NLS-1$
                        indent--;
                        index += insertReturnAndIndent(argList, index, indent);
                        break;
                    case "END":  //$NON-NLS-1$
                        indent--;
                        index += insertReturnAndIndent(argList, index, indent);
                        break;
                    case "OR":
                    case "WHEN":
                    case "ELSE":  //$NON-NLS-1$
                        index += insertReturnAndIndent(argList, index, indent);
                        break;
                    case "ON":
                        //indent++;
                        index += insertReturnAndIndent(argList, index + 1, indent);
                        break;
                    case "USING":  //$NON-NLS-1$ //$NON-NLS-2$
                        index += insertReturnAndIndent(argList, index, indent + 1);
                        break;
                    case "UNION":
                    case "INTERSECT":
                    case "EXCEPT": //$NON-NLS-1$
                        indent -= 2;
                        index += insertReturnAndIndent(argList, index, indent);
                        //index += insertReturnAndIndent(argList, index + 1, indent);
                        indent++;
                        break;
                    case "BETWEEN":  //$NON-NLS-1$
                        encounterBetween = true;
                        break;
                    case "AND":  //$NON-NLS-1$
                        if (!encounterBetween) {
                            index += insertReturnAndIndent(argList, index, indent);
                        }
                        encounterBetween = false;
                        break;
                }
            } else if (token.getType() == TokenType.COMMENT) {
                boolean isComment = false;
                String[] slComments = formatterCfg.getSyntaxManager().getDialect().getSingleLineComments();
                if (slComments != null) {
                    for (String slc : slComments) {
                        if (token.getString().startsWith(slc)) {
                            isComment = true;
                            break;
                        }
                    }
                }
                if (!isComment) {
                    Pair<String, String> mlComments = formatterCfg.getSyntaxManager().getDialect().getMultiLineComments();
                    if (mlComments != null) {
                        if (token.getString().startsWith(mlComments.getFirst())) {
                            index += insertReturnAndIndent(argList, index + 1, indent);
                        }
                    }
                }
            } else if (token.getType() == TokenType.COMMAND) {
                indent = 0;
                if (index > 0) {
                    index += insertReturnAndIndent(argList, index, 0);
                }
                index += insertReturnAndIndent(argList, index + 1, 0);
                if (!CommonUtils.isEmpty(delimiterRedefiner) && token.getString().startsWith(delimiterRedefiner)) {
                    final String command = token.getString().trim().toUpperCase(Locale.ENGLISH);
                    final int divPos = command.lastIndexOf(' ');
                    if (divPos > 0) {
                        String delimiter = command.substring(divPos).trim();
                        if (!CommonUtils.isEmpty(delimiter)) {
                            statementDelimiters.clear();
                            statementDelimiters.add(delimiter);
                        }
                    }
                }
            } else {
                if (statementDelimiters.contains(tokenString)) {
                    indent = 0;
                    index += insertReturnAndIndent(argList, index + 1, indent);
                }
            }
            prev = token;
        }

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
                t4.setString(t4.getString() + t2.getString() + t0.getString());
                argList.remove(index);
                argList.remove(index - 1);
                argList.remove(index - 2);
                argList.remove(index - 3);
            }
        }

        for (int index = 1; index < argList.size(); index++) {
            prev = argList.get(index - 1);
            token = argList.get(index);

            if (prev.getType() != TokenType.SPACE &&
                token.getType() != TokenType.SPACE &&
                !token.getString().startsWith("("))
            {
                if (token.getString().equals(",") || statementDelimiters.contains(token.getString())) { //$NON-NLS-1$
                    continue;
                }
                if (formatterCfg.isFunction(prev.getString())
                        && token.getString().equals("(")) { //$NON-NLS-1$
                    continue;
                }
                if (token.getType() == TokenType.VALUE && prev.getType() == TokenType.NAME) {
                    // Do not add space between name and value [JDBC:MSSQL]
                    continue;
                }
                if (token.getType() == TokenType.SYMBOL && isEmbeddedToken(token) ||
                    prev.getType() == TokenType.SYMBOL && isEmbeddedToken(prev))
                {
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

        return argList;
    }

    private static  boolean isEmbeddedToken(FormatterToken token) {
        return ":".equals(token.getString()) || ".".equals(token.getString());
    }

    private boolean isJoinStart(List<FormatterToken> argList, int index) {
        // Keyword sequence must start from LEFT, RIGHT, INNER, OUTER or JOIN and must end with JOIN
        // And we must be in the beginning of sequence

        // check current token
        if (!ArrayUtils.contains(JOIN_BEGIN, argList.get(index).getString())) {
            return false;
        }
        // check previous token
        for (int i = index - 1; i >= 0; i--) {
            FormatterToken token = argList.get(i);
            if (token.getType() == TokenType.SPACE) {
                continue;
            }
            if (ArrayUtils.contains(JOIN_BEGIN, token.getString())) {
                // It is not the begin of sequence
                return false;
            } else {
                break;
            }
        }
        // check last token
        for (int i = index; i < argList.size(); i++) {
            FormatterToken token = argList.get(i);
            if (token.getType() == TokenType.SPACE) {
                continue;
            }
            if (token.getString().equals("JOIN")) {
                return true;
            }
            if (!ArrayUtils.contains(JOIN_BEGIN, token.getString())) {
                // It is not the begin of sequence
                return false;
            }
        }
        return false;
    }

    private int insertReturnAndIndent(final List<FormatterToken> argList, final int argIndex, final int argIndent)
    {
        if (functionBracket.contains(Boolean.TRUE))
            return 0;
        try {
            String s = GeneralUtils.getDefaultLineSeparator();
            if (argIndex > 0) {
                final FormatterToken prevToken = argList.get(argIndex - 1);
                if (prevToken.getType() == TokenType.COMMENT &&
                    SQLUtils.isCommentLine(formatterCfg.getSyntaxManager().getDialect(), prevToken.getString()))
                {
                    s = ""; //$NON-NLS-1$
                }
            }
            for (int index = 0; index < argIndent; index++) {
                s += formatterCfg.getIndentString();
            }

            FormatterToken token = argList.get(argIndex);
            if (token.getType() == TokenType.SPACE) {
                token.setString(s);
                return 0;
            }
            boolean isDelimiter = statementDelimiters.contains(token.getString().toUpperCase());

            if (!isDelimiter) {
                token = argList.get(argIndex - 1);
                if (token.getType() == TokenType.SPACE) {
                    token.setString(s);
                    return 0;
                }
            }

            if (isDelimiter) {
                if (argList.size() > argIndex + 1) {
                    argList.add(argIndex + 1, new FormatterToken(TokenType.SPACE, s + s));
                }
            } else {
                argList.add(argIndex, new FormatterToken(TokenType.SPACE, s));
            }
            return 1;
        } catch (IndexOutOfBoundsException e) {
            e.printStackTrace();
            return 0;
        }
    }

}