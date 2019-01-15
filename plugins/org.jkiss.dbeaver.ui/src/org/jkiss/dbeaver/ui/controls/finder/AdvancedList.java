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
package org.jkiss.dbeaver.ui.controls.finder;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.ScrollBar;
import org.jkiss.dbeaver.Log;

import java.util.ArrayList;
import java.util.List;

/**
 * AdvancedList
 */
public class AdvancedList extends Canvas {
    private static final Log log = Log.getLog(AdvancedList.class);

    private Point itemSize = new Point(80, 80);

    private List<AdvancedListItem> items = new ArrayList<>();

    public AdvancedList(Composite parent, int style) {
        super(parent, style);

        setBackground(getDisplay().getSystemColor(SWT.COLOR_LIST_BACKGROUND));
        RowLayout layout = new RowLayout(SWT.HORIZONTAL);
        layout.wrap = true;
        layout.fill = true;
        layout.marginHeight = 10;
        layout.spacing = 10;
        setLayout(layout);

        ScrollBar verticalBar = getVerticalBar();
        if (verticalBar != null) {
            verticalBar.addSelectionListener(new SelectionAdapter() {
                public void widgetSelected(SelectionEvent event) {
                    scrollVertical();
                }
            });
        }
    }

    public Point getImageSize() {
        return itemSize;
    }

    public void setItemSize(Point itemSize) {
        this.itemSize = itemSize;
    }

    void addItem(AdvancedListItem item) {
        items.add(item);
    }

    public AdvancedListItem[] getItems() {
        return items.toArray(new AdvancedListItem[0]);
    }

    protected void scrollVertical() {
/*
        int areaHeight = getClientArea().height;

        if (gHeight > areaHeight) {
            // image is higher than client area
            ScrollBar bar = getVerticalBar();
            scroll(0, translate - bar.getSelection(), 0, 0,
                getClientArea().width, areaHeight, false);
            translate = bar.getSelection();
        } else {
            translate = 0;
        }
*/
    }

    void paintIcon(GC gc, int x, int y, int width, int height, Label iconLabel, Image icon) {
        Rectangle bounds = icon.getBounds();

        gc.setAntialias(SWT.ON);
        gc.setInterpolation(SWT.HIGH);
        gc.setBackground(getDisplay().getSystemColor(SWT.COLOR_LIST_BACKGROUND));
        gc.drawImage(icon, 0, 0, bounds.width, bounds.height, 0, 0, itemSize.x, itemSize.y);
    }
}
