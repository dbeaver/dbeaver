/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2022 DBeaver Corp and others
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
import org.jkiss.dbeaver.model.text.parser.TPRule;
import org.jkiss.dbeaver.model.text.parser.TPToken;
import org.jkiss.dbeaver.model.text.parser.TPTokenAbstract;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
* SQL variable rule.
* ${varName}
*/
public class ScriptVariableRule implements TPRule {

    private final TPToken parameterToken;
    private final Pattern variablePattern;

    public ScriptVariableRule(TPToken parameterToken, Pattern variablePattern) {
        this.parameterToken = parameterToken;
        this.variablePattern = variablePattern;
    }

    @Override
    public TPToken evaluate(TPCharacterScanner scanner)
    {
        CharacterScannerToSequenceWrapper stream = new CharacterScannerToSequenceWrapper(scanner);
        Matcher matcher = variablePattern.matcher(stream);
        if(matcher.lookingAt()) {
            for(int i = stream.getScannedCharacters(); i > matcher.end(); i --) {
                scanner.unread();
            }
            return parameterToken;
        } else {
            for (int i = stream.getScannedCharacters(); i > 0; i--) {
                scanner.unread();
            }
            return TPTokenAbstract.UNDEFINED;
        }
    }

    private static class CharacterScannerToSequenceWrapper implements CharSequence {
        final private TPCharacterScanner scanner;
        final private StringBuilder buffer = new StringBuilder();
        private int scannedCharacters = 0;

        private CharacterScannerToSequenceWrapper(TPCharacterScanner scanner) {
            this.scanner = scanner;
        }

        @Override
        public int length() {
            return Integer.MAX_VALUE;
        }

        @Override
        public char charAt(int index) {
            if(buffer.length() > index) {
                return buffer.charAt(index);
            }

            int c = 0;
            while(buffer.length() <= index) {
                c = scanner.read();
                scannedCharacters++;
                if(c == -1) {
                    return '\0';
                } else {
                    buffer.appendCodePoint(c);
                }
            }
            return (char)c;
        }

        @Override
        public CharSequence subSequence(int start, int end) {
            throw new UnsupportedOperationException("subSequence");
        }

        public int getScannedCharacters() {
            return scannedCharacters;
        }
    }
}