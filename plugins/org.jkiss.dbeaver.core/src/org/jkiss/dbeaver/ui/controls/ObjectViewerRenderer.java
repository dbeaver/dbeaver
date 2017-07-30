/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
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

import org.eclipse.jface.resource.JFaceColors;
import org.eclipse.jface.viewers.AbstractTreeViewer;
import org.eclipse.jface.viewers.ColumnViewer;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.widgets.*;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.DBPNamedObject;
import org.jkiss.dbeaver.model.DBPNamedValueObject;
import org.jkiss.dbeaver.ui.ImageUtils;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.utils.GeneralUtils;

/**
 * ObjectListControl
 */
public abstract class ObjectViewerRenderer {

    public static final int ES_CENTERED = 1;
    public static final int ES_LEFT = 2;

    private boolean isTree;
    // Current selection coordinates
    private transient Item selectedItem;
    private transient int selectedColumn = -1;

    private ColumnViewer itemsViewer;

    private final TextLayout linkLayout;
    //private final Color linkColor;
    private final Cursor linkCursor;
    private final Cursor arrowCursor;
    private final Color selectionBackgroundColor;

    public ObjectViewerRenderer(
        ColumnViewer viewer)
    {
        itemsViewer = viewer;
        this.isTree = (itemsViewer instanceof AbstractTreeViewer);
        Display display = itemsViewer.getControl().getDisplay();
        this.linkLayout = new TextLayout(display);
        this.selectionBackgroundColor = display.getSystemColor(SWT.COLOR_LIST_SELECTION);
        //this.linkColor = display.getSystemColor(SWT.COLOR_BLUE);
        this.linkCursor = display.getSystemCursor(SWT.CURSOR_HAND);
        this.arrowCursor = display.getSystemCursor(SWT.CURSOR_ARROW);

        itemsViewer.getControl().setCursor(arrowCursor);

        final CellTrackListener actionsListener = new CellTrackListener();
        itemsViewer.getControl().addMouseListener(new MouseListener());
        itemsViewer.getControl().addMouseTrackListener(actionsListener);
        itemsViewer.getControl().addMouseMoveListener(actionsListener);
        itemsViewer.getControl().addKeyListener(actionsListener);
    }

    public boolean isTree()
    {
        return isTree;
    }

    private TableItem detectTableItem(int x, int y)
    {
        selectedItem = null;
        selectedColumn = -1;
        Point pt = new Point(x, y);
        selectedItem = getTable().getItem(pt);
        if (selectedItem == null) return null;
        selectedColumn = UIUtils.getColumnAtPos((TableItem) selectedItem, x, y);
        return (TableItem) selectedItem;
    }

    private TreeItem detectTreeItem(int x, int y)
    {
        selectedItem = null;
        selectedColumn = -1;
        Point pt = new Point(x, y);
        selectedItem = getTree().getItem(pt);
        if (selectedItem == null) {
            return null;
        }
        selectedColumn = UIUtils.getColumnAtPos((TreeItem) selectedItem, x, y);
        return (TreeItem) selectedItem;
    }

    public String getSelectedText()
    {
        if (selectedItem == null || selectedColumn == -1) {
            return null;
        }
        Object cellValue = getCellValue(selectedItem.getData(), selectedColumn);
        return getCellString(cellValue, false);
    }

    protected ColumnViewer getItemsViewer()
    {
        return itemsViewer;
    }

    public Composite getControl()
    {
        // Both table and tree are composites so its ok
        return (Composite) itemsViewer.getControl();
    }

    public void dispose()
    {
        UIUtils.dispose(linkLayout);
    }

    private Tree getTree()
    {
        return ((TreeViewer)itemsViewer).getTree();
    }

    private Table getTable()
    {
        return ((TableViewer)itemsViewer).getTable();
    }

    private Rectangle getCellLinkBounds(Item item, int column, Object cellValue) {
        prepareLinkStyle(cellValue, null);

        Rectangle itemBounds;
        if (isTree) {
            itemBounds = ((TreeItem)item).getTextBounds(column);
        } else {
            itemBounds = ((TableItem)item).getTextBounds(column);
        }

        Rectangle linkBounds = linkLayout.getBounds();
        linkBounds.x += itemBounds.x;
        linkBounds.y += itemBounds.y + 1;
        linkBounds.height -= 2;

        return linkBounds;
    }

    //////////////////////////////////////////////////////
    // List sorter

    public void paintCell(Event event, Object element, Widget item, int columnIndex, boolean editable, boolean selected) {
        Object cellValue = getCellValue(element, columnIndex);
        if (cellValue != null ) {
            GC gc = event.gc;
            if (cellValue instanceof Boolean) {
                Image image = editable ?
                    ((Boolean)cellValue ? ImageUtils.getImageCheckboxEnabledOn() : ImageUtils.getImageCheckboxEnabledOff()) :
                    ((Boolean)cellValue ? ImageUtils.getImageCheckboxDisabledOn() : ImageUtils.getImageCheckboxDisabledOff());
                final Rectangle imageBounds = image.getBounds();

                Rectangle columnBounds = isTree ? ((TreeItem)item).getBounds(columnIndex) : ((TableItem)item).getBounds(columnIndex);

                if (getBooleanEditStyle() == ES_CENTERED) {
                    gc.drawImage(image, event.x + (columnBounds.width - imageBounds.width) / 2, event.y + 2);
                } else {
                    gc.drawImage(image, event.x/* + 4*/, event.y + (columnBounds.height - imageBounds.height) / 2);
                }

                event.doit = false;

            } else if (isHyperlink(cellValue)) {
                // Print link
                prepareLinkStyle(cellValue, selected ? gc.getForeground() : JFaceColors.getHyperlinkText(event.item.getDisplay()));
                Rectangle textBounds;
                if (event.item instanceof TreeItem) {
                    textBounds = ((TreeItem) event.item).getTextBounds(event.index);
                } else {
                    textBounds = ((TableItem) event.item).getTextBounds(event.index);
                }
                linkLayout.draw(gc, textBounds.x, textBounds.y + 1);
            }
        }
    }

