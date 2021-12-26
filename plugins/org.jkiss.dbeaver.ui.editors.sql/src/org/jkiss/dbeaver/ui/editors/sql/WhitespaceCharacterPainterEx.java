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
package org.jkiss.dbeaver.ui.editors.sql;

import org.eclipse.jface.text.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.custom.StyledTextContent;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.*;
import org.jkiss.dbeaver.Log;
import org.jkiss.utils.BeanUtils;


/**
 * A painter for drawing visible characters for (invisible) whitespace
 * characters.
 * <p>
 * This is a direct copy of {@link org.eclipse.jface.text.WhitespaceCharacterPainter} from JFace
 * with several enhancements.
 * <p>
 * This version introduces support for displaying zero-width whitespace symbol {@code u200b}
 *
 * @see org.eclipse.jface.text.WhitespaceCharacterPainter
 * @see WhitespaceCharacterPainterEx#ZERO_WIDTH_WHITESPACE_SIGN
 * @since 3.3
 */
public class WhitespaceCharacterPainterEx implements IPainter, PaintListener {
    private static final Log log = Log.getLog(WhitespaceCharacterPainterEx.class);

    private static final char SPACE_SIGN = '\u00b7';
    private static final char IDEOGRAPHIC_SPACE_SIGN = '\u00b0';
    private static final char TAB_SIGN = '\u00bb';
    private static final char ZERO_WIDTH_WHITESPACE_SIGN = '\u2588';
    private static final char CARRIAGE_RETURN_SIGN = '\u00a4';
    private static final char LINE_FEED_SIGN = '\u00b6';

    private static final String SPACE_SIGN_STRING = String.valueOf(SPACE_SIGN);
    private static final String IDEOGRAPHIC_SPACE_SIGN_STRING = String.valueOf(IDEOGRAPHIC_SPACE_SIGN);
    private static final String ZERO_WIDTH_WHITESPACE_SIGN_STRING = String.valueOf(ZERO_WIDTH_WHITESPACE_SIGN);

    /**
     * Indicates whether this painter is active.
     */
    private boolean fIsActive = false;
    /**
     * The source viewer this painter is attached to.
     */
    private ITextViewer fTextViewer;
    /**
     * The viewer's widget.
     */
    private StyledText fTextWidget;
    /**
     * Tells whether the advanced graphics sub system is available.
     */
    private final boolean fIsAdvancedGraphicsPresent;
    /**
     * Tells whether the text widget was created with the full selection style bit or not.
     *
     * @since 3.7
     */
    private final boolean fIsFullSelectionStyle;
    /**
     * @since 3.7
     */
    private boolean fShowLeadingSpaces = true;
    /**
     * @since 3.7
     */
    private boolean fShowEnclosedSpace = true;
    /**
     * @since 3.7
     */
    private boolean fShowTrailingSpaces = true;
    /**
     * @since 3.7
     */
    private boolean fShowLeadingIdeographicSpaces = true;
    /**
     * @since 3.7
     */
    private boolean fShowEnclosedIdeographicSpaces = true;
    /**
     * @since 3.7
     */
    private boolean fShowTrailingIdeographicSpaces = true;
    /**
     * @since 3.7
     */
    private boolean fShowLeadingTabs = true;
    /**
     * @since 3.7
     */
    private boolean fShowEnclosedTabs = true;
    /**
     * @since 3.7
     */
    private boolean fShowTrailingTabs = true;
    /**
     * @since 3.7
     */
    private boolean fShowCarriageReturn = true;
    /**
     * @since 3.7
     */
    private boolean fShowLineFeed = true;
    /**
     * @since 3.7
     */
    private int fAlpha = 80;

    /**
     * Creates a new painter for the given text viewer.
     *
     * @param textViewer the text viewer the painter should be attached to
     */
    public WhitespaceCharacterPainterEx(ITextViewer textViewer) {
        super();
        fTextViewer = textViewer;
        fTextWidget = textViewer.getTextWidget();
        GC gc = new GC(fTextWidget);
        gc.setAdvanced(true);
        fIsAdvancedGraphicsPresent = gc.getAdvanced();
        gc.dispose();
        fIsFullSelectionStyle = (fTextWidget.getStyle() & SWT.FULL_SELECTION) != SWT.NONE;
    }

