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
package org.jkiss.dbeaver.ui;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;

public class CompositeBorderPainter implements PaintListener {
    private final Control control;

    public CompositeBorderPainter(Control control) {
        this.control = control;
        this.control.addPaintListener(this);
    }

    @Override
    public void paintControl(PaintEvent e) {
        Rectangle bounds = control.getBounds();
        e.gc.setForeground(Display.getDefault().getSystemColor(
            UIStyles.isDarkTheme() ? SWT.COLOR_WIDGET_NORMAL_SHADOW : SWT.COLOR_WIDGET_NORMAL_SHADOW));
        e.gc.drawRectangle(0, 0, bounds.width - 1, bounds.height - 1);
    }
}
