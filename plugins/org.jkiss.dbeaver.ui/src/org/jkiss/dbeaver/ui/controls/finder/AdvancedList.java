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
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.*;
import org.jkiss.dbeaver.Log;

import java.util.ArrayList;
import java.util.List;

/**
 * AdvancedList
 */
public class AdvancedList extends ScrolledComposite {
    private static final Log log = Log.getLog(AdvancedList.class);
    public static final int ITEM_SPACING = 5;

    private Point itemSize = new Point(64, 64);

    private Canvas container;
    private List<AdvancedListItem> items = new ArrayList<>();
    private AdvancedListItem selectedItem;

    private Color backgroundColor, selectionBackgroundColor, foregroundColor, selectionForegroundColor, hoverBackgroundColor;
    private final Point textSize;

    public AdvancedList(Composite parent, int style) {
        super(parent, SWT.V_SCROLL | style);

        //CSSUtils.setCSSClass(this, "Table");
        this.backgroundColor = getDisplay().getSystemColor(SWT.COLOR_LIST_BACKGROUND);
        this.selectionBackgroundColor = getDisplay().getSystemColor(SWT.COLOR_LIST_SELECTION);
        this.foregroundColor = getDisplay().getSystemColor(SWT.COLOR_LIST_FOREGROUND);
        this.selectionForegroundColor = getDisplay().getSystemColor(SWT.COLOR_LIST_SELECTION_TEXT);
        this.hoverBackgroundColor = getDisplay().getSystemColor(SWT.COLOR_WIDGET_LIGHT_SHADOW);

        if (parent.getLayout() instanceof GridLayout) {
            setLayoutData(new GridData(GridData.FILL_BOTH));
        }

        this.container = new Canvas(this, SWT.NONE);

        this.setContent(this.container);
        this.setExpandHorizontal( true );
        this.setExpandVertical( true );
        //this.setShowFocusedControl( true );
        //scrolledComposite.setAlwaysShowScrollBars(true);
        this.setMinSize( 10, 10 );

        this.addListener( SWT.Resize, event -> {
            updateSize(false);
        } );

        this.setBackground(backgroundColor);
        this.container.setBackground(getBackground());

        RowLayout layout = new RowLayout(SWT.HORIZONTAL);
        layout.wrap = true;
        layout.fill = true;
        layout.marginHeight = 0;
        layout.spacing = ITEM_SPACING;
        this.container.setLayout(layout);

        GC gc = new GC(getDisplay());
        textSize = gc.stringExtent("X");
        gc.dispose();

        container.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                super.focusGained(e);
            }
        });
    }

    void navigateByKey(KeyEvent e) {
        if (selectedItem == null) {
            return;
        }
        int itemIndex = items.indexOf(selectedItem);
        int itemsPerRow = getItemsPerRow();
        switch (e.keyCode) {
            case SWT.ARROW_LEFT:
                if (itemIndex > 0) {
                    setSelection(items.get(itemIndex - 1));
                }
                break;
            case SWT.ARROW_RIGHT:
                if (itemIndex < items.size() - 1) {
                    setSelection(items.get(itemIndex + 1));
                }
                break;
            case SWT.ARROW_UP:
                if (itemIndex >= itemsPerRow) {
                    setSelection(items.get(itemIndex - itemsPerRow));
                }
                break;
            case SWT.ARROW_DOWN:
                if (itemIndex < items.size() - 1) {
                    int nextIndex = itemIndex + itemsPerRow;
                    if (nextIndex >= items.size() - 1) {
                        nextIndex = items.size() - 1;
                    }
                    setSelection(items.get(nextIndex));
                }
                break;
            case SWT.CR:
                notifyDefaultSelection();
                break;
        }
        showItem(selectedItem);
    }

    private int getItemsPerRow() {
        Point itemSize = selectedItem.getSize();
        Point containerSize = container.getSize();
        return Math.floorDiv(containerSize.x, itemSize.x);
    }

    public void updateSize(boolean layout) {
        this.setMinHeight(10);
        int width = this.getClientArea().width;
        this.setMinHeight( getParent().computeSize( width, SWT.DEFAULT ).y );
        if (layout) {
            this.layout(true, true);
        }
    }

    Color getBackgroundColor() {
        return backgroundColor;
    }

    Color getSelectionBackgroundColor() {
        return selectionBackgroundColor;
    }

    Color getForegroundColor() {
        return foregroundColor;
    }

    Color getSelectionForegroundColor() {
        return selectionForegroundColor;
    }

    Color getHoverBackgroundColor() {
        return hoverBackgroundColor;
    }

    Point getTextSize() {
        return textSize;
    }

    @Override
    public Point computeSize(int wHint, int hHint) {
        return computeSize(wHint, hHint, false);
    }

    @Override
    public Point computeSize(int wHint, int hHint, boolean changed) {
        if (wHint == SWT.DEFAULT && hHint == SWT.DEFAULT) {
            return new Point(100, 100);
        }
        return super.computeSize(wHint, hHint, changed);
/*
        // Do not calc real size because RowLayout will fill to maximum screen width
        if (wHint == SWT.DEFAULT && hHint == SWT.DEFAULT) {
            //return getParent().getSize();
            return super.computeSize(wHint, hHint, changed);
        }
        return new Point(wHint, hHint);
*/
    }

    public Canvas getContainer() {
        return container;
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

    void removeItem(AdvancedListItem item) {
        this.items.remove(item);
    }

    public AdvancedListItem[] getItems() {
        return items.toArray(new AdvancedListItem[0]);
    }

    public AdvancedListItem getSelectedItem() {
        return selectedItem;
    }

    void setSelection(AdvancedListItem item) {
        if (this.selectedItem == item) {
            return;
        }
        AdvancedListItem oldSelection = this.selectedItem;
        this.selectedItem = item;
        if (oldSelection != null) {
            oldSelection.redraw();
        }
        if (item != null) {
            item.redraw();
        }

        Event event = new Event();
        event.widget = item;
        notifyListeners(SWT.Selection, event);
    }

    void notifyDefaultSelection() {
        Event event = new Event();
        event.widget = selectedItem;
        notifyListeners(SWT.DefaultSelection, event);
    }

    public void addSelectionListener(SelectionListener listener) {
        checkWidget ();
        if (listener == null) {
            return;
        }
        TypedListener typedListener = new TypedListener (listener);
        addListener (SWT.Selection,typedListener);
        addListener (SWT.DefaultSelection,typedListener);
    }

    public void removeAll() {
        checkWidget ();
        setSelection(null);
        for (AdvancedListItem item : items.toArray(new AdvancedListItem[0])) {
            item.dispose();
        }
        items.clear(); // Just in case
        this.setMinSize( 10, 10 );
    }

    private void showItem(AdvancedListItem item) {
        showControl(item);
/*
        Point itemSize = item.getSize();

        int itemsPerRow = getItemsPerRow();
        int itemIndex = items.indexOf(item);
        int rowNumber = itemIndex / itemsPerRow;

        int vertOffset = (itemSize.y + ITEM_SPACING) * rowNumber;

        ScrollBar verticalBar = getVerticalBar();
        verticalBar.setSelection(vertOffset);
        layout(true);
*/
    }

}
