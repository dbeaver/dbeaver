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

package org.jkiss.dbeaver.model.sql.format.tokenized;

import org.jkiss.dbeaver.model.sql.format.SQLFormatter;
import org.jkiss.dbeaver.model.sql.format.SQLFormatterConfiguration;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.utils.Pair;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * SQL formatter
 */
public class SQLTokenizedFormatter implements SQLFormatter {
    private SQLFormatterConfiguration formatterCfg;
    private List<Boolean> functionBracket = new ArrayList<>();
    private Collection<String> statementDelimiters = new ArrayList<>(2);

    @Override
    public String format(final String argSql, SQLFormatterConfiguration configuration)
    {
        formatterCfg = configuration;
        statementDelimiters = formatterCfg.getSyntaxManager().getStatementDelimiters();
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
        if (token.getType() == FormatterConstants.SPACE) {
            argList.remove(0);
            if (argList.isEmpty()) {
                return argList;
            }
        }

        token = argList.get(argList.size() - 1);
        if (token.getType() == FormatterConstants.SPACE) {
            argList.remove(argList.size() - 1);
            if (argList.isEmpty()) {
                return argList;
            }
        }

        for (int index = 0; index < argList.size(); index++) {
            token = argList.get(index);
            if (token.getType() == FormatterConstants.KEYWORD) {
                switch (formatterCfg.getKeywordCase()) {
                case SQLFormatterConfiguration.KEYWORD_NONE:
                    break;
                case SQLFormatterConfiguration.KEYWORD_UPPER_CASE:
                    token.setString(token.getString().toUpperCase());
                    break;
                case SQLFormatterConfiguration.KEYWORD_LOWER_CASE:
                    token.setString(token.getString().toLowerCase());
                    break;
                }
            }
        }

        // Remove extra tokens (spaces, etc)
        for (int index = argList.size() - 1; index >= 1; index--) {
            token = argList.get(index);
            FormatterToken prevToken = argList.get(index - 1);
            if (token.getType() == FormatterConstants.SPACE && (prevToken.getType() == FormatterConstants.SYMBOL || prevToken.getType() == FormatterConstants.COMMENT)) {
                argList.remove(index);
            } else if ((token.getType() == FormatterConstants.SYMBOL || token.getType() == FormatterConstants.COMMENT) && prevToken.getType() == FormatterConstants.SPACE) {
                argList.remove(index - 1);
            } else if (token.getType() == FormatterConstants.SPACE) {
                token.setString(" "); //$NON-NLS-1$
            }
        }

        for (int index = 0; index < argList.size() - 2; index++) {
            FormatterToken t0 = argList.get(index);
            FormatterToken t1 = argList.get(index + 1);
            FormatterToken t2 = argList.get(index + 2);

            if (t0.getType() == FormatterConstants.KEYWORD
                    && t1.getType() == FormatterConstants.SPACE
                    && t2.getType() == FormatterConstants.KEYWORD) {
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
        FormatterToken prev = new FormatterToken(FormatterConstants.SPACE, " "); //$NON-NLS-1$
        boolean encounterBetween = false;
        for (int index = 0; index < argList.size(); index++) {
            token = argList.get(index);
            String tokenString = token.getString().toUpperCase();
            if (token.getType() == FormatterConstants.SYMBOL) {
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
            } else if (token.getType() == FormatterConstants.KEYWORD) {
                if (tokenString.equals("DELETE") //$NON-NLS-1$
                        || tokenString.equals("SELECT") //$NON-NLS-1$
                        || tokenString.equals("UPDATE")) //$NON-NLS-1$
                {
                    indent++;
                    index += insertReturnAndIndent(argList, index + 1, indent);
                } else if (tokenString.equals("INSERT") //$NON-NLS-1$
                        || tokenString.equals("INTO") //$NON-NLS-1$
                        || tokenString.equals("CREATE") //$NON-NLS-1$
                        || tokenString.equals("DROP") //$NON-NLS-1$
                        || tokenString.equals("TRUNCATE") //$NON-NLS-1$
                        || tokenString.equals("TABLE") //$NON-NLS-1$
                        || tokenString.equals("CASE")) { //$NON-NLS-1$
                    indent++;
                    index += insertReturnAndIndent(argList, index + 1, indent);
                } else if (tokenString.equals("FROM") //$NON-NLS-1$
                        || tokenString.equals("WHERE") //$NON-NLS-1$
                        || tokenString.equals("SET") //$NON-NLS-1$
                        || tokenString.equals("ORDER BY") //$NON-NLS-1$
                        || tokenString.equals("GROUP BY") //$NON-NLS-1$
                        || tokenString.equals("HAVING")) { //$NON-NLS-1$
                    index += insertReturnAndIndent(argList, index, indent - 1);
                    index += insertReturnAndIndent(argList, index + 1, indent);
                } else if (tokenString.equals("VALUES")) { //$NON-NLS-1$
                    indent--;
                    index += insertReturnAndIndent(argList, index, indent);
                } else if (tokenString.equals("END")) { //$NON-NLS-1$
                    indent--;
                    index += insertReturnAndIndent(argList, index, indent);
                } else if (tokenString.equals("OR") //$NON-NLS-1$
                        || tokenString.equals("THEN") //$NON-NLS-1$
                        || tokenString.equals("ELSE")) { //$NON-NLS-1$
                    index += insertReturnAndIndent(argList, index, indent);
                } else if (tokenString.equals("ON") || tokenString.equals("USING")) { //$NON-NLS-1$ //$NON-NLS-2$
                    index += insertReturnAndIndent(argList, index, indent + 1);
                } else if (tokenString.equals("UNION") //$NON-NLS-1$
                    || tokenString.equals("INTERSECT") //$NON-NLS-1$
                    || tokenString.equals("EXCEPT")) //$NON-NLS-1$
                {
                    indent -= 2;
                    index += insertReturnAndIndent(argList, index, indent);
                    //index += insertReturnAndIndent(argList, index + 1, indent);
                    indent++;
                } else if (tokenString.equals("BETWEEN")) { //$NON-NLS-1$
                    encounterBetween = true;
                } else if (tokenString.equals("AND")) { //$NON-NLS-1$
                    if (!encounterBetween) {
                        index += insertReturnAndIndent(argList, index, indent);
                    }
                    encounterBetween = false;
                }
            } else if (token.getType() == FormatterConstants.COMMENT) {
                Pair<String, String> mlComments = formatterCfg.getSyntaxManager().getDialect().getMultiLineComments();
                if (mlComments != null) {
                    if (token.getString().startsWith(mlComments.getFirst())) {
                        index += insertReturnAndIndent(argList, index + 1, indent);
                    }
                }
            } else {
                if (statementDelimiters.contains(tokenString)) {
                    indent = 0;
                    index += insertReturnAndIndent(argList, index, indent);
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

            if (t4.getString().equalsIgnoreCase("(") //$NON-NLS-1$
                    && t3.getString().trim().isEmpty()
                    && t1.getString().trim().isEmpty()
                    && t0.getString().equalsIgnoreCase(")")) { //$NON-NLS-1$
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

            if (prev.getType() != FormatterConstants.SPACE &&
                token.getType() != FormatterConstants.SPACE &&
                !token.getString().startsWith("("))
            {
                if (token.getString().equals(",") || statementDelimiters.contains(token.getString())) { //$NON-NLS-1$
                    continue;
                }
                if (formatterCfg.isFunction(prev.getString())
                        && token.getString().equals("(")) { //$NON-NLS-1$
                    continue;
                }
                if (token.getType() == FormatterConstants.VALUE && prev.getType() == FormatterConstants.NAME) {
                    // Do not add space between name and value [JDBC:MSSQL]
                    continue;
                }
                argList.add(index, new FormatterToken(FormatterConstants.SPACE, " ")); //$NON-NLS-1$
            }
        }

        return argList;
    }

    private int insertReturnAndIndent(final List<FormatterToken> argList, final int argIndex, final int argIndent)
    {
        if (functionBracket.contains(Boolean.TRUE))
            return 0;
        try {
            String s = GeneralUtils.getDefaultLineSeparator();
            final FormatterToken prevToken = argList.get(argIndex - 1);
            if (prevToken.getType() == FormatterConstants.COMMENT && prevToken.getString().startsWith("--")) { //$NON-NLS-1$
                s = ""; //$NON-NLS-1$
            }
            for (int index = 0; index < argIndent; index++) {
                s += formatterCfg.getIndentString();
            }

            FormatterToken token = argList.get(argIndex);
            if (token.getType() == FormatterConstants.SPACE) {
                token.setString(s);
                return 0;
            }
            boolean isDelimiter = statementDelimiters.contains(token.getString().toUpperCase());

            if (!isDelimiter) {
                token = argList.get(argIndex - 1);
                if (token.getType() == FormatterConstants.SPACE) {
                    token.setString(s);
                    return 0;
                }
            }

            if (isDelimiter) {
                if (argList.size() > argIndex + 1) {
                    argList.add(argIndex + 1, new FormatterToken(FormatterConstants.SPACE, s + s));
                }
            } else {
                argList.add(argIndex, new FormatterToken(FormatterConstants.SPACE, s));
            }
            return 1;
        } catch (IndexOutOfBoundsException e) {
            e.printStackTrace();
            return 0;
        }
    }

}