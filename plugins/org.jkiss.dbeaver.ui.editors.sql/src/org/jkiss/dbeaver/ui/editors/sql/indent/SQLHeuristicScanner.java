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
package org.jkiss.dbeaver.ui.editors.sql.indent;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITypedRegion;
import org.eclipse.jface.text.TextUtilities;
import org.jkiss.dbeaver.model.DBPKeywordType;
import org.jkiss.dbeaver.model.sql.SQLSyntaxManager;
import org.jkiss.dbeaver.model.sql.parser.SQLParserPartitions;


/**
 * Utility methods for heuristic based SQL manipulations in an incomplete SQL source file.
 * <p/>
 * <p>
 * An instance holds some internal position in the document and is therefore not threadsafe.
 * </p>
 *
 * @author Li Huang, Serge Rider
 */
public class SQLHeuristicScanner implements SQLIndentSymbols {
    /**
     * Returned by all methods when the requested position could not be found, or if a {@link BadLocationException}was
     * thrown while scanning.
     */
    public static final int NOT_FOUND = -1;

    /**
     * Special bound parameter that means either -1 (backward scanning) or <code>fDocument.getLength()</code> (forward
     * scanning).
     */
    public static final int UNBOUND = -2;

    /**
     * Stops at any delimiter or non-whitespace character
     */
    private class DelimiterCondition implements StopCondition {
        @Override
        public boolean stop(char ch, int position, boolean forward) {
            return isDelimiterChar(ch) || !Character.isWhitespace(ch);
        }

        private boolean isDelimiterChar(char ch) {
            for (String delim : syntaxManager.getStatementDelimiters()) {
                if (delim.length() == 1 && delim.charAt(0) == ch) {
                    return true;
                }
            }
            return false;
        }
    }

    /**
     * Stops upon a non-whitespace (as defined by {@link Character#isWhitespace(char)}) character.
     */
    private static class NonWhitespace implements StopCondition {
        @Override
        public boolean stop(char ch, int position, boolean forward) {
            return !Character.isWhitespace(ch);
        }
    }

    /**
     * Stops upon a non-whitespace character in the default partition.
     *
     * @see NonWhitespace
     */
    private class NonWhitespaceDefaultPartition extends NonWhitespace {
        @Override
        public boolean stop(char ch, int position, boolean forward) {
            return super.stop(ch, position, true) && isDefaultPartition(position);
        }
    }

    /**
     * Stops upon a non-sql identifier (as defined by {@link Character#isJavaIdentifierPart(char)}) character.
     */
    private static class NonSQLIdentifierPart implements StopCondition {
        @Override
        public boolean stop(char ch, int position, boolean forward) {
            return !Character.isJavaIdentifierPart(ch);
        }
    }

    /**
     * Stops upon a non-sql identifier character in the default partition.
     *
     * @see NonSQLIdentifierPart
     */
    private class NonSQLIdentifierPartDefaultPartition extends NonSQLIdentifierPart {
        @Override
        public boolean stop(char ch, int position, boolean forward) {
            return super.stop(ch, position, true) || !isDefaultPartition(position);
        }
    }

    /**
     * The document being scanned.
     */
    private IDocument document;
    /**
     * The partitioning being used for scanning.
     */
    private String partitioning;
    /**
     * The partition to scan in.
     */
    private String partition;

    private SQLSyntaxManager syntaxManager;

    /**
     * the most recently read character.
     */
    private char lastChar;
    /**
     * the most recently read position.
     */
    private int lastPos;
    // Last parsed token value
    private String lastToken;

    /* preset stop conditions */
    private final StopCondition _nonWSDefaultPart = new NonWhitespaceDefaultPartition();
    private final static StopCondition _nonWS = new NonWhitespace();
    private final StopCondition _nonIdent = new NonSQLIdentifierPartDefaultPartition();
    private final DelimiterCondition DELIMITER_CONDITION = new DelimiterCondition();

