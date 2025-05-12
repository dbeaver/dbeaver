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
package org.jkiss.dbeaver.ui.controls;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.jkiss.dbeaver.ui.UIStyles;

/**
 * Line separator
 */
public class LineSeparator extends Composite {

    public LineSeparator(Composite parent, int style) {
        super(parent, SWT.NONE);
        if (parent.getLayout() instanceof GridLayout) {
            setLayoutData(GridDataFactory.fillDefaults()
                .grab(true, false).hint(-1, 1).create());
        }
        addPaintListener(e -> {
            Point size = LineSeparator.this.getSize();
            e.gc.setBackground(getDisplay().getSystemColor(
                UIStyles.isDarkTheme() ? SWT.COLOR_WIDGET_NORMAL_SHADOW : SWT.COLOR_WIDGET_NORMAL_SHADOW));
            e.gc.fillRectangle(0, 0, size.x, size.y);
        });
    }
}
