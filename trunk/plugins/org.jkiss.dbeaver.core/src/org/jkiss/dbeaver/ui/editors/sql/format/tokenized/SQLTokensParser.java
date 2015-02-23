/*
 * Copyright (C) 2010-2015 Serge Rieder
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

package org.jkiss.dbeaver.ui.editors.sql.format.tokenized;

import org.jkiss.dbeaver.ui.editors.sql.format.SQLFormatterConfiguration;

import java.util.ArrayList;
import java.util.List;

/**
 * SQLTokensParser
 * TODO: check comment characters from syntax manager, not constants
 */
class SQLTokensParser {

    private SQLFormatterConfiguration configuration;
    private String fBefore;
    private int fPos;
    private static final String[] twoCharacterSymbol = { "<>", "<=", ">=", "||", "()", "!=", ":=", ".*" };
    private char structSeparator;
    private String catalogSeparator;

    public SQLTokensParser(SQLFormatterConfiguration configuration) {
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
        case '`': // apos
            return true;
        default:
            return false;
        }
    }

    FormatterToken nextToken() {
        int start_pos = fPos;
        if (fPos >= fBefore.length()) {
            fPos++;
            return new FormatterToken(FormatterConstants.END, "", start_pos);
        }

        char fChar = fBefore.charAt(fPos);

        if (isSpace(fChar)) {
            StringBuilder workString = new StringBuilder();
            for (;;) {
                workString.append(fChar);
                fChar = fBefore.charAt(fPos);
                if (!isSpace(fChar)) {
                    return new FormatterToken(FormatterConstants.SPACE, workString.toString(), start_pos);
                }
                fPos++;
                if (fPos >= fBefore.length()) {
                    return new FormatterToken(FormatterConstants.SPACE, workString.toString(), start_pos);
                }
            }
        } else if (fChar == ';') {
            fPos++;
            return new FormatterToken(FormatterConstants.SYMBOL, ";", start_pos);
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
            return new FormatterToken(FormatterConstants.VALUE, s.toString(), start_pos);
        } else if (isLetter(fChar)) {
            StringBuilder s = new StringBuilder();
            while (isLetter(fChar) || isDigit(fChar) || fChar == '*' || structSeparator == fChar || catalogSeparator.indexOf(fChar) != -1) {
                s.append(fChar);
                fPos++;
                if (fPos >= fBefore.length()) {
                    break;
                }

                fChar = fBefore.charAt(fPos);
            }
            String word = s.toString();
            if (configuration.getSyntaxManager().getDialect().getKeywordType(word) != null) {
                return new FormatterToken(FormatterConstants.KEYWORD, word, start_pos);
            }
            return new FormatterToken(FormatterConstants.NAME, word, start_pos);
        }
        // single line comment
        else if (fChar == '-') {
            fPos++;
            char ch2 = fBefore.charAt(fPos);
            if (ch2 != '-') {
                return new FormatterToken(FormatterConstants.SYMBOL, "-", start_pos);
            }
            fPos++;
            StringBuilder s = new StringBuilder("--");
            for (;;) {
                fChar = fBefore.charAt(fPos);
                s.append(fChar);
                fPos++;
                if (fChar == '\n' || fPos >= fBefore.length()) {
                    return new FormatterToken(FormatterConstants.COMMENT, s.toString(), start_pos);
                }
            }
        }
        else if (fChar == '/') {
            fPos++;
            char ch2 = fBefore.charAt(fPos);
            if (ch2 != '*') {
                return new FormatterToken(FormatterConstants.SYMBOL, "/", start_pos);
            }

            StringBuilder s = new StringBuilder("/*");
            fPos++;
            for (;;) {
                int ch0 = fChar;
                fChar = fBefore.charAt(fPos);
                s.append(fChar);
                fPos++;
                if (ch0 == '*' && fChar == '/') {
                    return new FormatterToken(FormatterConstants.COMMENT, s.toString(), start_pos);
                }
            }
        } else if (fChar == '\'' || fChar == '\"' || fChar == configuration.getSyntaxManager().getQuoteSymbol().charAt(0)) {
            fPos++;
            char quoteChar = fChar;
            StringBuilder s = new StringBuilder(String.valueOf(quoteChar));
            for (;;) {
                fChar = fBefore.charAt(fPos);
                s.append(fChar);
                fPos++;
                if (fChar == quoteChar) {
                    return new FormatterToken(FormatterConstants.VALUE, s.toString(), start_pos);
                }
            }
        }

        else if (isSymbol(fChar)) {
            String s = String.valueOf(fChar);
            fPos++;
            if (fPos >= fBefore.length()) {
                return new FormatterToken(FormatterConstants.SYMBOL, s, start_pos);
            }
            char ch2 = fBefore.charAt(fPos);
            for (int i = 0; i < twoCharacterSymbol.length; i++) {
                if (twoCharacterSymbol[i].charAt(0) == fChar && twoCharacterSymbol[i].charAt(1) == ch2) {
                    fPos++;
                    s += ch2;
                    break;
                }
            }
            return new FormatterToken(FormatterConstants.SYMBOL, s, start_pos);
        } else {
            fPos++;
            return new FormatterToken(FormatterConstants.UNKNOWN, String.valueOf(fChar), start_pos);
        }
    }

    public List<FormatterToken> parse(final String argSql) {
        fPos = 0;
        fBefore = argSql;

        final List<FormatterToken> list = new ArrayList<FormatterToken>();
        for (;;) {
            final FormatterToken token = nextToken();
            if (token.getType() == FormatterConstants.END) {
                break;
            }

            list.add(token);
        }
        return list;
    }
}
