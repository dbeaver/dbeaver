package org.jkiss.dbeaver.model.sql.format.tokenized;

import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ModelPreferences;
import org.jkiss.dbeaver.model.sql.SQLDialect;
import org.jkiss.dbeaver.model.sql.SQLUtils;
import org.jkiss.dbeaver.model.sql.format.SQLFormatterConfiguration;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.utils.ArrayUtils;
import org.jkiss.utils.CommonUtils;
import org.jkiss.utils.Pair;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

class IndentFormatter {
    private static final Log log = Log.getLog(SQLFormatterTokenized.class);

    private final SQLFormatterConfiguration formatterCfg;
    private final boolean isCompact;
    private final SQLDialect dialect;
    private List<String> statementDelimiters = new LinkedList<>();
    private String delimiterRedefiner;
    private int indent = 0;
    private int bracketsDepth = 0;
    private boolean encounterBetween = false;
    private List<Boolean> functionBracket = new ArrayList<>();
    private final String[] blockHeaderStrings;

    private static final String[] JOIN_BEGIN = {"LEFT", "RIGHT", "INNER", "OUTER", "FULL", "CROSS", "JOIN"};

    IndentFormatter(SQLFormatterConfiguration formatterCfg, boolean isCompact) {
        this.formatterCfg = formatterCfg;
        delimiterRedefiner = formatterCfg.getSyntaxManager().getDialect().getScriptDelimiterRedefiner();
        if (delimiterRedefiner != null) {
            delimiterRedefiner = delimiterRedefiner.toUpperCase(Locale.ENGLISH);
        }
        for (String delim : formatterCfg.getSyntaxManager().getStatementDelimiters()) {
            statementDelimiters.add(delim.toUpperCase(Locale.ENGLISH));
        }
        this.isCompact = isCompact;
        dialect = formatterCfg.getSyntaxManager().getDialect();
        blockHeaderStrings = dialect.getBlockHeaderStrings();
    }

    private int formatSymbol(String tokenString, List<Integer> bracketIndent, List<FormatterToken> argList, Integer index, FormatterToken prev) {
        int result = index;

        switch (tokenString) {
            case "(":
                functionBracket.add(formatterCfg.isFunction(prev.getString()) ? Boolean.TRUE : Boolean.FALSE);
                bracketIndent.add(indent);
                bracketsDepth++;
                // Adding indent after ( makes result too verbose and too multiline
                if (!isCompact && formatterCfg.getPreferenceStore().getBoolean(ModelPreferences.SQL_FORMAT_BREAK_BEFORE_CLOSE_BRACKET)) {
                    indent++;
                    index += insertReturnAndIndent(argList, index + 1, indent);
                }
                break;
            case ")":
                if (!bracketIndent.isEmpty() && !functionBracket.isEmpty()) {
                    indent = bracketIndent.remove(bracketIndent.size() - 1);
                    if (!isCompact && formatterCfg.getPreferenceStore().getBoolean(ModelPreferences.SQL_FORMAT_BREAK_BEFORE_CLOSE_BRACKET)) {
                        result += insertReturnAndIndent(argList, index, indent);
                    }
                    functionBracket.remove(functionBracket.size() - 1);
                    bracketsDepth--;
                }
                break;
            case ",":
                if (!isCompact) {
                    /*if (bracketsDepth <= 0 || "SELECT".equals(getPrevDMLKeyword(argList, index)))*/
                    if(bracketsDepth <= 0 || functionBracket.size() == 0)
                    {
                        boolean lfBeforeComma = formatterCfg.getPreferenceStore().getBoolean(ModelPreferences.SQL_FORMAT_LF_BEFORE_COMMA);
                        result += insertReturnAndIndent(
                            argList,
                            lfBeforeComma ? index : index + 1,
                            indent);
                    }
                }
                break;
            default:
                if (statementDelimiters.contains(tokenString)) {
                    indent = 0;
                    result += insertReturnAndIndent(argList, index, indent);
                }
        }
        return result;
    }

