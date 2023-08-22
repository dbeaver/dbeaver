/*******************************************************************************
 * Copyright (c) 2000, 2015 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Genady Beryozkin, me@genady.org - initial API and implementation
 *     Fabio Zadrozny <fabiofz at gmail dot com> - [typing] HippieCompleteAction is slow  ( Alt+/ ) - https://bugs.eclipse.org/bugs/show_bug.cgi?id=270385
 *******************************************************************************/
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
package org.jkiss.dbeaver.model.sql.completion.hippie;

import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.FindReplaceDocumentAdapter;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.jkiss.code.NotNull;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * This class contains the hippie completion engine methods that actually
 * compute the possible completions.
 * <p>
 * This engine is used by the <code>org.eclipse.ui.texteditor.HippieCompleteAction</code>.
 * </p>
 * <p>
 * TODO: Sort by editor type
 * TODO: Provide history option
 *
 * @author Genady Beryozkin, me@genady.org
 * @since 3.1
 */
public final class HippieCompletionEngine {

    /**
     * Regular expression that is used to find words.
     */
    // unicode identifier part
//	private static final String COMPLETION_WORD_REGEX= "[\\p{L}[\\p{Mn}[\\p{Pc}[\\p{Nd}[\\p{Nl}]]]]]+"; //$NON-NLS-1$
    // java identifier part (unicode id part + currency symbols)
    private static final String COMPLETION_WORD_REGEX = "[\\p{L}\\p{Mn}\\p{Pc}\\p{Nd}\\p{Nl}\\p{Sc}]+"; //$NON-NLS-1$
    /**
     * The pre-compiled word pattern.
     *
     * @since 3.2
     */
    private static final Pattern COMPLETION_WORD_PATTERN = Pattern.compile(COMPLETION_WORD_REGEX);

    /**
     * Word boundary pattern that does not allow searching at the beginning of the document.
     *
     * @since 3.2
     */
    private static final String NON_EMPTY_COMPLETION_BOUNDARY = "[\\s\\p{Z}[\\p{P}&&[\\P{Pc}]][\\p{S}&&[\\P{Sc}]]]+"; //$NON-NLS-1$

    /**
     * The word boundary pattern string.
     *
     * @since 3.2
     */
    private static final String COMPLETION_BOUNDARY = "(^|" + NON_EMPTY_COMPLETION_BOUNDARY + ")";  //$NON-NLS-1$ //$NON-NLS-2$
    // with a 1.5 JRE, you can do this:
//	private static final String COMPLETION_WORD_REGEX= "\\p{javaUnicodeIdentifierPart}+"; //$NON-NLS-1$
//	private static final String COMPLETION_WORD_REGEX= "\\p{javaJavaIdentifierPart}+"; //$NON-NLS-1$

    /**
     * Is completion case sensitive? Even if set to <code>false</code>, the
     * case of the prefix won't be changed.
     */
    private static final boolean CASE_SENSITIVE = true;

    /**
     * Creates a new engine.
     */
    public HippieCompletionEngine() {
    }

    /*
     * Copied from {@link FindReplaceDocumentAdapter#asRegPattern(java.lang.String)}.
     */

    /**
     * Converts a non-regex string to a pattern that can be used with the regex
     * search engine.
     *
     * @param string the non-regex pattern
     * @return the string converted to a regex pattern
     */
    private String asRegPattern(CharSequence string) {
        StringBuilder out = new StringBuilder(string.length());
        boolean quoting = false;

        for (int i = 0, length = string.length(); i < length; i++) {
            char ch = string.charAt(i);
            if (ch == '\\') {
                if (quoting) {
                    out.append("\\E"); //$NON-NLS-1$
                    quoting = false;
                }
                out.append("\\\\"); //$NON-NLS-1$
                continue;
            }
            if (!quoting) {
                out.append("\\Q"); //$NON-NLS-1$
                quoting = true;
            }
            out.append(ch);
        }
        if (quoting)
            out.append("\\E"); //$NON-NLS-1$

        return out.toString();
    }

