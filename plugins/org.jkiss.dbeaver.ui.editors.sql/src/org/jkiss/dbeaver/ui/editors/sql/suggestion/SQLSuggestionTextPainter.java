/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2026 DBeaver Corp and others
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
package org.jkiss.dbeaver.ui.editors.sql.suggestion;

import org.eclipse.jface.text.*;
import org.eclipse.swt.custom.LineBackgroundEvent;
import org.eclipse.swt.custom.LineBackgroundListener;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ui.UIUtils;

import java.util.Arrays;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

public class SQLSuggestionTextPainter implements IPainter, PaintListener, LineBackgroundListener {
    private static final Log log = Log.getLog(SQLSuggestionTextPainter.class);

    public static final String HINT_CATEGORY = "suggestion";
    private final ITextViewer viewerComponent;
    private Color fontColor;
    private Color suggestionBackground;
    private RenderState currentState;
    private final Semaphore lockObject;
    private boolean isEnabled;
    private HintContent activeHint;
    private IPositionUpdater updater;
    private boolean standaloneOperation = false;
    private int nextLineVerticalIndentLine = -1;

    public SQLSuggestionTextPainter(ITextViewer viewer) {
        this.viewerComponent = viewer;
        this.currentState = RenderState.IDLE;
        this.lockObject = new Semaphore(1);
        this.activeHint = HintContent.of(0, null);
        UIUtils.asyncExec(() -> ((ITextViewerExtension2) viewerComponent).addPainter(this));
    }

    public void setFontColor(Color color) {
        this.fontColor = color;
    }

    public void setSuggestionBackgroundColor(Color color) {
        this.suggestionBackground = color;
    }

    public void removeHint() {
        if (!tryLock()) {
            return;
        }
        this.currentState = RenderState.REMOVING;
        UIUtils.asyncExec(this::executeRemove);
    }

    /**
     * Displays a hint with the given content. Optionally removes any existing hint before displaying the new one.
     *
     * @param content the content of the hint to be displayed
     * @param cursorPosition the position of the cursor in editor
     */
    public void showHint(@NotNull String content, int cursorPosition) {
        if (!tryLock()) {
            return;
        }
        this.currentState = RenderState.SHOWING;
        UIUtils.asyncExec(() -> {
            executeRemove(); // removes any currently displayed hint before showing the new one
            executeShow(content, cursorPosition);
        });
    }

    public boolean isProcessing() {
        return currentState != RenderState.IDLE;
    }

    /**
     * Activates the suggestion text painter and enables its core functionality.
     */
    public void enable() {
        if (!isEnabled) {
            isEnabled = true;
            StyledText textWidget = getTextWidget();
            textWidget.addPaintListener(this);
            textWidget.addLineBackgroundListener(this);
            viewerComponent.getDocument().addPositionCategory(HINT_CATEGORY);
            updater = new DefaultPositionUpdater(HINT_CATEGORY);
            viewerComponent.getDocument().addPositionUpdater(updater);
        }
    }

    @Override
    public void deactivate(boolean redraw) {
        disable(redraw);
    }

    /**
     * Disables the suggestion text painter functionality.
     *
     * @param clearContent if true, removes any displayed hint before disabling the painter.
     */
    public void disable(boolean clearContent) {
        if (!isEnabled) {
            return;
        }
        if (clearContent) {
            removeHint();
        } else {
            clearHintVerticalIndent();
        }
        StyledText textWidget = getTextWidget();
        textWidget.removePaintListener(this);
        textWidget.removeLineBackgroundListener(this);
        viewerComponent.getDocument().removePositionUpdater(updater);
        try {
            viewerComponent.getDocument().removePositionCategory(HINT_CATEGORY);
        } catch (BadPositionCategoryException ignored) {
        }
        currentState = RenderState.IDLE;
        isEnabled = false;
    }

    @Override
    public void setPositionManager(IPaintPositionManager manager) {
    }

    @Override
    public void dispose() {
        clearHintVerticalIndent();
        StyledText textWidget = getTextWidget();
        textWidget.removePaintListener(this);
        textWidget.removeLineBackgroundListener(this);
        if (viewerComponent.getDocument() != null) {
            viewerComponent.getDocument().removePositionUpdater(updater);
        }
    }

    @Override
    public void paint(int reason) {
        if (!isEnabled) {
            enable();
        }
    }

    private volatile boolean repainting = false;

    @Override
    public void paintControl(PaintEvent event) {
        if (this.repainting) {
            // prevent recursive invocation while asking text viewer to draw original text fragments during hint rendering
            return;
        } else {
            this.repainting = true;
        }
        try {
            if (standaloneOperation && currentState == RenderState.SHOWING) {
                drawHintContent(event.gc);
                return;
            }
            if (!hasContentToShow()) {
                resetState();
                return;
            }

            switch (currentState) {
                case SHOWING:
                    drawHintContent(event.gc);
                    break;
                case REMOVING:
                    resetState();
                    drawHintContent(event.gc);
                    break;
                default:
                    drawHintContent(event.gc);
                    break;
            }
        } finally {
            this.repainting = false;
        }
    }

    @Override
    public void lineGetBackground(LineBackgroundEvent event) {
    }

    public void applyHint() {
        if (!hasContentToShow()) {
            return;
        }
        String content = activeHint.content();
        int position = activeHint.position();
        clearHintVerticalIndent();
        activeHint = HintContent.of(position, null);
        getTextWidget().redraw();
        insertTextAtCursor(content);
    }