    public WhitespaceCharacterPainterEx(ITextViewer viewer, WhitespaceCharacterPainter original) {
        this(viewer);

        try {
            fShowLeadingSpaces = BeanUtils.getFieldValue(original, "fShowLeadingSpaces");
            fShowEnclosedSpace = BeanUtils.getFieldValue(original, "fShowEnclosedSpace");
            fShowTrailingSpaces = BeanUtils.getFieldValue(original, "fShowTrailingSpaces");
            fShowLeadingIdeographicSpaces = BeanUtils.getFieldValue(original, "fShowLeadingIdeographicSpaces");
            fShowEnclosedIdeographicSpaces = BeanUtils.getFieldValue(original, "fShowEnclosedIdeographicSpaces");
            fShowTrailingIdeographicSpaces = BeanUtils.getFieldValue(original, "fShowTrailingIdeographicSpaces");
            fShowLeadingTabs = BeanUtils.getFieldValue(original, "fShowLeadingTabs");
            fShowEnclosedTabs = BeanUtils.getFieldValue(original, "fShowEnclosedTabs");
            fShowTrailingTabs = BeanUtils.getFieldValue(original, "fShowTrailingTabs");
            fShowCarriageReturn = BeanUtils.getFieldValue(original, "fShowCarriageReturn");
            fShowLineFeed = BeanUtils.getFieldValue(original, "fShowLineFeed");
            fAlpha = BeanUtils.getFieldValue(original, "fAlpha");
        } catch (Throwable e) {
            log.error("Error copying whitespace character printer from original", e);
        }
    }

    @Override
    public void dispose() {
        fTextViewer = null;
        fTextWidget = null;
    }

    @Override
    public void paint(int reason) {
        IDocument document = fTextViewer.getDocument();
        if (document == null) {
            deactivate(false);
            return;
        }
        if (!fIsActive) {
            fIsActive = true;
            fTextWidget.addPaintListener(this);
            redrawAll();
        } else if (reason == CONFIGURATION || reason == INTERNAL) {
            redrawAll();
        }
    }

    @Override
    public void deactivate(boolean redraw) {
        if (fIsActive) {
            fIsActive = false;
            fTextWidget.removePaintListener(this);
            if (redraw) {
                redrawAll();
            }
        }
    }

    @Override
    public void setPositionManager(IPaintPositionManager manager) {
        // no need for a position manager
    }

    @Override
    public void paintControl(PaintEvent event) {
        if (fTextWidget != null) {
            handleDrawRequest(event.gc, event.x, event.y, event.width, event.height);
        }
    }

    /*
     * Draw characters in view range.
     */
    private void handleDrawRequest(GC gc, int x, int y, int w, int h) {
        int startLine = fTextWidget.getLineIndex(y);
        int endLine = fTextWidget.getLineIndex(y + h - 1);
        if (startLine <= endLine && startLine < fTextWidget.getLineCount()) {

            // avoid painting into the margins:
            Rectangle clipping = gc.getClipping();
            Rectangle clientArea = fTextWidget.getClientArea();
            int leftMargin = fTextWidget.getLeftMargin();
            int rightMargin = fTextWidget.getRightMargin();
            clientArea.x += leftMargin;
            clientArea.width -= leftMargin + rightMargin;
            clipping.intersect(clientArea);
            gc.setClipping(clientArea);
            if (fIsAdvancedGraphicsPresent) {
                int alpha = gc.getAlpha();
                gc.setAlpha(fAlpha);
                drawLineRange(gc, startLine, endLine, x, w);
                gc.setAlpha(alpha);
            } else {
                drawLineRange(gc, startLine, endLine, x, w);
            }
            gc.setClipping(clipping);
        }
    }

    /**
     * Draw the given line range.
     *
     * @param gc        the GC
     * @param startLine first line number
     * @param endLine   last line number (inclusive)
     * @param x         the X-coordinate of the drawing range
     * @param w         the width of the drawing range
     */
    private void drawLineRange(GC gc, int startLine, int endLine, int x, int w) {
        final int viewPortWidth = fTextWidget.getClientArea().width;
        final int spaceCharWidth = gc.stringExtent(" ").x; //$NON-NLS-1$
        final boolean spaceCharsAreSameWidth =
            spaceCharWidth == gc.stringExtent(SPACE_SIGN_STRING).x &&
            spaceCharWidth == gc.stringExtent(IDEOGRAPHIC_SPACE_SIGN_STRING).x &&
            spaceCharWidth == gc.stringExtent(ZERO_WIDTH_WHITESPACE_SIGN_STRING).x;

        for (int line = startLine; line <= endLine; line++) {
            int lineOffset = fTextWidget.getOffsetAtLine(line);
            // line end offset including line delimiter
            int lineEndOffset;
            if (line < fTextWidget.getLineCount() - 1) {
                lineEndOffset = fTextWidget.getOffsetAtLine(line + 1);
            } else {
                lineEndOffset = fTextWidget.getCharCount();
            }
            // line length excluding line delimiter
            int lineLength = lineEndOffset - lineOffset;
            while (lineLength > 0) {
                char c = fTextWidget.getTextRange(lineOffset + lineLength - 1, 1).charAt(0);
                if (c != '\r' && c != '\n') {
                    break;
                }
                --lineLength;
            }
            // compute coordinates of last character on line
            Point endOfLine = fTextWidget.getLocationAtOffset(lineOffset + lineLength);
            if (x - endOfLine.x > viewPortWidth) {
                // line is not visible
                continue;
            }
            // Y-coordinate of line
            int y = fTextWidget.getLinePixel(line);
            // compute first visible char offset
            int startOffset = fTextWidget.getOffsetAtPoint(new Point(x, y)) - 1;
            if (startOffset == -1) {
                startOffset = lineOffset;
            } else if (startOffset - 2 <= lineOffset) {
                startOffset = lineOffset;
            }
            // compute last visible char offset
            int endOffset;
            if (x + w >= endOfLine.x) {
                // line end is visible
                endOffset = lineEndOffset;
            } else {
                endOffset = fTextWidget.getOffsetAtPoint(new Point(x + w - 1, y)) + 1;
                if (endOffset == -1) {
                    endOffset = lineEndOffset;
                } else if (endOffset + 2 >= lineEndOffset) {
                    endOffset = lineEndOffset;
                }
            }
            // draw character range
            if (endOffset > startOffset) {
                drawCharRange(gc, startOffset, endOffset, lineOffset, lineEndOffset, spaceCharsAreSameWidth);
            }
        }
    }

