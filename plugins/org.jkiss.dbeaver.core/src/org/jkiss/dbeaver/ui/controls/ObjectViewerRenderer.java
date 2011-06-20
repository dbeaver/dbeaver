/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.controls;

import org.eclipse.jface.viewers.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseMoveListener;
import org.eclipse.swt.events.MouseTrackListener;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.widgets.*;
import org.jkiss.dbeaver.model.DBPNamedObject;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.utils.ImageUtils;

import java.text.Collator;

/**
 * ObjectListControl
 */
public abstract class ObjectViewerRenderer {
    //static final Log log = LogFactory.getLog(ObjectViewerRenderer.class);

    private boolean isTree;
    // Current selection coordinates
    private transient int selectedItem = -1;
    private transient int selectedColumn = -1;

    private ColumnViewer itemsViewer;

    private SortListener sortListener;

    private final TextLayout linkLayout;
    private final Color linkColor;
    private final Cursor linkCursor;
    private final Cursor arrowCursor;

    public ObjectViewerRenderer(
        ColumnViewer viewer)
    {
        itemsViewer = viewer;
        this.isTree = (itemsViewer instanceof AbstractTreeViewer);
        Display display = itemsViewer.getControl().getDisplay();
        this.linkLayout = new TextLayout(display);
        this.linkColor = display.getSystemColor(SWT.COLOR_LIST_SELECTION);
        this.linkCursor = display.getSystemCursor(SWT.CURSOR_HAND);
        this.arrowCursor = display.getSystemCursor(SWT.CURSOR_ARROW);

        itemsViewer.getControl().setCursor(arrowCursor);
        CellTrackListener mouseListener = new CellTrackListener();
        itemsViewer.getControl().addMouseListener(new MouseListener());

        itemsViewer.getControl().addMouseTrackListener(mouseListener);
        itemsViewer.getControl().addMouseMoveListener(mouseListener);

        sortListener = new SortListener();
    }

    public boolean isTree()
    {
        return isTree;
    }

    public SortListener getSortListener()
    {
        return sortListener;
    }

    private TableItem detectTableItem(int x, int y)
    {
        selectedItem = -1;
        selectedColumn = -1;
        Point pt = new Point(x, y);
        TableItem item = getTable().getItem(pt);
        if (item == null) return null;
        selectedColumn = UIUtils.getColumnAtPos(item, x, y);
        selectedItem = getTable().indexOf(item);
        return item;
    }

    private TreeItem detectTreeItem(int x, int y)
    {
        selectedItem = -1;
        selectedColumn = -1;
        Point pt = new Point(x, y);
        TreeItem item = getTree().getItem(pt);
        if (item == null) return null;
        selectedColumn = UIUtils.getColumnAtPos(item, x, y);
        selectedItem = getTree().indexOf(item);
        return item;
    }

