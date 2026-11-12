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

import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Region;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Layout;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.ui.UIStyles;
import org.jkiss.dbeaver.ui.css.CSSUtils;
import org.jkiss.dbeaver.utils.RuntimeUtils;

/**
 * Single-line text that looks like a label until it receives focus.
 * Keeps one native Text control to preserve the caret and text position when editing starts.
 */
public class CEditableLabel extends Composite {
    private static final int WINDOWS_DARK_TEXT_OFFSET = 2;

    private final Composite clip;
    private final Text text;
    private boolean editing;

    public CEditableLabel(@NotNull Composite parent, int style) {
        super(parent, style & (SWT.LEFT_TO_RIGHT | SWT.RIGHT_TO_LEFT));
        clip = new Composite(this, SWT.NONE);
        text = new Text(clip, SWT.SINGLE | SWT.BORDER | (style & (SWT.LEFT_TO_RIGHT | SWT.RIGHT_TO_LEFT)));
        setLayout(new Layout() {
            @NotNull
            @Override
            protected Point computeSize(@NotNull Composite composite, int widthHint, int heightHint, boolean flushCache) {
                Point size = text.computeSize(SWT.DEFAULT, SWT.DEFAULT, flushCache);
                return new Point(widthHint == SWT.DEFAULT ? size.x : widthHint,
                    heightHint == SWT.DEFAULT ? size.y + getTopInset() : heightHint);
            }

            @Override
            protected void layout(@NotNull Composite composite, boolean flushCache) {
                layoutText();
            }
        });
        text.addListener(SWT.FocusIn, event -> setEditing(true));
        text.addListener(SWT.FocusOut, event -> setEditing(false));
        CSSUtils.applyStyles(this);
        updateColors();

        var display = getDisplay();
        var themeManager = PlatformUI.getWorkbench().getThemeManager();
        IPropertyChangeListener themeListener = event -> display.asyncExec(() -> {
            if (!isDisposed()) {
                updateColors();
                requestLayout();
            }
        });
        themeManager.addPropertyChangeListener(themeListener);
        addDisposeListener(event -> {
            themeManager.removePropertyChangeListener(themeListener);
            Region region = clip.getRegion();
            if (region != null) {
                clip.setRegion(null);
                region.dispose();
            }
        });
    }

    private void setEditing(boolean editing) {
        this.editing = editing;
        updateColors();
        layout(true);
        notifyListeners(editing ? SWT.FocusIn : SWT.FocusOut, new Event());
    }

    private void updateColors() {
        text.setBackground(null);
        text.setForeground(null);
        CSSUtils.applyStyles(text);
        if (!editing) {
            text.setBackground(getBackground());
        }
    }

    private void layoutText() {
        var bounds = getClientArea();
        int topInset = getTopInset();
        bounds.y += topInset;
        bounds.height = Math.max(0, bounds.height - topInset);
        clip.setBounds(bounds);
        text.setBounds(0, 0, bounds.width, bounds.height);
        // Hide the native border without moving/resizing the Text between focus states.
        Region previous = clip.getRegion();
        Region region = null;
        if (!editing) {
            region = new Region(getDisplay());
            if (RuntimeUtils.isMacOS()) {
                // Cocoa's native text field trim is not included in getBorderWidth().
                var trim = text.computeTrim(0, 0, 0, 0);
                region.add(-trim.x, -trim.y,
                    Math.max(0, bounds.width - trim.width), Math.max(0, bounds.height - trim.height));
            } else {
                // computeTrim() also includes text margins on Windows, which must not be clipped.
                int inset = text.getBorderWidth();
                region.add(inset, inset,
                    Math.max(0, bounds.width - inset * 2), Math.max(0, bounds.height - inset * 2));
            }
        }
        clip.setRegion(region);
        if (previous != null) {
            previous.dispose();
        }
    }

    private int getTopInset() {
        // In a vertically centered row, twice the offset moves the native field down by the offset
        // while preserving its preferred height. Windows dark Text renders higher than Label.
        return RuntimeUtils.isWindows() && UIStyles.isDarkTheme() ? WINDOWS_DARK_TEXT_OFFSET * 2 : 0;
    }

    @NotNull
    public String getText() {
        return text.getText();
    }

    public void setText(@NotNull String value) {
        if (!text.getText().equals(value)) {
            text.setText(value);
        }
    }

    @NotNull
    public Point getSelection() {
        return text.getSelection();
    }

    public void setSelection(@NotNull Point selection) {
        text.setSelection(selection);
    }

    public void addModifyListener(@NotNull ModifyListener listener) {
        text.addModifyListener(listener);
    }

    public void removeModifyListener(@NotNull ModifyListener listener) {
        text.removeModifyListener(listener);
    }

    @Override
    public boolean setFocus() {
        return text.setFocus();
    }

    @Override
    public boolean isFocusControl() {
        return text.isFocusControl();
    }
}