    public SQLHeuristicScanner(IDocument document, String partitioning, String partition, SQLSyntaxManager syntaxManager) {
        assert (document != null);
        assert (partitioning != null);
        assert (partition != null);
        this.document = document;
        this.partitioning = partitioning;
        this.partition = partition;

        this.syntaxManager = syntaxManager;
    }

    public SQLHeuristicScanner(IDocument document, SQLSyntaxManager syntaxManager) {
        this(document, SQLParserPartitions.SQL_PARTITIONING, IDocument.DEFAULT_CONTENT_TYPE, syntaxManager);
    }

    public int getPosition() {
        return lastPos;
    }

    public String getLastToken() {
        return lastToken;
    }

    /**
     * Returns the next token in forward direction, starting at <code>start</code>, and not extending further than
     * <code>bound</code>. The return value is one of the constants defined in {@link SQLIndentSymbols}. After a call,
     * {@link #getPosition()}will return the position just after the scanned token (i.e. the next position that will be
     * scanned).
     *
     * @param start the first character position in the document to consider
     * @param bound the first position not to consider any more
     * @return a constant from {@link SQLIndentSymbols}describing the next token
     */
    public int nextToken(int start, int bound) {
        int pos = scanForward(start, bound, _nonWSDefaultPart);
        if (pos == NOT_FOUND) {
            return TokenEOF;
        }

        lastPos++;

        if (Character.isJavaIdentifierPart(lastChar)) {
            // assume an ident or keyword
            int from = pos, to;
            pos = scanForward(pos + 1, bound, _nonIdent);
            if (pos == NOT_FOUND) {
                to = bound == UNBOUND ? document.getLength() : bound;
            } else {
                to = pos;
            }

            try {
                lastToken = document.get(from, to - from);
            } catch (BadLocationException e) {
//                _log.debug(EditorMessages.error_badLocationException, e);
                return TokenEOF;
            }

            return getToken(lastToken);

        } else {
            // operators, number literals etc
            return TokenOTHER;
        }
    }

    /**
     * Returns the next token in backward direction, starting at <code>start</code>, and not extending further than
     * <code>bound</code>. The return value is one of the constants defined in {@link SQLIndentSymbols}. After a call,
     * {@link #getPosition()}will return the position just before the scanned token starts (i.e. the next position that
     * will be scanned).
     *
     * @param start the first character position in the document to consider
     * @param bound the first position not to consider any more
     * @return a constant from {@link SQLIndentSymbols}describing the previous token
     */
    public int previousToken(int start, int bound) {
        int pos = scanBackward(start, bound, _nonWSDefaultPart);
        if (pos == NOT_FOUND) {
            return TokenEOF;
        }

        lastPos--;

        if (Character.isJavaIdentifierPart(lastChar)) {
            // assume an ident or keyword
            int from, to = pos + 1;
            pos = scanBackward(pos - 1, bound, _nonIdent);
            if (pos == NOT_FOUND) {
                from = bound == UNBOUND ? 0 : bound + 1;
            } else {
                from = pos + 1;
            }

            try {
                lastToken = document.get(from, to - from);
            } catch (BadLocationException e) {
//                _log.debug(EditorMessages.error_badLocationException, e);
                return TokenEOF;
            }

            return getToken(lastToken);

        } else {
            // operators, number literals etc
            lastToken = String.valueOf(lastChar);
            return TokenOTHER;
        }

    }

