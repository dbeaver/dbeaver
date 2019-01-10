/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2019 Serge Rider (serge@jkiss.org)
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

import org.eclipse.jface.text.*;
import org.eclipse.ui.texteditor.AbstractDecoratedTextEditorPreferenceConstants;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.editors.sql.SQLPreferenceConstants;
import org.jkiss.dbeaver.utils.GeneralUtils;

import java.text.BreakIterator;


/**
 * Auto indent strategy for SQL multi-line comments
 */
public class SQLCommentAutoIndentStrategy extends DefaultIndentLineAutoEditStrategy {

    private String partitioning;

    /**
     * Creates a new SQL multi-line comment auto indent strategy for the given document partitioning.
     *
     * @param partitioning the document partitioning
     */
    public SQLCommentAutoIndentStrategy(String partitioning)
    {
        this.partitioning = partitioning;
    }

    private static String getLineDelimiter(IDocument document)
    {
        try {
            if (document.getNumberOfLines() > 1) {
                return document.getLineDelimiter(0);
            }
        }
        catch (BadLocationException e) {
//            _log.error(EditorMessages.error_badLocationException, e);
        }

        return GeneralUtils.getDefaultLineSeparator();
    }

    /**
     * Copies the indentation of the previous line and add a star. If the SQL multi-line comment just started on this
     * line add standard method tags and close the comment.
     *
     * @param d the document to work on
     * @param c the command to deal with
     */
    private void commentIndentAfterNewLine(IDocument d, DocumentCommand c)
    {

        if (c.offset == -1 || d.getLength() == 0) {
            return;
        }

        try {
            // find start of line
            int p = (c.offset == d.getLength() ? c.offset - 1 : c.offset);
            IRegion info = d.getLineInformationOfOffset(p);
            int start = info.getOffset();

            // find white spaces
            int end = findEndOfWhiteSpace(d, start, c.offset);

            StringBuilder buf = new StringBuilder(c.text);
            if (end >= start) {
                String indentation = commentExtractLinePrefix(d, d.getLineOfOffset(c.offset));
                buf.append(indentation);
                if (end < c.offset) {
                    //If it is the sinle line comment '//', don't append '*'.
                    if (d.getChar(end) == '/' && d.getChar(end + 1) != '/') {
                        // SQL multi-line comment started on this line
                        buf.append(" * "); //$NON-NLS-1$

                        if (DBWorkbench.getPlatform().getPreferenceStore().getBoolean(
                            SQLPreferenceConstants.SQLEDITOR_CLOSE_COMMENTS)
                            && isNewComment(d, c.offset, partitioning)) {
                            String lineDelimiter = getLineDelimiter(d);

                            String endTag = lineDelimiter + indentation + " */"; //$NON-NLS-1$
                            d.replace(c.offset, 0, endTag); //$NON-NLS-1$

                        }

                    }
                }
            }

            c.text = buf.toString();

        }
        catch (BadLocationException excp) {
            // stop work
        }
    }

    protected void commentIndentForCommentEnd(IDocument d, DocumentCommand c)
    {
        if (c.offset < 2 || d.getLength() == 0) {
            return;
        }
        try {
            if ("* ".equals(d.get(c.offset - 2, 2))) {
                //$NON-NLS-1$
                // modify document command
                c.length++;
                c.offset--;
            }
        }
        catch (BadLocationException excp) {
            // stop work
        }
    }

    /**
     * Guesses if the command operates within a newly created SQL multi-line comment or not. If in doubt, it will assume
     * that the SQL multi-line comment is new.
     */
    private static boolean isNewComment(IDocument document, int commandOffset, String partitioning)
    {

        try {
            int lineIndex = document.getLineOfOffset(commandOffset) + 1;
            if (lineIndex >= document.getNumberOfLines()) {
                return true;
            }

            IRegion line = document.getLineInformation(lineIndex);
            ITypedRegion partition = TextUtilities.getPartition(document, partitioning, commandOffset, false);
            int partitionEnd = partition.getOffset() + partition.getLength();
            if (line.getOffset() >= partitionEnd) {
                return false;
            }

            if (document.getLength() == partitionEnd) {
                return true; // partition goes to end of document - probably a new comment
            }

            String comment = document.get(partition.getOffset(), partition.getLength());
            return comment.indexOf("/*", 2) != -1;

        }
        catch (BadLocationException e) {
            return false;
        }
    }