    /**
     * Return the list of completion suggestions that correspond to the
     * provided prefix.
     *
     * @param document        the document to be scanned
     * @param prefix          the prefix to search for
     * @param firstPosition   the initial position in the document that
     *                        the search will start from. In order to search from the
     *                        beginning of the document use <code>firstPosition=0</code>.
     * @param currentWordLast if <code>true</code> the word at caret position
     *                        should be that last completion. <code>true</code> is good
     *                        for searching in the currently open document and <code>false</code>
     *                        is good for searching in other documents.
     * @return a {@link List} of possible completions (as {@link String}s),
     * excluding the common prefix
     */
    public List<String> getCompletionsForward(IDocument document, CharSequence prefix,
                                              int firstPosition, boolean currentWordLast) {
        ArrayList<String> res = new ArrayList<>();
        for (Iterator<String> it = getForwardIterator(document, prefix, firstPosition, currentWordLast); it.hasNext(); ) {
            res.add(it.next());
        }
        return res;
    }

    /**
     * Search for possible completions in the backward direction. If there
     * is a possible completion that begins before <code>firstPosition</code>
     * but ends after that position, it will not be included in the results.
     *
     * @param document      the document to be scanned
     * @param prefix        the completion prefix
     * @param firstPosition the caret position
     * @return a {@link List} of possible completions ({@link String}s)
     * from the caret position to the beginning of the document.
     * The empty suggestion is not included in the results.
     */
    public List<String> getCompletionsBackwards(IDocument document, CharSequence prefix, int firstPosition) {
        ArrayList<String> res = new ArrayList<>();
        for (Iterator<String> it = getBackwardIterator(document, prefix, firstPosition); it.hasNext(); ) {
            res.add(it.next());
        }
        return res;
    }

    /**
     * Returns the text between the provided position and the preceding word boundary.
     *
     * @param doc the document that will be scanned.
     * @param pos the caret position.
     * @return the text if found, or null.
     * @throws BadLocationException if an error occurs.
     * @since 3.2
     */
    public String getPrefixString(IDocument doc, int pos) throws BadLocationException {
        Matcher m = COMPLETION_WORD_PATTERN.matcher(""); //$NON-NLS-1$
        int prevNonAlpha = pos;
        while (prevNonAlpha > 0) {
            m.reset(doc.get(prevNonAlpha - 1, pos - prevNonAlpha + 1));
            if (!m.matches()) {
                break;
            }
            prevNonAlpha--;
        }
        if (prevNonAlpha != pos) {
            return doc.get(prevNonAlpha, pos - prevNonAlpha);
        }
        return null;
    }

    /**
     * Remove duplicate suggestions (excluding the prefix), leaving the closest
     * to list head.
     *
     * @param suggestions a list of suggestions ({@link String}).
     * @return a list of unique completion suggestions.
     */
    public List<String> makeUnique(List<String> suggestions) {
        HashSet<String> seenAlready = new HashSet<>();
        ArrayList<String> uniqueSuggestions = new ArrayList<>();

        for (String suggestion : suggestions) {
            if (!seenAlready.contains(suggestion)) {
                seenAlready.add(suggestion);
                uniqueSuggestions.add(suggestion);
            }
        }
        return uniqueSuggestions;
    }

    /**
     * Provides an iterator that will get the completions that start with the passed prefix after
     * the passed position (forward until the end of the document).
     *
     * @param document        the document to be scanned
     * @param prefix          the prefix to search for
     * @param firstPosition   the initial position in the document that the search will start from. In
     *                        order to search from the beginning of the document use
     *                        <code>firstPosition=0</code>.
     * @param currentWordLast if <code>true</code> the word at caret position should be that last
     *                        completion. <code>true</code> is good for searching in the currently open document
     *                        and <code>false</code> is good for searching in other documents.
     * @return Iterator (for Strings) that will get the completions forward from the passed
     * position.
     * @since 3.6
     */
    public Iterator<String> getForwardIterator(IDocument document, CharSequence prefix, int firstPosition, boolean currentWordLast) {
        return new HippieCompletionForwardIterator(document, prefix, firstPosition, currentWordLast);
    }

