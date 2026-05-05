/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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
package org.jkiss.dbeaver.ui.editors.sql;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextViewerExtension5;
import org.eclipse.jface.text.link.LinkedModeModel;
import org.eclipse.jface.text.link.LinkedPosition;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ST;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.Point;
import org.eclipse.ui.texteditor.ITextEditorActionDefinitionIds;
import org.eclipse.ui.texteditor.TextNavigationAction;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.ui.editors.sql.syntax.SQLDocumentCharacterIterator;
import org.jkiss.dbeaver.ui.editors.sql.syntax.SQLWordBreakIterator;

import java.text.BreakIterator;
import java.text.CharacterIterator;

public class SQLEditorCustomActions {

    static protected final Log log = Log.getLog(SQLEditorCustomActions.class);

    public static void registerCustomActions(SQLEditorBase editor) {
        StyledText textWidget = editor.getViewer().getTextWidget();

        editor.setAction(ITextEditorActionDefinitionIds.WORD_PREVIOUS, new NavigatePreviousSubWordAction(editor));
        textWidget.setKeyBinding(SWT.CTRL | SWT.ARROW_LEFT, SWT.NULL);

        editor.setAction(ITextEditorActionDefinitionIds.WORD_NEXT, new NavigateNextSubWordAction(editor));
        textWidget.setKeyBinding(SWT.CTRL | SWT.ARROW_RIGHT, SWT.NULL);

        editor.setAction(ITextEditorActionDefinitionIds.SELECT_WORD_PREVIOUS, new SelectPreviousSubWordAction(editor));
        textWidget.setKeyBinding(SWT.CTRL | SWT.SHIFT | SWT.ARROW_LEFT, SWT.NULL);

        editor.setAction(ITextEditorActionDefinitionIds.SELECT_WORD_NEXT, new SelectNextSubWordAction(editor));
        textWidget.setKeyBinding(SWT.CTRL | SWT.SHIFT | SWT.ARROW_RIGHT, SWT.NULL);
    }

    protected abstract static class BaseTextNavigateAction extends TextNavigationAction {

        protected final SQLEditorBase editor;

        public BaseTextNavigateAction(SQLEditorBase editor, String actionDefinitionId, int action) {
            super(editor.getViewer().getTextWidget(), action);
            this.editor = editor;
            setActionDefinitionId(actionDefinitionId);
        }

        protected ISourceViewer getSourceViewer() {
            return editor.getViewer();
        }

        protected int widgetOffset2ModelOffset(int widgetOffset) {
            ISourceViewer viewer = getSourceViewer();
            if (viewer instanceof ITextViewerExtension5) {
                ITextViewerExtension5 extension = (ITextViewerExtension5) viewer;
                return extension.widgetOffset2ModelOffset(widgetOffset);
            }
            return widgetOffset + viewer.getVisibleRegion().getOffset();
        }

        protected int modelOffset2WidgetOffset(int modelOffset) {
            ISourceViewer viewer = getSourceViewer();
            if (viewer instanceof ITextViewerExtension5) {
                ITextViewerExtension5 extension = (ITextViewerExtension5) viewer;
                return extension.modelOffset2WidgetOffset(modelOffset);
            }
            return modelOffset - viewer.getVisibleRegion().getOffset();
        }
    }

    protected abstract static class NextSubWordAction extends BaseTextNavigateAction {

        protected SQLWordIterator wordIterator = new SQLWordIterator();

        protected NextSubWordAction(SQLEditorBase editor, String actionDefinitionId, int code) {
            super(editor, actionDefinitionId, code);
        }

        @Override
        public void run() {
            // Check whether we are in a java code partition and the preference is enabled
            final DBPPreferenceStore store = editor.getActivePreferenceStore();
            if (!store.getBoolean(SQLPreferenceConstants.SMART_WORD_ITERATOR)) {
                super.run();
                return;
            }

            final ISourceViewer viewer = getSourceViewer();
            final IDocument document = viewer.getDocument();
            try {
                wordIterator.setText((CharacterIterator) new SQLDocumentCharacterIterator(document));
                int position = widgetOffset2ModelOffset(viewer.getTextWidget().getCaretOffset());
                if (position == -1)
                    return;

                int next = findNextPosition(position);
                if (editor.isBlockSelectionModeEnabled() && document.getLineOfOffset(next) != document.getLineOfOffset(position)) {
                    super.run(); // may navigate into virtual white space
                } else if (next != BreakIterator.DONE) {
                    setCaretPosition(next);
                    getTextWidget().showSelection();
                    fireSelectionChanged();
                }
            } catch (BadLocationException e) {
                log.debug(e);
            }
        }

