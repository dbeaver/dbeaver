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

import org.jkiss.dbeaver.model.sql.parser.tokens.SQLControlToken;
import org.jkiss.dbeaver.model.sql.registry.SQLCommandHandlerDescriptor;
import org.jkiss.dbeaver.model.text.parser.TPCharacterScanner;
import org.jkiss.dbeaver.model.text.parser.rules.EndOfLineRule;

public class SQLCommandRule extends EndOfLineRule {
    public SQLCommandRule(String commandPrefix, SQLCommandHandlerDescriptor controlCommand, SQLControlToken controlToken) {
        super(commandPrefix + controlCommand.getId() + ' ', controlToken);
    }

    protected boolean sequenceDetected(TPCharacterScanner scanner, char[] sequence, boolean eofAllowed) {
        for (int i= 1; i < sequence.length; i++) {
            int c= scanner.read();
            char seqChar = sequence[i];
            boolean validChar = (seqChar == ' ' && Character.isWhitespace(c)) ||
                    Character.toUpperCase(c) == Character.toUpperCase(seqChar);
            if (!validChar) {
                // Non-matching character detected, rewind the scanner back to the start.
                // Do not unread the first character.
                scanner.unread();
                for (int j= i-1; j > 0; j--)
                    scanner.unread();
                return false;
            }
        }

        return true;
    }

}
