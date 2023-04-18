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
package org.jkiss.dbeaver.ui.editors.sql.syntax;

import org.eclipse.core.runtime.Assert;

import java.text.BreakIterator;
import java.text.CharacterIterator;

public class SQLWordBreakIterator extends BreakIterator {
    protected static abstract class Run {
        /**
         * The length of this run.
         */
        protected int length;

        public Run() {
            init();
        }

        /**
         * Returns <code>true</code> if this run consumes <code>ch</code>,
         * <code>false</code> otherwise. If <code>true</code> is returned,
         * the length of the receiver is adjusted accordingly.
         *
         * @param ch the character to test
         * @return <code>true</code> if <code>ch</code> was consumed
         */
        protected boolean consume(char ch) {
            if (isValid(ch)) {
                length++;
                return true;
            }
            return false;
        }

        /**
         * Whether this run accepts that character; does not update state. Called
         * from the default implementation of <code>consume</code>.
         *
         * @param ch the character to test
         * @return <code>true</code> if <code>ch</code> is accepted
         */
        protected abstract boolean isValid(char ch);

        /**
         * Resets this run to the initial state.
         */
        protected void init() {
            length = 0;
        }
    }

    static final class Whitespace extends SQLWordBreakIterator.Run {
        @Override
        protected boolean isValid(char ch) {
            return Character.isWhitespace(ch) && ch != '\n' && ch != '\r';
        }
    }

    static final class LineDelimiter extends SQLWordBreakIterator.Run {
        /**
         * State: INIT -> delimiter -> EXIT.
         */
        private char fState;
        private static final char INIT = '\0';
        private static final char EXIT = '\1';

        /*
         * @see org.eclipse.jdt.internal.ui.text.SQLWordBreakIterator.Run#init()
         */
        @Override
        protected void init() {
            super.init();
            fState = INIT;
        }

        /*
         * @see org.eclipse.jdt.internal.ui.text.SQLWordBreakIterator.Run#consume(char)
         */
        @Override
        protected boolean consume(char ch) {
            if (!isValid(ch) || fState == EXIT)
                return false;

            if (fState == INIT) {
                fState = ch;
                length++;
                return true;
            } else if (fState != ch) {
                fState = EXIT;
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

    static final class SQLIdentifier extends SQLWordBreakIterator.Run {
        @Override
        protected boolean isValid(char ch) {
            return Character.isJavaIdentifierPart(ch);
        }
    }

    static final class Other extends SQLWordBreakIterator.Run {
        @Override
        protected boolean isValid(char ch) {
            return !Character.isWhitespace(ch) && !Character.isJavaIdentifierPart(ch);
        }
    }

    private static final SQLWordBreakIterator.Run WHITESPACE = new SQLWordBreakIterator.Whitespace();
    private static final SQLWordBreakIterator.Run DELIMITER = new SQLWordBreakIterator.LineDelimiter();
    private static final SQLWordBreakIterator.Run IDENTIFIER = new SQLIdentifier(); // new Identifier();
    private static final SQLWordBreakIterator.Run OTHER = new SQLWordBreakIterator.Other();

    /**
     * The platform break iterator (word instance) used as a base.
     */
    protected final BreakIterator fIterator;
    /**
     * The text we operate on.
     */
    protected CharSequence fText;
    /**
     * our current position for the stateful methods.
     */
    private int fIndex;


    /**
     * Creates a new break iterator.
     */
    public SQLWordBreakIterator() {
        fIterator = BreakIterator.getWordInstance();
        fIndex = fIterator.current();
    }

    public CharSequence getTextValue() {
        return fText;
    }

    /*
     * @see java.text.BreakIterator#current()
     */
    @Override
    public int current() {
        return fIndex;
    }

    /*
     * @see java.text.BreakIterator#first()
     */
    @Override
    public int first() {
        fIndex = fIterator.first();
        return fIndex;
    }

    /*
     * @see java.text.BreakIterator#following(int)
     */
    @Override
    public int following(int offset) {
        // work around too eager IAEs in standard implementation
        if (offset == getText().getEndIndex())
            return DONE;

        int next = fIterator.following(offset);
        if (next == DONE)
            return DONE;

        // TODO deal with complex script word boundaries
        // Math.min(offset + run.length, next) does not work
        // since BreakIterator.getWordInstance considers _ as boundaries
        // seems to work fine, however
        SQLWordBreakIterator.Run run = consumeRun(offset);
        return offset + run.length;

    }

    /**
     * Consumes a run of characters at the limits of which we introduce a break.
     *
     * @param offset the offset to start at
     * @return the run that was consumed
     */
    private SQLWordBreakIterator.Run consumeRun(int offset) {
        // assert offset < length

        char ch = fText.charAt(offset);
        int length = fText.length();
        SQLWordBreakIterator.Run run = getRun(ch);
        while (run.consume(ch) && offset < length - 1) {
            offset++;
            ch = fText.charAt(offset);
        }

        return run;
    }

    /**
     * Returns a run based on a character.
     *
     * @param ch the character to test
     * @return the correct character given <code>ch</code>
     */
    private SQLWordBreakIterator.Run getRun(char ch) {
        SQLWordBreakIterator.Run run;
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

    /*
     * @see java.text.BreakIterator#getText()
     */
    @Override
    public CharacterIterator getText() {
        return fIterator.getText();
    }

    /*
     * @see java.text.BreakIterator#isBoundary(int)
     */
    @Override
    public boolean isBoundary(int offset) {
        if (offset == getText().getBeginIndex())
            return true;
        else
            return following(offset - 1) == offset;
    }

    /*
     * @see java.text.BreakIterator#last()
     */
    @Override
    public int last() {
        fIndex = fIterator.last();
        return fIndex;
    }

    /*
     * @see java.text.BreakIterator#next()
     */
    @Override
    public int next() {
        fIndex = following(fIndex);
        return fIndex;
    }

    /*
     * @see java.text.BreakIterator#next(int)
     */
    @Override
    public int next(int n) {
        return fIterator.next(n);
    }

    /*
     * @see java.text.BreakIterator#preceding(int)
     */
    @Override
    public int preceding(int offset) {
        if (offset == getText().getBeginIndex())
            return DONE;

        if (isBoundary(offset - 1))
            return offset - 1;

        int previous = offset - 1;
        do {
            previous = fIterator.preceding(previous);
        } while (!isBoundary(previous));

        int last = DONE;
        while (previous < offset) {
            last = previous;
            previous = following(previous);
        }

        return last;
    }

    /*
     * @see java.text.BreakIterator#previous()
     */
    @Override
    public int previous() {
        fIndex = preceding(fIndex);
        return fIndex;
    }

    /*
     * @see java.text.BreakIterator#setText(java.lang.String)
     */
    @Override
    public void setText(String newText) {
        setText((CharSequence) newText);
    }

    /**
     * Creates a break iterator given a char sequence.
     *
     * @param newText the new text
     */
    public void setText(CharSequence newText) {
        fText = newText;
        fIterator.setText(new SQLSequenceCharacterIterator(newText));
        first();
    }

    /*
     * @see java.text.BreakIterator#setText(java.text.CharacterIterator)
     */
    @Override
    public void setText(CharacterIterator newText) {
        if (newText instanceof CharSequence) {
            fText = (CharSequence) newText;
            fIterator.setText(newText);
            first();
        } else {
            throw new UnsupportedOperationException("CharacterIterator not supported"); //$NON-NLS-1$
        }
    }

}