    /**
     * Provides an iterator that will get the completions that start with the passed prefix before
     * the passed position (backwards until the start of the document).
     *
     * @param document      the document to be scanned
     * @param prefix        the prefix to search for
     * @param firstPosition the initial position in the document that the search will start from. In
     *                      order to search from the end of the document use
     *                      <code>firstPosition=document.getLength()</code>.
     * @return Iterator that will get the completions backward from the passed position.
     * @since 3.6
     */
    public Iterator<String> getBackwardIterator(IDocument document, CharSequence prefix, int firstPosition) {
        return new HippieCompletionBackwardIterator(document, prefix, firstPosition);
    }

    /**
     * Provides an iterator that will get the completions for all the documents received, starting
     * at the "document" passed (first going backward and then forward from the position passed) and
     * later going forward through each of the "otherDocuments".
     *
     * @param document       the document to be scanned
     * @param otherDocuments the additional documents to be scanned
     * @param prefix         the prefix to search for
     * @param firstPosition  the initial position in the document that the search will start from.
     * @return Iterator that will first get the completions backward from the document passed, then
     * forward in that same document and when that is finished it will get it forward for
     * the other documents (in the same sequence the documents are available).
     * @since 3.6
     */
    public Iterator<String> getMultipleDocumentsIterator(IDocument document, List<IDocument> otherDocuments, CharSequence prefix, int firstPosition) {
        return new MultipleDocumentsIterator(document, otherDocuments, prefix, firstPosition);
    }


    /**
     * Class that keeps the state while iterating the suggestions
     *
     * @since 3.6
     */
    private final class MultipleDocumentsIterator implements Iterator<String> {

        /**
         * This is the next token to be returned (when null, no more tokens should be returned)
         */
        private String fNext;

        /**
         * -1 means that we still haven't checked the current do completions Any other number means
         * that we'll get the completions for some other editor.
         */
        private int fCurrLocation = -1;

        /**
         * These are the suggestions which we already loaded.
         */
        private final List<String> fSuggestions;

        /**
         * This marks the current suggestion to be returned
         */
        private int fCurrSuggestion = 0;

        /**
         * This is the prefix that should be searched
         */
        private final CharSequence fPrefix;

        /**
         * The list of IDocuments that we should search
         */
        private final List<IDocument> fOtherDocuments;

        /**
         * The document that's currently opened (that's the 1st we should look and we should 1st
         * search backwards from the current offset and later forwards)
         */
        private final IDocument fOpenDocument;

        /**
         * The current offset in the opened document
         */
        private final int fSelectionOffset;

        /**
         * Indicates whether we already added the empty completion.
         */
        private boolean fAddedEmpty = false;

        /**
         * The 'current' forward iterator.
         */
        private Iterator<String> fCompletionsForwardIterator;

        /**
         * The 'current' backward iterator.
         */
        private Iterator<String> fCompletionsBackwardIterator;

        private MultipleDocumentsIterator(IDocument openDocument, List<IDocument> otherDocuments,
                                          CharSequence prefix, int selectionOffset) {
            this.fPrefix = prefix;
            this.fSuggestions = new ArrayList<>();
            this.fOtherDocuments = otherDocuments;
            this.fSelectionOffset = selectionOffset;
            this.fOpenDocument = openDocument;
            calculateNext();
        }