    private int formatKeyword(List<FormatterToken> argList, String tokenString, int index) {
        int result = index;
        if (statementDelimiters.contains(tokenString)) { //$NON-NLS-1$
            indent = 0;
            if (index > 0) {
                result += insertReturnAndIndent(argList, index - 1, indent);
            }
            result += insertReturnAndIndent(argList, index + 1, indent);
        } else {
            if (blockHeaderStrings != null && ArrayUtils.contains(blockHeaderStrings, tokenString) || SQLUtils.isBlockStartKeyword(dialect, tokenString)) {
                if (index > 0) {
                    result += insertReturnAndIndent(argList, index, indent - 1);
                }
                indent++;
                result += insertReturnAndIndent(argList, index + 1, indent);
            } else if (SQLUtils.isBlockEndKeyword(dialect, tokenString)) {
                indent--;
                result += insertReturnAndIndent(argList, index, indent);
            } else switch (tokenString) {
                case "CREATE":
                    if (!isCompact) {
                        int nextIndex = getNextKeywordIndex(argList, index);
                        if (nextIndex > 0 && "OR".equals(argList.get(nextIndex).getString().toUpperCase(Locale.ENGLISH))) {
                            nextIndex = getNextKeywordIndex(argList, nextIndex);
                            if (nextIndex > 0 && "REPLACE".equals(argList.get(nextIndex).getString().toUpperCase(Locale.ENGLISH))) {
                                insertReturnAndIndent(argList, nextIndex + 1, indent);
                                break;
                            }
                        }
                    }
                case "DROP": //$NON-NLS-1$
                case "ALTER": //$NON-NLS-1$
                    break;
                case "DELETE": //$NON-NLS-1$
                case "SELECT": //$NON-NLS-1$
                case "UPDATE": //$NON-NLS-1$
                case "INSERT": //$NON-NLS-1$
                case "INTO": //$NON-NLS-1$
                case "TRUNCATE": //$NON-NLS-1$
                case "TABLE": //$NON-NLS-1$
                    if (!isCompact) {
                        if (!"TABLE".equals(tokenString)) {
                            if (bracketsDepth > 0) {
                                result += insertReturnAndIndent(argList, index, indent);
                            } else if (index > 0) {
                                // just add lf before keyword
                                indent = 0;
                                result += insertReturnAndIndent(argList, index - 1, indent);
                            }
                            indent++;
                            result += insertReturnAndIndent(argList, result + 1, indent);
                        }
                    }
                    break;
                case "CASE":  //$NON-NLS-1$
                    if (!isCompact) {
                        result += insertReturnAndIndent(argList, index - 1, indent);
                        if ("WHEN".equalsIgnoreCase(getNextKeyword(argList, index))) {
                            indent++;
                            result += insertReturnAndIndent(argList, index + 1, indent);
                        }
                    }
                    break;
                case "END": // CASE ... END
                    if (!isCompact) {
	                    indent--;
	                    result += insertReturnAndIndent(argList, index, indent);
                    }
                	break;
                case "FROM":
                case "WHERE":
                case "START WITH":
                case "CONNECT BY":
                case "ORDER BY":
                case "GROUP BY":
                case "HAVING":  //$NON-NLS-1$
                    result += insertReturnAndIndent(argList, index, indent - 1);
                    if (!isCompact) {
                        result += insertReturnAndIndent(argList, index + 1, indent);
                    }
                    break;
                case "LEFT":
                case "RIGHT":
                case "INNER":
                case "OUTER":
                case "FULL":
                case "CROSS":
                case "JOIN":
                    if (isJoinStart(argList, index)) {
                        result += insertReturnAndIndent(argList, index, indent - 1);
                    }
                    if (tokenString.equals("JOIN")) {
                        //index += insertReturnAndIndent(argList, index + 1, indent);
                    }
                    break;
                case "VALUES":  //$NON-NLS-1$
                case "LIMIT":  //$NON-NLS-1$
                    indent--;
                    result += insertReturnAndIndent(argList, index, indent);
                    break;
                case "OR":
                    if ("CREATE".equalsIgnoreCase(getPrevKeyword(argList, index))) {
                        break;
                    }
                case "WHEN":
                    if ("CASE".equalsIgnoreCase(getPrevKeyword(argList, index))) {
                        break;
                    }
                case "ELSE":  //$NON-NLS-1$
                    result += insertReturnAndIndent(argList, index, indent);
                    break;
                case "SET": {
                    if (index > 1) {
                        if ("UPDATE".equalsIgnoreCase(getPrevKeyword(argList, index))) {
                            // Extra line feed
                            result += insertReturnAndIndent(argList, index, indent - 1);
                        }
                    }
                    result += insertReturnAndIndent(argList, index + 1, indent);
                    break;
                }
                case "ON": {
                    // FIXME: This produces double indent - #3679. But still needed in some cases?
                    // Initially was added for proper MySQL views formatting.
                    //indent++;
                    result += insertReturnAndIndent(argList, index + 1, indent);
                    break;
                }
                case "USING":  //$NON-NLS-1$ //$NON-NLS-2$
                    result += insertReturnAndIndent(argList, index, indent + 1);
                    break;
                case "TOP":  //$NON-NLS-1$ //$NON-NLS-2$
                    // SQL Server specific
                    result += insertReturnAndIndent(argList, index, indent);
                    if (argList.size() < index + 3) {
                        result += insertReturnAndIndent(argList, index + 3, indent);
                    }
                    break;
                case "UNION":
                case "INTERSECT":
                case "EXCEPT": //$NON-NLS-1$
                    indent -= 2;
                    result += insertReturnAndIndent(argList, index, indent);
                    //index += insertReturnAndIndent(argList, index + 1, indent);
                    indent++;
                    break;
                case "BETWEEN":  //$NON-NLS-1$
                    encounterBetween = true;
                    break;
                case "AND":  //$NON-NLS-1$
                    if (!encounterBetween) {
                        result += insertReturnAndIndent(argList, index, indent);
                    }
                    encounterBetween = false;
                    break;
            }
        }
        return result;
    }

