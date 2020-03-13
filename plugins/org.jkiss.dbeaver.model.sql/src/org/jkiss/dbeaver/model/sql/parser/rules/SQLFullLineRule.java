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


import org.jkiss.dbeaver.model.text.parser.TPCharacterScanner;
import org.jkiss.dbeaver.model.text.parser.TPToken;
import org.jkiss.dbeaver.model.text.parser.TPTokenAbstract;
import org.jkiss.dbeaver.model.text.parser.rules.EndOfLineRule;

/**
* Rule which starts in the beginning of line
*/
public class SQLFullLineRule extends EndOfLineRule {

    public SQLFullLineRule(String startSequence, TPToken token) {
        super(startSequence, token);
    }

    public SQLFullLineRule(String startSequence, TPToken token, char escapeCharacter) {
        super(startSequence, token, escapeCharacter);
    }

    public SQLFullLineRule(String startSequence, TPToken token, char escapeCharacter, boolean escapeContinuesLine) {
        super(startSequence, token, escapeCharacter, escapeContinuesLine);
    }

    @Override
    public TPToken evaluate(TPCharacterScanner scanner) {
        // Must be in the beginning of line
        {
            scanner.unread();
            int prevChar = scanner.read();
            if (prevChar != TPCharacterScanner.EOF && prevChar != '\r' && prevChar != '\n') {
                return TPTokenAbstract.UNDEFINED;
            }
        }
        return super.evaluate(scanner);
    }
}
