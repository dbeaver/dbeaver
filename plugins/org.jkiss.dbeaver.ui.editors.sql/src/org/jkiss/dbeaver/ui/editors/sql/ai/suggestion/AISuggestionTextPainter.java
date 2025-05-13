/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2025 DBeaver Corp and others
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
package org.jkiss.dbeaver.ui.editors.sql.ai.suggestion;

import org.eclipse.jface.text.*;
import org.eclipse.swt.custom.LineBackgroundEvent;
import org.eclipse.swt.custom.LineBackgroundListener;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.jkiss.dbeaver.ui.UIUtils;

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

public class AISuggestionTextPainter implements IPainter, PaintListener, LineBackgroundListener {

    public static final String HINT_CATEGORY = "suggestion";
    private final ITextViewer viewerComponent;
    private Color fontColor;
    private RenderState currentState;
    private final Semaphore lockObject;
    private boolean isEnabled;
    private HintContent activeHint;
    private IPositionUpdater updater;
    private boolean standaloneOperation = false;

    public AISuggestionTextPainter(ITextViewer viewer) {
        this.viewerComponent = viewer;
        this.currentState = RenderState.IDLE;
        this.lockObject = new Semaphore(1);
        this.activeHint = HintContent.initialize(0, "");
        UIUtils.asyncExec(() -> ((ITextViewerExtension2) viewerComponent).addPainter(this));
    }

    public void setHintColor(Color color) {
        this.fontColor = color;
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
     * @param content        the content of the hint to be displayed
     * @param removeExisting if true, removes any currently displayed hint before showing the new one
     */
    public void showHint(String content, boolean removeExisting, int scriptEndOffset) {
        if (!tryLock()) {
            return;
        }
        this.currentState = RenderState.SHOWING;
        UIUtils.asyncExec(() -> {
            if (removeExisting) {
                executeRemove();
            }
            executeShow(content, scriptEndOffset);
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
    }

    @Override
    public void paint(int reason) {
        if (!isEnabled) {
            enable();
        }
    }

    @Override
    public void paintControl(PaintEvent event) {
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
    }

    @Override
    public void lineGetBackground(LineBackgroundEvent event) {
    }

    public void applyHint() {
        if (!hasContentToShow()) {
            return;
        }
        insertTextAtCursor(activeHint.getContent());
        removeHint();
    }

    public boolean hasContentToShow() {
        return activeHint != null && !activeHint.isEmpty();
    }

    public int getCurrentPosition() {
        return activeHint != null ? activeHint.getPosition() : -1;
    }

    private void drawHintContent(GC gc) {
        configureGraphicsContext(gc);
        int position = activeHint.getPosition();
        String[] textLines = activeHint.getTextLines();
        if (textLines.length > 0) {
            TextRenderingUtils.drawFirstLine(textLines[0], gc, getTextWidget(), position);
            configureGraphicsContext(gc);
            if (textLines.length > 1) {
                TextRenderingUtils.drawNextLines(textLines[1], gc, getTextWidget(), position);
            }
        }
        resetState();
    }

    private void executeShow(String text, int scriptEndOffset) {
        int positionToShow = getCursorPosition();

        if (scriptEndOffset >= 0) {
            positionToShow = scriptEndOffset;
        }

        String wordPrefix = extractCurrentWord();
        String fragment = text;
        if (!wordPrefix.isEmpty() && fragment.toLowerCase().startsWith(wordPrefix.toLowerCase())) {
            fragment = fragment.substring(wordPrefix.length());
        }
        activeHint = HintContent.initialize(positionToShow, fragment);
        getTextWidget().redraw();
    }


    private void executeRemove() {
        activeHint = HintContent.initialize(activeHint.getPosition(), "");
        getTextWidget().redraw();
    }

    private void insertTextAtCursor(String text) {
        try {
            IDocument document = viewerComponent.getDocument();
            int modelPosition = TextRenderingUtils.widgetOffset2ModelOffset(viewerComponent, activeHint.getPosition());
            document.replace(modelPosition, 0, text);
            getTextWidget().setCaretOffset(activeHint.getPosition() + text.length());
        } catch (Exception ignored) {
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

    private static class HintContent {
        private final int position;
        private final String content;

        private HintContent(int position, String content) {
            this.position = position;
            this.content = content == null ? "" : content;
        }

        static HintContent initialize(int position, String content) {
            return new HintContent(position, content);
        }

        int getPosition() {
            return position;
        }

        String getContent() {
            return content;
        }

        boolean isEmpty() {
            return content.isEmpty();
        }

        String[] getTextLines() {
            return content.split("\\R", 2);
        }
    }

    private enum RenderState {
        IDLE,
        SHOWING,
        REMOVING
    }
}