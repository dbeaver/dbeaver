/*
 * Copyright (C) 2010-2012 Serge Rieder
 * serge@jkiss.org
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package org.jkiss.dbeaver.ui.editors.text.handlers;

import org.eclipse.jface.text.*;
import org.jkiss.dbeaver.model.DBPCommentsManager;
import org.jkiss.dbeaver.ui.editors.text.BaseTextEditor;
import org.jkiss.utils.Pair;

public final class ToggleMultiLineCommentHandler extends AbstractCommentHandler {

    @Override
    protected void processAction(BaseTextEditor textEditor, IDocument document, ITextSelection selection) throws BadLocationException
    {
        DBPCommentsManager commentsSupport = textEditor.getCommentsSupport();
        if (commentsSupport == null) {
            return;
        }
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
        String selText = document.get(selOffset, selLength);
        boolean isMultiLine = selection.getStartLine() != selection.getEndLine() || selText.contains(lineDelimiter);
        if (selText.trim().startsWith(comment.getFirst()) && selText.endsWith(comment.getSecond())) {
            // Remove comments
            if (isMultiLine) {

            } else {
                document.replace(selection.getOffset(), selection.getLength(), comment.getFirst() + selText + comment.getSecond());
                selLength -= comment.getFirst().length() + comment.getSecond().length();
            }
        } else {
            // Add comment
            if (isMultiLine) {
                document.replace(selection.getOffset() + selection.getLength(), 0, comment.getSecond() + lineDelimiter);
                document.replace(selection.getOffset(), 0, comment.getFirst() + lineDelimiter);
                selLength += comment.getFirst().length() + comment.getSecond().length() + lineDelimiter.length() * 2;
            } else {
                document.replace(selection.getOffset(), selection.getLength(), comment.getFirst() + selText + comment.getSecond());
                selLength += comment.getFirst().length() + comment.getSecond().length();
            }
        }

        if (rewriteSession != null) {
            ((IDocumentExtension4) document).stopRewriteSession(rewriteSession);
        }
        if (selLength > 0) {
            textEditor.getSelectionProvider().setSelection(new TextSelection(selOffset, selLength));
        }

    }
}