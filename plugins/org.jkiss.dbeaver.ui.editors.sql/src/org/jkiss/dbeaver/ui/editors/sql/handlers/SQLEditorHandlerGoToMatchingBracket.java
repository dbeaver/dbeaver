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
package org.jkiss.dbeaver.ui.editors.sql.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.text.*;
import org.eclipse.jface.text.source.ICharacterPairMatcher;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.Point;
import org.eclipse.ui.handlers.HandlerUtil;
import org.jkiss.dbeaver.model.sql.SQLConstants;
import org.jkiss.dbeaver.ui.editors.sql.SQLEditorBase;
import org.jkiss.dbeaver.utils.RuntimeUtils;

public class SQLEditorHandlerGoToMatchingBracket extends AbstractHandler {

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        SQLEditorBase editor = RuntimeUtils.getObjectAdapter(HandlerUtil.getActiveEditor(event), SQLEditorBase.class);
        if (editor != null) {
            gotoMatchingBracket(editor);
        }
        return null;
    }

    // copied from JDT code
    private void gotoMatchingBracket(SQLEditorBase editor) {

        ISourceViewer sourceViewer = editor.getViewer();
        if (sourceViewer == null) {
            return;
        }
        IDocument document = sourceViewer.getDocument();
        if (document == null) {
            return;
        }

        IRegion selection = getSignedSelection(sourceViewer);

        ICharacterPairMatcher characterPairMatcher = editor.getCharacterPairMatcher();
        IRegion region = characterPairMatcher.match(document, selection.getOffset());
        if (region == null) {
            return;
        }
        int offset = region.getOffset();
        int length = region.getLength();

        if (length < 1)
            return;

        int anchor = characterPairMatcher.getAnchor();
        // http://dev.eclipse.org/bugs/show_bug.cgi?id=34195
        int targetOffset = (ICharacterPairMatcher.RIGHT == anchor) ? offset + 1 : offset + length - 1;

        boolean visible = false;
        if (sourceViewer instanceof ITextViewerExtension5) {
            ITextViewerExtension5 extension = (ITextViewerExtension5) sourceViewer;
            visible = (extension.modelOffset2WidgetOffset(targetOffset) > -1);
        } else {
            IRegion visibleRegion = sourceViewer.getVisibleRegion();
            // http://dev.eclipse.org/bugs/show_bug.cgi?id=34195
            visible = (targetOffset >= visibleRegion.getOffset() && targetOffset <= visibleRegion.getOffset() + visibleRegion.getLength());
        }

        if (!visible) {
            return;
        }

        int adjustment = getOffsetAdjustment(document, selection.getOffset() + selection.getLength(), selection.getLength());
        targetOffset += adjustment;
        int direction = Integer.compare(selection.getLength(), 0);

        sourceViewer.setSelectedRange(targetOffset, direction);
        sourceViewer.revealRange(targetOffset, direction);
    }

    // copied from JDT code
    private static IRegion getSignedSelection(ISourceViewer sourceViewer) {
        Point viewerSelection = sourceViewer.getSelectedRange();

        StyledText text = sourceViewer.getTextWidget();
        Point selection = text.getSelectionRange();
        if (text.getCaretOffset() == selection.x) {
            viewerSelection.x = viewerSelection.x + viewerSelection.y;
            viewerSelection.y = -viewerSelection.y;
        }

        return new Region(viewerSelection.x, viewerSelection.y);
    }

    // copied from JDT code
    private static int getOffsetAdjustment(IDocument document, int offset, int length) {
        if (length == 0 || Math.abs(length) > 1)
            return 0;
        try {
            if (length < 0) {
                if (isOpeningBracket(document.getChar(offset))) {
                    return 1;
                }
            } else {
                if (isClosingBracket(document.getChar(offset - 1))) {
                    return -1;
                }
            }
        } catch (BadLocationException e) {
            //do nothing
        }
        return 0;
    }

    private static boolean isOpeningBracket(char character) {
        for (int i = 0; i < SQLConstants.BRACKETS.length; i += 2) {
            if (character == SQLConstants.BRACKETS[i])
                return true;
        }
        return false;
    }

    private static boolean isClosingBracket(char character) {
        for (int i = 1; i < SQLConstants.BRACKETS.length; i += 2) {
            if (character == SQLConstants.BRACKETS[i])
                return true;
        }
        return false;
    }

}