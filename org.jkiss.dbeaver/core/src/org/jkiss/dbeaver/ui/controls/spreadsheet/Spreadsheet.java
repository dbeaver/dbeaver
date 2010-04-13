/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.controls.spreadsheet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.GroupMarker;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.commands.ActionHandler;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.handlers.IHandlerActivation;
import org.eclipse.ui.handlers.IHandlerService;
import org.eclipse.ui.texteditor.ITextEditorActionDefinitionIds;
import org.eclipse.ui.texteditor.IWorkbenchActionDefinitionIds;
import org.jkiss.dbeaver.ui.controls.grid.Grid;
import org.jkiss.dbeaver.ui.controls.grid.GridEditor;
import org.jkiss.dbeaver.ui.controls.grid.GridColumn;
import org.jkiss.dbeaver.ui.controls.grid.GridItem;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * ResultSetControl
 */
public class Spreadsheet extends Composite implements Listener {
    static Log log = LogFactory.getLog(Spreadsheet.class);

    public static final int MAX_DEF_COLUMN_WIDTH = 300;
    public static final int MAX_INLINE_EDIT_WITH = 300;

    private static final int Event_ChangeCursor = 1000;

    private Grid grid;
    private GridEditor tableEditor;
    private List<GridColumn> curColumns = new ArrayList<GridColumn>();

    //private GridPanel gridPanel;

    private IWorkbenchPartSite site;
    private IGridDataProvider dataProvider;
    private GridSelectionProvider selectionProvider;

    private Clipboard clipboard;
    private ActionInfo[] actionsInfo;

    private Color foregroundNormal;
    private Color foregroundLines;
    private Color foregroundSelected;
    private Color backgroundModified;
    private Color backgroundNormal;
    private Color backgroundControl;
    private Color backgroundSelected;

    private transient LazyGridRow lazyRow;
    private SelectionListener gridSelectionListener;

    public Spreadsheet(Composite parent, int style, IWorkbenchPartSite site, IGridDataProvider dataProvider)
    {
        super(parent, SWT.NONE);
        GridLayout layout = new GridLayout(1, true);
        layout.numColumns = 1;
        layout.makeColumnsEqualWidth = false;
        layout.marginWidth = 0;
        this.setLayout(layout);

        this.site = site;
        this.dataProvider = dataProvider;
        this.selectionProvider = new GridSelectionProvider(this);

        foregroundNormal = getDisplay().getSystemColor(SWT.COLOR_LIST_FOREGROUND);
        foregroundLines = getDisplay().getSystemColor(SWT.COLOR_GRAY);
        foregroundSelected = getDisplay().getSystemColor(SWT.COLOR_LIST_SELECTION_TEXT);
        backgroundModified = new Color(getDisplay(), 0xFF, 0xE4,
                                       0xB5);//getDisplay().getSystemColor(SWT.COLOR_DARK_RED);
        backgroundNormal = getDisplay().getSystemColor(SWT.COLOR_LIST_BACKGROUND);
        backgroundSelected = getDisplay().getSystemColor(SWT.COLOR_LIST_SELECTION);
        backgroundControl = getDisplay().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND);

        clipboard = new Clipboard(getDisplay());

        actionsInfo = new ActionInfo[]{
            new ActionInfo(new GridAction(IWorkbenchActionDefinitionIds.COPY) {
                public void run()
                {
                    copySelectionToClipboard();
                }
            }),
            new ActionInfo(new CursorMoveAction(ITextEditorActionDefinitionIds.LINE_START)),
            new ActionInfo(new CursorMoveAction(ITextEditorActionDefinitionIds.LINE_END)),
            new ActionInfo(new CursorMoveAction(ITextEditorActionDefinitionIds.TEXT_START)),
            new ActionInfo(new CursorMoveAction(ITextEditorActionDefinitionIds.TEXT_END)),
            new ActionInfo(new GridAction(ITextEditorActionDefinitionIds.SELECT_ALL) {
                public void run()
                {
                    grid.selectAll();
                }
            }),
        };

