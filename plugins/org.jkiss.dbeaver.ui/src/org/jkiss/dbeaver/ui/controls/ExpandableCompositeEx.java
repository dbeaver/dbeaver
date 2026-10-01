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

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.events.IExpansionListener;
import org.eclipse.ui.forms.widgets.ExpandableComposite;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.ui.BaseThemeSettings;
import org.jkiss.dbeaver.ui.UIUtils;

/**
 * An extension of ExpandableComposite that has the following advantages:
 * <ul>
 * <li>Support for drawing a separator line by supplying the {@link SWT#SEPARATOR} style.</li>
 * <li>Support for persisting the expansion state by setting the {@link #setPersistenceKey(String) persistence key}.</li>
 * </ul>
 *
 * @see ExpandableComposite
 */
public final class ExpandableCompositeEx extends ExpandableComposite {
    private String persistenceKey;
    private boolean showTextAsTitle;

    public ExpandableCompositeEx(@NotNull Composite parent, int style, int expansionStyle) {
        super(parent, style, expansionStyle);

        if ((getStyle() & SWT.SEPARATOR) == SWT.SEPARATOR) {
            addPaintListener(e -> paintSeparator(e.gc));
        }

        addExpansionListener(IExpansionListener.expansionStateChangedAdapter(e -> persistExpansionState()));
    }

    /**
     * Sets whether the text of this composite should be shown as a title (i.e. in bold font).
     *
     * @param value {@code true} to show the text as a title, {@code false} to show it in normal font
     */
    public void setShowTextAsTitle(boolean value) {
        if (showTextAsTitle != value) {
            showTextAsTitle = value;
            updateFont();
        }
    }

    private void updateFont() {
        setFont(showTextAsTitle ? BaseThemeSettings.instance.baseFontBold : null);
    }

    /**
     * Sets the persistence key for this expandable composite.
     * <p>If set, the expansion state of this composite will be persisted in the dialog
     * settings and restored when the composite is created.
     *
     * @param key the persistence key, or {@code null} to disable persistence
     */
    public void setPersistenceKey(@NotNull String key) {
        persistenceKey = key;
        restoreExpansionState();
    }

    private void restoreExpansionState() {
        if (persistenceKey != null) {
            var settings = UIUtils.getDialogSettings(getClass().getName());
            var expanded = settings.getBoolean(persistenceKey);
            if (isExpanded() != expanded) {
                setExpanded(expanded, true);
            }
        }
    }

    private void persistExpansionState() {
        if (persistenceKey != null) {
            var settings = UIUtils.getDialogSettings(getClass().getName());
            settings.put(persistenceKey, isExpanded());
        }
    }

    private void paintSeparator(@NotNull GC gc) {
        Rectangle bounds = getBounds();
        Rectangle label = textLabel.getBounds();

        gc.setForeground(gc.getDevice().getSystemColor(SWT.COLOR_WIDGET_NORMAL_SHADOW));
        gc.drawLine(
            label.x + label.width + 6,
            label.y + label.height / 2,
            bounds.width,
            label.y + label.height / 2
        );
    }
}