    /*
     * @see IAutoIndentStrategy#customizeDocumentCommand
     */
    @Override
    public void customizeDocumentCommand(IDocument document, DocumentCommand command)
    {
        try {

            if (command.text != null && command.length == 0) {
                String[] lineDelimiters = document.getLegalLineDelimiters();
                int index = TextUtilities.endsWith(lineDelimiters, command.text);
                if (index > -1) {
                    // ends with line delimiter
                    if (lineDelimiters[index].equals(command.text)) {
                        // just the line delimiter
                        commentIndentAfterNewLine(document, command);
                    }
                    return;
                }
            }

            if (command.text != null && command.text.equals("/")) {
                //$NON-NLS-1$
                commentIndentForCommentEnd(document, command);
                return;
            }

            ITypedRegion partition = TextUtilities.getPartition(document, partitioning, command.offset, true);
            int partitionStart = partition.getOffset();
            int partitionEnd = partition.getLength() + partitionStart;

            String text = command.text;
            int offset = command.offset;
            int length = command.length;

/*
            // partition change
            final int PREFIX_LENGTH = SQLConstants.ML_COMMENT_START.length();
            final int POSTFIX_LENGTH = SQLConstants.ML_COMMENT_END.length(); //$NON-NLS-1$
            if ((offset < partitionStart + PREFIX_LENGTH || offset + length > partitionEnd - POSTFIX_LENGTH)
                || text != null && text.length() >= 2
                && ((text.contains(SQLConstants.ML_COMMENT_END)) || (document.getChar(offset) == '*' && text.startsWith(
                "/")))) //$NON-NLS-1$ //$NON-NLS-2$
            {
                return;
            }
*/

        }
        catch (BadLocationException e) {
//            _log.error(EditorMessages.error_badLocationException, e);
        }
    }

    private void flushCommand(IDocument document, DocumentCommand command)
        throws BadLocationException
    {

        if (!command.doit) {
            return;
        }

        document.replace(command.offset, command.length, command.text);

        command.doit = false;
        if (command.text != null) {
            command.offset += command.text.length();
        }
        command.length = 0;
        command.text = null;
    }

    protected void commentWrapParagraphOnInsert(IDocument document, DocumentCommand command)
        throws BadLocationException
    {

        int line = document.getLineOfOffset(command.offset);
        IRegion region = document.getLineInformation(line);
        int lineOffset = region.getOffset();
        int lineLength = region.getLength();

        String lineContents = document.get(lineOffset, lineLength);
        StringBuilder buffer = new StringBuilder(lineContents);
        int start = command.offset - lineOffset;
        int end = command.length + start;
        buffer.replace(start, end, command.text);

        // handle whitespace
        if (command.text != null && command.text.length() != 0 && command.text.trim().length() == 0) {

            String endOfLine = document.get(command.offset, lineOffset + lineLength - command.offset);

            // end of line
            if (endOfLine.length() == 0) {
                // move caret to next line
                flushCommand(document, command);

                if (isLineTooShort(document, line)) {
                    int[] caretOffset =
                        {
                            command.offset
                        };
                    commentWrapParagraphFromLine(document, line, caretOffset, false);
                    command.offset = caretOffset[0];
                    return;
                }

                // move caret to next line if possible
                if (line < document.getNumberOfLines() - 1 && isCommentLine(document, line + 1)) {
                    String lineDelimiter = document.getLineDelimiter(line);
                    String nextLinePrefix = commentExtractLinePrefix(document, line + 1);
                    command.offset += lineDelimiter.length() + nextLinePrefix.length();
                }
                return;

                // inside whitespace at end of line
            } else if (endOfLine.trim().length() == 0) {
                // simply insert space
                return;
            }
        }

        // change in prefix region
        String prefix = commentExtractLinePrefix(document, line);
        boolean wrapAlways = command.offset >= lineOffset && command.offset <= lineOffset + prefix.length();

        // must insert the text now because it may include whitepace
        flushCommand(document, command);

        if (wrapAlways || calculateDisplayedWidth(buffer.toString()) > getMargin() || isLineTooShort(document, line)) {
            int[] caretOffset =
                {
                    command.offset
                };
            commentWrapParagraphFromLine(document, line, caretOffset, wrapAlways);

            if (!wrapAlways) {
                command.offset = caretOffset[0];
            }
        }
    }