    public boolean hasContentToShow() {
        return activeHint != null && !activeHint.isEmpty();
    }

    public int getCurrentPosition() {
        return activeHint != null ? activeHint.position() : -1;
    }

    private void drawHintContent(GC gc) {
        configureGraphicsContext(gc);
        int position = activeHint.position();
        String[] textLines = activeHint.getTextLines();
        if (textLines.length > 0) {
            TextRenderingUtils.drawFirstLine(textLines[0], gc, getTextWidget(), position, this.suggestionBackground);
            configureGraphicsContext(gc);
            if (textLines.length > 1) {
                TextRenderingUtils.drawNextLines(activeHint.getContinuationText(), gc, getTextWidget(), position);
            }
        }
        resetState();
    }

    private void executeShow(String text, int cursorPosition) {
        String wordPrefix = extractCurrentWord();
        String fragment = text;
        if (!wordPrefix.isEmpty() && fragment.toLowerCase().startsWith(wordPrefix.toLowerCase())) {
            fragment = fragment.substring(wordPrefix.length());
        }
        activeHint = HintContent.of(cursorPosition, fragment);
        updateHintVerticalIndent();
        getTextWidget().redraw();
    }


    private void executeRemove() {
        clearHintVerticalIndent();
        activeHint = HintContent.of(activeHint.position(), null);
        getTextWidget().redraw();
    }

    private void insertTextAtCursor(String text) {
        try {
            IDocument document = viewerComponent.getDocument();
            int modelPosition = TextRenderingUtils.widgetOffset2ModelOffset(viewerComponent, activeHint.position());
            document.replace(modelPosition, 0, text);
            int newModelPosition = modelPosition + text.length();
            int newWidgetPosition = TextRenderingUtils.modelOffset2WidgetOffset(viewerComponent, newModelPosition);
            getTextWidget().setCaretOffset(Math.max(0, newWidgetPosition));
        } catch (BadLocationException e) {
            log.debug("Exception trying to insert AI suggestion", e);
        }
    }

    private boolean tryLock() {
        try {
            return lockObject.tryAcquire(100, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            return false;
        }
    }

    private void resetState() {
        currentState = RenderState.IDLE;
        if (lockObject.availablePermits() == 0) {
            lockObject.release();
        }
    }

    private void configureGraphicsContext(GC gc) {
        if (fontColor != null) {
            gc.setForeground(fontColor);
        }
        gc.setBackground(getTextWidget().getBackground());
    }

    private int getCursorPosition() {
        return getTextWidget().getCaretOffset();
    }

    private String extractCurrentWord() {
        StyledText widget = getTextWidget();
        int position = getCursorPosition();
        String lineContent = widget.getText().substring(0, position);
        int separator = Math.max(lineContent.lastIndexOf(' '), lineContent.lastIndexOf('\t'));
        return separator >= 0 ? lineContent.substring(separator + 1) : lineContent;
    }

    private StyledText getTextWidget() {
        return viewerComponent.getTextWidget();
    }

    private void updateHintVerticalIndent() {
        clearHintVerticalIndent();
        if (activeHint == null || activeHint.getContinuationLineCount() == 0) {
            return;
        }

        StyledText widget = getTextWidget();
        if (widget == null || widget.isDisposed()) {
            return;
        }

        int lineCount = widget.getLineCount();
        if (lineCount <= 1) {
            return;
        }

        int maxOffset = Math.max(0, widget.getCharCount() - 1);
        int anchorOffset = Math.max(0, Math.min(activeHint.position(), maxOffset));
        int anchorLine = widget.getLineAtOffset(anchorOffset);
        int nextLine = anchorLine + 1;
        if (nextLine >= lineCount) {
            return;
        }

        int extraHeight = widget.getLineHeight() * activeHint.getContinuationLineCount();
        if (extraHeight <= 0) {
            return;
        }

        widget.setLineVerticalIndent(nextLine, extraHeight);
        nextLineVerticalIndentLine = nextLine;
    }

    private void clearHintVerticalIndent() {
        StyledText widget = getTextWidget();
        if (widget == null || widget.isDisposed()) {
            nextLineVerticalIndentLine = -1;
            return;
        }
        if (nextLineVerticalIndentLine >= 0 && nextLineVerticalIndentLine < widget.getLineCount()) {
            widget.setLineVerticalIndent(nextLineVerticalIndentLine, 0);
        }
        nextLineVerticalIndentLine = -1;
    }

    private record HintContent(int position, @NotNull String content) {

        static HintContent of(int position, @Nullable String content) {
            return new HintContent(position, content == null ? "" : content);
        }

        boolean isEmpty() {
            return content.isEmpty();
        }

        @NotNull
        String[] getTextLines() {
            String[] lines = content.split("\\R", -1);
            int size = lines.length;
            while (size > 0 && lines[size - 1].isEmpty()) {
                size--;
            }
            return size == lines.length ? lines : Arrays.copyOf(lines, size);
        }

        @NotNull
        String getContinuationText() {
            String[] lines = getTextLines();
            if (lines.length <= 1) {
                return "";
            }
            return String.join("\n", Arrays.copyOfRange(lines, 1, lines.length));
        }

        int getContinuationLineCount() {
            return Math.max(0, getTextLines().length - 1);
        }
    }

    private enum RenderState {
        IDLE,
        SHOWING,
        REMOVING
    }
}
