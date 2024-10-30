/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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

import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.IToolTipProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.accessibility.ACC;
import org.eclipse.swt.accessibility.Accessible;
import org.eclipse.swt.accessibility.AccessibleAdapter;
import org.eclipse.swt.accessibility.AccessibleEvent;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.ui.UIStyles;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.CustomToolTipHandler;
import org.jkiss.dbeaver.ui.css.CSSUtils;
import org.jkiss.utils.ArrayUtils;
import org.jkiss.utils.CommonUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * AdvancedList
 */
public class AdvancedList extends Canvas {

    private Point itemSize = new Point(64, 64);

    private final List<AdvancedListItem> items = new ArrayList<>();
    private AdvancedListItem selectedItem;
    private AdvancedListItem hoverItem;

    private final Color backgroundColor, selectionBackgroundColor, foregroundColor, selectionForegroundColor, hoverBackgroundColor;
    private final Point textSize;
    private final ScrollBar vScroll;
    private int topRowIndex;
    private int topRowOffset;

    private final CustomToolTipHandler toolTipHandler;

    public AdvancedList(Composite parent, int style) {
        super(parent, SWT.V_SCROLL | SWT.DOUBLE_BUFFERED | style);

        CSSUtils.setCSSClass(this, "List");
        this.backgroundColor = UIStyles.getDefaultTextBackground();
        this.foregroundColor = UIStyles.getDefaultTextForeground();
        this.selectionBackgroundColor = UIStyles.getDefaultTextSelectionBackground();
        this.selectionForegroundColor = UIStyles.getDefaultTextSelectionForeground();
        this.hoverBackgroundColor = UIUtils.getSharedTextColors().getColor(
            UIUtils.blend(this.selectionBackgroundColor.getRGB(), new RGB(255, 255, 255), 70));

        Font normalFont = getFont();
        FontData[] fontData = normalFont.getFontData();
        fontData[0].height -= 1.3;
        Font smallFont = new Font(normalFont.getDevice(), fontData[0]);
        setFont(smallFont);
        addDisposeListener(e -> {
            smallFont.dispose();
            cleanupItems();
        });

        if (parent.getLayout() instanceof GridLayout) {
            setLayoutData(new GridData(GridData.FILL_BOTH));
        }

        this.setBackground(backgroundColor);

        vScroll = getVerticalBar();
        vScroll.setVisible(true);
        vScroll.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                onVerticalScroll();
            }
        });

        GC gc = new GC(getDisplay());
        textSize = gc.stringExtent("X");
        gc.dispose();

        addPaintListener(this::onPaint);

        addListener(SWT.Resize, event -> {
            updateMeasures();
            redraw();
        });

        this.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                switch (e.keyCode) {
                    case SWT.ARROW_LEFT:
                    case SWT.ARROW_RIGHT:
                    case SWT.ARROW_UP:
                    case SWT.ARROW_DOWN:
                    case SWT.PAGE_DOWN:
                    case SWT.PAGE_UP:
                    case SWT.HOME:
                    case SWT.END:
                    case SWT.CR:
                        if (getSelectedItem() != null) {
                            navigateByKey(e);
                        }
                        break;
                }
            }
        });

        this.addMouseMoveListener(this::onMouseMove);
        this.addMouseTrackListener(new MouseTrackAdapter() {
            @Override
            public void mouseEnter(MouseEvent e) {
            }

            @Override
            public void mouseExit(MouseEvent e) {
                onMouseMove(e);
            }

            @Override
            public void mouseHover(MouseEvent e) {
            }
        });
        this.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseDoubleClick(MouseEvent e) {
                notifyDefaultSelection();
            }

            @Override
            public void mouseDown(MouseEvent e) {
                AdvancedListItem item = getItemByPos(e.x, e.y);
                if (item != null) {
                    setSelection(item);
                    setFocus();
                }
            }
        });
        this.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                super.focusGained(e);
                if (getSelectedItem() == null && !items.isEmpty()) {
                    setSelection(items.get(0));
                }
            }
        });
        toolTipHandler = new CustomToolTipHandler(this);

        initAccessible();
    }

    private void cleanupItems() {
        for (AdvancedListItem item : items) {
            item.dispose();
        }
    }

    public void refreshFilters() {
        selectedItem = null;
        hoverItem = null;
        updateMeasures();
    }

    private void onMouseMove(MouseEvent e) {
        AdvancedListItem item = getItemByPos(e.x, e.y);
        if (item == hoverItem) {
            return;
        }
        AdvancedListItem[] redrawItems = new AdvancedListItem[] { item, hoverItem };
        hoverItem = item;
        if (item == null) {
            toolTipHandler.updateToolTipText(null);
        } else {
            ILabelProvider labelProvider = item.getLabelProvider();
            if (labelProvider instanceof IToolTipProvider) {
                String toolTipText = ((IToolTipProvider) labelProvider).getToolTipText(item.getData());
                if (!CommonUtils.isEmpty(toolTipText)) {
                    toolTipHandler.updateToolTipText(toolTipText);
                }
            }
        }
        GC gc = new GC(this);
        try {
            paintList(gc, redrawItems);
        } finally {
            gc.dispose();
        }
    }

    private AdvancedListItem getItemByPos(int x, int y) {
        Point size = getSize();
        if (x < 0 || y < 0 || x >= size.x || y >= size.y) {
            return null;
        }
        Point itemSize = getItemSize();
        int row = topRowIndex + (y + topRowOffset) / itemSize.y;
        int col = x / itemSize.x;
        if (col < 0 || col >= getItemsPerRow()) {
            return null;
        }
        int itemIndex = row * getItemsPerRow() + col;
        return itemIndex < items.size() ? items.get(itemIndex) : null;
    }

    private void onVerticalScroll() {
        redraw();
    }

    private void updateMeasures() {
        int itemsPerRow = getItemsPerRow();
        int totalRows = itemsPerRow == 0 ? 0 : (items.size() / itemsPerRow) + 2;
        int itemHeight = getItemSize().y;
        int visibleRowCount = getVisibleRowCount();

        vScroll.setValues(0, 0, totalRows * itemHeight, visibleRowCount * itemHeight, itemHeight / 2, itemHeight);
        vScroll.setVisible(totalRows * itemHeight > getSize().y);
    }

    private void onPaint(PaintEvent e) {
        paintList(e.gc, null);
    }

    private void paintList(@NotNull GC gc, @Nullable AdvancedListItem[] redrawItems) {
        Point itemSize = getItemSize();
        int itemsPerRow = getItemsPerRow();
        int itemRowsVisible = getVisibleRowCount() + 1;

        topRowOffset = vScroll.getSelection();
        topRowIndex = topRowOffset / itemSize.y;
        topRowOffset = topRowOffset - topRowIndex * itemSize.y;

        int topItemIndex = topRowIndex * itemsPerRow;

        //int drawItemCount = itemRowsVisible * itemsPerRow;
        int x = 0, y = -topRowOffset;
        for (int i = 0; i < itemRowsVisible; i++) {
            if (topItemIndex + i * itemsPerRow >= items.size()) {
                break;
            }
            for (int k = 0; k < itemsPerRow; k++) {
                int itemIndex = topItemIndex + i * itemsPerRow + k;
                if (itemIndex >= items.size()) {
                    break;
                }
                AdvancedListItem item = items.get(itemIndex);
                if (redrawItems == null || ArrayUtils.contains(redrawItems, item)) {
                    item.painItem(gc, x, y);
                }

                x += itemSize.x;
            }
            y += itemSize.y;
            x = 0;
        }
    }

    private int getVisibleRowCount() {
        return getSize().y / getItemSize().y + 1;
    }

    Point getItemSize() {
        Point imageSize = getImageSize();
        int itemLength = imageSize.x + AdvancedListItem.BORDER_MARGIN * 4 + getTextSize().y;
        return new Point(itemLength, itemLength + AdvancedListItem.BORDER_MARGIN * 2);
    }

    private int getItemsPerRow() {
        Point itemSize = getItemSize();
        Point containerSize = getSize();
        return Math.floorDiv(containerSize.x, itemSize.x);
    }

    private void navigateByKey(KeyEvent e) {
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
            case SWT.PAGE_UP:
                itemIndex -= itemsPerRow * getVisibleRowCount();
                if (itemIndex < 0) itemIndex = 0;
                setSelection(items.get(itemIndex));
                break;
            case SWT.PAGE_DOWN:
                itemIndex += itemsPerRow * getVisibleRowCount();
                if (itemIndex > items.size() - 1) itemIndex = items.size() - 1;
                setSelection(items.get(itemIndex));
                break;
            case SWT.HOME:
                if (!items.isEmpty()) {
                    setSelection(items.get(0));
                }
                break;
            case SWT.END:
                if (!items.isEmpty()) {
                    setSelection(items.get(items.size() - 1));
                }
                break;
            case SWT.CR:
                notifyDefaultSelection();
                break;
        }
        showItem(selectedItem);
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

    AdvancedListItem getHoverItem() {
        return hoverItem;
    }

    public AdvancedListItem getSelectedItem() {
        return selectedItem;
    }

    void setSelection(AdvancedListItem item) {
        if (this.selectedItem == item) {
            return;
        }
        this.selectedItem = item;

        Event event = new Event();
        event.widget = this;
        notifyListeners(SWT.Selection, event);

        redraw();

        if (isFocusControl() && item != null) {
            //getAccessible().sendEvent(ACC.EVENT_SELECTION_CHANGED, new Object[]{null, item.getData()});
            getAccessible().sendEvent(ACC.EVENT_NAME_CHANGED, new Object[]{null, item.getData()});
        }
    }

    void notifyDefaultSelection() {
        Event event = new Event();
        event.widget = this;
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
        cleanupItems();
        items.clear();
    }

    private void showItem(AdvancedListItem item) {
        if (!vScroll.isVisible()) {
            return;
        }
        int itemIndex = items.indexOf(item);
        int itemRow = itemIndex / getItemsPerRow();
        if (itemRow < topRowIndex) {
            // Scroll up
            if (itemRow < topRowIndex - 1) {
                // To exact row
                vScroll.setSelection(itemRow * getItemSize().y);
            } else {
                // One row up
                vScroll.setSelection(Math.max(0, vScroll.getSelection() - vScroll.getPageIncrement()));
            }
        } else {
            int bottomRowIndex = topRowIndex + getVisibleRowCount();
            if (itemRow >= bottomRowIndex) {
                // Scroll down
                if (itemRow > bottomRowIndex + 1) {
                    // Scroll to exact row
                    vScroll.setSelection(itemRow * getItemSize().y);
                } else {
                    // Just one row down
                    vScroll.setSelection(vScroll.getSelection() + vScroll.getPageIncrement());
                }
            }
        }
    }

    private void initAccessible() {
        final Accessible accessible = getAccessible();
        accessible.addAccessibleListener(new AccessibleAdapter() {

            public void getName(AccessibleEvent e) {
                AdvancedListItem item = getSelectedItem();
                if (item != null) {
                    e.result = item.getLabelProvider().getText(item.getData());
                }
            }
        });
    }

}