        this.createControl(style);
    }

    public Grid getGrid()
    {
        return grid;
    }

    public Color getForegroundNormal()
    {
        return foregroundNormal;
    }

    public Color getForegroundLines()
    {
        return foregroundLines;
    }

    public Color getForegroundSelected()
    {
        return foregroundSelected;
    }

    public void setForegroundSelected(Color foregroundSelected)
    {
        this.foregroundSelected = foregroundSelected;
        this.grid.redraw();
    }

    public Color getBackgroundNormal()
    {
        return backgroundNormal;
    }

    public Color getBackgroundControl()
    {
        return backgroundControl;
    }

    public Color getBackgroundSelected()
    {
        return backgroundSelected;
    }

    public void setBackgroundSelected(Color backgroundSelected)
    {
        this.backgroundSelected = backgroundSelected;
        this.grid.redraw();
    }

    public void setFont(Font font)
    {
        grid.setFont(font);
        //gridPanel.setFont(font);
    }

    public List<GridPos> getSelection()
    {
        return Collections.emptyList();
    }

    public Point getCursorPosition()
    {
        if (grid.isDisposed() || grid.getItemCount() <= 0 || grid.getColumnCount() <= 0) {
            return new Point(-1, -1);
        }
        return grid.getFocusCell();
    }

    public void setRowHeaderWidth(int width)
    {
        grid.setItemHeaderWidth(width);
    }

    public void shiftCursor(int xOffset, int yOffset, boolean keepSelection)
    {
        if (xOffset == 0 && yOffset == 0) {
            return;
        }
        Point curPos = getCursorPosition();
        if (curPos == null) {
            return;
        }
        Point newPos = new Point(curPos.x, curPos.y);
        Event fakeEvent = new Event();
        fakeEvent.widget = grid;
        SelectionEvent selectionEvent = new SelectionEvent(fakeEvent);
        // Move row
        if (yOffset != 0) {
            int newRow = curPos.y + yOffset;
            if (newRow < 0) {
                newRow = 0;
            }
            if (newRow >= getItemCount()) {
                newRow = getItemCount() - 1;
            }
            newPos.y = newRow;
            GridItem item = grid.getItem(newRow);
            if (item != null) {
                selectionEvent.item = item;
                grid.setFocusItem(item);
                grid.showItem(item);
            }
        }
        // Move column
        if (xOffset != 0) {
            int newCol = curPos.x + xOffset;
            if (newCol < 0) {
                newCol = 0;
            }
            if (newCol >= getColumnsCount()) {
                newCol = getColumnsCount() - 1;
            }
            newPos.x = newCol;
            GridColumn column = grid.getColumn(newCol);
            if (column != null) {
                grid.setFocusColumn(column);
                grid.showColumn(column);
            }
        }
        if (!keepSelection) {
            grid.deselectAll();
        }
        grid.selectCell(newPos);
        //spreadsheet.s
        grid.redraw();

        // Change selection event
        selectionEvent.x = newPos.x;
        selectionEvent.y = newPos.y;
        gridSelectionListener.widgetSelected(selectionEvent);
/*
        if (currentPosition == null) {
            currentPosition = cursorPosition;
        }
        if (newCol != currentPosition.col || newRow != currentPosition.row) {
            changeSelection(newCol, newRow, false, inKeyboardSelection, keepSelection, false);
            // Ensure seletion is visible
            TableItem tableItem = table.getItem(newRow);
            if (newCol != currentPosition.col) {
                TableColumn newColumn = table.getColumn(newCol);
                table.showColumn(newColumn);
            }
            if (newRow != currentPosition.row) {
                table.showItem(tableItem);
                gridPanel.redraw();
            }

            currentPosition = new GridPos(newCol, newRow);
        }
*/
    }

    public void addCursorChangeListener(Listener listener)
    {
        super.addListener(Event_ChangeCursor, listener);
    }

    public void removeCursorChangeListener(Listener listener)
    {
        super.removeListener(Event_ChangeCursor, listener);
    }

    private void createControl(int style)
    {
        Composite group = new Composite(this, SWT.NONE);
        GridData gd = new GridData(GridData.FILL_BOTH);
        group.setLayoutData(gd);

        GridLayout layout = new GridLayout(1, true);
        layout.numColumns = 2;
        layout.makeColumnsEqualWidth = false;
        layout.marginWidth = 0;
        layout.marginHeight = 0;
        layout.horizontalSpacing = 0;
        group.setLayout(layout);

        grid = new Grid(group, style);
        grid.setCellSelectionEnabled(true);
        grid.setRowHeaderVisible(true);
        //spreadsheet.set
        //spreadsheet.setRowHeaderRenderer(new IRenderer() {
        //});

        grid.setLinesVisible(true);
        grid.setHeaderVisible(true);

        gd = new GridData(GridData.FILL_BOTH);
        grid.setLayoutData(gd);

        grid.addListener(SWT.MouseDoubleClick, this);
        grid.addListener(SWT.MouseDown, this);
        grid.addListener(SWT.KeyDown, this);
        grid.addListener(SWT.FocusIn, this);
        grid.addListener(SWT.FocusOut, this);

        if ((style & SWT.VIRTUAL) != 0) {
            lazyRow = new LazyGridRow();
            grid.addListener(SWT.SetData, this);
        }

        gridSelectionListener = new SelectionListener() {
            public void widgetSelected(SelectionEvent e)
            {
                GridItem item = (GridItem) e.item;
                Point focusCell = grid.getFocusCell();
                if (focusCell != null) {
                    Event event = new Event();
                    event.item = item;
                    event.data = e.data;
                    event.x = focusCell.x;
                    event.y = focusCell.y;
                    notifyListeners(Event_ChangeCursor, event);
                }
            }

            public void widgetDefaultSelected(SelectionEvent e)
            {
            }
        };
        grid.addSelectionListener(gridSelectionListener);

        tableEditor = new GridEditor(grid);
        tableEditor.horizontalAlignment = SWT.LEFT;
        tableEditor.verticalAlignment = SWT.TOP;
        tableEditor.grabHorizontal = true;
        tableEditor.grabVertical = true;
        tableEditor.minimumWidth = 50;

        hookContextMenu();
    }

    public void dispose()
    {
        this.clearGrid();
        super.dispose();
    }

    public void handleEvent(Event event)
    {
        switch (event.type) {
            case SWT.SetData: {
                lazyRow.item = (GridItem) event.item;
                lazyRow.index = event.index;
                if (dataProvider != null) {
                    dataProvider.fillRowData(lazyRow);
                }
                break;
            }
            case SWT.KeyDown:
                switch (event.keyCode) {
                    case SWT.CR:
                        openCellViewer(true);
                        break;
                    default:
                        return;
                }
                break;
            case SWT.MouseDoubleClick:
                openCellViewer(false);
                break;
            case SWT.MouseDown:
                cancelInlineEditor();
                break;
            case SWT.FocusIn:
                registerActions(true);
                break;
            case SWT.FocusOut:
                registerActions(false);
                break;
        }
    }