    /**
     * Method commentWrapParagraphFromLine.
     */
    private void commentWrapParagraphFromLine(IDocument document, int line, int[] caretOffset, boolean always)
        throws BadLocationException
    {

        String indent = commentExtractLinePrefix(document, line);
        if (!always) {
            if (!indent.trim().startsWith("*")) //$NON-NLS-1$
            {
                return;
            }

            if (indent.trim().startsWith("*/")) //$NON-NLS-1$
            {
                return;
            }

            if (!isLineTooLong(document, line) && !isLineTooShort(document, line)) {
                return;
            }
        }

        boolean caretRelativeToParagraphOffset = false;
        int caret = caretOffset[0];

        int caretLine = document.getLineOfOffset(caret);
        int lineOffset = document.getLineOffset(line);
        int paragraphOffset = lineOffset + indent.length();
        if (paragraphOffset < caret) {
            caret -= paragraphOffset;
            caretRelativeToParagraphOffset = true;
        } else {
            caret -= lineOffset;
        }

        StringBuilder buffer = new StringBuilder();
        int currentLine = line;
        while (line == currentLine || isCommentLine(document, currentLine)) {

            if (buffer.length() != 0 && !Character.isWhitespace(buffer.charAt(buffer.length() - 1))) {
                buffer.append(' ');
                if (currentLine <= caretLine) {
                    // in this case caretRelativeToParagraphOffset is always true
                    ++caret;
                }
            }

            String string = getLineContents(document, currentLine);
            buffer.append(string);
            currentLine++;
        }
        String paragraph = buffer.toString();

        if (paragraph.trim().length() == 0) {
            return;
        }

        caretOffset[0] = caretRelativeToParagraphOffset ? caret : 0;
        String delimiter = document.getLineDelimiter(0);
        String wrapped = formatParagraph(paragraph, caretOffset, indent, delimiter, getMargin());

        int beginning = document.getLineOffset(line);
        int end = document.getLineOffset(currentLine);
        document.replace(beginning, end - beginning, wrapped);

        caretOffset[0] = caretRelativeToParagraphOffset ? caretOffset[0] + beginning : caret + beginning;
    }

    /**
     * Line break iterator to handle whitespaces as first class citizens.
     */
    private static class LineBreakIterator {

        private final String _string;
        private final BreakIterator _iterator = BreakIterator.getLineInstance();

        private int _start;
        private int _end;
        private int _bufferedEnd;

        public LineBreakIterator(String string)
        {
            _string = string;
            _iterator.setText(string);
        }

        public int first()
        {
            _bufferedEnd = -1;
            _start = _iterator.first();
            return _start;
        }

        public int next()
        {

            if (_bufferedEnd != -1) {
                _start = _end;
                _end = _bufferedEnd;
                _bufferedEnd = -1;
                return _end;
            }

            _start = _end;
            _end = _iterator.next();

            if (_end == BreakIterator.DONE) {
                return _end;
            }

            final String string = _string.substring(_start, _end);

            // whitespace
            if (string.trim().length() == 0) {
                return _end;
            }

            final String word = string.trim();
            if (word.length() == string.length()) {
                return _end;
            }

            // suspected whitespace
            _bufferedEnd = _end;
            return _start + word.length();
        }
    }