    public String getSelectedText()
    {
        if (selectedItem == -1 || selectedColumn == -1) {
            return null;
        }
        if (isTree) {
            if (selectedItem >= getTree().getItemCount()) {
                return null;
            }
            return getTree().getItem(selectedItem).getText(selectedColumn);
        } else {
            if (selectedItem >= getTable().getItemCount()) {
                return null;
            }
            return getTable().getItem(selectedItem).getText(selectedColumn);
        }
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
        prepareLinkStyle(cellValue, linkColor);

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

    public void paintCell(Event event, Object element, int columnIndex, boolean editable) {
        Object cellValue = getCellValue(element, columnIndex);
        if (cellValue != null ) {
            GC gc = event.gc;
            if (cellValue instanceof Boolean) {
                //int columnWidth = UIUtils.getColumnWidth(item);
                int columnHeight = isTree ? getTree().getItemHeight() : getTable().getItemHeight();
                Image image = (Boolean)cellValue ?
                    (editable ? ImageUtils.getImageCheckboxEnabledOn() : ImageUtils.getImageCheckboxDisabledOn()) :
                    (editable ? ImageUtils.getImageCheckboxEnabledOff() : ImageUtils.getImageCheckboxDisabledOff());
                final Rectangle imageBounds = image.getBounds();
                gc.drawImage(image, event.x + 4/*(columnWidth - imageBounds.width) / 2*/, event.y + (columnHeight - imageBounds.height) / 2);
                event.doit = false;
//                            System.out.println("PAINT " + cellValue + " " + System.currentTimeMillis());
            } else if (isHyperlink(cellValue)) {
                boolean isSelected = linkColor.equals(gc.getBackground());
                // Print link
                prepareLinkStyle(cellValue, isSelected ? gc.getForeground() : linkColor);
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

    //////////////////////////////////////////////////////
    // List sorter

    private class SortListener implements Listener
    {
        int sortDirection = SWT.DOWN;
        Item prevColumn = null;

        public void handleEvent(Event e) {
            Collator collator = Collator.getInstance();
            Item column = (Item)e.widget;
            final int colIndex = isTree ? getTree().indexOf((TreeColumn) column) : getTable().indexOf((TableColumn)column );
            if (prevColumn == column) {
                // Set reverse order
                sortDirection = sortDirection == SWT.UP ? SWT.DOWN : SWT.UP;
            }
            prevColumn = column;
            if (isTree) {
                getTree().setSortColumn((TreeColumn) column);
                getTree().setSortDirection(sortDirection);
            } else {
                getTable().setSortColumn((TableColumn) column);
                getTable().setSortDirection(sortDirection);
            }

            itemsViewer.setSorter(new ViewerSorter(collator) {
                public int compare(Viewer viewer, Object e1, Object e2)
                {
                    int result;
                    Object value1 = getCellValue(e1, colIndex);
                    Object value2 = getCellValue(e2, colIndex);
                    if (value1 == null && value2 == null) {
                        result = 0;
                    } else if (value1 == null) {
                        result = -1;
                    } else if (value2 == null) {
                        result = 1;
                    } else if (value1 instanceof Comparable && value1.getClass() == value2.getClass()) {
                        result = ((Comparable)value1).compareTo(value2);
                    } else {
                        result = value1.toString().compareToIgnoreCase(value2.toString());
                    }
                    return sortDirection == SWT.DOWN ? result : -result;
                }
            });
        }
    }

    class CellTrackListener implements MouseTrackListener, MouseMoveListener {

        public void mouseEnter(MouseEvent e)
        {
        }

        public void mouseExit(MouseEvent e)
        {
            resetCursor();
        }

        public void mouseHover(MouseEvent e)
        {
        }

        private void resetCursor()
        {
            getItemsViewer().getControl().setCursor(arrowCursor);
        }

        public void mouseMove(MouseEvent e)
        {
            Item hoverItem;
            if (isTree) {
                hoverItem = detectTreeItem(e.x, e.y);
            } else {
                hoverItem = detectTableItem(e.x, e.y);
            }
            //String tip = null;
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
                    if (isHyperlink(cellValue) && getCellLinkBounds(hoverItem, checkColumn, cellValue).contains(e.x, e.y)) {
                        getItemsViewer().getControl().setCursor(linkCursor);
                    } else {
                        resetCursor();
                    }
                }
            }
            //setToolTipText(tip);
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

        String text = getCellString(cellValue);
        linkLayout.setText(text);
        linkLayout.setIndent(0);
        linkLayout.setStyle(linkStyle, 0, text.length());
    }

    public static String getCellString(Object value)
    {
        if (value == null || value instanceof Boolean) {
            return "";
        }
        if (value instanceof DBPNamedObject) {
            value = ((DBPNamedObject)value).getName();
        }
        return UIUtils.makeStringForUI(value).toString();
    }

    private class MouseListener extends MouseAdapter {

        @Override
        public void mouseDown(MouseEvent e)
        {
            if (isTree) {
                detectTreeItem(e.x, e.y);
            } else {
                detectTableItem(e.x, e.y);
            }
        }

        @Override
        public void mouseUp(MouseEvent e)
        {
            Item hoverItem;
            if (isTree) {
                hoverItem = detectTreeItem(e.x, e.y);
            } else {
                hoverItem = detectTableItem(e.x, e.y);
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

    public boolean isHyperlink(Object cellValue)
    {
        return false;
    }

    public void navigateHyperlink(Object cellValue)
    {
        // do nothing. by default all cells are not navigable
    }

    protected abstract Object getCellValue(Object element, int columnIndex);

}