    protected int getBooleanEditStyle() {
        return ES_CENTERED;
    }

    class CellTrackListener implements MouseTrackListener, MouseMoveListener, KeyListener {

        @Override
        public void mouseEnter(MouseEvent e)
        {
        }

        @Override
        public void mouseExit(MouseEvent e)
        {
            resetCursor();
        }

        @Override
        public void mouseHover(MouseEvent e)
        {
        }

        private void resetCursor()
        {
            getItemsViewer().getControl().setCursor(arrowCursor);
        }

        @Override
        public void mouseMove(MouseEvent e)
        {
            updateCursor(e.x, e.y, e.stateMask);
        }

        private void updateCursor(int x, int y, int stateMask) {
            Item hoverItem;
            if (isTree) {
                hoverItem = detectTreeItem(x, y);
            } else {
                hoverItem = detectTableItem(x, y);
            }

            if (hoverItem == null || selectedColumn < 0) {
                resetCursor();
            } else {
                Object element = hoverItem.getData();

                int checkColumn = selectedColumn;
                Object cellValue = getCellValue(element, checkColumn);
                if (cellValue == null) {
                    resetCursor();
                } else {
                    //tip = getCellString(cellValue);
                    boolean ctrlPRessed = (stateMask & SWT.CTRL) != 0 || (stateMask & SWT.ALT) != 0;
                    if (ctrlPRessed && isHyperlink(cellValue) && getCellLinkBounds(hoverItem, checkColumn, cellValue).contains(x, y)) {
                        getItemsViewer().getControl().setCursor(linkCursor);
                    } else {
                        resetCursor();
                    }
                }
            }
        }

        @Override
        public void keyPressed(KeyEvent e) {
            if (e.keyCode == SWT.CTRL || e.keyCode == SWT.ALT) {
                Point mousePoint = itemsViewer.getControl().getDisplay().getCursorLocation();
                mousePoint = itemsViewer.getControl().getDisplay().map(null, itemsViewer.getControl(), mousePoint);
                updateCursor(mousePoint.x, mousePoint.y, e.keyCode);
            }
        }

        @Override
        public void keyReleased(KeyEvent e) {
            if (e.keyCode == SWT.CTRL || e.keyCode == SWT.ALT) {
                Point mousePoint = itemsViewer.getControl().getDisplay().getCursorLocation();
                mousePoint = itemsViewer.getControl().getDisplay().map(null, itemsViewer.getControl(), mousePoint);
                updateCursor(mousePoint.x, mousePoint.y, SWT.NONE);
            }
        }
    }

    private void prepareLinkStyle(Object cellValue, Color foreground)
    {
        // Print link
        TextStyle linkStyle = new TextStyle(
            getControl().getFont(),
            foreground,
            null);
        linkStyle.underline = true;
        linkStyle.underlineStyle = SWT.UNDERLINE_LINK;

        String text = getCellString(cellValue, false);
        linkLayout.setText(text);
        linkLayout.setIndent(0);
        linkLayout.setStyle(linkStyle, 0, text.length());
    }

    public static String getCellString(@Nullable Object value, boolean nameColumn)
    {
        if (value == null) {
            return "";
        } else {
            if (!nameColumn && value instanceof DBPNamedValueObject) {
                value = ((DBPNamedValueObject) value).getObjectValue();
            } else if (value instanceof DBPNamedObject) {
                value = ((DBPNamedObject) value).getName();
            }
        }
        return GeneralUtils.makeDisplayString(value).toString();
    }

    private class MouseListener extends MouseAdapter {

        @Override
        public void mouseDown(MouseEvent e)
        {
            Item hoverItem;
            if (isTree) {
                hoverItem = detectTreeItem(e.x, e.y);
            } else {
                hoverItem = detectTableItem(e.x, e.y);
            }
            if ((e.stateMask & SWT.CTRL) == 0 && (e.stateMask & SWT.ALT) == 0) {
                // Navigate only if CTRL pressed
                return;
            }
            if (hoverItem != null && selectedColumn >= 0 && e.button == 1) {
                Object element = hoverItem.getData();
                int checkColumn = selectedColumn;
                Object cellValue = getCellValue(element, checkColumn);
                if (isHyperlink(cellValue) && getCellLinkBounds(hoverItem, checkColumn, cellValue).contains(e.x, e.y)) {
                    navigateHyperlink(cellValue);
                }
            }
        }

    }

    public boolean isHyperlink(@Nullable Object cellValue)
    {
        return false;
    }

    public void navigateHyperlink(Object cellValue)
    {
        // do nothing. by default all cells are not navigable
    }

    @Nullable
    protected abstract Object getCellValue(Object element, int columnIndex);

}
