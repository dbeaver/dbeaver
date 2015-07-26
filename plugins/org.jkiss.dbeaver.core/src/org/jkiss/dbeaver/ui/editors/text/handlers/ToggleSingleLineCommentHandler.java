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
package org.jkiss.dbeaver.ui.editors.text.handlers;

import org.eclipse.jface.text.*;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.jkiss.dbeaver.ui.ICommentsSupport;
import org.jkiss.utils.ArrayUtils;

public final class ToggleSingleLineCommentHandler extends AbstractCommentHandler {

    @Override
    protected void processAction(ISelectionProvider selectionProvider, ICommentsSupport commentsSupport, IDocument document, ITextSelection textSelection) throws BadLocationException
   {
        String[] singleLineComments = commentsSupport.getSingleLineComments();
        if (ArrayUtils.isEmpty(singleLineComments)) {
            // Single line comments are not supported
            return;
        }
        int selOffset = textSelection.getOffset();
        int originalLength = textSelection.getLength();
        int selLength = originalLength;
        DocumentRewriteSession rewriteSession = null;
        if (document instanceof IDocumentExtension4) {
            rewriteSession = ((IDocumentExtension4) document).startRewriteSession(DocumentRewriteSessionType.SEQUENTIAL);
        }
        int endLine = textSelection.getEndLine();
        int startLine = textSelection.getStartLine();
        for (int lineNum = endLine; lineNum >= startLine; lineNum--) {
            int lineOffset = document.getLineOffset(lineNum);
            int lineLength = document.getLineLength(lineNum);
            String lineComment = null;
            for (String commentString : singleLineComments) {
                if (document.get(lineOffset, lineLength).startsWith(commentString)) {
                    lineComment = commentString;
                    break;
                }
            }
            if (lineComment != null) {
                // Remove comment
                document.replace(lineOffset, lineComment.length(), "");
                selLength -= lineComment.length();
            } else {
                // Add comment
                document.replace(lineOffset, 0, singleLineComments[0]);
                selLength += singleLineComments[0].length();
            }
        }
        if (rewriteSession != null) {
            ((IDocumentExtension4) document).stopRewriteSession(rewriteSession);
        }
        if (originalLength > 0) {
            selectionProvider.setSelection(new TextSelection(selOffset, selLength));
        }
    }
}