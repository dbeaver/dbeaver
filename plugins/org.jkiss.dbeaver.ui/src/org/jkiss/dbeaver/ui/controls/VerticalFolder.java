/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
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
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.jkiss.dbeaver.ui.UIUtils;

import java.util.ArrayList;
import java.util.List;

public class VerticalFolder extends Canvas {

    private boolean isLeft;
    private List<VerticalButton> items = new ArrayList<>();
    private VerticalButton selectedItem;

    public VerticalFolder(Composite parent, int style) {
        super(parent, style);

        this.isLeft = (style & SWT.LEFT) == SWT.LEFT;

        GridLayout gl = new GridLayout(1, true);
        gl.horizontalSpacing = 0;
        gl.verticalSpacing = 0;
        gl.marginHeight = 0;
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
        items.add(item);
    }

    public void setSelection(VerticalButton item) {
        selectedItem =  item;
        for (Control child : getChildren()) {
            child.redraw();
        }
        Event event = new Event();
        event.widget = this;
        notifyListeners(SWT.Selection, event);
    }

    public VerticalButton getSelection() {
        return selectedItem;
    }

    public VerticalButton[] getItems() {
        return items.toArray(new VerticalButton[0]);
    }

    public int getItemCount() {
        return items.size();
    }

    public void removeItem(VerticalButton item) {
        this.items.remove(item);
    }

    public void removeAll() {
        UIUtils.disposeChildControls(this);
    }

    public void addVerticalGap() {
        UIUtils.createEmptyLabel(this, 1, 1).setLayoutData(new GridData(GridData.FILL_VERTICAL));
    }

    public void addSelectionListener(SelectionListener listener) {
        addListener(SWT.Selection, event -> listener.widgetSelected(new SelectionEvent(event)));
    }

    @Override
    public void redraw() {
        super.redraw();

        for (VerticalButton b : items) {
            b.redraw();
        }
    }
}