        /**
         * This method calculates the next token to be returned (so, after creating the class or
         * after calling next(), this function must be called).
         * <p>
         * It'll check which document should be used and will get the completions on that document
         * until some completion is found.
         * <p>
         * An empty completion is always added at the end.
         * <p>
         * After the empty completion, the next is set to null.
         */
        private void calculateNext() {
            if (fCurrLocation == -1) {
                fCompletionsBackwardIterator = getBackwardIterator(
                    fOpenDocument, fPrefix, fSelectionOffset);

                fCompletionsForwardIterator = getForwardIterator(
                    fOpenDocument, fPrefix, (fSelectionOffset - fPrefix.length()), true);
                fCurrLocation++;
            }
            if (checkNext()) {
                return;
            }


            while (fCurrLocation < this.fOtherDocuments.size()) {
                fCompletionsForwardIterator = getForwardIterator(
                    (this.fOtherDocuments.get(fCurrLocation)), fPrefix, 0, false);
                fCurrLocation++;
                if (checkNext()) {
                    return;
                }
            }

            // add the empty suggestion (last one)
            if (!fAddedEmpty) {
                fSuggestions.add(""); //$NON-NLS-1$
                fAddedEmpty = true;
            }
            checkNext();
        }

        /**
         * @return true if a completion was found and false if it couldn't be found -- in which case
         * the next is set to null.
         */
        private boolean checkNext() {
            if (fCompletionsBackwardIterator != null) {
                if (fCompletionsBackwardIterator.hasNext()) {
                    fSuggestions.add(fCompletionsBackwardIterator.next());
                } else {
                    fCompletionsBackwardIterator = null;
                }
            }
            //only get if backward completions are consumed
            if (fCompletionsBackwardIterator == null && fCompletionsForwardIterator != null && fCompletionsForwardIterator.hasNext()) {
                fSuggestions.add(fCompletionsForwardIterator.next());
            }

            if (fSuggestions.size() > fCurrSuggestion) {
                fNext = fSuggestions.get(fCurrSuggestion);
                fCurrSuggestion++;
                return true;
            }
            fNext = null;
            return false;
        }

        /**
         * We always calculate the next to see if it's available.
         *
         * @return <code>true</code> if the next token to be returned is not null (we always
         * pre-calculate things)
         */
        @Override
        public boolean hasNext() {
            return fNext != null;
        }


        /**
         * @return the next suggestion
         */
        @Override
        public String next() {
            if (fNext == null) {
                throw new NoSuchElementException("No more elements to iterate"); //$NON-NLS-1$
            }
            String ret = fNext;
            calculateNext();
            return ret;
        }

        /**
         * Not supported!
         *
         * @throws UnsupportedOperationException always.
         */
        @Override
        public void remove() {
            throw new UnsupportedOperationException("Not supported"); //$NON-NLS-1$

        }

    }

    /**
     * Base class for Iterator that gets the word completions in a document, and returns them one by
     * one (lazily gotten).
     *
     * @since 3.6
     */
    private abstract static class HippieCompletionIterator implements Iterator<String> {

        /**
         * The document to be scanned
         */
        protected IDocument fDocument;

        /**
         * The prefix to search for
         */
        protected CharSequence fPrefix;

        /**
         * The initial position in the document that the search will start from. In order to search
         * from the beginning of the document use <code>firstPosition=0</code>.
         */
        protected int fFirstPosition;

        /**
         * Determines if we have a next element to be returned.
         */
        protected boolean fHasNext;

        /**
         * The next element to be returned
         */
        protected String fNext;

        /**
         * The current state for the iterator
         */
        protected int fCurrentState = 0;

        /**
         * The class that'll do the search
         */
        protected FindReplaceDocumentAdapter fSearcher;

        /**
         * Pattern to be used -- search only at word boundaries
         */
        protected String fSearchPattern;

        /**
         * The next place to search for
         */
        protected int fNextPos;


        /**
         * Constructor
         *
         * @param document      the document to be scanned
         * @param prefix        the prefix to search for
         * @param firstPosition the initial position in the document that the search will start
         *                      from. In order to search from the beginning of the document use
         *                      <code>firstPosition=0</code>.
         */
        public HippieCompletionIterator(IDocument document, CharSequence prefix, int firstPosition) {
            this.fDocument = document;
            this.fPrefix = prefix;
            this.fFirstPosition = firstPosition;
        }

