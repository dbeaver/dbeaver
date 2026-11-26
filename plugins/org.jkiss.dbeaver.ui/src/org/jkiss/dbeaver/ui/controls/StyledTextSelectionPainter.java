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
package org.jkiss.dbeaver.ui.controls;

import org.eclipse.jface.resource.FontDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ST;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.graphics.Region;
import org.eclipse.swt.graphics.TextLayout;
import org.eclipse.swt.graphics.TextStyle;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.ui.IStartup;
import org.eclipse.ui.PlatformUI;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.ui.UIUtils;

import java.text.Bidi;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Repaints selected text using its original foreground styles after StyledText paints the selection.
 */
public class StyledTextSelectionPainter implements IStartup {
    private static final String INSTALLED = StyledTextSelectionPainter.class.getName();
    private static final int LIGHT_SELECTION_SYNTAX_PERCENT = 25;

    @Override
    public void earlyStartup() {
        Display display = PlatformUI.getWorkbench().getDisplay();
        display.asyncExec(() -> {
            if (!display.isDisposed()) {
                install(display);
            }
        });
    }

    /**
     * Installs selection painting for existing and subsequently created StyledText controls.
     * Must be called on the display thread.
     */
    public static void install(@NotNull Display display) {
        if (display.getData(INSTALLED) != null) {
            return;
        }
        display.setData(INSTALLED, Boolean.TRUE);
        Listener filter = event -> {
            if (event.widget instanceof StyledText text && text.getData(INSTALLED) == null) {
                text.setData(INSTALLED, Boolean.TRUE);
                // StyledText installs its own paint listener first. Insert our repaint immediately
                // after it so existing annotation and editor overlay painters still run last.
                Listener[] painters = text.getListeners(SWT.Paint);
                for (int i = 1; i < painters.length; i++) {
                    text.removeListener(SWT.Paint, painters[i]);
                }
                text.addPaintListener(StyledTextSelectionPainter::paintSelection);
                for (int i = 1; i < painters.length; i++) {
                    text.addListener(SWT.Paint, painters[i]);
                }
            }
        };
        display.addFilter(SWT.Paint, filter);
        display.disposeExec(() -> display.removeFilter(SWT.Paint, filter));
    }

    private static void paintSelection(@NotNull PaintEvent event) {
        StyledText text = (StyledText) event.widget;
        // SWT already preserves styled glyphs under a translucent rectangle in block-selection mode.
        // Keep that rectangle uniform, including its virtual whitespace beyond the line ends.
        if (text.getBlockSelection()) {
            return;
        }
        // These listeners supply layout information which is not exposed by StyledText's getters.
        // Keep native painting rather than replace their styles with an incomplete layout.
        if (text.isListening(ST.LineGetStyle) || text.isListening(ST.LineGetSegments)) {
            return;
        }
        int[] selections = text.getSelectionRanges();
        boolean hasSelection = false;
        for (int i = 1; i < selections.length; i += 2) {
            hasSelection |= selections[i] != 0;
        }
        if (!hasSelection) {
            return;
        }

        GC gc = event.gc;
        Color foreground = gc.getForeground();
        Color background = gc.getBackground();
        Font font = gc.getFont();
        Region originalClip = new Region(text.getDisplay());
        Map<Integer, Font> fonts = new HashMap<>();
        Map<RGB, Color> colors = new HashMap<>();
        TextLayout layout = new TextLayout(text.getDisplay());
        try {
            gc.getClipping(originalClip);
            Rectangle client = text.getClientArea();
            Rectangle content = new Rectangle(
                text.getLeftMargin(), text.getTopMargin(),
                Math.max(0, client.width - text.getLeftMargin() - text.getRightMargin()),
                Math.max(0, client.height - text.getTopMargin() - text.getBottomMargin()));
            Rectangle clip = gc.getClipping().intersection(content);
            if (clip.isEmpty()) {
                return;
            }
            // Use the control background so customized editor colors also work independently of the workbench theme.
            Color selectionForeground = UIUtils.isDark(text.getBackground().getRGB()) ? null : text.getSelectionForeground();
            gc.setFont(text.getFont());
            gc.setForeground(getSelectionForeground(text.getForeground(), selectionForeground, colors));
            gc.setBackground(text.getSelectionBackground());
            layout.setFont(text.getFont());
            layout.setAscent(text.getBaseline());
            layout.setDescent(Math.max(0, text.getLineHeight() - text.getLineSpacing() - text.getBaseline()));
            layout.setOrientation(text.getOrientation());
            layout.setTextDirection(text.getTextDirection());
            layout.setSpacing(text.getLineSpacing());
            layout.setWidth(text.getWordWrap() ? Math.max(1, content.width) : -1);
            int tabWidth = gc.stringExtent(" ".repeat(text.getTabs())).x;
            int firstLine = text.getLineIndex(clip.y);
            int lastLine = text.getLineIndex(clip.y + clip.height - 1);
            for (int line = firstLine; line <= lastLine; line++) {
                int offset = text.getOffsetAtLine(line);
                String value = text.getLine(line);
                if (value.isEmpty() || !intersectsSelection(selections, offset, value.length())) {
                    continue;
                }
                StyleRange[] styles = text.getStyleRanges(offset, value.length(), true);
                if (text.getLineBullet(line) != null || Arrays.stream(styles).anyMatch(style -> style.metrics != null)) {
                    continue;
                }
                layout.setText(value);
                layout.setStyle(null, 0, value.length() - 1);
                layout.setIndent(text.getLineIndent(line));
                layout.setWrapIndent(text.getLineWrapIndent(line));
                layout.setAlignment(text.getLineAlignment(line));
                layout.setJustify(text.getLineJustify(line));
                int[] tabs = text.getLineTabStops(line);
                layout.setTabs(tabs == null ? new int[] {tabWidth} : tabs);
                for (StyleRange style : styles) {
                    TextStyle paintStyle = new TextStyle(style);
                    // TextLayout consumes TextStyle, which has no StyleRange.fontStyle field.
                    if (paintStyle.font == null && style.fontStyle != SWT.NORMAL) {
                        paintStyle.font = fonts.computeIfAbsent(style.fontStyle, key ->
                            FontDescriptor.createFrom(text.getFont()).withStyle(key).createFont(text.getDisplay()));
                    }
                    if (paintStyle.foreground != null) {
                        paintStyle.foreground = getSelectionForeground(paintStyle.foreground, selectionForeground, colors);
                    }
                    paintStyle.background = null;
                    layout.setStyle(paintStyle, style.start - offset, style.start + style.length - offset - 1);
                }
                Point origin = text.getLocationAtOffset(offset);
                Point layoutOrigin = layout.getLocation(0, false);
                int x = origin.x - layoutOrigin.x;
                int y = origin.y - layoutOrigin.y;
                paintLine(text, gc, layout, selections, offset, x, y, clip, originalClip);
            }
        } finally {
            gc.setClipping(originalClip);
            gc.setForeground(foreground);
            gc.setBackground(background);
            gc.setFont(font);
            layout.dispose();
            fonts.values().forEach(Font::dispose);
            colors.values().forEach(Color::dispose);
            originalClip.dispose();
        }
    }