    /**
     * Formats a paragraph, using break iterator.
     *
     * @param offset an offset within the paragraph, which will be updated with respect to formatting.
     */
    private static String formatParagraph(String paragraph, int[] offset, String prefix, String lineDelimiter,
                                          int margin)
    {

        LineBreakIterator iterator = new LineBreakIterator(paragraph);

        StringBuilder paragraphBuffer = new StringBuilder();
        StringBuilder lineBuffer = new StringBuilder();
        StringBuilder whiteSpaceBuffer = new StringBuilder();

        int index = offset[0];
        int indexBuffer = -1;

        // line delimiter could be null
        if (lineDelimiter == null) {
            lineDelimiter = ""; //$NON-NLS-1$
        }

        for (int start = iterator.first(), end = iterator.next(); end != BreakIterator.DONE; start = end, end = iterator
            .next()) {

            String word = paragraph.substring(start, end);

            // word is whitespace
            if (word.trim().length() == 0) {
                whiteSpaceBuffer.append(word);

                // first word of line is always appended
            } else if (lineBuffer.length() == 0) {
                lineBuffer.append(prefix);
                lineBuffer.append(whiteSpaceBuffer.toString());
                lineBuffer.append(word);

            } else {
                String line = lineBuffer.toString() + whiteSpaceBuffer.toString() + word;

                // margin exceeded
                if (calculateDisplayedWidth(line) > margin) {
                    // flush line buffer and wrap paragraph
                    paragraphBuffer.append(lineBuffer.toString());
                    paragraphBuffer.append(lineDelimiter);
                    lineBuffer.setLength(0);
                    lineBuffer.append(prefix);
                    lineBuffer.append(word);

                    // flush index buffer
                    if (indexBuffer != -1) {
                        offset[0] = indexBuffer;
                        // correct for caret in whitespace at the end of line
                        if (whiteSpaceBuffer.length() != 0 && index < start
                            && index >= start - whiteSpaceBuffer.length()) {
                            offset[0] -= (index - (start - whiteSpaceBuffer.length()));
                        }
                        indexBuffer = -1;
                    }

                    whiteSpaceBuffer.setLength(0);

                    // margin not exceeded
                } else {
                    lineBuffer.append(whiteSpaceBuffer.toString());
                    lineBuffer.append(word);
                    whiteSpaceBuffer.setLength(0);
                }
            }

            if (index >= start && index < end) {
                indexBuffer = paragraphBuffer.length() + lineBuffer.length() + (index - start);
                if (word.trim().length() != 0) {
                    indexBuffer -= word.length();
                }
            }
        }

        // flush line buffer
        paragraphBuffer.append(lineBuffer.toString());
        paragraphBuffer.append(lineDelimiter);

        // flush index buffer
        if (indexBuffer != -1) {
            offset[0] = indexBuffer;
        }

        // last position is not returned by break iterator
        else if (offset[0] == paragraph.length()) {
            offset[0] = paragraphBuffer.length() - lineDelimiter.length();
        }

        return paragraphBuffer.toString();
    }

    private static DBPPreferenceStore getPreferenceStore()
    {
        return DBWorkbench.getPlatform().getPreferenceStore();
    }

    /**
     * Returns the displayed width of a string, taking in account the displayed tab width. The result can be compared
     * against the print margin.
     */
    private static int calculateDisplayedWidth(String string)
    {

        final int tabWidth = getPreferenceStore().getInt(
            AbstractDecoratedTextEditorPreferenceConstants.EDITOR_TAB_WIDTH);

        int column = 0;
        for (int i = 0; i < string.length(); i++) {
            if ('\t' == string.charAt(i)) {
                column += tabWidth - (column % tabWidth);
            } else {
                column++;
            }
        }
        return column;
    }

    private String commentExtractLinePrefix(IDocument d, int line)
        throws BadLocationException
    {

        IRegion region = d.getLineInformation(line);
        int lineOffset = region.getOffset();
        int index = findEndOfWhiteSpace(d, lineOffset, lineOffset + d.getLineLength(line));
        if (d.getChar(index) == '*') {
            index++;
            if (index != lineOffset + region.getLength() && d.getChar(index) == ' ') {
                index++;
            }
        }
        return d.get(lineOffset, index - lineOffset);
    }

    private String getLineContents(IDocument d, int line)
        throws BadLocationException
    {
        int offset = d.getLineOffset(line);
        int length = d.getLineLength(line);
        String lineDelimiter = d.getLineDelimiter(line);
        if (lineDelimiter != null) {
            length = length - lineDelimiter.length();
        }
        String lineContents = d.get(offset, length);
        int trim = commentExtractLinePrefix(d, line).length();
        return lineContents.substring(trim);
    }

