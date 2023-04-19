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

import java.text.CharacterIterator;

public class SQLSequenceCharacterIterator implements CharacterIterator {
    private int index;
    private final CharSequence sequence;
    private final int first;
    private final int last;

    private void invariant() {
        Assert.isTrue(this.index >= this.first);
        Assert.isTrue(this.index <= this.last);
    }

    public SQLSequenceCharacterIterator(CharSequence sequence) {
        this(sequence, 0);
    }

    public SQLSequenceCharacterIterator(CharSequence sequence, int first) throws IllegalArgumentException {
        this(sequence, first, sequence.length());
    }

    public SQLSequenceCharacterIterator(CharSequence sequence, int first, int last) throws IllegalArgumentException {
        this.index = -1;
        if (sequence == null) {
            throw new NullPointerException();
        } else if (first >= 0 && first <= last) {
            if (last > sequence.length()) {
                throw new IllegalArgumentException();
            } else {
                this.sequence = sequence;
                this.first = first;
                this.last = last;
                this.index = first;
                this.invariant();
            }
        } else {
            throw new IllegalArgumentException();
        }
    }

    public char first() {
        return this.setIndex(this.getBeginIndex());
    }

    public char last() {
        return this.first == this.last ? this.setIndex(this.getEndIndex()) : this.setIndex(this.getEndIndex() - 1);
    }

    public char current() {
        return this.index >= this.first && this.index < this.last ? this.sequence.charAt(this.index) : '\uffff';
    }

    public char next() {
        return this.setIndex(Math.min(this.index + 1, this.getEndIndex()));
    }

    public char previous() {
        return this.index > this.getBeginIndex() ? this.setIndex(this.index - 1) : '\uffff';
    }

    public char setIndex(int position) {
        if (position >= this.getBeginIndex() && position <= this.getEndIndex()) {
            this.index = position;
            this.invariant();
            return this.current();
        } else {
            throw new IllegalArgumentException();
        }
    }

    public int getBeginIndex() {
        return this.first;
    }

    public int getEndIndex() {
        return this.last;
    }

    public int getIndex() {
        return this.index;
    }

    public Object clone() {
        try {
            return super.clone();
        } catch (CloneNotSupportedException var1) {
            throw new InternalError();
        }
    }
}
