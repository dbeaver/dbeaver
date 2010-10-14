/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.editors.sql.format;

import org.jkiss.dbeaver.utils.ContentUtils;

import java.util.List;
import java.util.Stack;

/**
 * SQL formatter
 */
public class SQLFormatter {
    private final SQLParser fParser;

    private SQLFormatterConfiguration formatterCfg = null;
    private Stack<Boolean> functionBracket = new Stack<Boolean>();

    public SQLFormatter(final SQLFormatterConfiguration cfg) {
        formatterCfg = cfg;
        fParser = new SQLParser(cfg);
    }

    public String format(final String argSql)
            throws SQLFormatterException
    {
        functionBracket.clear();
        try {
            boolean isSqlEndsWithNewLine = false;
            if (argSql.endsWith("\n")) {
                isSqlEndsWithNewLine = true;
            }

            List<SQLFormatterToken> list = fParser.parse(argSql);
            list = format(list);

            String after = "";
            for (int index = 0; index < list.size(); index++) {
                SQLFormatterToken token = list.get(index);
                after += token.getString();
            }

            if (isSqlEndsWithNewLine) {
                after += ContentUtils.getDefaultLineSeparator();
            }

            return after;
        } catch (Exception ex) {
            final SQLFormatterException sqlException = new SQLFormatterException(
                    ex.toString());
            sqlException.initCause(ex);
            throw sqlException;
        }
    }

