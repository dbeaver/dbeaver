/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2015 Serge Rieder (serge@jkiss.org)
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

import org.eclipse.jface.text.*;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPPreferenceStore;
import org.jkiss.dbeaver.ui.editors.sql.SQLPreferenceConstants;
import org.jkiss.dbeaver.ui.editors.sql.syntax.SQLPartitionScanner;
import org.jkiss.dbeaver.utils.GeneralUtils;

import java.util.HashMap;
import java.util.Map;

public class SQLAutoIndentStrategy extends DefaultIndentLineAutoEditStrategy {
    static final Log log = Log.getLog(SQLAutoIndentStrategy.class);

    private String partitioning;

    private Map<Integer, String> autoCompletionMap = new HashMap<Integer, String>();

    /**
     * Creates a new SQL auto indent strategy for the given document partitioning.
     *
     * @param partitioning the document partitioning
     */
    public SQLAutoIndentStrategy(String partitioning)
    {
        this.partitioning = partitioning;
    }

    private void smartIndentAfterNewLine(IDocument d, DocumentCommand c)
    {
        int docLength = d.getLength();
        if (c.offset == -1 || docLength == 0) {
            return;
        }

        SQLHeuristicScanner scanner = new SQLHeuristicScanner(d);
        SQLIndenter indenter = new SQLIndenter(d, scanner);

        //get previous token
        int previousToken = scanner.previousToken(c.offset - 1, SQLHeuristicScanner.UNBOUND);

        StringBuilder indent;

        StringBuilder beginIndentaion = new StringBuilder();

        if (isSupportedAutoCompletionToken(previousToken)) {
            indent = indenter.computeIndentation(c.offset);

            beginIndentaion.append(indenter.getReferenceIndentation(c.offset));
        } else {
            indent = indenter.getReferenceIndentation(c.offset);
        }

        if (indent == null) {
            indent = new StringBuilder(); //$NON-NLS-1$
        }

        try {
            int p = (c.offset == docLength ? c.offset - 1 : c.offset);
            int line = d.getLineOfOffset(p);

            StringBuilder buf = new StringBuilder(c.text + indent);

            IRegion reg = d.getLineInformation(line);
            int lineEnd = reg.getOffset() + reg.getLength();

            int contentStart = findEndOfWhiteSpace(d, c.offset, lineEnd);
            c.length = Math.max(contentStart - c.offset, 0);

            int start = reg.getOffset();
            ITypedRegion region = TextUtilities.getPartition(d, partitioning, start, true);
            if (SQLPartitionScanner.SQL_MULTILINE_COMMENT.equals(region.getType())) {
                start = d.getLineInformationOfOffset(region.getOffset()).getOffset();
            }

            c.caretOffset = c.offset + buf.length();
            c.shiftsCaret = false;

            if (isSupportedAutoCompletionToken(previousToken) && !isClosed(d, c.offset, previousToken) && getTokenCount(start, c.offset, scanner, previousToken) > 0) {
                buf.append(getLineDelimiter(d));
                buf.append(beginIndentaion);
                buf.append(getAutoCompletionTrail(previousToken));
            }
            c.text = buf.toString();

        }
        catch (BadLocationException e) {
            log.error(e);
        }
    }

    private static String getLineDelimiter(IDocument document)
    {
        try {
            if (document.getNumberOfLines() > 1) {
                return document.getLineDelimiter(0);
            }
        }
        catch (BadLocationException e) {
            log.error(e);
        }

        return GeneralUtils.getDefaultLineSeparator();
    }

    private boolean isLineDelimiter(IDocument document, String text)
    {
        String[] delimiters = document.getLegalLineDelimiters();
        return delimiters != null && TextUtilities.equals(delimiters, text) > -1;
    }

    /*
     * @see org.eclipse.jface.text.IAutoIndentStrategy#customizeDocumentCommand(org.eclipse.jface.text.IDocument,
     *      org.eclipse.jface.text.DocumentCommand)
     */

    @Override
    public void customizeDocumentCommand(IDocument d, DocumentCommand c)
    {

        if (!c.doit) {
            return;
        }

        clearCachedValues();

        if (c.length == 0 && c.text != null && isLineDelimiter(d, c.text)) {
            smartIndentAfterNewLine(d, c);
        }
    }

    private void clearCachedValues()
    {
        autoCompletionMap.clear();
        DBPPreferenceStore preferenceStore = DBeaverCore.getGlobalPreferenceStore();
        boolean closeBeginEnd = preferenceStore.getBoolean(SQLPreferenceConstants.SQLEDITOR_CLOSE_BEGIN_END);
        if (closeBeginEnd) {
            autoCompletionMap.put(SQLIndentSymbols.Tokenbegin, SQLIndentSymbols.beginTrail);
            autoCompletionMap.put(SQLIndentSymbols.TokenBEGIN, SQLIndentSymbols.BEGINTrail);
        }

    }

    private boolean isSupportedAutoCompletionToken(int token)
    {
        return autoCompletionMap.containsKey(token);
    }

    private String getAutoCompletionTrail(int token)
    {
        return autoCompletionMap.get(token);
    }


    /**
     * To count token numbers from start offset to end offset.
     */
    private int getTokenCount(int startOffset, int endOffset, SQLHeuristicScanner scanner,
                              int token)
    {

        int tokenCount = 0;
        while (startOffset < endOffset) {
            int nextToken = scanner.nextToken(startOffset, endOffset);
            int position = scanner.getPosition();
            if (nextToken != SQLIndentSymbols.TokenEOF && scanner.isSameToken(nextToken, token)) {
                tokenCount++;
            }
            startOffset = position;
        }
        return tokenCount;
    }

    private boolean isClosed(IDocument document, int offset, int token)
    {
        //currently only BEGIN/END is supported. Later more typing aids will be added here.
        if (token == SQLIndentSymbols.TokenBEGIN || token == SQLIndentSymbols.Tokenbegin) {
            return getBlockBalance(document, offset) <= 0;
        }
        return false;
    }

    /**
     * Returns the block balance, i.e. zero if the blocks are balanced at <code>offset</code>, a negative number if
     * there are more closing than opening peers, and a positive number if there are more opening than closing peers.
     */
    private static int getBlockBalance(IDocument document, int offset)
    {
        if (offset < 1) {
            return -1;
        }
        if (offset >= document.getLength()) {
            return 1;
        }

        int begin = offset;
        int end = offset;

        SQLHeuristicScanner scanner = new SQLHeuristicScanner(document);

        while (true) {

            begin = scanner.findOpeningPeer(begin, SQLIndentSymbols.TokenBEGIN, SQLIndentSymbols.TokenEND);
            end = scanner.findClosingPeer(end, SQLIndentSymbols.TokenBEGIN, SQLIndentSymbols.TokenEND);
            if (begin == -1 && end == -1) {
                return 0;
            }
            if (begin == -1) {
                return -1;
            }
            if (end == -1) {
                return 1;
            }
        }
    }


}

