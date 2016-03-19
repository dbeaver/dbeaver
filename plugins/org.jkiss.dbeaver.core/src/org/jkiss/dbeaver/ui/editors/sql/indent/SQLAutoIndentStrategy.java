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

import org.eclipse.jface.text.*;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.model.DBPPreferenceStore;
import org.jkiss.dbeaver.model.sql.SQLSyntaxManager;
import org.jkiss.dbeaver.ui.editors.sql.SQLPreferenceConstants;
import org.jkiss.dbeaver.ui.editors.sql.syntax.SQLPartitionScanner;
import org.jkiss.dbeaver.utils.GeneralUtils;

import java.util.HashMap;
import java.util.Map;

public class SQLAutoIndentStrategy extends DefaultIndentLineAutoEditStrategy {
    static final Log log = Log.getLog(SQLAutoIndentStrategy.class);

    private String partitioning;
    private SQLSyntaxManager syntaxManager;

    private Map<Integer, String> autoCompletionMap = new HashMap<>();
    private String[] delimiters;

    /**
     * Creates a new SQL auto indent strategy for the given document partitioning.
     */
    public SQLAutoIndentStrategy(String partitioning, SQLSyntaxManager syntaxManager)
    {
        this.partitioning = partitioning;
        this.syntaxManager = syntaxManager;
    }


    @Override
    public void customizeDocumentCommand(IDocument d, DocumentCommand c)
    {
        if (!c.doit) {
            return;
        }

        if (c.length == 0 && c.text != null) {
            final boolean lineDelimiter = isLineDelimiter(d, c.text);
            if (lineDelimiter || c.text.length() == 1 && Character.isWhitespace(c.text.charAt(0)) &&
                syntaxManager.getPreferenceStore().getBoolean(SQLPreferenceConstants.SQL_FORMAT_KEYWORD_CASE_AUTO))
            {
                // Whitespace - check for keyword
            }
            if (lineDelimiter) {
                smartIndentAfterNewLine(d, c);
            }
        }
    }

    private void smartIndentAfterNewLine(IDocument document, DocumentCommand command)
    {
        clearCachedValues();

        int docLength = document.getLength();
        if (command.offset == -1 || docLength == 0) {
            return;
        }
        SQLHeuristicScanner scanner = new SQLHeuristicScanner(document, syntaxManager);
        SQLIndenter indenter = new SQLIndenter(document, scanner);

        //get previous token
        int previousToken = scanner.previousToken(command.offset - 1, SQLHeuristicScanner.UNBOUND);

        StringBuilder indent;

        StringBuilder beginIndentaion = new StringBuilder();

        if (isSupportedAutoCompletionToken(previousToken)) {
            indent = indenter.computeIndentation(command.offset);

            beginIndentaion.append(indenter.getReferenceIndentation(command.offset));
        } else {
            indent = indenter.getReferenceIndentation(command.offset);
        }

        if (indent == null) {
            indent = new StringBuilder(); //$NON-NLS-1$
        }

        try {
            int p = (command.offset == docLength ? command.offset - 1 : command.offset);
            int line = document.getLineOfOffset(p);

            StringBuilder buf = new StringBuilder(command.text + indent);

            IRegion reg = document.getLineInformation(line);
            int lineEnd = reg.getOffset() + reg.getLength();

            int contentStart = findEndOfWhiteSpace(document, command.offset, lineEnd);
            command.length = Math.max(contentStart - command.offset, 0);

            int start = reg.getOffset();
            ITypedRegion region = TextUtilities.getPartition(document, partitioning, start, true);
            if (SQLPartitionScanner.SQL_MULTILINE_COMMENT.equals(region.getType())) {
                start = document.getLineInformationOfOffset(region.getOffset()).getOffset();
            }

            command.caretOffset = command.offset + buf.length();
            command.shiftsCaret = false;

            if (isSupportedAutoCompletionToken(previousToken) && !isClosed(document, command.offset, previousToken) && getTokenCount(start, command.offset, scanner, previousToken) > 0) {
                buf.append(getLineDelimiter(document));
                buf.append(beginIndentaion);
                buf.append(getAutoCompletionTrail(previousToken));
            }
            command.text = buf.toString();

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
        if (delimiters == null) {
            delimiters = document.getLegalLineDelimiters();
        }
        return delimiters != null && TextUtilities.equals(delimiters, text) > -1;
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
    private int getBlockBalance(IDocument document, int offset)
    {
        if (offset < 1) {
            return -1;
        }
        if (offset >= document.getLength()) {
            return 1;
        }

        int begin = offset;
        int end = offset;

        SQLHeuristicScanner scanner = new SQLHeuristicScanner(document, syntaxManager);

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