        /**
         * Must be called to calculate the first completion (subclasses must explicitly call it when
         * properly initialized).
         */
        protected void calculateFirst() {
            try {
                calculateNext();
            } catch (BadLocationException e) {
                fHasNext = false;
                fNext = null;
            }
        }

        @Override
        public boolean hasNext() {
            return fHasNext;
        }

        @Override
        public String next() {
            if (!fHasNext) {
                throw new NoSuchElementException();
            }
            String ret = fNext;
            try {
                calculateNext();
            } catch (BadLocationException e) {
                fHasNext = false;
                fNext = null;
            }
            return ret;
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }

        /**
         * Subclasses must override to calculates whether we have a next element to be returned and
         * which element it is (set fHasNext and fNext).
         *
         * @throws BadLocationException if we're at an invalid position in the document.
         */
        protected abstract void calculateNext() throws BadLocationException;
    }


    /**
     * Iterator that gets the word completions in a document, and returns them one by one (lazily
     * gotten) from the current position.
     *
     * @since 3.6
     */
    private class HippieCompletionForwardIterator extends HippieCompletionIterator {

        /**
         * If <code>true</code> the word at caret position should be that last completion.
         * <code>true</code> is good for searching in the currently open document and
         * <code>false</code> is good for searching in other documents.
         */
        private final boolean fCurrentWordLast;


        /**
         * The completion for the current word -- fix bug 132533
         */
        private String fCurrentWordCompletion = null;


        private HippieCompletionForwardIterator(IDocument document, CharSequence prefix, int firstPosition, boolean currentWordLast) {
            super(document, prefix, firstPosition);
            this.fCurrentWordLast = currentWordLast;
            calculateFirst();
        }

        @Override
        protected void calculateNext() throws BadLocationException {
            if (fCurrentState == 0) {
                if (fFirstPosition == fDocument.getLength()) {
                    this.fHasNext = false;
                    return;
                }
                fSearcher = new FindReplaceDocumentAdapter(fDocument);

                // unless we are at the beginning of the document, the completion boundary
                // matches one character. It is enough to move just one character backwards
                // because the boundary pattern has the (....)+ form.
                // see HippieCompletionTest#testForwardSearch().
                if (fFirstPosition > 0) {
                    fFirstPosition--;
                    // empty spacing is not permitted now.
                    fSearchPattern = NON_EMPTY_COMPLETION_BOUNDARY + asRegPattern(fPrefix);
                } else {
                    fSearchPattern = COMPLETION_BOUNDARY + asRegPattern(fPrefix);
                }

                fNextPos = fFirstPosition;
                fCurrentState = 1;
            }

            if (fCurrentState == 1) {
                fHasNext = false;
                IRegion reg = fSearcher.find(fNextPos, fSearchPattern, true, CASE_SENSITIVE, false, true);
                while (reg != null) {
                    fNextPos = consumeWordAt(reg);
                    if (fNextPos >= fDocument.getLength()) {
                        fCurrentState = 2;
                        if (fHasNext) {
                            return;
                        }
                        break;
                    } else {
                        if (fHasNext) {
                            return;
                        }
                        reg = fSearcher.find(fNextPos, fSearchPattern, true, CASE_SENSITIVE, false, true);
                    }
                }
                fCurrentState = 2;
            }

            if (fCurrentState == 2) {
                fCurrentState = 3;
                // the word at caret position goes last (bug 132533).
                if (fCurrentWordCompletion != null) {
                    fNext = fCurrentWordCompletion;
                    fHasNext = true;
                    return;
                }
            }

            fNext = null;
            fHasNext = false;
        }

