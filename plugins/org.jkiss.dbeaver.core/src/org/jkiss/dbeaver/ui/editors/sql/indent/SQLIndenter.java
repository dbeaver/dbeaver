/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2016 Serge Rieder (serge@jkiss.org)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (version 2)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package org.jkiss.dbeaver.ui.editors.sql.indent;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;


public class SQLIndenter {
    /**
     * The document being scanned.
     */
    private IDocument document;
    /**
     * The indentation accumulated by <code>findPreviousIndenationUnit</code>.
     */
    private int indent;
    /**
     * The stateful scanposition for the indentation methods.
     */
    private int position;
    /**
     * The most recent token.
     */
    private int token;
    /**
     * The line of <code>fPosition</code>.
     */
    private int line;
    /**
     * The scanner we will use to scan the document. It has to be installed on the same document as the one we get.
     */
    private SQLHeuristicScanner scanner;

    /**
     * Creates a new instance.
     *
     * @param document the document to scan
     * @param scanner  the {@link SQLHeuristicScanner}to be used for scanning the document. It must be installed on the
     *                 same <code>IDocument</code>.
     */
    public SQLIndenter(IDocument document, SQLHeuristicScanner scanner)
    {
        assert (document != null);
        assert (scanner != null);
        this.document = document;
        this.scanner = scanner;
    }

    /**
     * Computes the indentation at the reference point of <code>position</code>.
     *
     * @param offset the offset in the document
     * @return a String which reflects the indentation at the line in which the reference position to
     *         <code>offset</code> resides, or <code>null</code> if it cannot be determined
     */
    public StringBuilder getReferenceIndentation(int offset)
    {
        int unit;
        unit = findReferencePosition(offset);
        //   if we were unable to find anything, return null
        if (unit == SQLHeuristicScanner.NOT_FOUND) {
            return null;
        }

        return getLeadingWhitespace(unit);
    }

    /**
     * Computes the indentation at <code>offset</code>.
     *
     * @param offset the offset in the document
     * @return a String which reflects the correct indentation for the line in which offset resides, or
     *         <code>null</code> if it cannot be determined
     */
    public StringBuilder computeIndentation(int offset)
    {
        return computeIndentation(offset, false);

    }

    /**
     * Computes the indentation at <code>offset</code>.
     *
     * @param offset        the offset in the document
     * @param assumeOpening <code>true</code> if an opening statement should be assumed
     * @return a String which reflects the correct indentation for the line in which offset resides, or
     *         <code>null</code> if it cannot be determined
     */
    public StringBuilder computeIndentation(int offset, boolean assumeOpening)
    {

        indent = 1;

        // add additional indent
        StringBuilder indent = createIndent(this.indent);

        if (this.indent < 0) {
            unindent(indent);
        }

        if (indent == null) {
            return null;
        }

        //adding offset, after adding indent to keep consistency on whitespace of the last line.
        indent.append(getReferenceIndentation(offset));

        return indent;
    }

    /**
     * Returns the indentation of the line at <code>offset</code> as a <code>StringBuilder</code>. If the offset is
     * not valid, the empty string is returned.
     *
     * @param offset the offset in the document
     * @return the indentation (leading whitespace) of the line in which <code>offset</code> is located
     */
    private StringBuilder getLeadingWhitespace(int offset)
    {
        StringBuilder indent = new StringBuilder();
        try {
            IRegion line = document.getLineInformationOfOffset(offset);
            int lineOffset = line.getOffset();
            int nonWS = scanner.findNonWhitespaceForwardInAnyPartition(lineOffset, lineOffset + line.getLength());
            indent.append(document.get(lineOffset, nonWS - lineOffset));
            return indent;
        }
        catch (BadLocationException e) {
//            _log.debug(EditorMessages.error_badLocationException, e);
            return indent;
        }
    }

    /**
     * Reduces indentation in <code>indent</code> by one indentation unit.
     *
     * @param indent the indentation to be modified
     */
    private void unindent(StringBuilder indent)
    {
        CharSequence oneIndent = createIndent();
        int i = indent.lastIndexOf(oneIndent.toString()); //$NON-NLS-1$
        if (i != -1) {
            indent.delete(i, i + oneIndent.length());
        }
    }

    /**
     * Creates a string that represents the given number of indents (can be spaces or tabs..)
     *
     * @param indent the requested indentation level.
     * @return the indentation specified by <code>indent</code>
     */
    private StringBuilder createIndent(int indent)
    {
        StringBuilder oneIndent = createIndent();

        StringBuilder ret = new StringBuilder();
        while (indent-- > 0) {
            ret.append(oneIndent);
        }

        return ret;
    }

    /**
     * Creates a string that represents one indent (can be spaces or tabs..)
     *
     * @return one indentation
     */
    private StringBuilder createIndent()
    {
        // get a sensible default when running without the infrastructure for testing
        StringBuilder oneIndent = new StringBuilder();
        oneIndent.append('\t');
        return oneIndent;
    }

    /**
     * Returns the reference position regarding to indentation for <code>offset</code>, or <code>NOT_FOUND</code>.
     * This method calls {@link #findReferencePosition(int, int) findReferencePosition(offset, nextChar)}where
     * <code>nextChar</code> is the next character after <code>offset</code>.
     *
     * @param offset the offset for which the reference is computed
     * @return the reference statement relative to which <code>offset</code> should be indented, or
     *         {@link SQLHeuristicScanner#NOT_FOUND}
     */
    public int findReferencePosition(int offset)
    {
        indent = 0; // the indentation modification
        position = offset;
        nextToken();
        return skipToPreviousListItemOrListStart();
    }

    /**
     * Returns the reference position for a list element. The algorithm tries to match any previous indentation on the
     * same list. If there is none, the reference position returned is determined depending on the type of list: The
     * indentation will either match the list scope introducer (e.g. for method declarations), so called deep indents,
     * or simply increase the indentation by a number of standard indents. See also
     * {@link #handleScopeIntroduction(int)}.
     *
     * @return the reference position for a list item: either a previous list item that has its own indentation, or the
     *         list introduction start.
     */
    private int skipToPreviousListItemOrListStart()
    {
        int startLine = line;
        int startPosition = position;
        while (true) {
            nextToken();

            // if any line item comes with its own indentation, adapt to it
            if (line < startLine) {
                try {
                    int lineOffset = document.getLineOffset(startLine);
                    int bound = Math.min(document.getLength(), startPosition + 1);
                    int align = scanner.findNonWhitespaceForwardInAnyPartition(lineOffset, bound);
                }
                catch (BadLocationException e) {
//                    _log.debug(EditorMessages.error_badLocationException, e);
                    // ignore and return just the position
                }
                return startPosition;
            }

            switch (token) {
                case SQLIndentSymbols.TokenEOF:
                    return 0;

            }
        }
    }

    /**
     * Reads the next token in backward direction from the heuristic scanner and sets the fields
     * <code>fToken, fPreviousPosition</code> and <code>fPosition</code> accordingly.
     */
    private void nextToken()
    {
        nextToken(position);
    }

    public void nextToken(int start)
    {
        token = scanner.previousToken(start - 1, SQLHeuristicScanner.UNBOUND);
        position = scanner.getPosition() + 1;
        try {
            line = document.getLineOfOffset(position);
        }
        catch (BadLocationException e) {
            line = -1;
        }
    }

}