    private static String getLine(IDocument document, int line)
        throws BadLocationException
    {
        IRegion region = document.getLineInformation(line);
        return document.get(region.getOffset(), region.getLength());
    }

    /**
     * Returns <code>true</code> if the comment line is too short, <code>false</code> otherwise.
     */
    private boolean isLineTooShort(IDocument document, int line)
        throws BadLocationException
    {

        if (!isCommentLine(document, line + 1)) {
            return false;
        }

        String nextLine = getLineContents(document, line + 1);
        return nextLine.trim().length() != 0;

    }

    /**
     * Returns <code>true</code> if the line is too long, <code>false</code> otherwise.
     */
    private boolean isLineTooLong(IDocument document, int line)
        throws BadLocationException
    {
        String lineContents = getLine(document, line);
        return calculateDisplayedWidth(lineContents) > getMargin();
    }

    private static int getMargin()
    {
        return getPreferenceStore().getInt(AbstractDecoratedTextEditorPreferenceConstants.EDITOR_PRINT_MARGIN_COLUMN);
    }

    /**
     * returns true if the specified line is part of a paragraph and should be merged with the previous line.
     */
    private boolean isCommentLine(IDocument document, int line)
        throws BadLocationException
    {

        if (document.getNumberOfLines() < line)
            return false;

        int offset = document.getLineOffset(line);
        int length = document.getLineLength(line);
        int firstChar = findEndOfWhiteSpace(document, offset, offset + length);
        length -= firstChar - offset;
        String lineContents = document.get(firstChar, length);

        String prefix = lineContents.trim();
        if (!prefix.startsWith("*") || prefix.startsWith("*/")) //$NON-NLS-1$ //$NON-NLS-2$
        {
            return false;
        }

        //lineContents = lineContents.substring(1).trim().toLowerCase();

        return true;
    }

    protected void commentHandleBackspaceDelete(IDocument document, DocumentCommand c)
    {

        try {
            String text = document.get(c.offset, c.length);
            int line = document.getLineOfOffset(c.offset);
            int lineOffset = document.getLineOffset(line);

            // erase line delimiter
            String lineDelimiter = document.getLineDelimiter(line);
            if (lineDelimiter != null && lineDelimiter.equals(text)) {

                String prefix = commentExtractLinePrefix(document, line + 1);

                // strip prefix if any
                if (prefix.length() > 0) {
                    int length = document.getLineDelimiter(line).length() + prefix.length();
                    document.replace(c.offset, length, null);

                    c.doit = false;
                    c.length = 0;
                    return;
                }

                // backspace: beginning of a SQL multi-line comment line
            } else if (document.getChar(c.offset - 1) == '*'
                && commentExtractLinePrefix(document, line).length() - 1 >= c.offset - lineOffset) {

                lineDelimiter = document.getLineDelimiter(line - 1);
                String prefix = commentExtractLinePrefix(document, line);
                int length = (lineDelimiter != null ? lineDelimiter.length() : 0) + prefix.length();
                document.replace(c.offset - length + 1, length, null);

                c.doit = false;
                c.offset -= length - 1;
                c.length = 0;
                return;

            } else {
                document.replace(c.offset, c.length, null);
                c.doit = false;
                c.length = 0;
            }

        }
        catch (BadLocationException e) {
//            _log.error(EditorMessages.error_badLocationException, e);
        }

        try {
            int line = document.getLineOfOffset(c.offset);
            int lineOffset = document.getLineOffset(line);
            String prefix = commentExtractLinePrefix(document, line);
            boolean always = c.offset > lineOffset && c.offset <= lineOffset + prefix.length();
            int[] caretOffset =
                {
                    c.offset
                };
            commentWrapParagraphFromLine(document, document.getLineOfOffset(c.offset), caretOffset, always);
            c.offset = caretOffset[0];

        }
        catch (BadLocationException e) {
//            _log.error(EditorMessages.error_badLocationException, e);
        }
    }

}