        /**
         * Checks the given region for a word to be returned in this iterator.
         *
         * @param wordHead the region to check
         * @return the word region.
         * @throws BadLocationException if we're at an invalid position in the document.
         */
        private int consumeWordAt(@NotNull IRegion wordHead) throws BadLocationException {
            // since the boundary may be of nonzero length
            int wordSearchPos = wordHead.getOffset() + wordHead.getLength();
            // try to complete to a word. case is irrelevant here.
            IRegion wordTail = fSearcher.find(wordSearchPos, COMPLETION_WORD_REGEX, true, true, false, true);
            if (wordTail != null && wordTail.getOffset() == wordSearchPos 
                && wordTail.getLength() > 0 // empty suggestion will be added later
            ) {
                String completion = fDocument.get(wordTail.getOffset(), wordTail.getLength());
                if (completion.length() > 0) { // empty suggestion will be added later
                    if (fCurrentWordLast && wordHead.getOffset() == fFirstPosition) { // we got the word at caret as completion
                        if (fCurrentWordCompletion == null) {
                            fCurrentWordCompletion = completion; // add it as the last word.
                        }
                    } else {
                        fNext = completion;
                        fHasNext = true;
                    }
                }
                return wordTail.getOffset() + wordTail.getLength();
            } else {
                return wordSearchPos;
            }
        }
    }


    /**
     * Iterator that gets the word completions in a document, and returns them one by one (lazily
     * gotten) backward from the current position.
     *
     * @since 3.6
     */
    private class HippieCompletionBackwardIterator extends HippieCompletionIterator {

        /**
         * Last position searched
         **/
        private int fLastSearchPos = -1;

        private HippieCompletionBackwardIterator(IDocument document, CharSequence prefix, int firstPosition) {
            super(document, prefix, firstPosition);
            calculateFirst();
        }

        @Override
        protected void calculateNext() throws BadLocationException {
            if (fCurrentState == 0) {
                fCurrentState = 1;
                // FindReplaceDocumentAdapter expects the start offset to be before the
                // actual caret position, probably for compatibility with forward search.
                if (fFirstPosition <= 1) {
                    this.fNext = null;
                    this.fHasNext = false;
                    return;
                }
                fSearcher = new FindReplaceDocumentAdapter(fDocument);

                // search only at word boundaries
                fSearchPattern = COMPLETION_BOUNDARY + asRegPattern(fPrefix);

                int length = fDocument.getLength();
                fNextPos = fFirstPosition;
                if (fNextPos >= length) {
                    fNextPos = length - 1;
                }
            }
            while (true) {
                if (fNextPos <= 0) {
                    this.fNext = null;
                    this.fHasNext = false;
                    return;
                }

                Assert.isTrue(fLastSearchPos != fNextPos, "Position did not change in loop (this would lead to recursion -- and should never happen)."); //$NON-NLS-1$

                fLastSearchPos = fNextPos;
                IRegion wordHead = fSearcher.find(fNextPos, fSearchPattern, false, CASE_SENSITIVE, false, true);
                if (wordHead == null) {
                    this.fNext = null;
                    this.fHasNext = false;
                    return;
                }

                // since the boundary may be of nonzero length
                int wordSearchPos = wordHead.getOffset() + wordHead.getLength();
                // try to complete to a word. case is of no matter here
                IRegion wordTail = fSearcher.find(wordSearchPos, COMPLETION_WORD_REGEX, true, true, false, true);
                fNextPos = wordHead.getOffset() - 1;
                if (wordTail == null 
                    || wordTail.getOffset() != wordSearchPos 
                    || wordTail.getOffset() + wordTail.getLength() > fFirstPosition
                ) {
                    continue;
                }
                if (wordTail.getLength() > 0) { // empty suggestion will be added later
                    String found = fDocument.get(wordTail.getOffset(), wordTail.getLength());
                    this.fHasNext = true;
                    this.fNext = found;
                    return;
                }
            }

            //Note: unreachable section
        }

    }

}