    @NotNull
    private static Color getSelectionForeground(
        @NotNull Color original, @Nullable Color selectionForeground, @NotNull Map<RGB, Color> colors
    ) {
        if (selectionForeground == null) {
            return original;
        }
        // Retain the token hue as a tint of the theme's readable selection text color.
        return colors.computeIfAbsent(original.getRGB(), rgb -> new Color(
            original.getDevice(), UIUtils.blend(rgb, selectionForeground.getRGB(), LIGHT_SELECTION_SYNTAX_PERCENT)));
    }

    private static boolean intersectsSelection(@NotNull int[] selections, int offset, int length) {
        for (int i = 0; i < selections.length; i += 2) {
            if (selections[i] < offset + length && selections[i] + selections[i + 1] > offset) {
                return true;
            }
        }
        return false;
    }

    private static void paintLine(
        @NotNull StyledText text, @NotNull GC gc, @NotNull TextLayout layout, @NotNull int[] selections,
        int offset, int x, int y, @NotNull Rectangle clip, @NotNull Region originalClip
    ) {
        Region selectionClip = new Region(text.getDisplay());
        try {
            char[] characters = layout.getText().toCharArray();
            boolean bidi = Bidi.requiresBidi(characters, 0, characters.length);
            int[] lines = layout.getLineOffsets();
            for (int line = 0; line < lines.length - 1; line++) {
                Rectangle bounds = layout.getLineBounds(line);
                bounds.x += x;
                bounds.y += y;
                if (!bounds.intersects(clip)) {
                    continue;
                }
                for (int i = 0; i < selections.length; i += 2) {
                    int start = Math.max(lines[line], selections[i] - offset);
                    int end = Math.min(lines[line + 1], selections[i] + selections[i + 1] - offset);
                    if (start >= end) {
                        continue;
                    }
                    if (!bidi) {
                        // A single rectangle per visual line avoids visiting every character in long SQL lines.
                        addSelectionBounds(selectionClip, layout.getBounds(start, end - 1), x, y, clip);
                        continue;
                    }
                    // Individual cluster bounds also handle disjoint visual selections in bidi text.
                    for (int pos = start; pos < end;) {
                        int next = layout.getNextOffset(pos, SWT.MOVEMENT_CLUSTER);
                        addSelectionBounds(selectionClip, layout.getBounds(pos, Math.min(next, end) - 1), x, y, clip);
                        pos = next;
                    }
                }
            }
            selectionClip.intersect(originalClip);
            if (!selectionClip.isEmpty()) {
                gc.setClipping(selectionClip);
                // Erase SWT's monochrome glyphs before drawing the original syntax styles.
                gc.fillRectangle(selectionClip.getBounds());
                layout.draw(gc, x, y);
            }
        } finally {
            gc.setClipping(originalClip);
            selectionClip.dispose();
        }
    }

    private static void addSelectionBounds(
        @NotNull Region region, @NotNull Rectangle bounds, int x, int y, @NotNull Rectangle clip
    ) {
        bounds.x += x;
        bounds.y += y;
        Rectangle intersection = bounds.intersection(clip);
        if (!intersection.isEmpty()) {
            region.add(intersection);
        }
    }
}