/*
    public void handleEvent(Event event)
    {
        switch (event.type) {
            case SWT.Paint: {
                paintSelection(event);
                break;
            }
            case SWT.SetData: {
                lazyRow.item = (TableItem)event.item;
                lazyRow.index = event.index;
                if (dataProvider != null) {
                    dataProvider.fillRowData(lazyRow);
                }
                break;
            }
*/
/*
            case SWT.PaintItem: {
                paintItem(event);
                break;
            }
*/
/*
            case SWT.EraseItem: {
                eraseItem(event);
                break;
            }

            case SWT.Selection: {
                table.setSelection(-1);
                break;
            }
            case SWT.MouseDown: {
                cancelInlineEditor();
                if (!startMouseSelection(event, false)) {
                    endMouseSelection();
                    openCellViewer(event, true);
                }
                break;
            }
            case SWT.MouseUp: {
                endMouseSelection();
                break;
            }
            case SWT.MouseMove: {
                moveMouseSelection(event);
                break;
            }
            case SWT.MouseExit: {
                //endMouseSelection();
                break;
            }
            case SWT.MouseDoubleClick: {
                cancelInlineEditor();
                endMouseSelection();
                openCellViewer(event, false);
                break;
            }
            case SWT.KeyDown: {
                processKeyDown(event);
                break;
            }
            case SWT.KeyUp: {
                processKeyUp(event);
                break;
            }
            case SWT.MeasureItem:
                event.height = event.gc.getFontMetrics().getHeight() + 3;
                break;
        }
    }
*/

