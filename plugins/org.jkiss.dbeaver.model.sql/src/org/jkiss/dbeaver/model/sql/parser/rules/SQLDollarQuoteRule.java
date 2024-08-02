/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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

import org.jkiss.dbeaver.model.sql.parser.tokens.SQLBlockToggleToken;
import org.jkiss.dbeaver.model.sql.parser.tokens.SQLTokenType;
import org.jkiss.dbeaver.model.text.parser.*;

/**
 * See <a href="https://www.postgresql.org/docs/current/sql-syntax-lexical.html#SQL-SYNTAX-DOLLAR-QUOTING">documentation</a>
 */
public class SQLDollarQuoteRule implements TPPredicateRule {

    private final boolean partitionRule;
    private final boolean allowNamedQuotes;
    private final boolean fullyConsumeNamed;
    private final boolean fullyConsumeUnnamed;
    private final TPToken stringToken;
    private final TPToken delimiterToken;

    /**
     * Dollar quoting rule constructor
     * @param partitionRule       whether this rule is a partition rule or not
     * @param allowNamedQuotes    whether this rule supports named quotes ({@code $named$}) or not
     * @param fullyConsumeNamed   whether this rule should stop after consuming named quote
     *                            or continue until matching the closing one, treating everything between as a string
     * @param fullyConsumeUnnamed same as {@code fullyConsumeNamed}, but for unnamed quotes
     */
    public SQLDollarQuoteRule(boolean partitionRule, boolean allowNamedQuotes, boolean fullyConsumeNamed, boolean fullyConsumeUnnamed) {
        this.partitionRule = partitionRule;
        this.allowNamedQuotes = allowNamedQuotes;
        this.fullyConsumeNamed = fullyConsumeNamed || partitionRule;
        this.fullyConsumeUnnamed = fullyConsumeUnnamed || partitionRule;

        this.stringToken = new TPTokenDefault(SQLTokenType.T_STRING);
        this.delimiterToken = new SQLBlockToggleToken();
    }

    @Override
    public TPToken getSuccessToken() {
        return stringToken;
    }

    @Override
    public TPToken evaluate(TPCharacterScanner scanner) {
        return evaluate(scanner, false);
    }

    @Override
    public TPToken evaluate(TPCharacterScanner scanner, boolean resume) {
        String start = this.tryReadDollarQuote(scanner);
        if (start != null) {
            if ((start.length() == 2 && this.fullyConsumeUnnamed) || (start.length() > 2 && this.fullyConsumeNamed)) {
                int c = scanner.read();
                int captured = 1;
                while (c != TPCharacterScanner.EOF) {
                    if (c == '$') {
                        scanner.unread();
                        captured--;
                        String end = this.tryReadDollarQuote(scanner);
                        if (end != null) {
                            if (end.equals(start)) {
                                return this.stringToken;
                            } else {
                                // unread ending quote in case it is the real ending
                                scanner.unread();
                                captured += end.length() - 1;
                            }
                        } else {
                            scanner.read();
                            captured++;
                        }
                    }
                    c = scanner.read();
                    captured++;
                }
                unread(scanner, captured + start.length());
            } else {
                if (!this.partitionRule) {
                    return this.delimiterToken;
                }
            }
        }

        return TPTokenAbstract.UNDEFINED;
    }

    private String tryReadDollarQuote(TPCharacterScanner scanner) {
        int totalRead = 0;
        int c = scanner.read();
        totalRead++;

        if (c == '$') {
            if (this.allowNamedQuotes) {
                StringBuilder qname = new StringBuilder();
                do {
                    qname.append((char) c);
                    c = scanner.read();
                    totalRead++;
                    if (c == '$') {
                        qname.append((char) c);
                        return qname.toString();
                    }
                } while (c != TPCharacterScanner.EOF && (Character.isLetterOrDigit(c) || c == '_'));
            } else {
                c = scanner.read();
                totalRead++;
                if (c == '$') {
                    return  "$$";
                }
            }
        }

        unread(scanner, totalRead);
        return null;
    }

    private static void unread(TPCharacterScanner scanner, int totalRead) {
        while (totalRead-- > 0) {
            scanner.unread();
        }
    }

}
