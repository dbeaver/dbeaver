/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
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

import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.sql.parser.tokens.SQLBlockToggleToken;
import org.jkiss.dbeaver.model.sql.parser.tokens.SQLTokenType;
import org.jkiss.dbeaver.model.text.parser.*;

public class SQLDollarQuoteRule implements TPPredicateRule {

    private final boolean partitionRule;
    private final TPToken stringToken, delimiterToken;

    public SQLDollarQuoteRule(DBPDataSourceContainer dataSource, boolean partitionRule) {
        this.partitionRule = partitionRule;

        this.stringToken = new TPTokenDefault(SQLTokenType.T_STRING);
        this.delimiterToken = new SQLBlockToggleToken();
    }

    @Override
    public TPToken evaluate(TPCharacterScanner scanner) {
        return evaluate(scanner, false);
    }

    @Override
    public TPToken getSuccessToken() {
        return stringToken;
    }

    @Override
    public TPToken evaluate(TPCharacterScanner scanner, boolean resume) {
        int totalRead = 0;
        int c = scanner.read();
        totalRead++;
        if (c == '$') {
            int charsRead = 0;
            do {
                c = scanner.read();
                charsRead++;
                totalRead++;
                if (c == '$') {

                    if (charsRead <= 1) {
                        // Here is a trick - dollar quote without preceding AS or DO and without tag is a string.
                        // Quote with tag is just a block toggle.
                        // I'm afraid we can't do more (#6608, #7183)
                        boolean stringEndFound = false;
                        //StringBuilder stringValue = new StringBuilder();
                        for (;;) {
                            c = scanner.read();
                            totalRead++;
                            if (c == TPCharacterScanner.EOF) {
                                break;
                            }
                            if (c == '$') {
                                int c2 = scanner.read();
                                totalRead++;
                                if (c2 == '$') {
                                    stringEndFound = true;
                                    break;
                                } else {
                                    scanner.unread();
                                    totalRead--;
                                }
                            }
                            //stringValue.append((char)c);
                        }

                        if (!stringEndFound) {
                            if (!partitionRule) {
                                unread(scanner, totalRead - 2);
                                return delimiterToken;
                            } else {
                                break;
                            }
                        }
/*
                        String encString = stringValue.toString().toUpperCase(Locale.ENGLISH);
                        if (encString.contains(SQLConstants.BLOCK_BEGIN) && encString.contains(SQLConstants.BLOCK_END)) {
                            // Seems to be a code block
                            if (!partitionRule) {
                                unread(scanner, totalRead - 2);
                                return delimiterToken;
                            } else {
                                return partCodeToken;
                            }
                        }
*/
                        // Find the end of the string
                        return stringToken;
                    }
                    if (!partitionRule) {
                        return delimiterToken;
                    } else {
                        break;
                    }
                }
            } while (Character.isLetterOrDigit(c) || c == '_');
        }

        unread(scanner, totalRead);

        return TPTokenAbstract.UNDEFINED;
    }

    private static void unread(TPCharacterScanner scanner, int totalRead) {
        while (totalRead-- > 0) {
            scanner.unread();
        }
    }

}
