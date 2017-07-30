/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
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
package org.jkiss.dbeaver.ui.editors.text.handlers;

import org.eclipse.jface.text.*;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.jkiss.dbeaver.ui.ICommentsSupport;
import org.jkiss.utils.ArrayUtils;

public final class ToggleSingleLineCommentHandler extends AbstractCommentHandler {

    @Override
    protected void processAction(ISelectionProvider selectionProvider, ICommentsSupport commentsSupport, IDocument document, ITextSelection textSelection) throws BadLocationException {
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

        boolean forceComment = true;
        String firstLineText = document.get(document.getLineOffset(startLine), document.getLineLength(startLine));
        for (String commentString : singleLineComments) {
            if (firstLineText.startsWith(commentString)) {
                forceComment = false;
                break;
            }
        }
        for (int lineNum = endLine; lineNum >= startLine; lineNum--) {
            int lineOffset = document.getLineOffset(lineNum);
            int lineLength = document.getLineLength(lineNum);
            if (forceComment) {
                // Add comment
                document.replace(lineOffset, 0, singleLineComments[0]);
                selLength += singleLineComments[0].length();
            } else {
                String lineComment = null;
                String lineText = document.get(lineOffset, lineLength);
                for (String commentString : singleLineComments) {
                    if (lineText.startsWith(commentString)) {
                        lineComment = commentString;
                        break;
                    }
                }
                if (lineComment != null) {
                    // Remove comment
                    document.replace(lineOffset, lineComment.length(), "");
                    selLength -= lineComment.length();
                }
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