/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2021 DBeaver Corp and others
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


import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.text.parser.TPCharacterScanner;
import org.jkiss.dbeaver.model.text.parser.TPToken;
import org.jkiss.dbeaver.model.text.parser.rules.MultiLineRule;

public class NestedMultiLineRule extends MultiLineRule {
    private static final Log log = Log.getLog(NestedMultiLineRule.class);

    /**
     * Current nesting depth. If level is zero i.e start and
     * end sequences are balanced, then this rule evaluated
     * to the result of <code>getSuccessToken()</code>
     */
    private int fNestingLevel;

    /**
     * Controls whether rollback to the last end sequence index
     * should be performed in order to avoid applying this rule
     * to the entire document when EOF is reached.
     */
    private boolean fRollback;

    public NestedMultiLineRule(String startSequence, String endSequence, TPToken token, char escapeCharacter, boolean breaksOnEOF) {
        super(startSequence, endSequence, token, escapeCharacter, breaksOnEOF);
    }

    @Override
    protected TPToken doEvaluate(TPCharacterScanner scanner, boolean resume) {
        fNestingLevel = 1;
        return super.doEvaluate(scanner, resume);
    }

    @Override
    protected boolean endSequenceDetected(TPCharacterScanner scanner) {
        int currentIndex = 0;
        int endSequenceIndex = 0;

        while (true) {
            final int ch = scanner.read();

            if (fStartSequence.length > 0 && ch == fStartSequence[0]) {
                if (sequenceDetected(scanner, fStartSequence, fBreaksOnEOF)) {
                    fNestingLevel += 1;
                }
            } else if (fEndSequence.length > 0 && ch == fEndSequence[0]) {
                if (sequenceDetected(scanner, fEndSequence, fBreaksOnEOF)) {
                    fNestingLevel -= 1;
                    if (fNestingLevel > 0 && fRollback) {
                        // Update to last end sequence index at positive
                        // nesting level (> 0) so we can rollback later
                        endSequenceIndex = currentIndex;
                    }
                }
            } else if (ch == TPCharacterScanner.EOF) {
                log.trace("Found unterminated start sequences after scanning");
                if (fRollback) {
                    // Rollback to last end index - at least this rule
                    // won't be applied to the entire document
                    for (; currentIndex > endSequenceIndex; currentIndex--) {
                        scanner.unread();
                    }
                }
                return true;
            }

            if (fNestingLevel <= 0) {
                return true;
            }

            currentIndex += 1;
        }
    }

    public boolean isRollback() {
        return fRollback;
    }

    public void setRollback(boolean rollback) {
        fRollback = rollback;
    }
}