    private boolean isWhitespaceCharacter(char c) {
        return c == ' ' || c == '\u3000' || c == '\u200b' || c == '\t' || c == '\r' || c == '\n';
    }

    /**
     * Draw characters of content range.
     *
     * @param gc                     the GC
     * @param startOffset            inclusive start index of the drawing range
     * @param endOffset              exclusive end index of the drawing range
     * @param lineOffset             inclusive start index of the line
     * @param lineEndOffset          exclusive end index of the line
     * @param spaceCharsAreSameWidth whether or not all space chars are same width, if <code>true</code>
     *                               rendering can be optimized
     */
    private void drawCharRange(GC gc, int startOffset, int endOffset, int lineOffset, int lineEndOffset, boolean spaceCharsAreSameWidth) {
        StyledTextContent content = fTextWidget.getContent();
        String lineText = content.getTextRange(lineOffset, lineEndOffset - lineOffset);
        int startOffsetInLine = startOffset - lineOffset;
        int endOffsetInLine = endOffset - lineOffset;

        int textBegin = -1;
        for (int i = 0; i < lineText.length(); ++i) {
            if (!isWhitespaceCharacter(lineText.charAt(i))) {
                textBegin = i;
                break;
            }
        }
        boolean isEmptyLine = textBegin == -1;
        int textEnd = lineText.length() - 1;
        if (!isEmptyLine) {
            for (int i = lineText.length() - 1; i >= 0; --i) {
                if (!isWhitespaceCharacter(lineText.charAt(i))) {
                    textEnd = i;
                    break;
                }
            }
        }

        StyleRange styleRange = null;
        Color fg = null;
        StringBuilder visibleChar = new StringBuilder(10);
        int delta = 0;
        for (int textOffset = startOffsetInLine; textOffset <= endOffsetInLine; ++textOffset) {
            boolean eol = false;
            delta++;
            if (textOffset < endOffsetInLine) {
                char c = lineText.charAt(textOffset);
                switch (c) {
                    case ' ':
                        appendVisibleChar(visibleChar, textOffset, textBegin, textEnd, isEmptyLine, fShowLeadingSpaces, fShowEnclosedSpace, fShowTrailingSpaces, SPACE_SIGN);
                        // 'continue' improves performance but may produce drawing errors
                        // for long runs of space if width of space and dot differ, therefore
                        // it can be used only for monospace fonts
                        if (spaceCharsAreSameWidth) {
                            continue;
                        }
                        break;
                    case '\u3000': // ideographic whitespace
                        appendVisibleChar(visibleChar, textOffset, textBegin, textEnd, isEmptyLine, fShowLeadingIdeographicSpaces, fShowEnclosedIdeographicSpaces, fShowTrailingIdeographicSpaces, IDEOGRAPHIC_SPACE_SIGN);
                        // 'continue' improves performance but may produce drawing errors
                        // for long runs of space if width of space and dot differ, therefore
                        // it can be used only for monospace fonts
                        if (spaceCharsAreSameWidth) {
                            continue;
                        }
                        break;
                    case '\u200b': // zero-width whitespace
                        appendVisibleChar(visibleChar, textOffset, textBegin, textEnd, isEmptyLine, fShowLeadingSpaces, fShowEnclosedSpace, fShowTrailingSpaces, ZERO_WIDTH_WHITESPACE_SIGN);
                        // 'continue' improves performance but may produce drawing errors
                        // for long runs of space if width of space and dot differ, therefore
                        // it can be used only for monospace fonts
                        if (spaceCharsAreSameWidth) {
                            continue;
                        }
                        break;
                    case '\t':
                        appendVisibleChar(visibleChar, textOffset, textBegin, textEnd, isEmptyLine, fShowLeadingTabs, fShowEnclosedTabs, fShowTrailingTabs, TAB_SIGN);
                        break;
                    case '\r':
                        if (fShowCarriageReturn) {
                            visibleChar.append(CARRIAGE_RETURN_SIGN);
                        }
                        if (textOffset >= endOffsetInLine - 1 || lineText.charAt(textOffset + 1) != '\n') {
                            eol = true;
                            break;
                        }
                        continue;
                    case '\n':
                        if (fShowLineFeed) {
                            visibleChar.append(LINE_FEED_SIGN);
                        }
                        eol = true;
                        break;
                    default:
                        break;
                }
            }
            if (visibleChar.length() > 0) {
                int widgetOffset = startOffset + textOffset - startOffsetInLine - delta + 1;
                if (!eol || !isFoldedLine(content.getLineAtOffset(widgetOffset))) {
                    /*
                     * Block selection is drawn using alpha and no selection-inverting
                     * takes place, we always draw as 'unselected' in block selection mode.
                     */
                    if (!fTextWidget.getBlockSelection() && fIsFullSelectionStyle && isOffsetSelected(fTextWidget, widgetOffset)) {
                        fg = fTextWidget.getSelectionForeground();
                    } else if (styleRange == null || styleRange.start + styleRange.length <= widgetOffset) {
                        styleRange = fTextWidget.getStyleRangeAtOffset(widgetOffset);
                        if (styleRange == null || styleRange.foreground == null) {
                            fg = fTextWidget.getForeground();
                        } else {
                            fg = styleRange.foreground;
                        }
                    }
                    draw(gc, widgetOffset, visibleChar.toString(), fg);
                }
                visibleChar.delete(0, visibleChar.length());
            }
            delta = 0;
        }
    }

