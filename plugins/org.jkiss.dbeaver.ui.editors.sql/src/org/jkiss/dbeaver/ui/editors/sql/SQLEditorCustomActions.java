/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2023 DBeaver Corp and others
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

import com.ibm.icu.text.BreakIterator;
import org.eclipse.core.runtime.Assert;
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
import org.eclipse.ui.texteditor.IUpdate;
import org.eclipse.ui.texteditor.TextNavigationAction;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.ui.editors.sql.syntax.SQLDocumentCharacterIterator;
import org.jkiss.dbeaver.ui.editors.sql.syntax.SQLWordBreakIterator;

import java.text.CharacterIterator;

public class SQLEditorCustomActions {

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

        /**
         * Creates a new next sub-word action.
         *
         * @param code Action code for the default operation. Must be an action code from @see org.eclipse.swt.custom.ST.
         */
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
            } catch (BadLocationException x) {
                // ignore
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

        /**
         * Sets the caret position to the sub-word boundary given with <code>position</code>.
         *
         * @param position Position where the action should move the caret
         */
        protected abstract void setCaretPosition(int position);
    }

    /**
     * Text navigation action to navigate to the next sub-word.
     *
     * @since 3.0
     */
    protected static class NavigateNextSubWordAction extends NextSubWordAction {

        /**
         * Creates a new navigate next sub-word action.
         */
        public NavigateNextSubWordAction(SQLEditorBase editor) {
            super(editor, ITextEditorActionDefinitionIds.WORD_NEXT, ST.WORD_NEXT);
        }

        /*
         * @see org.eclipse.jdt.internal.ui.NextSubWordAction#setCaretPosition(int)
         */
        @Override
        protected void setCaretPosition(final int position) {
            getTextWidget().setCaretOffset(modelOffset2WidgetOffset(position));
        }
    }

    /**
     * Text operation action to delete the next sub-word.
     *
     * @since 3.0
     */
    protected static class DeleteNextSubWordAction extends NextSubWordAction implements IUpdate {

        /**
         * Creates a new delete next sub-word action.
         */
        public DeleteNextSubWordAction(SQLEditorBase editor) {
            super(editor, ITextEditorActionDefinitionIds.WORD_NEXT, ST.DELETE_WORD_NEXT);
        }

        /*
         * @see org.eclipse.jdt.internal.ui.NextSubWordAction#setCaretPosition(int)
         */
        @Override
        protected void setCaretPosition(final int position) {
            if (!editor.validateEditorInputState())
                return;

            final ISourceViewer viewer = getSourceViewer();
            StyledText text = viewer.getTextWidget();
            Point widgetSelection = text.getSelection();
            if (editor.isBlockSelectionModeEnabled() && widgetSelection.y != widgetSelection.x) {
                final int caret = text.getCaretOffset();
                final int offset = modelOffset2WidgetOffset(position);

                if (caret == widgetSelection.x)
                    text.setSelectionRange(widgetSelection.y, offset - widgetSelection.y);
                else
                    text.setSelectionRange(widgetSelection.x, offset - widgetSelection.x);
                text.invokeAction(ST.DELETE_NEXT);
            } else {
                Point selection = viewer.getSelectedRange();
                final int caret, length;
                if (selection.y != 0) {
                    caret = selection.x;
                    length = selection.y;
                } else {
                    caret = widgetOffset2ModelOffset(text.getCaretOffset());
                    length = position - caret;
                }

                try {
                    viewer.getDocument().replace(caret, length, ""); //$NON-NLS-1$
                } catch (BadLocationException exception) {
                    // Should not happen
                }
            }
        }

        /*
         * @see org.eclipse.ui.texteditor.IUpdate#update()
         */
        @Override
        public void update() {
            setEnabled(editor.isEditorInputModifiable());
        }
    }

    /**
     * Text operation action to select the next sub-word.
     *
     * @since 3.0
     */
    protected static class SelectNextSubWordAction extends NextSubWordAction {

        /**
         * Creates a new select next sub-word action.
         */
        public SelectNextSubWordAction(SQLEditorBase editor) {
            super(editor, ITextEditorActionDefinitionIds.SELECT_WORD_NEXT, ST.SELECT_WORD_NEXT);
        }

        /*
         * @see org.eclipse.jdt.internal.ui.NextSubWordAction#setCaretPosition(int)
         */
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

    /**
     * Text navigation action to navigate to the previous sub-word.
     *
     * @since 3.0
     */
    protected abstract static class PreviousSubWordAction extends BaseTextNavigateAction {

        protected SQLWordIterator wordIterator = new SQLWordIterator();

        /**
         * Creates a new previous sub-word action.
         *
         * @param code Action code for the default operation. Must be an action code from @see org.eclipse.swt.custom.ST.
         */
        protected PreviousSubWordAction(SQLEditorBase editor, String actionDefinitionId, final int code) {
            super(editor, actionDefinitionId, code);
        }

        /*
         * @see org.eclipse.jface.action.IAction#run()
         */
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

                int previous = findPreviousPosition(position);
                if (editor.isBlockSelectionModeEnabled() && document.getLineOfOffset(previous) != document.getLineOfOffset(position)) {
                    super.run(); // may navigate into virtual white space
                } else if (previous != BreakIterator.DONE) {
                    setCaretPosition(previous);
                    getTextWidget().showSelection();
                    fireSelectionChanged();
                }
            } catch (BadLocationException x) {
                // ignore - getLineOfOffset failed
            }

        }

        /**
         * Finds the previous position before the given position.
         *
         * @param position the current position
         * @return the previous position
         */
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

        /**
         * Sets the caret position to the sub-word boundary given with <code>position</code>.
         *
         * @param position Position where the action should move the caret
         */
        protected abstract void setCaretPosition(int position);
    }

    /**
     * Text navigation action to navigate to the previous sub-word.
     *
     * @since 3.0
     */
    protected static class NavigatePreviousSubWordAction extends PreviousSubWordAction {

        /**
         * Creates a new navigate previous sub-word action.
         */
        public NavigatePreviousSubWordAction(SQLEditorBase editor) {
            super(editor, ITextEditorActionDefinitionIds.WORD_PREVIOUS, ST.WORD_PREVIOUS);
        }

        /*
         * @see org.eclipse.jdt.internal.ui.PreviousSubWordAction#setCaretPosition(int)
         */
        @Override
        protected void setCaretPosition(final int position) {
            getTextWidget().setCaretOffset(modelOffset2WidgetOffset(position));
        }
    }

    /**
     * Text operation action to delete the previous sub-word.
     *
     * @since 3.0
     */
    protected static class DeletePreviousSubWordAction extends PreviousSubWordAction implements IUpdate {

        /**
         * Creates a new delete previous sub-word action.
         */
        public DeletePreviousSubWordAction(SQLEditorBase editor) {
            super(editor, ITextEditorActionDefinitionIds.WORD_PREVIOUS, ST.DELETE_WORD_PREVIOUS);
        }

        /*
         * @see org.eclipse.jdt.internal.ui.PreviousSubWordAction#setCaretPosition(int)
         */
        @Override
        protected void setCaretPosition(int position) {
            if (!editor.validateEditorInputState())
                return;

            final int length;
            final ISourceViewer viewer = getSourceViewer();
            StyledText text = viewer.getTextWidget();
            Point widgetSelection = text.getSelection();
            if (editor.isBlockSelectionModeEnabled() && widgetSelection.y != widgetSelection.x) {
                final int caret = text.getCaretOffset();
                final int offset = modelOffset2WidgetOffset(position);

                if (caret == widgetSelection.x)
                    text.setSelectionRange(widgetSelection.y, offset - widgetSelection.y);
                else
                    text.setSelectionRange(widgetSelection.x, offset - widgetSelection.x);
                text.invokeAction(ST.DELETE_PREVIOUS);
            } else {
                Point selection = viewer.getSelectedRange();
                if (selection.y != 0) {
                    position = selection.x;
                    length = selection.y;
                } else {
                    length = widgetOffset2ModelOffset(text.getCaretOffset()) - position;
                }

                try {
                    viewer.getDocument().replace(position, length, ""); //$NON-NLS-1$
                } catch (BadLocationException exception) {
                    // Should not happen
                }
            }
        }

        /*
         * @see org.eclipse.ui.texteditor.IUpdate#update()
         */
        @Override
        public void update() {
            setEnabled(editor.isEditorInputModifiable());
        }
    }

    /**
     * Text operation action to select the previous sub-word.
     *
     * @since 3.0
     */
    protected static class SelectPreviousSubWordAction extends PreviousSubWordAction {

        /**
         * Creates a new select previous sub-word action.
         */
        public SelectPreviousSubWordAction(SQLEditorBase editor) {
            super(editor, ITextEditorActionDefinitionIds.SELECT_WORD_PREVIOUS, ST.SELECT_WORD_PREVIOUS);
        }

        /*
         * @see org.eclipse.jdt.internal.ui.PreviousSubWordAction#setCaretPosition(int)
         */
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
                --n;
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
                Assert.isTrue(offset >= 0);
                Assert.isTrue(exclusiveEnd <= this.getText().getEndIndex());
                Assert.isTrue(exclusiveEnd > offset);

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
