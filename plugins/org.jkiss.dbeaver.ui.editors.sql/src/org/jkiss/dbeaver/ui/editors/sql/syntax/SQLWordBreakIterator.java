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
package org.jkiss.dbeaver.ui.editors.sql.syntax;

import org.eclipse.core.runtime.Assert;

import java.text.BreakIterator;
import java.text.CharacterIterator;

public class SQLWordBreakIterator extends BreakIterator {

    protected static abstract class Run {
        protected int length;

        public Run() {
            init();
        }

        protected boolean consume(char ch) {
            if (isValid(ch)) {
                length++;
                return true;
            }
            return false;
        }

        protected abstract boolean isValid(char ch);

        protected void init() {
            length = 0;
        }
    }

    static final class Whitespace extends Run {
        @Override
        protected boolean isValid(char ch) {
            return Character.isWhitespace(ch) && ch != '\n' && ch != '\r';
        }
    }

    static final class LineDelimiter extends Run {
        private char state;
        private static final char INIT = '\0';
        private static final char EXIT = '\1';

        @Override
        protected void init() {
            super.init();
            state = INIT;
        }

        @Override
        protected boolean consume(char ch) {
            if (!isValid(ch) || state == EXIT)
                return false;

            if (state == INIT) {
                state = ch;
                length++;
                return true;
            } else if (state != ch) {
                state = EXIT;
                length++;
                return true;
            } else {
                return false;
            }
        }

        @Override
        protected boolean isValid(char ch) {
            return ch == '\n' || ch == '\r';
        }
    }

    static final class SQLIdentifier extends Run {
        @Override
        protected boolean isValid(char ch) {
            return Character.isJavaIdentifierPart(ch);
        }
    }

    static final class Other extends Run {
        @Override
        protected boolean isValid(char ch) {
            return !Character.isWhitespace(ch) && !Character.isJavaIdentifierPart(ch);
        }
    }

    private static final Run WHITESPACE = new Whitespace();
    private static final Run DELIMITER = new LineDelimiter();
    private static final Run IDENTIFIER = new SQLIdentifier(); // new Identifier();
    private static final Run OTHER = new Other();

    protected final BreakIterator iterator;
    protected CharSequence text;
    private int index;

    public SQLWordBreakIterator() {
        iterator = BreakIterator.getWordInstance();
        index = iterator.current();
    }

    public CharSequence getTextValue() {
        return text;
    }

    @Override
    public int current() {
        return index;
    }

    @Override
    public int first() {
        index = iterator.first();
        return index;
    }

    @Override
    public int following(int offset) {
        // work around too eager IAEs in standard implementation
        if (offset == getText().getEndIndex())
            return DONE;

        int next = iterator.following(offset);
        if (next == DONE)
            return DONE;

        // TODO deal with complex script word boundaries
        // Math.min(offset + run.length, next) does not work
        // since BreakIterator.getWordInstance considers _ as boundaries
        // seems to work fine, however
        Run run = consumeRun(offset);
        return offset + run.length;

    }

    private Run consumeRun(int offset) {
        char ch = text.charAt(offset);
        int length = text.length();
        Run run = getRun(ch);
        while (run.consume(ch) && offset < length - 1) {
            offset++;
            ch = text.charAt(offset);
        }

        return run;
    }

    private Run getRun(char ch) {
        Run run;
        if (WHITESPACE.isValid(ch))
            run = WHITESPACE;
        else if (DELIMITER.isValid(ch))
            run = DELIMITER;
        else if (IDENTIFIER.isValid(ch))
            run = IDENTIFIER;
        else if (OTHER.isValid(ch))
            run = OTHER;
        else {
            Assert.isTrue(false);
            return null;
        }

        run.init();
        return run;
    }

    @Override
    public CharacterIterator getText() {
        return iterator.getText();
    }

    @Override
    public boolean isBoundary(int offset) {
        if (offset == getText().getBeginIndex())
            return true;
        else
            return following(offset - 1) == offset;
    }

    @Override
    public int last() {
        index = iterator.last();
        return index;
    }

    @Override
    public int next() {
        index = following(index);
        return index;
    }

    @Override
    public int next(int n) {
        return iterator.next(n);
    }

    @Override
    public int preceding(int offset) {
        if (offset == getText().getBeginIndex())
            return DONE;

        if (isBoundary(offset - 1))
            return offset - 1;

        int previous = offset - 1;
        do {
            previous = iterator.preceding(previous);
        } while (!isBoundary(previous));

        int last = DONE;
        while (previous < offset) {
            last = previous;
            previous = following(previous);
        }

        return last;
    }

    @Override
    public int previous() {
        index = preceding(index);
        return index;
    }

    @Override
    public void setText(String newText) {
        setText((CharSequence) newText);
    }

    public void setText(CharSequence newText) {
        text = newText;
        iterator.setText(new SQLSequenceCharacterIterator(newText));
        first();
    }

    @Override
    public void setText(CharacterIterator newText) {
        if (newText instanceof CharSequence) {
            text = (CharSequence) newText;
            iterator.setText(newText);
            first();
        } else {
            throw new UnsupportedOperationException("CharacterIterator not supported"); //$NON-NLS-1$
        }
    }

}
