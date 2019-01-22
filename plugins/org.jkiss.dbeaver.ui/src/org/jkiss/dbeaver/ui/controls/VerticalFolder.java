/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2019 Serge Rider (serge@jkiss.org)
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
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;

public class VerticalFolder extends Canvas {

    private boolean isLeft;
    private VerticalButton selectedItem;

    public VerticalFolder(Composite parent, int style) {
        super(parent, style);

        this.isLeft = (style & SWT.LEFT) == SWT.LEFT;

        GridLayout gl = new GridLayout(1, true);
        gl.horizontalSpacing = 0;
        gl.verticalSpacing = 0;
        gl.marginWidth = 2;
        this.setLayout(gl);
    }

    public boolean isLeft() {
        return isLeft;
    }

    @Override
    public Point computeSize(int wHint, int hHint, boolean changed) {
        Point size = super.computeSize(wHint, hHint, changed);
        return size;
    }

    public void addItem(VerticalButton item) {
        item.addListener(SWT.Selection, event -> {
            setSelection((VerticalButton)event.widget);
        });
    }

    private void setSelection(VerticalButton item) {
        selectedItem =  item;
        for (Control child : getChildren()) {
            child.redraw();
        }
    }

    public VerticalButton getSelection() {
        return selectedItem;
    }
}