/*
    private void processKeyDown(Event event)
    {
        inKeyboardSelection = (event.stateMask & SWT.SHIFT) != 0;
        if (!inKeyboardSelection) {
            endSelectItem();
            currentPosition = null;
        }
        if (currentPosition == null) {
            currentPosition = cursorPosition;
        }
        boolean altPressed = (event.stateMask & SWT.ALT) != 0;
        int totalColumns = table.getColumnCount();
        int totalRows = table.getItemCount();
        if (totalColumns == 0 || totalRows == 0) {
            return;
        }
        int pageSize = getVisibleRowsCount();
        int newCol = currentPosition.col;
        int newRow = currentPosition.row;
        switch (event.keyCode) {
            case SWT.PAGE_UP:
                newRow = Math.max(0, newRow - pageSize);
                break;
            case SWT.PAGE_DOWN:
                newRow = Math.min(totalRows - 1, newRow + pageSize);
                break;
            case SWT.ARROW_UP:
                if (newRow > 0) {
                    newRow--;
                }
                break;
            case SWT.ARROW_DOWN:
                newRow = Math.min(totalRows - 1, newRow + 1);
                break;
            case SWT.ARROW_LEFT:
                if (newCol > 0) {
                    newCol--;
                }
                break;
            case SWT.ARROW_RIGHT:
                newCol = Math.min(totalColumns - 1, newCol + 1);
                break;
            case SWT.HOME:
                newCol = 0;
                if (altPressed) {
                    newRow = 0;
                }
                break;
            case SWT.END:
                newCol = totalColumns - 1;
                if (altPressed) {
                    newRow = totalRows - 1;
                }
                break;
            default:
                return;
        }
        shiftCursor(newCol, newRow, (event.stateMask & SWT.SHIFT) != 0);
        event.doit = false;
    }
*/

    /*

        private void processKeyUp(Event event)
        {
            if ((event.stateMask & SWT.SHIFT) == 0) {
                endSelectItem();
                currentPosition = null;
                inKeyboardSelection = false;
            }
        }

        private boolean startMouseSelection(Event event, boolean move)
        {
            GridPos pos = getPosFromPoint(event.x, event.y);
            if (pos == null) {
                return true;
            }
            boolean contextMenu = (event.button == 3);
            if (!contextMenu) {
                inMouseSelection = true;

                if (!move && selection.size() == 1) {
                    // Check for click on the same cell - it'll open inline cell editor
                    GridPos selPos = selection.iterator().next();
                    if (selPos.col == pos.col && selPos.row == pos.row) {
                        return false;
                    }
                }
            }
            if (currentPosition == null || currentPosition.col != pos.col || currentPosition.row != pos.row) {
                changeSelection(event, pos.col, pos.row, contextMenu, move);
                currentPosition = pos;
            }
            return true;
        }

        private void endMouseSelection()
        {
            inMouseSelection = false;
            currentPosition = null;
            endSelectItem();
        }

        private void moveMouseSelection(Event event)
        {
            if (inMouseSelection) {
                startMouseSelection(event, true);
            }
        }

        private void changeSelection(Event event, int columnSelected, int rowSelected, boolean contextMenu, boolean move)
        {
            changeSelection(
                columnSelected,
                rowSelected,
                contextMenu,
                move,
                (event.stateMask & SWT.SHIFT) != 0,
                (event.stateMask & SWT.CONTROL) != 0);
        }

        private void changeSelection(int columnSelected, int rowSelected, boolean contextMenu, boolean move, boolean keepSelection, boolean control)
        {
            boolean changeCursor = !move;
            GridPos thisPos = new GridPos(columnSelected, rowSelected);
            final Set<GridPos> oldSelection = new HashSet<GridPos>(selection);
            oldSelection.addAll(tempSelection);
            if (cursorPosition != null) {
                oldSelection.add(cursorPosition);
            }
            Object newItems;
            if ((keepSelection || move) && !contextMenu) {
                // Select all cells from last cursor position
                if (cursorPosition == null) {
                    // No previous selection or the same selection
                    newItems = thisPos;
                } else if (!cursorPosition.equals(thisPos)) {
                    List<GridPos> newRegion = new ArrayList<GridPos>();
                    for (int i = Math.min(cursorPosition.col, thisPos.col); i <= Math.max(cursorPosition.col, thisPos.col); i++) {
                        for (int k = Math.min(cursorPosition.row, thisPos.row); k <= Math.max(cursorPosition.row, thisPos.row); k++) {
                            newRegion.add(new GridPos(i, k));
                        }
                    }
                    newItems = newRegion;
                    // Do not change cursor position
                    changeCursor = false;
                } else {
                    newItems = thisPos;
                }
            } else if (control && !contextMenu) {
                // Select one additional cell (or row)
                newItems = thisPos;
            } else if (contextMenu) {
                // Select new cell or select nothing (if current cell is already selected)
                if (!selection.contains(thisPos)) {
                    selection.clear();
                }
                newItems = thisPos;
            } else {
                // Select new cell
                selection.clear();
                newItems = thisPos;
            }
            if (changeCursor) {
                changeCursorPosition(thisPos);
            }

            // Make new temp selection
            tempSelection.clear();
            if (newItems instanceof List) {
                @SuppressWarnings("unchecked")
                List<GridPos> itemList = (List<GridPos>)newItems;
                tempSelection.addAll(itemList);
            } else {
                tempSelection.add((GridPos)newItems);
            }

            // Redraw selection
            oldSelection.addAll(tempSelection);
            redrawItems(oldSelection);
        }

        private void changeCursorPosition(GridPos pos)
        {
            cursorPosition = pos;
            Event event = new Event();
            event.item = this;
            event.data = cursorPosition;
            event.x = cursorPosition.col;
            event.y = cursorPosition.row;
            super.notifyListeners(Event_ChangeCursor, event);
        }

        private void endSelectItem()
        {
            // Save temp selection in main selection
            if (!tempSelection.isEmpty()) {
                selection.addAll(tempSelection);
                tempSelection.clear();
            }
        }

        private void eraseItem(Event event)
        {
            event.detail &= ~SWT.BACKGROUND;
            event.detail &= ~SWT.HOT;
            event.detail &= ~SWT.FOCUSED;
            event.detail &= ~SWT.SELECTED;
            GC gc = event.gc;
            TableItem item = (TableItem) event.item;
            int rowIndex = table.indexOf(item);
            //boolean isSelected = isCellSelected(event.index, rowIndex);
            //boolean isCursorCell = (cursorPosition != null && cursorPosition.col == event.index && cursorPosition.row == rowIndex);
            Rectangle itemBounds = item.getBounds(event.index);
            if (itemBounds.x + itemBounds.width < 0 || itemBounds.x > table.getSize().x) {
                return;
            }

            Color oldBackground = gc.getBackground();
            if (dataProvider.isCellModified(event.index, rowIndex)) {
                gc.setBackground(backgroundModified);
                gc.fillRectangle(itemBounds.x, itemBounds.y, itemBounds.width, itemBounds.height);
            }
            gc.setBackground(oldBackground);
        }

        private void paintItem(Event event)
        {
            TableItem item = (TableItem) event.item;
            int rowIndex = table.indexOf(item);

            boolean isCursorCell = (cursorPosition != null && cursorPosition.col == event.index && cursorPosition.row == rowIndex);

            Rectangle itemBounds = item.getBounds(event.index);

            event.gc.setForeground(foregroundLines);
            event.gc.drawLine(itemBounds.x, itemBounds.y + itemBounds.height - 1, itemBounds.x + itemBounds.width, itemBounds.y + itemBounds.height - 1);
            event.gc.drawLine(itemBounds.x + itemBounds.width - 1, itemBounds.y, itemBounds.x + itemBounds.width - 1, itemBounds.y + itemBounds.height);

            // Draw text
            String text = item.getText(event.index);
            event.gc.setForeground(foregroundNormal);

            event.x += 3;
            event.gc.drawText(
                text,
                event.x,
                event.y + Math.max(0, (event.height - event.gc.getFontMetrics().getHeight()) / 2),
                true);
            if (isCursorCell) {
                event.gc.setForeground(cursorRectangle);
                event.gc.drawRectangle(itemBounds.x, itemBounds.y, itemBounds.width - 2, itemBounds.height - 2);
            }
            event.doit = false;
        }

        private void paintSelection(Event event)
        {
            int headerHeight = table.getHeaderVisible() ? table.getHeaderHeight() : 0;
            int eventY = event.y;
            int eventHeight = event.height;
            int eventX = event.x;
            int eventWidth = event.width;
            if (headerHeight > 0) {
                if (eventY > 0) {
                    eventY -= headerHeight;
                } else {
                    eventHeight -= headerHeight;
                }
            }

            int itemHeight = table.getItemHeight();
            int topIndex = table.getTopIndex() + eventY / itemHeight;
            int itemCount = table.getItemCount();
            if (itemCount == 0) {
                return;
            }
            int bottomIndex = topIndex + eventHeight / itemHeight;
            if (bottomIndex >= itemCount) {
                bottomIndex = itemCount - 1;
            }
            if (topIndex < 0) {
                topIndex = 0;
            }
            int leftIndex = 0;
            int rightIndex = 0;

            TableColumn[] columns = table.getColumns();
            int leftOffset = table.getHorizontalBar().getSelection();

            {
                int tmpOffset = 0;
                for (int i = 0; i < columns.length; i++) {
                    TableColumn column = columns[i];
                    tmpOffset += column.getWidth();
                    if (tmpOffset > leftOffset + eventX) {
                        leftIndex = i;
                        break;
                    }
                }
            }
            {
                int tmpOffset = 0;
                boolean found = false;
                for (int i = leftIndex + 1; i < columns.length; i++) {
                    tmpOffset += columns[i].getWidth();
                    if (tmpOffset > eventWidth) {
                        rightIndex = i;
                        found = true;
                        break;
                    }
                }
                if (!found || rightIndex >= columns.length) {
                    rightIndex = columns.length - 1;
                }
            }

            // Draw selection
            event.gc.setBackground(backgroundSelected);
            event.gc.setForeground(foregroundSelected);
            for (int y = topIndex; y <= bottomIndex; y++) {
                TableItem item = table.getItem(y);
                for (int x = leftIndex; x <= rightIndex; x++) {
                    if (isCellSelected(x, y)) {
                        event.gc.setBackground(backgroundSelected);
                        event.gc.setForeground(foregroundSelected);
                        Rectangle bounds = item.getBounds(x);
                        event.gc.setClipping(bounds.x, bounds.y, bounds.width, bounds.height);
                        event.gc.fillRectangle(bounds.x, bounds.y, bounds.width, bounds.height);
                        //event.gc.drawFocus(bounds.x, bounds.y, bounds.width, bounds.height);
                        event.gc.drawText(item.getText(x), bounds.x + 5, bounds.y + 1);
                    }
                }
            }
        }
    */
    private void redrawItems(Collection<GridPos> posList)
    {
        int topIndex = grid.getTopIndex();
        int bottomIndex = grid.getTopIndex() + getVisibleRowsCount();

        for (GridPos pos : posList) {
            if (pos.row < topIndex || pos.row > bottomIndex) {
                continue;
            }
            GridItem tableItem = grid.getItem(pos.row);
            if (tableItem != null) {
                Rectangle cellBounds = tableItem.getBounds(pos.col);
                grid.redraw(cellBounds.x, cellBounds.y, cellBounds.width, cellBounds.height, false);
            }
        }
    }

    private List<GridPos> makeRowPositions(int rowSelected)
    {
        List<GridPos> rowPoses = new ArrayList<GridPos>();
        for (int i = 0; i < curColumns.size(); i++) {
            rowPoses.add(new GridPos(i, rowSelected));
        }
        return rowPoses;
    }

    public GridColumn getColumn(int index)
    {
        return curColumns.get(index);
    }

    public int getColumnsNum()
    {
        return curColumns.size();
    }

    public GridColumn addColumn(String text, String toolTipText, Image image)
    {
        GridColumn column = new GridColumn(grid, SWT.NONE);
        column.setText(text);
        if (toolTipText != null) {
            column.setHeaderTooltip(toolTipText);
        }
        if (image != null) {
            column.setImage(image);
        }

        curColumns.add(column);
        return column;
    }

    public void reinitState()
    {
        cancelInlineEditor();
        // Repack columns
        if (curColumns.size() == 1) {
            curColumns.get(0).setWidth(grid.getSize().x);
        } else {
            for (GridColumn curColumn : curColumns) {
                curColumn.pack();
                if (curColumn.getWidth() > MAX_DEF_COLUMN_WIDTH) {
                    curColumn.setWidth(MAX_DEF_COLUMN_WIDTH);
                }
            }
        }
    }

    private void clearColumns()
    {
        if (!curColumns.isEmpty()) {
            for (GridColumn column : curColumns) {
                column.dispose();
            }
            curColumns.clear();
        }
    }

    public int getVisibleRowsCount()
    {
        Rectangle clientArea = grid.getClientArea();
        int itemHeight = grid.getItemHeight();
        int count = (clientArea.height - grid.getHeaderHeight() + itemHeight - 1) / itemHeight;
        if (count == 0) {
            count = 1;
        }
        return count;
    }

    public void clearGrid()
    {
        //spreadsheet.setSelection(new int[0]);

        cancelInlineEditor();
        grid.removeAll();
        this.clearColumns();
    }

    private void copySelectionToClipboard()
    {
        String lineSeparator = System.getProperty("line.separator");
        List<Integer> colsSelected = new ArrayList<Integer>();
        int firstCol = Integer.MAX_VALUE, lastCol = Integer.MIN_VALUE;
        int firstRow = Integer.MAX_VALUE;
        List<GridPos> selection = getSelection();
        for (GridPos pos : selection) {
            if (firstCol > pos.col) {
                firstCol = pos.col;
            }
            if (lastCol < pos.col) {
                lastCol = pos.col;
            }
            if (firstRow > pos.row) {
                firstRow = pos.row;
            }
            if (!colsSelected.contains(pos.col)) {
                colsSelected.add(pos.col);
            }
        }
        StringBuilder tdt = new StringBuilder();
        int prevRow = firstRow;
        int prevCol = firstCol;
        for (GridPos pos : selection) {
            GridItem tableItem = grid.getItem(pos.row);
            if (pos.row > prevRow) {
                if (prevCol < lastCol) {
                    for (int i = prevCol; i < lastCol; i++) {
                        tdt.append("\t");
                    }
                }
                tdt.append(lineSeparator);
                prevRow = pos.row;
                prevCol = firstCol;
            }
            if (pos.col > prevCol) {
                for (int i = 0; i < pos.col - prevCol; i++) {
                    tdt.append("\t");
                }
                prevCol = pos.col;
            }
            String text = tableItem.getText(pos.col);
            tdt.append(text == null ? "" : text);
        }
        TextTransfer textTransfer = TextTransfer.getInstance();
        clipboard.setContents(
            new Object[]{tdt.toString()},
            new Transfer[]{textTransfer});
    }

    private void hookContextMenu()
    {
        MenuManager menuMgr = new MenuManager();
        Menu menu = menuMgr.createContextMenu(grid);
        menuMgr.addMenuListener(new IMenuListener() {
            public void menuAboutToShow(IMenuManager manager)
            {
                IAction copyAction = new Action("Copy selection") {
                    public void run()
                    {
                        copySelectionToClipboard();
                    }
                };
                copyAction.setActionDefinitionId(IWorkbenchActionDefinitionIds.COPY);
                copyAction.setId(IWorkbenchActionDefinitionIds.COPY);
                IAction selectAllAction = new Action("Select All") {
                    public void run()
                    {
                        grid.selectAll();
                    }
                };
                copyAction.setEnabled(grid.getSelectionCount() > 0);
                manager.add(copyAction);
                manager.add(selectAllAction);
                manager.add(new GroupMarker(IWorkbenchActionConstants.MB_ADDITIONS));
            }
        });
        menuMgr.setRemoveAllWhenShown(true);
        grid.setMenu(menu);
        site.registerContextMenu(menuMgr, selectionProvider);
    }

    public void openCellViewer(boolean inline)
    {
        if (dataProvider == null) {
            return;
        }
        // The control that will be the editor must be a child of the Table
        Point focusCell = grid.getFocusCell();
        //GridPos pos = getPosFromPoint(event.x, event.y);
        if (focusCell == null || focusCell.y < 0 || focusCell.x < 0) {
            return;
        }
        if (!dataProvider.isEditable() || !dataProvider.isCellEditable(focusCell.x, focusCell.y)) {
            return;
        }
        GridItem item = grid.getItem(focusCell.y);

        Composite placeholder = null;
        if (inline) {
            cancelInlineEditor();

            placeholder = new Composite(grid, SWT.BORDER);
            placeholder.setFont(grid.getFont());
            GridLayout layout = new GridLayout(1, true);
            layout.marginWidth = 0;
            layout.marginHeight = 0;
            layout.horizontalSpacing = 0;
            layout.verticalSpacing = 0;
            placeholder.setLayout(layout);

            GridData gd = new GridData(GridData.FILL_BOTH);
            gd.horizontalIndent = 0;
            gd.verticalIndent = 0;
            gd.grabExcessHorizontalSpace = true;
            gd.grabExcessVerticalSpace = true;
            placeholder.setLayoutData(gd);
        }
        lazyRow.index = focusCell.y;
        lazyRow.column = focusCell.x;
        lazyRow.item = item;
        boolean editSuccess = dataProvider.showCellEditor(lazyRow, inline, placeholder);
        if (inline) {
            if (editSuccess) {
                int minHeight, minWidth;
                Point editorSize = placeholder.computeSize(SWT.DEFAULT, SWT.DEFAULT);
                minHeight = editorSize.y;
                minWidth = editorSize.x;
                if (minWidth > MAX_INLINE_EDIT_WITH) {
                    minWidth = MAX_INLINE_EDIT_WITH;
                }
                tableEditor.minimumHeight = minHeight;// + placeholder.getBorderWidth() * 2;//placeholder.getBounds().height;
                tableEditor.minimumWidth = minWidth;
/*
                if (pos.row == 0) {
                    tableEditor.verticalAlignment = SWT.TOP;
                } else {
                    tableEditor.verticalAlignment = SWT.CENTER;
                }
*/
                tableEditor.setEditor(placeholder, item, focusCell.x);
            } else {
                // No editor was created so just drop placeholder
                placeholder.dispose();
            }
        }
    }

    public void cancelInlineEditor()
    {
        Control oldEditor = tableEditor.getEditor();
        if (oldEditor != null) oldEditor.dispose();
    }

    public int getItemCount()
    {
        return grid.getItemCount();
    }

    public void setItemCount(int count)
    {
        grid.setItemCount(count);
    }

    public int getColumnsCount()
    {
        return grid.getColumnCount();
    }

    private void registerActions(boolean register)
    {
        IHandlerService service = (IHandlerService) site.getService(IHandlerService.class);
        for (ActionInfo actionInfo : actionsInfo) {
            if (register) {
                assert (actionInfo.handlerActivation == null);
                ActionHandler handler = new ActionHandler(actionInfo.action);
                actionInfo.handlerActivation = service.activateHandler(
                    actionInfo.action.getActionDefinitionId(),
                    handler);
            } else {
                assert (actionInfo.handlerActivation != null);
                service.deactivateHandler(actionInfo.handlerActivation);
                actionInfo.handlerActivation = null;
            }
            // TODO: want to remove but can't
            // where one editor page have many controls each with its own behavior
            if (register) {
                site.getKeyBindingService().registerAction(actionInfo.action);
            } else {
                site.getKeyBindingService().unregisterAction(actionInfo.action);
            }
        }
    }

    public void redrawGrid()
    {
        Rectangle bounds = grid.getBounds();
        grid.redraw(bounds.x, bounds.y, bounds.width, bounds.height, true);
    }

    private static class ActionInfo {
        IAction action;
        IHandlerActivation handlerActivation;

        private ActionInfo(IAction action)
        {
            this.action = action;
        }
    }

    private abstract class GridAction extends Action {
        GridAction(String actionId)
        {
            setActionDefinitionId(actionId);
        }

        public abstract void run();
    }

    private class CursorMoveAction extends GridAction {
        private CursorMoveAction(String actionId)
        {
            super(actionId);
        }

        public void run()
        {
            Event event = new Event();
            event.doit = true;
            String actionId = getActionDefinitionId();
            boolean keepSelection = (event.stateMask & SWT.SHIFT) != 0;
            if (actionId.equals(ITextEditorActionDefinitionIds.LINE_START)) {
                shiftCursor(-grid.getColumnCount(), 0, keepSelection);
            } else if (actionId.equals(ITextEditorActionDefinitionIds.LINE_END)) {
                shiftCursor(grid.getColumnCount(), 0, keepSelection);
            } else if (actionId.equals(ITextEditorActionDefinitionIds.TEXT_START)) {
                shiftCursor(-grid.getColumnCount(), -grid.getItemCount(), keepSelection);
            } else if (actionId.equals(ITextEditorActionDefinitionIds.TEXT_END)) {
                shiftCursor(grid.getColumnCount(), grid.getItemCount(), keepSelection);
            }
        }
    }

    private class LazyGridRow implements IGridRowData {

        private GridItem item;
        private int index;
        private int column;

        public int getIndex()
        {
            return index;
        }

        public int getColumn()
        {
            return column;
        }

        public void setImage(int column, Image image)
        {
            item.setImage(column, image);
        }

        public String getText(int column)
        {
            return item.getText(column);
        }

        public void setText(int column, String text)
        {
            item.setText(column, text);
        }

        public void setHeaderText(String text)
        {
            item.setHeaderText(text);
        }

        public void setHeaderImage(Image image)
        {
            item.setHeaderImage(image);
        }

        public void setModified(int column, boolean modified)
        {
            item.setBackground(column, modified ? backgroundModified : backgroundNormal);
        }

        public void setEmpty(int column, boolean empty)
        {
            item.setGrayed(column, empty);
        }

        public Object getData()
        {
            return item.getData();
        }

        public void setData(Object data)
        {
            item.setData(data);
        }

    }

}
