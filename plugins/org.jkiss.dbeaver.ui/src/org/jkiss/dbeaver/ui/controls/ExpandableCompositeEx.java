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
import org.eclipse.ui.forms.widgets.ExpandableComposite;
import org.jkiss.code.NotNull;

/**
 * An extension of ExpandableComposite that supports drawing a separator line
 * by supplying the {@link SWT#SEPARATOR} style.
 *
 * @see ExpandableComposite
 */
public final class ExpandableCompositeEx extends ExpandableComposite {
    public ExpandableCompositeEx(@NotNull Composite parent, int style, int expansionStyle) {
        super(parent, style, expansionStyle);

        if ((getStyle() & SWT.SEPARATOR) == SWT.SEPARATOR) {
            addPaintListener(e -> paintSeparator(e.gc));
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