        /**
         * Finds the next position after the given position.
         *
         * @param position the current position
         * @return the next position
         */
        protected int findNextPosition(int position) {
            ISourceViewer viewer = getSourceViewer();
            int widget = -1;
            int next = position;
            while (next != BreakIterator.DONE && widget == -1) { // XXX: optimize
                if (next != BreakIterator.DONE)
                    widget = modelOffset2WidgetOffset(next);
                next = wordIterator.following(next);
            }

            IDocument document = viewer.getDocument();
            LinkedModeModel model = LinkedModeModel.getModel(document, position);
            if (model != null && next != BreakIterator.DONE) {
                LinkedPosition linkedPosition = model.findPosition(new LinkedPosition(document, position, 0));
                if (linkedPosition != null) {
                    int linkedPositionEnd = linkedPosition.getOffset() + linkedPosition.getLength();
                    if (position != linkedPositionEnd && linkedPositionEnd < next)
                        next = linkedPositionEnd;
                } else {
                    LinkedPosition nextLinkedPosition = model.findPosition(new LinkedPosition(document, next, 0));
                    if (nextLinkedPosition != null) {
                        int nextLinkedPositionOffset = nextLinkedPosition.getOffset();
                        if (position != nextLinkedPositionOffset && nextLinkedPositionOffset < next)
                            next = nextLinkedPositionOffset;
                    }
                }
            }

            return next;
        }

        protected abstract void setCaretPosition(int position);
    }

    protected static class NavigateNextSubWordAction extends NextSubWordAction {

        public NavigateNextSubWordAction(SQLEditorBase editor) {
            super(editor, ITextEditorActionDefinitionIds.WORD_NEXT, ST.WORD_NEXT);
        }

        @Override
        protected void setCaretPosition(final int position) {
            getTextWidget().setCaretOffset(modelOffset2WidgetOffset(position));
        }
    }

    protected static class SelectNextSubWordAction extends NextSubWordAction {

        public SelectNextSubWordAction(SQLEditorBase editor) {
            super(editor, ITextEditorActionDefinitionIds.SELECT_WORD_NEXT, ST.SELECT_WORD_NEXT);
        }

        @Override
        protected void setCaretPosition(final int position) {
            final ISourceViewer viewer = getSourceViewer();

            final StyledText text = viewer.getTextWidget();
            if (text != null && !text.isDisposed()) {

                final Point selection = text.getSelection();
                final int caret = text.getCaretOffset();
                final int offset = modelOffset2WidgetOffset(position);

                if (caret == selection.x)
                    text.setSelectionRange(selection.y, offset - selection.y);
                else
                    text.setSelectionRange(selection.x, offset - selection.x);
            }
        }
    }

    protected abstract static class PreviousSubWordAction extends BaseTextNavigateAction {

        protected SQLWordIterator wordIterator = new SQLWordIterator();

        protected PreviousSubWordAction(SQLEditorBase editor, String actionDefinitionId, final int code) {
            super(editor, actionDefinitionId, code);
        }

        @Override
        public void run() {
            final DBPPreferenceStore store = editor.getActivePreferenceStore();
            if (!store.getBoolean(SQLPreferenceConstants.SMART_WORD_ITERATOR)) {
                super.run();
                return;
            }

            final ISourceViewer viewer = getSourceViewer();
            final IDocument document = viewer.getDocument();
            try {
                wordIterator.setText((CharacterIterator) new SQLDocumentCharacterIterator(document));
                int position = widgetOffset2ModelOffset(viewer.getTextWidget().getCaretOffset());
                if (position == -1)
                    return;

                int previous = findPreviousPosition(position);
                if (editor.isBlockSelectionModeEnabled() && document.getLineOfOffset(previous) != document.getLineOfOffset(position)) {
                    super.run(); // may navigate into virtual white space
                } else if (previous != BreakIterator.DONE) {
                    setCaretPosition(previous);
                    getTextWidget().showSelection();
                    fireSelectionChanged();
                }
            } catch (BadLocationException e) {
                log.debug(e);
            }

        }

        protected int findPreviousPosition(int position) {
            ISourceViewer viewer = getSourceViewer();
            int widget = -1;
            int previous = position;
            while (previous != BreakIterator.DONE && widget == -1) { // XXX: optimize
                previous = wordIterator.preceding(previous);
                if (previous != BreakIterator.DONE)
                    widget = modelOffset2WidgetOffset(previous);
            }

            IDocument document = viewer.getDocument();
            LinkedModeModel model = LinkedModeModel.getModel(document, position);
            if (model != null && previous != BreakIterator.DONE) {
                LinkedPosition linkedPosition = model.findPosition(new LinkedPosition(document, position, 0));
                if (linkedPosition != null) {
                    int linkedPositionOffset = linkedPosition.getOffset();
                    if (position != linkedPositionOffset && previous < linkedPositionOffset)
                        previous = linkedPositionOffset;
                } else {
                    LinkedPosition previousLinkedPosition = model.findPosition(new LinkedPosition(document, previous, 0));
                    if (previousLinkedPosition != null) {
                        int previousLinkedPositionEnd = previousLinkedPosition.getOffset() + previousLinkedPosition.getLength();
                        if (position != previousLinkedPositionEnd && previous < previousLinkedPositionEnd)
                            previous = previousLinkedPositionEnd;
                    }
                }
            }

            return previous;
        }