    /**
     * Returns one of the keyword constants or <code>TokenIDENT</code> for a scanned identifier.
     *
     * @param s a scanned identifier
     * @return one of the constants defined in {@link SQLIndentSymbols}
     */
    private int getToken(String s) {
        assert (s != null);

        switch (s.length()) {
            case 3:
                if (SQLIndentSymbols.end.equals(s)) {
                    return Tokenend;
                }
                if (SQLIndentSymbols.END.equalsIgnoreCase(s)) {
                    return TokenEND;
                }
                break;
            case 5:
                if (SQLIndentSymbols.begin.equals(s)) {
                    return Tokenbegin;
                }
                if (SQLIndentSymbols.BEGIN.equalsIgnoreCase(s)) {
                    return TokenBEGIN;
                }
                break;

        }
        final DBPKeywordType keywordType = syntaxManager.getDialect().getKeywordType(s);
        if (keywordType == DBPKeywordType.KEYWORD) {
            return TokenKeyword;
        }
//        if (syntaxManager.getDialect().isKeywordStart(s)) {
//            return TokenKeywordStart;
//        }
        return TokenOTHER;
    }

    /**
     * Finds the smallest position in <code>fDocument</code> such that the position is &gt;= <code>position</code>
     * and &lt; <code>bound</code> and <code>Character.isWhitespace(fDocument.getChar(pos))</code> evaluates to
     * <code>false</code>.
     *
     * @param position the first character position in <code>fDocument</code> to be considered
     * @param bound    the first position in <code>fDocument</code> to not consider any more, with <code>bound</code>
     *                 &gt; <code>position</code>, or <code>UNBOUND</code>
     * @return the smallest position of a non-whitespace character in [<code>position</code>,<code>bound</code>),
     * or <code>NOT_FOUND</code> if none can be found
     */
    public int findNonWhitespaceForwardInAnyPartition(int position, int bound) {
        return scanForward(position, bound, _nonWS);
    }

    public boolean endsWithDelimiter(int position, int bound) {
        int endPos = scanBackward(bound, position, DELIMITER_CONDITION);
        try {
            return endPos > position && DELIMITER_CONDITION.isDelimiterChar(document.getChar(endPos));
        } catch (BadLocationException e) {
            return false;
        }
    }
    /**
     * Finds the lowest position <code>p</code> in <code>fDocument</code> such that <code>start</code> &lt;= p
     * &lt; <code>bound</code> and <code>condition.stop(fDocument.getChar(p), p)</code> evaluates to
     * <code>true</code>.
     *
     * @param start     the first character position in <code>fDocument</code> to be considered
     * @param bound     the first position in <code>fDocument</code> to not consider any more, with <code>bound</code>
     *                  &gt; <code>start</code>, or <code>UNBOUND</code>
     * @param condition the <code>StopCondition</code> to check
     * @return the lowest position in [<code>start</code>,<code>bound</code>) for which <code>condition</code>
     * holds, or <code>NOT_FOUND</code> if none can be found
     */
    public int scanForward(int start, int bound, StopCondition condition) {
        assert (start >= 0);

        if (bound == UNBOUND) {
            bound = document.getLength();
        }

        assert (bound <= document.getLength());

        try {
            lastPos = start;
            while (lastPos < bound) {

                lastChar = document.getChar(lastPos);
                if (condition.stop(lastChar, lastPos, true)) {
                    return lastPos;
                }

                lastPos++;
            }
        } catch (BadLocationException e) {
//            _log.debug(EditorMessages.error_badLocationException, e);
        }
        return NOT_FOUND;
    }

    /**
     * Finds the highest position <code>p</code> in <code>fDocument</code> such that <code>bound</code> &lt;
     * <code>p</code> &lt;= <code>start</code> and <code>condition.stop(fDocument.getChar(p), p)</code> evaluates
     * to <code>true</code>.
     *
     * @param start     the first character position in <code>fDocument</code> to be considered
     * @param bound     the first position in <code>fDocument</code> to not consider any more, with <code>bound</code>
     *                  &lt; <code>start</code>, or <code>UNBOUND</code>
     * @param condition the <code>StopCondition</code> to check
     * @return the highest position in (<code>bound</code>,<code>start</code> for which <code>condition</code>
     * holds, or <code>NOT_FOUND</code> if none can be found
     */
    public int scanBackward(int start, int bound, StopCondition condition) {
        if (bound == UNBOUND) {
            bound = -1;
        }

        assert (bound >= -1);
        assert (start < document.getLength());

        try {
            lastPos = start;
            while (lastPos > bound) {

                lastChar = document.getChar(lastPos);
                if (condition.stop(lastChar, lastPos, false)) {
                    return lastPos;
                }

                lastPos--;
            }
        } catch (BadLocationException e) {
//            _log.debug(EditorMessages.error_badLocationException, e);
        }
        return NOT_FOUND;
    }