    private List<SQLFormatterToken> format(final List<SQLFormatterToken> argList) {

        SQLFormatterToken token = argList.get(0);
        if (token.getType() == SQLFormatterConstants.SPACE) {
            argList.remove(0);
        }

        token = argList.get(argList.size() - 1);
        if (token.getType() == SQLFormatterConstants.SPACE) {
            argList.remove(argList.size() - 1);
        }

        for (int index = 0; index < argList.size(); index++) {
            token = argList.get(index);
            if (token.getType() == SQLFormatterConstants.KEYWORD) {
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

        for (int index = argList.size() - 1; index >= 1; index--) {
            token = argList.get(index);
            SQLFormatterToken prevToken = argList.get(index - 1);
            if (token.getType() == SQLFormatterConstants.SPACE
                    && (prevToken.getType() == SQLFormatterConstants.SYMBOL || prevToken
                            .getType() == SQLFormatterConstants.COMMENT)) {
                argList.remove(index);
            } else if ((token.getType() == SQLFormatterConstants.SYMBOL || token
                    .getType() == SQLFormatterConstants.COMMENT)
                    && prevToken.getType() == SQLFormatterConstants.SPACE) {
                argList.remove(index - 1);
            } else if (token.getType() == SQLFormatterConstants.SPACE) {
                token.setString(" ");
            }
        }

        for (int index = 0; index < argList.size() - 2; index++) {
            SQLFormatterToken t0 = argList.get(index);
            SQLFormatterToken t1 = argList.get(index + 1);
            SQLFormatterToken t2 = argList.get(index + 2);

            if (t0.getType() == SQLFormatterConstants.KEYWORD
                    && t1.getType() == SQLFormatterConstants.SPACE
                    && t2.getType() == SQLFormatterConstants.KEYWORD) {
                if (((t0.getString().equalsIgnoreCase("ORDER") || t0
                        .getString().equalsIgnoreCase("GROUP")) && t2
                        .getString().equalsIgnoreCase("BY"))) {
                    t0.setString(t0.getString() + " " + t2.getString());
                    argList.remove(index + 1);
                    argList.remove(index + 1);
                }
            }

            // Oracle style joins
            if (t0.getString().equals("(") && t1.getString().equals("+") && t2.getString().equals(")")) {
                t0.setString("(+)");
                argList.remove(index + 1);
                argList.remove(index + 1);
            }
        }

        int indent = 0;
        final Stack<Integer> bracketIndent = new Stack<Integer>();
        SQLFormatterToken prev = new SQLFormatterToken(SQLFormatterConstants.SPACE, " ");
        boolean encounterBetween = false;
        for (int index = 0; index < argList.size(); index++) {
            token = argList.get(index);
            if (token.getType() == SQLFormatterConstants.SYMBOL) {
                if (token.getString().equals("(")) {
                    functionBracket
                            .push(formatterCfg.isFunction(prev.getString()) ? Boolean.TRUE
                                    : Boolean.FALSE);
                    bracketIndent.push(indent);
                    indent++;
                    index += insertReturnAndIndent(argList, index + 1, indent);
                }
                else if (token.getString().equals(")")) {
                    indent = bracketIndent.pop();
                    index += insertReturnAndIndent(argList, index, indent);
                    functionBracket.pop();
                }
                else if (token.getString().equals(",")) {
                    index += insertReturnAndIndent(argList, index, indent);
                } else if (token.getString().equals(";")) {
                    indent = 0;
                    index += insertReturnAndIndent(argList, index, indent);
                }
            } else if (token.getType() == SQLFormatterConstants.KEYWORD) {
                if (token.getString().equalsIgnoreCase("DELETE")
                        || token.getString().equalsIgnoreCase("SELECT")
                        || token.getString().equalsIgnoreCase("UPDATE")) {
                    indent += 2;
                    index += insertReturnAndIndent(argList, index + 1, indent);
                }
                if (token.getString().equalsIgnoreCase("INSERT")
                        || token.getString().equalsIgnoreCase("INTO")
                        || token.getString().equalsIgnoreCase("CREATE")
                        || token.getString().equalsIgnoreCase("DROP")
                        || token.getString().equalsIgnoreCase("TRUNCATE")
                        || token.getString().equalsIgnoreCase("TABLE")
                        || token.getString().equalsIgnoreCase("CASE")) {
                    indent++;
                    index += insertReturnAndIndent(argList, index + 1, indent);
                }
                if (token.getString().equalsIgnoreCase("FROM")
                        || token.getString().equalsIgnoreCase("WHERE")
                        || token.getString().equalsIgnoreCase("SET")
                        || token.getString().equalsIgnoreCase("ORDER BY")
                        || token.getString().equalsIgnoreCase("GROUP BY")
                        || token.getString().equalsIgnoreCase("HAVING")) {
                    index += insertReturnAndIndent(argList, index, indent - 1);
                    index += insertReturnAndIndent(argList, index + 1, indent);
                }
                if (token.getString().equalsIgnoreCase("VALUES")) {
                    indent--;
                    index += insertReturnAndIndent(argList, index, indent);
                }
                if (token.getString().equalsIgnoreCase("END")) {
                    indent--;
                    index += insertReturnAndIndent(argList, index, indent);
                }
                if (token.getString().equalsIgnoreCase("OR")
                        || token.getString().equalsIgnoreCase("THEN")
                        || token.getString().equalsIgnoreCase("ELSE")) {
                    index += insertReturnAndIndent(argList, index, indent);
                }
                if (token.getString().equalsIgnoreCase("ON") || token.getString().equalsIgnoreCase("USING")) {
                    index += insertReturnAndIndent(argList, index, indent + 1);
                }
                if (token.getString().equalsIgnoreCase("UNION")
                    || token.getString().equalsIgnoreCase("INTERSECT")
                    || token.getString().equalsIgnoreCase("EXCEPT"))
                {
                    indent -= 2;
                    index += insertReturnAndIndent(argList, index, indent);
                    index += insertReturnAndIndent(argList, index + 1, indent);
                }
                if (token.getString().equalsIgnoreCase("BETWEEN")) {
                    encounterBetween = true;
                }
                if (token.getString().equalsIgnoreCase("AND")) {
                    if (!encounterBetween) {
                        index += insertReturnAndIndent(argList, index, indent);
                    }
                    encounterBetween = false;
                }
            } else if (token.getType() == SQLFormatterConstants.COMMENT) {
                if (token.getString().startsWith("/*")) {
                    index += insertReturnAndIndent(argList, index + 1, indent);
                }
            }
            prev = token;
        }

        for (int index = argList.size() - 1; index >= 4; index--) {
            if (index >= argList.size()) {
                continue;
            }

            SQLFormatterToken t0 = argList.get(index);
            SQLFormatterToken t1 = argList.get(index - 1);
            SQLFormatterToken t2 = argList.get(index - 2);
            SQLFormatterToken t3 = argList.get(index - 3);
            SQLFormatterToken t4 = argList.get(index - 4);

            if (t4.getString().equalsIgnoreCase("(")
                    && t3.getString().trim().isEmpty()
                    && t1.getString().trim().isEmpty()
                    && t0.getString().equalsIgnoreCase(")")) {
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

            if (prev.getType() != SQLFormatterConstants.SPACE && token.getType() != SQLFormatterConstants.SPACE) {
                if (prev.getString().equals(",")) {
                    continue;
                }
                if (formatterCfg.isFunction(prev.getString())
                        && token.getString().equals("(")) {
                    continue;
                }
                argList.add(index, new SQLFormatterToken(SQLFormatterConstants.SPACE, " "));
            }
        }

        return argList;
    }

    private int insertReturnAndIndent(final List<SQLFormatterToken> argList, final int argIndex, final int argIndent)
    {
        if (functionBracket.contains(Boolean.TRUE))
            return 0;
        try {
            String s = ContentUtils.getDefaultLineSeparator();
            final SQLFormatterToken prevToken = argList.get(argIndex - 1);
            if (prevToken.getType() == SQLFormatterConstants.COMMENT && prevToken.getString().startsWith("--")) {
                s = "";
            }
            for (int index = 0; index < argIndent; index++) {
                s += formatterCfg.getIndentString();
            }

            SQLFormatterToken token = argList.get(argIndex);
            if (token.getType() == SQLFormatterConstants.SPACE) {
                token.setString(s);
                return 0;
            }

            token = argList.get(argIndex - 1);
            if (token.getType() == SQLFormatterConstants.SPACE) {
                token.setString(s);
                return 0;
            }
            argList.add(argIndex, new SQLFormatterToken(SQLFormatterConstants.SPACE, s));
            return 1;
        } catch (IndexOutOfBoundsException e) {
            // e.printStackTrace();
            return 0;
        }
    }

}