/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2021 DBeaver Corp and others
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