    private void appendVisibleChar(StringBuilder buffer, int textOffset, int textBegin, int textEnd, boolean isEmptyLine, boolean showLeading, boolean showEnclosed, boolean showTrailing, char ch) {
        if (isEmptyLine) {
            if (showLeading || showEnclosed || showTrailing) {
                buffer.append(ch);
            }
        } else if (textOffset < textBegin) {
            if (showLeading) {
                buffer.append(ch);
            }
        } else if (textOffset < textEnd) {
            if (showEnclosed) {
                buffer.append(ch);
            }
        } else {
            if (showTrailing) {
                buffer.append(ch);
            }
        }
    }

    /**
     * Returns <code>true</code> if <code>offset</code> is selection in <code>widget</code>,
     * <code>false</code> otherwise.
     *
     * @param widget the widget
     * @param offset the offset
     * @return <code>true</code> if <code>offset</code> is selection, <code>false</code> otherwise
     * @since 3.5
     */
    private static boolean isOffsetSelected(StyledText widget, int offset) {
        Point selection = widget.getSelection();
        return offset >= selection.x && offset < selection.y;
    }

    /**
     * Check if the given widget line is a folded line.
     *
     * @param widgetLine the widget line number
     * @return <code>true</code> if the line is folded
     */
    private boolean isFoldedLine(int widgetLine) {
        if (fTextViewer instanceof ITextViewerExtension5) {
            ITextViewerExtension5 extension = (ITextViewerExtension5) fTextViewer;
            int modelLine = extension.widgetLine2ModelLine(widgetLine);
            int widgetLine2 = extension.modelLine2WidgetLine(modelLine + 1);
            return widgetLine2 == -1;
        }
        return false;
    }

    /**
     * Redraw all of the text widgets visible content.
     */
    private void redrawAll() {
        fTextWidget.redraw();
    }

    /**
     * Draw string at widget offset.
     *
     * @param gc     the GC
     * @param offset the widget offset
     * @param s      the string to be drawn
     * @param fg     the foreground color
     */
    private void draw(GC gc, int offset, String s, Color fg) {
        // Compute baseline delta (see https://bugs.eclipse.org/bugs/show_bug.cgi?id=165640)
        int baseline = fTextWidget.getBaseline(offset);
        FontMetrics fontMetrics = gc.getFontMetrics();
        int fontBaseline = fontMetrics.getAscent() + fontMetrics.getLeading();
        int baslineDelta = baseline - fontBaseline;

        Point pos = fTextWidget.getLocationAtOffset(offset);
        gc.setForeground(fg);
        gc.drawString(s, pos.x, pos.y + baslineDelta, true);
    }
}
