/*
 * Copyright (C) 2010-2012 Serge Rieder
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

package org.jkiss.dbeaver.ui.editors.sql.format;

import java.util.ArrayList;
import java.util.List;

/**
 * SQLParser
 * TODO: check comment characters from syntax manager, not constants
 */
public class SQLParser {

    private SQLFormatterConfiguration configuration;
    private String fBefore;
    private int fPos;
    private static final String[] twoCharacterSymbol = { "<>", "<=", ">=", "||", "()", "!=", ":=" };
    private char structSeparator;
    private String catalogSeparator;

    public SQLParser(SQLFormatterConfiguration configuration) {
        this.configuration = configuration;
        this.structSeparator = configuration.getSyntaxManager().getStructSeparator();
        this.catalogSeparator = configuration.getSyntaxManager().getCatalogSeparator();
    }

    public static boolean isSpace(final char argChar) {
        return Character.isWhitespace(argChar);
    }

    public static boolean isLetter(final char argChar) {
        return !isSpace(argChar) && !isDigit(argChar) && !isSymbol(argChar);
    }

    public static boolean isDigit(final char argChar) {
        return Character.isDigit(argChar);
    }

    public static boolean isSymbol(final char argChar) {
        switch (argChar) {
        case '"': // double quote
        case '?': // question mark
        case '%': // percent
        case '&': // ampersand
        case '\'': // quote
        case '(': // left paren
        case ')': // right paren
        case '|': // vertical bar
        case '*': // asterisk
        case '+': // plus sign
        case ',': // comma
        case '-': // minus sign
        case '.': // period
        case '/': // solidus
        case ':': // colon
        case ';': // semicolon
        case '<': // less than operator
        case '=': // equals operator
        case '>': // greater than operator
        case '!': // greater than operator
        case '~': // greater than operator
            return true;
        default:
            return false;
        }
    }

    SQLFormatterToken nextToken() {
        int start_pos = fPos;
        if (fPos >= fBefore.length()) {
            fPos++;
            return new SQLFormatterToken(SQLFormatterConstants.END, "", start_pos);
        }

        char fChar = fBefore.charAt(fPos);

        if (isSpace(fChar)) {
            StringBuilder workString = new StringBuilder();
            for (;;) {
                workString.append(fChar);
                fChar = fBefore.charAt(fPos);
                if (!isSpace(fChar)) {
                    return new SQLFormatterToken(SQLFormatterConstants.SPACE, workString.toString(), start_pos);
                }
                fPos++;
                if (fPos >= fBefore.length()) {
                    return new SQLFormatterToken(SQLFormatterConstants.SPACE, workString.toString(), start_pos);
                }
            }
        } else if (fChar == ';') {
            fPos++;
            return new SQLFormatterToken(SQLFormatterConstants.SYMBOL, ";", start_pos);
        } else if (isDigit(fChar)) {
            StringBuilder s = new StringBuilder();
            while (isDigit(fChar) || fChar == '.') {
                // if (ch == '.') type = Token.REAL;
                s.append(fChar);
                fPos++;

                if (fPos >= fBefore.length()) {
                    break;
                }

                fChar = fBefore.charAt(fPos);
            }
            return new SQLFormatterToken(SQLFormatterConstants.VALUE, s.toString(), start_pos);
        } else if (isLetter(fChar)) {
            StringBuilder s = new StringBuilder();
            while (isLetter(fChar) || isDigit(fChar) || structSeparator == fChar || catalogSeparator.indexOf(fChar) != -1) {
                s.append(fChar);
                fPos++;
                if (fPos >= fBefore.length()) {
                    break;
                }

                fChar = fBefore.charAt(fPos);
            }
            String word = s.toString();
            if (configuration.getSyntaxManager().getKeywordManager().getKeywordType(word) != null) {
                return new SQLFormatterToken(SQLFormatterConstants.KEYWORD, word, start_pos);
            }
            return new SQLFormatterToken(SQLFormatterConstants.NAME, word, start_pos);
        }
        // single line comment
        else if (fChar == '-') {
            fPos++;
            char ch2 = fBefore.charAt(fPos);
            if (ch2 != '-') {
                return new SQLFormatterToken(SQLFormatterConstants.SYMBOL, "-", start_pos);
            }
            fPos++;
            StringBuilder s = new StringBuilder("--");
            for (;;) {
                fChar = fBefore.charAt(fPos);
                s.append(fChar);
                fPos++;
                if (fChar == '\n' || fPos >= fBefore.length()) {
                    return new SQLFormatterToken(SQLFormatterConstants.COMMENT, s.toString(), start_pos);
                }
            }
        }
        else if (fChar == '/') {
            fPos++;
            char ch2 = fBefore.charAt(fPos);
            if (ch2 != '*') {
                return new SQLFormatterToken(SQLFormatterConstants.SYMBOL, "/", start_pos);
            }

            StringBuilder s = new StringBuilder("/*");
            fPos++;
            for (;;) {
                int ch0 = fChar;
                fChar = fBefore.charAt(fPos);
                s.append(fChar);
                fPos++;
                if (ch0 == '*' && fChar == '/') {
                    return new SQLFormatterToken(SQLFormatterConstants.COMMENT, s.toString(), start_pos);
                }
            }
        } else if (fChar == '\'') {
            fPos++;
            StringBuilder s = new StringBuilder("'");
            for (;;) {
                fChar = fBefore.charAt(fPos);
                s.append(fChar);
                fPos++;
                if (fChar == '\'') {
                    return new SQLFormatterToken(SQLFormatterConstants.VALUE, s.toString(), start_pos);
                }
            }
        } else if (fChar == '\"') {
            fPos++;
            StringBuilder s = new StringBuilder("\"");
            for (;;) {
                fChar = fBefore.charAt(fPos);
                s.append(fChar);
                fPos++;
                if (fChar == '\"') {
                    return new SQLFormatterToken(SQLFormatterConstants.NAME, s.toString(), start_pos);
                }
            }
        }

        else if (isSymbol(fChar)) {
            String s = String.valueOf(fChar);
            fPos++;
            if (fPos >= fBefore.length()) {
                return new SQLFormatterToken(SQLFormatterConstants.SYMBOL, s, start_pos);
            }
            char ch2 = fBefore.charAt(fPos);
            for (int i = 0; i < twoCharacterSymbol.length; i++) {
                if (twoCharacterSymbol[i].charAt(0) == fChar && twoCharacterSymbol[i].charAt(1) == ch2) {
                    fPos++;
                    s += ch2;
                    break;
                }
            }
            return new SQLFormatterToken(SQLFormatterConstants.SYMBOL, s, start_pos);
        } else {
            fPos++;
            return new SQLFormatterToken(SQLFormatterConstants.UNKNOWN, String.valueOf(fChar), start_pos);
        }
    }

    public List<SQLFormatterToken> parse(final String argSql) {
        fPos = 0;
        fBefore = argSql;

        final List<SQLFormatterToken> list = new ArrayList<SQLFormatterToken>();
        for (;;) {
            final SQLFormatterToken token = nextToken();
            if (token.getType() == SQLFormatterConstants.END) {
                break;
            }

            list.add(token);
        }
        return list;
    }
}
