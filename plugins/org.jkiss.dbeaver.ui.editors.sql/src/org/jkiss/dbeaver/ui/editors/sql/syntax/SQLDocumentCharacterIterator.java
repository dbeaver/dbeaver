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
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;

import java.text.CharacterIterator;


/**
 * An <code>IDocument</code> based implementation of
 * <code>CharacterIterator</code> and <code>CharSequence</code>. Note that
 * the supplied document is not copied; if the document is modified during the
 * lifetime of a <code>DocumentCharacterIterator</code>, the methods
 * returning document content may not always return the same values. Also, if
 * accessing the document fails with a {@link BadLocationException}, any of
 * <code>CharacterIterator</code> methods as well as <code>charAt</code>may
 * return {@link CharacterIterator#DONE}.
 *
 * @since 3.0
 */
public class SQLDocumentCharacterIterator implements CharacterIterator, CharSequence {

    private int index;
    private final IDocument document;
    private final int first;
    private final int last;

    private void invariant() {
        Assert.isTrue(index >= first);
        Assert.isTrue(index <= last);
    }

    /**
     * Creates an iterator for the entire document.
     *
     * @param document the document backing this iterator
     * @throws BadLocationException if the indices are out of bounds
     */
    public SQLDocumentCharacterIterator(IDocument document) throws BadLocationException {
        this(document, 0);
    }

    /**
     * Creates an iterator, starting at offset <code>first</code>.
     *
     * @param document the document backing this iterator
     * @param first    the first character to consider
     * @throws BadLocationException if the indices are out of bounds
     */
    public SQLDocumentCharacterIterator(IDocument document, int first) throws BadLocationException {
        this(document, first, document.getLength());
    }

    /**
     * Creates an iterator for the document contents from <code>first</code> (inclusive) to
     * <code>last</code> (exclusive).
     *
     * @param document the document backing this iterator
     * @param first    the first character to consider
     * @param last     the last character index to consider
     * @throws BadLocationException if the indices are out of bounds
     */
    public SQLDocumentCharacterIterator(IDocument document, int first, int last) throws BadLocationException {
        if (document == null)
            throw new NullPointerException();
        if (first < 0 || first > last)
            throw new BadLocationException();
        if (last > document.getLength()) {
            throw new BadLocationException();
        }
        this.document = document;
        this.first = first;
        this.last = last;
        this.index = first;
        this.invariant();
    }

    @Override
    public char first() {
        return setIndex(getBeginIndex());
    }

    @Override
    public char last() {
        if (first == last)
            return setIndex(getEndIndex());
        else
            return setIndex(getEndIndex() - 1);
    }

    @Override
    public char current() {
        if (index >= first && index < last)
            try {
                return document.getChar(index);
            } catch (BadLocationException e) {
                // ignore
            }
        return DONE;
    }

    @Override
    public char next() {
        return setIndex(Math.min(index + 1, getEndIndex()));
    }

    @Override
    public char previous() {
        if (index > getBeginIndex()) {
            return setIndex(index - 1);
        } else {
            return DONE;
        }
    }

    @Override
    public char setIndex(int position) {
        if (position >= getBeginIndex() && position <= getEndIndex())
            index = position;
        else
            throw new IllegalArgumentException();

        invariant();
        return current();
    }

    @Override
    public int getBeginIndex() {
        return first;
    }

    @Override
    public int getEndIndex() {
        return last;
    }

    @Override
    public int getIndex() {
        return index;
    }

    @Override
    public Object clone() {
        try {
            return super.clone();
        } catch (CloneNotSupportedException e) {
            throw new InternalError();
        }
    }

    /*
     * @see java.lang.CharSequence#length()
     */
    @Override
    public int length() {
        return getEndIndex() - getBeginIndex();
    }

    @Override
    public char charAt(int index) {
        if (index >= 0 && index < length())
            try {
                return document.getChar(getBeginIndex() + index);
            } catch (BadLocationException e) {
                // ignore and return DONE
                return DONE;
            }
        else
            throw new IndexOutOfBoundsException();
    }

    @Override
    public CharSequence subSequence(int start, int end) {
        if (start < 0)
            throw new IndexOutOfBoundsException();
        if (end < start)
            throw new IndexOutOfBoundsException();
        if (end > length())
            throw new IndexOutOfBoundsException();
        try {
            return new SQLDocumentCharacterIterator(document, getBeginIndex() + start, getBeginIndex() + end);
        } catch (BadLocationException ex) {
            throw new IndexOutOfBoundsException();
        }
    }

    /**
     * @see CharSequence#toString
     **/
    @Override
    public String toString() {
        return document.get().substring(getBeginIndex(), getEndIndex());
    }
}
