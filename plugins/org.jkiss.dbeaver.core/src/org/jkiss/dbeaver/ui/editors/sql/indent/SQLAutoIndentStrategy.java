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
import org.jkiss.dbeaver.model.DBPKeywordType;
import org.jkiss.dbeaver.model.DBPPreferenceStore;
import org.jkiss.dbeaver.model.sql.SQLSyntaxManager;
import org.jkiss.dbeaver.ui.editors.sql.SQLPreferenceConstants;
import org.jkiss.dbeaver.ui.editors.sql.syntax.SQLPartitionScanner;
import org.jkiss.dbeaver.utils.GeneralUtils;

import java.util.HashMap;
import java.util.Map;

public class SQLAutoIndentStrategy extends DefaultIndentLineAutoEditStrategy {
    private static final Log log = Log.getLog(SQLAutoIndentStrategy.class);
    private static final int MINIMUM_SOUCE_CODE_LENGTH = 10;

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
    public void customizeDocumentCommand(IDocument document, DocumentCommand command)
    {
        if (!command.doit) {
            return;
        }
        if (command.offset < 0) {
            return;
        }

        if (command.text != null && command.text.length() > MINIMUM_SOUCE_CODE_LENGTH) {
            if (syntaxManager.getPreferenceStore().getBoolean(SQLPreferenceConstants.SQL_FORMAT_EXTRACT_FROM_SOURCE)) {
                transformSourceCode(document, command);
            }
        } else if (command.length == 0 && command.text != null) {
            final boolean lineDelimiter = isLineDelimiter(document, command.text);
            try {
                if (command.offset > 1 && Character.isJavaIdentifierPart(document.getChar(command.offset - 1)) &&
                    (lineDelimiter || (command.text.length() == 1 && !Character.isJavaIdentifierPart(command.text.charAt(0)))) &&
                    syntaxManager.getPreferenceStore().getBoolean(SQLPreferenceConstants.SQL_FORMAT_KEYWORD_CASE_AUTO))
                {
                    updateKeywordCase(document, command);
                }
            } catch (BadLocationException e) {
                log.debug(e);
            }
            if (lineDelimiter) {
                smartIndentAfterNewLine(document, command);
            }
        }
    }

    private boolean transformSourceCode(IDocument document, DocumentCommand command) {
        String sourceCode = command.text;
        int quoteStart = -1, quoteEnd = -1;
        for (int i = 0; i < sourceCode.length(); i++) {
            final char ch = sourceCode.charAt(i);
            if (ch == '"') {
                quoteStart = i;
                break;
            } else if (Character.isUnicodeIdentifierPart(ch)) {
                // Letter before quote
                return false;
            }
        }
        for (int i = sourceCode.length() - 1; i >= 0; i--) {
            final char ch = sourceCode.charAt(i);
            if (ch == '"') {
                quoteEnd = i;
                break;
            } else if (Character.isUnicodeIdentifierPart(ch)) {
                // Letter before quote
                return false;
            }
        }
        if (quoteStart == -1 || quoteEnd == -1) {
            return false;
        }
        StringBuilder result = new StringBuilder(sourceCode.length());
        char prevChar = (char)-1;
        boolean inString = true;
        for (int i = quoteStart + 1; i < quoteEnd; i++) {
            final char ch = sourceCode.charAt(i);
            if (prevChar == '\\' && inString) {
                switch (ch) {
                    case 'n': result.append("\n"); break;
                    case 'r': result.append("\r"); break;
                    case 't': result.append("\t"); break;
                    default: result.append(ch); break;
                }
            } else {
                switch (ch) {
                    case '"':
                        inString = !inString;
                        break;
                    case '\\':
                        break;
                    default:
                        if (inString) {
                            result.append(ch);
                        }
                }
            }
            prevChar = ch;
        }

        try {
            document.replace(command.offset, command.length, command.text);
            document.replace(command.offset, command.text.length(), result.toString());
        } catch (Exception e) {
            log.warn(e);
        }

        command.caretOffset = command.offset + result.length();
        command.text = null;
        command.length = 0;
        command.doit = false;

        return true;
    }

    private boolean updateKeywordCase(final IDocument document, DocumentCommand command) throws BadLocationException {
        // Whitespace - check for keyword
        final int startPos, endPos;
        int pos = command.offset - 1;
        while (pos >= 0 && Character.isWhitespace(document.getChar(pos))) {
            pos--;
        }
        endPos = pos + 1;
        while (pos >= 0 && Character.isJavaIdentifierPart(document.getChar(pos))) {
            pos--;
        }
        startPos = pos + 1;
        final String keyword = document.get(startPos, endPos - startPos);
        if (syntaxManager.getDialect().getKeywordType(keyword) == DBPKeywordType.KEYWORD) {
            final String fixedKeyword = syntaxManager.getKeywordCase().transform(keyword);
            if (!fixedKeyword.equals(keyword)) {
                command.addCommand(startPos, endPos - startPos, fixedKeyword, null);
                command.doit = false;
                return true;
            }
        }
        return false;
    }

    private void smartIndentAfterNewLine(IDocument document, DocumentCommand command)
    {
        clearCachedValues();

        int docLength = document.getLength();
        if (docLength == 0) {
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
            if (SQLPartitionScanner.CONTENT_TYPE_SQL_MULTILINE_COMMENT.equals(region.getType())) {
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

