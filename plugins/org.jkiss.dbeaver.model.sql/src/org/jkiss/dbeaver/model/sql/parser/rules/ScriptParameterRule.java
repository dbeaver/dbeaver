/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2023 DBeaver Corp and others
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
package org.jkiss.dbeaver.model.sql.parser.rules;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.sql.SQLConstants;
import org.jkiss.dbeaver.model.sql.SQLDialect;
import org.jkiss.dbeaver.model.sql.SQLSyntaxManager;
import org.jkiss.dbeaver.model.sql.parser.tokens.SQLParameterToken;
import org.jkiss.dbeaver.model.text.parser.TPCharacterScanner;
import org.jkiss.dbeaver.model.text.parser.TPRule;
import org.jkiss.dbeaver.model.text.parser.TPToken;
import org.jkiss.dbeaver.model.text.parser.TPTokenAbstract;

/**
* SQL parameter rule
*/
public class ScriptParameterRule implements TPRule {

    private final SQLSyntaxManager syntaxManager;
    private final SQLParameterToken parameterToken;
    private final StringBuilder buffer;
    private final char anonymousParameterMark;
    private final String namedParameterPrefix;
    private final String[][] quoteStrings;
    

    public ScriptParameterRule(SQLSyntaxManager syntaxManager, SQLParameterToken parameterToken, String prefix) {
        this.syntaxManager = syntaxManager;
        this.parameterToken = parameterToken;
        this.buffer = new StringBuilder();
        this.anonymousParameterMark = syntaxManager.getAnonymousParameterMark();
        this.namedParameterPrefix = prefix;
        this.quoteStrings = syntaxManager.getIdentifierQuoteStrings();
    }

    @Override
    public TPToken evaluate(TPCharacterScanner scanner)
    {
        scanner.unread();
        int prevChar = scanner.read();
        char namedPrefix = namedParameterPrefix.charAt(0);
        if (Character.isJavaIdentifierPart(prevChar) ||
            prevChar == namedPrefix || prevChar == anonymousParameterMark || prevChar == '\\' || prevChar == '/' || (prevChar == '[' && namedPrefix == ':'))
        {
            return TPTokenAbstract.UNDEFINED;
        }
        int c = scanner.read();
        if (c != TPCharacterScanner.EOF && (c == anonymousParameterMark || c == namedPrefix)) {
            buffer.setLength(0);
            buffer.append((char) c);
            c = scanner.read();
            
            if (isIdentifierQuote((char) c, true, false, quoteStrings)) {
                do {
                    buffer.append((char) c);
                    c = scanner.read();
                    if (isIdentifierQuote((char) c, false, true, quoteStrings)) {
                        buffer.append((char) c);
                        c = scanner.read();
                        break;
                    }
                } while (c != TPCharacterScanner.EOF);
            } else {
                while (c != TPCharacterScanner.EOF && Character.isJavaIdentifierPart(c)) {
                    buffer.append((char) c);
                    c = scanner.read();
                }
            }
            scanner.unread();

            // Check for parameters
            if (syntaxManager.isAnonymousParametersEnabled()) {
                if (buffer.length() == 1 && buffer.charAt(0) == anonymousParameterMark) {
                    return parameterToken;
                }
            }
            if (syntaxManager.isParametersEnabled()) {
                if (buffer.charAt(0) == namedPrefix && buffer.length() > 1) {
                    if (isIdentifierQuote(buffer.charAt(1), true, false, quoteStrings)
                        && isIdentifierQuote(buffer.charAt(buffer.length() - 1), false, true, quoteStrings)
                        && buffer.length() > 3
                    ) {
                        return parameterToken;
                    }
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
        return TPTokenAbstract.UNDEFINED;
    }
    
    /**
     * Parse variable name from buffer
     * Return the position of the last variable name character or -1 if the name is not valid
     */
    public static int tryConsumeParameterName(@NotNull SQLDialect sqlDialect, @NotNull CharSequence buffer, int position) {
        int endPos = tryConsumeParameterNameImpl(sqlDialect, buffer, position);
        return endPos > position ? endPos : -1;
    }
    
    private static int tryConsumeParameterNameImpl(@NotNull SQLDialect sqlDialect, @NotNull CharSequence buffer, int position) {
        String[][] quoteStrings = sqlDialect.getIdentifierQuoteStrings();
 
        // keep in sync with the above
        int c = position < buffer.length() ? buffer.charAt(position) : TPCharacterScanner.EOF;
        position++;
        if (isIdentifierQuote((char) c, true, false, quoteStrings)) {
            do {
                c = position < buffer.length() ? buffer.charAt(position) : TPCharacterScanner.EOF;
                position++;
                if (isIdentifierQuote((char) c, false, true, quoteStrings)) {
                    c = position < buffer.length() ? buffer.charAt(position) : TPCharacterScanner.EOF;
                    position++;
                    break;
                }
            } while (c != TPCharacterScanner.EOF);
        } else {
            while (c != TPCharacterScanner.EOF && Character.isJavaIdentifierPart(c)) {
                c = position < buffer.length() ? buffer.charAt(position) : TPCharacterScanner.EOF;
                position++;
            }
        }
        return position - 1;
    }
    
    private static boolean isIdentifierQuote(char c, boolean testStart, boolean testEnd, String[][] quoteStrings) {
        String testStr = Character.toString(c);
        if (testStr.equals(SQLConstants.STR_QUOTE_DOUBLE)) {
            return true;
        }
        if (quoteStrings != null) {
            for (String[] quotePair : quoteStrings) {
                if ((testStart && testStr.equals(quotePair[0])) || (testEnd && testStr.equals(quotePair[1]))) {
                    return true;
                }
            }
        }
        return false;
    }

}
