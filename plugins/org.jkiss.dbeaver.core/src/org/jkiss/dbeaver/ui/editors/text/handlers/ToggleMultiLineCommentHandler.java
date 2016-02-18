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
package org.jkiss.dbeaver.ui.editors.text.handlers;

import org.eclipse.jface.text.*;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.jkiss.dbeaver.ui.ICommentsSupport;
import org.jkiss.utils.Pair;

public final class ToggleMultiLineCommentHandler extends AbstractCommentHandler {

    @Override
    protected void processAction(ISelectionProvider selectionProvider, ICommentsSupport commentsSupport, IDocument document, ITextSelection selection) throws BadLocationException
    {
        Pair<String,String> comment = commentsSupport.getMultiLineComments();
        if (comment == null) {
            // Multi line comments are not supported
            return;
        }
        int selOffset = selection.getOffset();
        int selLength = selection.getLength();
        DocumentRewriteSession rewriteSession = null;
        if (document instanceof IDocumentExtension4) {
            rewriteSession = ((IDocumentExtension4) document).startRewriteSession(DocumentRewriteSessionType.SEQUENTIAL);
        }

        String lineDelimiter = TextUtilities.getDefaultLineDelimiter(document);
        String selText = selection.getText();
        boolean isMultiLine = selection.getStartLine() != selection.getEndLine() || selText.contains(lineDelimiter);
        String testText = selText.trim();
        if (testText.startsWith(comment.getFirst()) && testText.endsWith(comment.getSecond())) {
            // Remove comments (also remove all extra line feeds)
            int startPos = selText.indexOf(comment.getFirst()) + comment.getFirst().length();
            while (lineDelimiter.indexOf(selText.charAt(startPos)) != -1) {
                startPos++;
            }
            int endPos = selText.lastIndexOf(comment.getSecond());
            while (lineDelimiter.indexOf(selText.charAt(endPos)) != -1) {
                endPos--;
            }
            String newSel = selText.substring(startPos, endPos);
            document.replace(selection.getOffset(), selection.getLength(), newSel);
            selLength -= (selText.length() - newSel.length());

        } else {
            // Add comment
            if (isMultiLine) {
                // Determine - whether we need to insert extra line feeds
                // We use it only if begin and end of selection is on the beginning of line
                int endOffset = selOffset + selLength;
                boolean firstAtBegin = document.getLineOffset(selection.getStartLine()) == selOffset;
                boolean secondAtBegin = document.getLineOffset(document.getLineOfOffset(endOffset)) == endOffset;
                boolean useLineFeeds = firstAtBegin && secondAtBegin;
                document.replace(selection.getOffset() + selection.getLength(), 0, comment.getSecond() + (useLineFeeds ? lineDelimiter : ""));
                document.replace(selection.getOffset(), 0, comment.getFirst() + (useLineFeeds ? lineDelimiter : ""));
                selLength += comment.getFirst().length() + comment.getSecond().length() + (useLineFeeds ? lineDelimiter.length() * 2 : 0);
            } else {
                document.replace(selection.getOffset(), selection.getLength(), comment.getFirst() + selText + comment.getSecond());
                selLength += comment.getFirst().length() + comment.getSecond().length();
            }
        }

        if (rewriteSession != null) {
            ((IDocumentExtension4) document).stopRewriteSession(rewriteSession);
        }
        if (selLength > 0) {
            selectionProvider.setSelection(new TextSelection(selOffset, selLength));
        }

    }
}