    public void format(List<FormatterToken> argList) {
        final List<Integer> bracketIndent = new ArrayList<>();
        FormatterToken prev = new FormatterToken(TokenType.SPACE, " "); //$NON-NLS-1$
        for (int index = 0; index < argList.size(); index++) {
            FormatterToken token = argList.get(index);
            String tokenString = token.getString().toUpperCase(Locale.ENGLISH);
            switch (token.getType()) {
                case SYMBOL:
                    index = formatSymbol(tokenString, bracketIndent, argList, index, prev);
                    break;
                case KEYWORD:
                    index = formatKeyword(argList, tokenString, index);
                    break;
                case COMMENT:
                    index = formatComment(argList, index, token);
                    break;
                case COMMAND:
                    index = formatCommand(argList, index, token);
                    break;
                default:
                    if (statementDelimiters.contains(tokenString)) {
                        indent = 0;
                        index += insertReturnAndIndent(argList, index + 1, indent);
                    }
            }
            prev = token;
        }
    }

    private int formatCommand(List<FormatterToken> argList, int index, FormatterToken token) {
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
        return index;
    }

    private int formatComment(List<FormatterToken> argList, int index, FormatterToken token) {
        boolean isComment = false;
        String[] slComments = formatterCfg.getSyntaxManager().getDialect().getSingleLineComments();
        if (slComments != null) {
            for (String slc : slComments) {
                if (token.getString().startsWith(slc)) {
                    index += insertReturnAndIndent(argList, index, indent);
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
        return index;
    }

    private int insertReturnAndIndent(final List<FormatterToken> argList, final int argIndex, final int argIndent) {
        if (argIndex >= argList.size()) {
            return 0;
        }
        if (functionBracket.contains(Boolean.TRUE))
            return 0;
        try {
            String s = GeneralUtils.getDefaultLineSeparator();
            if (argIndex > 0) {
                final FormatterToken prevToken = argList.get(argIndex - 1);
                if (prevToken.getType() == TokenType.COMMENT &&
                    SQLUtils.isCommentLine(formatterCfg.getSyntaxManager().getDialect(), prevToken.getString())) {
                    s = ""; //$NON-NLS-1$
                }
            }
            for (int index = 0; index < argIndent; index++) {
                s += formatterCfg.getIndentString();
            }

            FormatterToken token = argList.get(argIndex);
            if (token.getType() == TokenType.SPACE) {
                if (!token.getString().contains(s)) {
                    // Replace space token only if it has less chars
                    token.setString(s);
                }
                return 0;
            }
            boolean isDelimiter = statementDelimiters.contains(token.getString().toUpperCase());

            if (!isDelimiter && argIndex > 0) {
                token = argList.get(argIndex - 1);
                if (token.getType() == TokenType.SPACE) {
                    token.setString(s);
                    return 0;
                }
            }

            if (isDelimiter) {
                if (argList.size() > argIndex + 1) {
                    FormatterToken lineFeed = new FormatterToken(TokenType.SPACE, s + s);
                    if (argList.get(argIndex + 1).getType() == TokenType.SPACE) {
                        argList.set(argIndex + 1, lineFeed);
                    } else {
                        argList.add(argIndex + 1, lineFeed);
                    }
                }
            } else {
                argList.add(argIndex, new FormatterToken(TokenType.SPACE, s));
            }
            return 1;
        } catch (IndexOutOfBoundsException e) {
            log.debug(e);
            return 0;
        }
    }

    private boolean isJoinStart(List<FormatterToken> argList, int index) {
        // Keyword sequence must start from LEFT, RIGHT, INNER, OUTER or JOIN and must end with JOIN
        // And we must be in the beginning of sequence

        // check current token
        if (!ArrayUtils.contains(JOIN_BEGIN, argList.get(index).getString().toUpperCase(Locale.ENGLISH))) {
            return false;
        }
        // check previous token
        for (int i = index - 1; i >= 0; i--) {
            FormatterToken token = argList.get(i);
            if (token.getType() == TokenType.SPACE || token.getType() == TokenType.SYMBOL) {
                continue;
            }
            if (ArrayUtils.contains(JOIN_BEGIN, token.getString().toUpperCase(Locale.ENGLISH))) {
                // It is not the begin of sequence
                return false;
            } else {
                break;
            }
        }
        // check last token
        for (int i = index; i < argList.size(); i++) {
            FormatterToken token = argList.get(i);
            if (token.getType() == TokenType.SPACE || token.getType() == TokenType.SYMBOL) {
                continue;
            }
            if (token.getString().toUpperCase(Locale.ENGLISH).equals("JOIN")) {
                return true;
            }
            if (!ArrayUtils.contains(JOIN_BEGIN, token.getString().toUpperCase(Locale.ENGLISH))) {
                // It is not the begin of sequence
                return false;
            }
        }
        return false;
    }

    private String getPrevKeyword(List<FormatterToken> argList, int index) {
        for (int i = index - 1; i >= 0; i--) {
            FormatterToken token = argList.get(i);
            if (token.getType() == TokenType.KEYWORD) {
                return token.getString();
            }
        }
        return null;
    }

    private static int getNextKeywordIndex(List<FormatterToken> argList, int index) {
        for (int i = index + 1; i < argList.size(); i++) {
            if (argList.get(i).getType() == TokenType.KEYWORD) {
                return i;
            }
        }
        return -1;
    }

    private static String getNextKeyword(List<FormatterToken> argList, int index) {
        int ki = getNextKeywordIndex(argList, index);
        if (ki < 0) {
            return null;
        }
        return argList.get(ki).getString();
    }

}