    /**
     * Checks whether <code>position</code> resides in a default (SQL) partition of <code>document</code>.
     *
     * @param position the position to be checked
     * @return <code>true</code> if <code>position</code> is in the default partition of <code>document</code>,
     * <code>false</code> otherwise
     */
    public boolean isDefaultPartition(int position) {
        assert (position >= 0);
        assert (position <= document.getLength());

        try {
            ITypedRegion region = TextUtilities.getPartition(document, partitioning, position, false);
            return region.getType().equals(partition);

        } catch (BadLocationException e) {
//            _log.debug(EditorMessages.error_badLocationException, e);
        }

        return false;
    }


    /**
     * Returns the position of the opening peer token (backward search). Any scopes introduced by closing peers are
     * skipped. All peers accounted for must reside in the default partition.
     * <p/>
     * <p>
     * Note that <code>start</code> must not point to the closing peer, but to the first token being searched.
     * </p>
     *
     * @param start       the start position
     * @param openingPeer the opening peer token (e.g. 'begin')
     * @param closingPeer the closing peer token (e.g. 'end')
     * @return the matching peer character position, or <code>NOT_FOUND</code>
     */
    public int findOpeningPeer(int start, int openingPeer, int closingPeer) {
        assert (start < document.getLength());

        int depth = 1;
        start += 1;
        int token;
        int offset = start;
        while (true) {
            token = previousToken(offset, UNBOUND);
            offset = getPosition();
            if (token == SQLIndentSymbols.TokenEOF) {
                return NOT_FOUND;
            }
            if (isSameToken(token, closingPeer)) {
                depth++;
            } else if (isSameToken(token, openingPeer)) {
                depth--;
            }

            if (depth == 0) {
                if (offset == -1) {
                    return 0;
                }
                return offset;
            }
        }

    }

    /**
     * Returns the position of the closing peer token (forward search). Any scopes introduced by opening peers
     * are skipped. All peers accounted for must reside in the default partition.
     * <p/>
     * <p>Note that <code>start</code> must not point to the opening peer, but to the first
     * token being searched.</p>
     *
     * @param start       the start position
     * @param openingPeer the opening peer character (e.g. 'begin')
     * @param closingPeer the closing peer character (e.g. 'end')
     * @return the matching peer character position, or <code>NOT_FOUND</code>
     */
    public int findClosingPeer(int start, int openingPeer, int closingPeer) {
        assert (start <= document.getLength());

        int depth = 1;
        start += 1;
        int token;
        int offset = start;
        while (true) {

            token = nextToken(offset, document.getLength());
            offset = getPosition();

            if (token == SQLIndentSymbols.TokenEOF) {
                return NOT_FOUND;
            }

            if (isSameToken(token, openingPeer)) {
                depth++;
            } else if (isSameToken(token, closingPeer)) {
                depth--;
            }

            if (depth == 0) {
                return offset;
            }
        }
    }

    public boolean isSameToken(int firstToken, int secondToken) {
        return firstToken == secondToken ||
            (firstToken == TokenBEGIN && secondToken == Tokenbegin) ||
            (firstToken == Tokenbegin && secondToken == TokenBEGIN) ||
            (firstToken == TokenEND && secondToken == Tokenend) ||
            (firstToken == Tokenend && secondToken == TokenEND);
    }

}