        protected abstract void setCaretPosition(int position);
    }

    protected static class NavigatePreviousSubWordAction extends PreviousSubWordAction {

        public NavigatePreviousSubWordAction(SQLEditorBase editor) {
            super(editor, ITextEditorActionDefinitionIds.WORD_PREVIOUS, ST.WORD_PREVIOUS);
        }

        @Override
        protected void setCaretPosition(final int position) {
            getTextWidget().setCaretOffset(modelOffset2WidgetOffset(position));
        }
    }

    protected static class SelectPreviousSubWordAction extends PreviousSubWordAction {

        public SelectPreviousSubWordAction(SQLEditorBase editor) {
            super(editor, ITextEditorActionDefinitionIds.SELECT_WORD_PREVIOUS, ST.SELECT_WORD_PREVIOUS);
        }

        @Override
        protected void setCaretPosition(final int position) {
            final ISourceViewer viewer = getSourceViewer();

            final StyledText text = viewer.getTextWidget();
            if (text != null && !text.isDisposed()) {

                final Point selection = text.getSelection();
                final int caret = text.getCaretOffset();
                final int offset = modelOffset2WidgetOffset(position);

                if (caret == selection.x)
                    text.setSelectionRange(selection.y, offset - selection.y);
                else
                    text.setSelectionRange(selection.x, offset - selection.x);
            }
        }
    }

    public static class SQLWordIterator extends java.text.BreakIterator {
        private final SQLWordBreakIterator sqlIterator = new SQLWordBreakIterator();
        private int index;

        public SQLWordIterator() {
            this.first();
        }

        public int first() {
            this.index = this.sqlIterator.first();
            return this.index;
        }

        public int last() {
            this.index = this.sqlIterator.last();
            return this.index;
        }

        public int next(int n) {
            int next = 0;

            while (true) {
                n--;
                if (n <= 0 || next == -1) {
                    return next;
                }

                next = this.next();
            }
        }

        public int next() {
            this.index = this.following(this.index);
            return this.index;
        }

        public int previous() {
            this.index = this.preceding(this.index);
            return this.index;
        }

        public int preceding(int offset) {
            int first = this.sqlIterator.preceding(offset);
            if (this.isWhitespace(first, offset)) {
                int second = this.sqlIterator.preceding(first);
                if (second != -1 && !this.isDelimiter(second, first)) {
                    return second;
                }
            }

            return first;
        }

        public int following(int offset) {
            int first = this.sqlIterator.following(offset);
            if (this.eatFollowingWhitespace(offset, first)) {
                int second = this.sqlIterator.following(first);
                if (this.isWhitespace(first, second)) {
                    return second;
                }
            }

            return first;
        }

        private boolean eatFollowingWhitespace(int offset, int exclusiveEnd) {
            if (exclusiveEnd != -1 && offset != -1) {
                if (this.isWhitespace(offset, exclusiveEnd)) {
                    return false;
                } else {
                    return !this.isDelimiter(offset, exclusiveEnd);
                }
            } else {
                return false;
            }
        }

        private boolean isDelimiter(int offset, int exclusiveEnd) {
            if (exclusiveEnd != -1 && offset != -1) {
                for (CharSequence seq = this.sqlIterator.getTextValue(); offset < exclusiveEnd; ++offset) {
                    char ch = seq.charAt(offset);
                    if (ch != '\n' && ch != '\r') {
                        return false;
                    }
                }

                return true;
            } else {
                return false;
            }
        }

        private boolean isWhitespace(int offset, int exclusiveEnd) {
            if (exclusiveEnd != -1 && offset != -1) {
                for (CharSequence seq = this.sqlIterator.getTextValue(); offset < exclusiveEnd; ++offset) {
                    char ch = seq.charAt(offset);
                    if (!Character.isWhitespace(ch)) {
                        return false;
                    }

                    if (ch == '\n' || ch == '\r') {
                        return false;
                    }
                }

                return true;
            } else {
                return false;
            }
        }

        public int current() {
            return this.index;
        }

        public CharacterIterator getText() {
            return this.sqlIterator.getText();
        }

        public void setText(CharSequence newText) {
            this.sqlIterator.setText(newText);
            this.first();
        }

        public void setText(CharacterIterator newText) {
            this.sqlIterator.setText(newText);
            this.first();
        }

        public void setText(String newText) {
            this.setText((CharSequence) newText);
        }
    }

}
