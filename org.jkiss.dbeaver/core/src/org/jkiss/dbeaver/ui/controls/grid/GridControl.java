package org.jkiss.dbeaver.ui.controls.grid;

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
import org.eclipse.swt.custom.TableEditor;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.GC;
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
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.handlers.IHandlerActivation;
import org.eclipse.ui.handlers.IHandlerService;
import org.eclipse.ui.texteditor.ITextEditorActionDefinitionIds;
import org.eclipse.ui.texteditor.IWorkbenchActionDefinitionIds;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * ResultSetControl
 */
public class GridControl extends Composite implements Listener
{
    static Log log = LogFactory.getLog(GridControl.class);

    public static final int MAX_DEF_COLUMN_WIDTH = 300;
    public static final int MAX_INLINE_EDIT_WITH = 300;

    private static final int Event_ChangeCursor = 1000;

    private Table table;
    private TableEditor tableEditor;
    private List<TableColumn> curColumns = new ArrayList<TableColumn>();

    private GridPanel gridPanel;

    private IWorkbenchPartSite site;
    private IGridDataProvider dataProvider;
    private GridSelectionProvider selectionProvider;
    private GridRowInfo rowInfo = new GridRowInfo();

    private GridPos cursorPosition;
    private Set<GridPos> selection;
    private Set<GridPos> tempSelection = new HashSet<GridPos>();
    private boolean inMouseSelection;
    private boolean inKeyboardSelection;
    private GridPos currentPosition;

    private Clipboard clipboard;
    private ActionInfo[] actionsInfo;

    private Color foregroundNormal;
    private Color foregroundLines;
    private Color foregroundSelected;
    private Color backgroundModified;
    private Color cursorRectangle;
    private Color backgroundNormal;
    private Color backgroundControl;
    private Color backgroundSelected;

    private transient LazyGridRow lazyRow;

    public GridControl(Composite parent, int style, IWorkbenchPartSite site, IGridDataProvider dataProvider)
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
        this.selection = new TreeSet<GridPos>(new Comparator<GridPos>() {
            public int compare(GridPos pos1, GridPos pos2)
            {
                int res = pos1.row - pos2.row;
                return res != 0 ? res : pos1.col - pos2.col;
            }
        });

        foregroundNormal = getDisplay().getSystemColor(SWT.COLOR_LIST_FOREGROUND);
        foregroundLines = getDisplay().getSystemColor(SWT.COLOR_GRAY);
        foregroundSelected = getDisplay().getSystemColor(SWT.COLOR_LIST_SELECTION_TEXT);
        backgroundModified = new Color(getDisplay(), 0xFF, 0xE4, 0xB5);//getDisplay().getSystemColor(SWT.COLOR_DARK_RED);
        cursorRectangle = getDisplay().getSystemColor(SWT.COLOR_DARK_RED);
        backgroundNormal = getDisplay().getSystemColor(SWT.COLOR_LIST_BACKGROUND);
        backgroundSelected = getDisplay().getSystemColor(SWT.COLOR_LIST_SELECTION);
        backgroundControl = getDisplay().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND);

        clipboard = new Clipboard(getDisplay());

        actionsInfo = new ActionInfo[] {
            new ActionInfo(new CopyAction()),
            new ActionInfo(new CursorMoveAction(ITextEditorActionDefinitionIds.LINE_START)),
            new ActionInfo(new CursorMoveAction(ITextEditorActionDefinitionIds.LINE_END)),
            new ActionInfo(new CursorMoveAction(ITextEditorActionDefinitionIds.TEXT_START)),
            new ActionInfo(new CursorMoveAction(ITextEditorActionDefinitionIds.TEXT_END)),
        };

        this.createControl(style);
    }

    Table getTable()
    {
        return table;
    }

    public Set<GridPos> getSelection()
    {
        return selection;
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
        this.table.redraw();
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
        this.table.redraw();
    }

    public void setFont(Font font)
    {
        table.setFont(font);
        gridPanel.setFont(font);
    }

    public Point getCursorPosition()
    {
        if (cursorPosition == null) {
            return null;
        } else {
            return new Point(cursorPosition.col, cursorPosition.row);
        }
    }

    public void moveCursor(int newCol, int newRow, boolean keepSelection)
    {
        if (currentPosition == null) {
            currentPosition = cursorPosition;
        }
        if (newCol != currentPosition.col || newRow != currentPosition.row) {
            changeSelection(newCol, newRow, false, inKeyboardSelection, keepSelection, false);
            // Ensure seletion is visible
            TableItem tableItem = table.getItem(newRow);
            if (newCol != currentPosition.col) {
/*
                int newOffset = 0;
                for (int i = 0; i < newCol; i++) {
                    newOffset += curColumns.get(i).getWidth();
                }
                table.getHorizontalBar().setSelection(newOffset);
*/
                TableColumn newColumn = table.getColumn(newCol);
                table.showColumn(newColumn);
            }
            if (newRow != currentPosition.row) {
                table.showItem(tableItem);
                gridPanel.redraw();
            }

            currentPosition = new GridPos(newCol, newRow);
        }
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

        gridPanel = new GridPanel(group, SWT.NONE, this);
        gd = new GridData(GridData.FILL_VERTICAL);
        gd.grabExcessVerticalSpace = true;
        gd.grabExcessHorizontalSpace = false;
        gd.minimumWidth = 20;
        gd.widthHint = 20;
        gridPanel.setLayoutData(gd);

        table = new Table(group, (style & ~SWT.BORDER));
        table.setLinesVisible(true);
        table.setHeaderVisible(true);
        
        gd = new GridData(GridData.FILL_BOTH);
        table.setLayoutData(gd);

        //TableCursor cyr = new TableCursor(table, SWT.NONE);
        //table.addListener(SWT.PaintItem, this);
        table.addListener(SWT.EraseItem, this);
        table.addListener(SWT.Paint, this);
        table.addListener(SWT.Selection, this);
        table.addListener(SWT.MouseDown, this);
        table.addListener(SWT.MouseUp, this);
        table.addListener(SWT.MouseMove, this);
        table.addListener(SWT.MouseExit, this);
        table.addListener(SWT.MouseDoubleClick, this);
        table.addListener(SWT.KeyDown, this);
        table.addListener(SWT.KeyUp, this);
        table.addListener(SWT.FocusIn, this);
        table.addListener(SWT.FocusOut, this);
        table.addListener(SWT.MeasureItem, this);

        if ((style & SWT.VIRTUAL) != 0) {
            lazyRow = new LazyGridRow();
            table.addListener(SWT.SetData, this);
        }

        Listener resizeListener = new Listener()
        {
            public void handleEvent(Event event)
            {
                if (gridPanel != null) {
                    gridPanel.redraw();
                }
            }
        };
        table.getHorizontalBar().addListener(SWT.Selection, resizeListener);
        table.getVerticalBar().addListener(SWT.Selection, resizeListener);

        tableEditor = new TableEditor(table);
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
/*
            case SWT.PaintItem: {
                paintItem(event);
                break;
            }
*/
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
            case SWT.FocusIn:
                registerActions(true);
                break;
            case SWT.FocusOut:
                registerActions(false);
                break;
            case SWT.MeasureItem:
                event.height = event.gc.getFontMetrics().getHeight() + 3;
                break;
        }
    }

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
        moveCursor(newCol, newRow, (event.stateMask & SWT.SHIFT) != 0);
        event.doit = false;
    }

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
/*
        // Erase background
        Color oldBackground = gc.getBackground();
        gc.setBackground(isSelected ? backgroundSelected : backgroundNormal);
        gc.fillRectangle(itemBounds.x, itemBounds.y, itemBounds.width, itemBounds.height);

        event.gc.setForeground(foregroundLines);
        event.gc.drawLine(itemBounds.x, itemBounds.y + itemBounds.height - 1, itemBounds.x + itemBounds.width, itemBounds.y + itemBounds.height - 1);
        event.gc.drawLine(itemBounds.x + itemBounds.width - 1, itemBounds.y, itemBounds.x + itemBounds.width - 1, itemBounds.y + itemBounds.height);

        if (isCursorCell) {
            event.gc.setForeground(cursorRectangle);
            event.gc.drawRectangle(itemBounds.x, itemBounds.y, itemBounds.width - 2, itemBounds.height - 2);
        }

        gc.setBackground(oldBackground);
        gc.setForeground(foregroundNormal);
*/
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

    private void redrawItems(Collection<GridPos> posList)
    {
        int topIndex = table.getTopIndex();
        int bottomIndex = table.getTopIndex() + getVisibleRowsCount();

        for (GridPos pos : posList) {
            if (pos.row < topIndex || pos.row > bottomIndex) {
                continue;
            }
            TableItem tableItem = table.getItem(pos.row);
            if (tableItem != null) {
                Rectangle cellBounds = tableItem.getBounds(pos.col);
                table.redraw(cellBounds.x, cellBounds.y, cellBounds.width, cellBounds.height, false);
            }
        }
    }

    private GridPos getPosFromPoint(int x, int y)
    {
        Point clickPoint = new Point(x, y);
        TableItem tableItem = table.getItem(clickPoint);
        if (tableItem == null) {
            return null;
        }
        int rowSelected = table.indexOf(tableItem);
        int columnSelected = -1;
        {
            TableColumn[] columns = table.getColumns();
            for (int i = 0; i < columns.length; i++) {
                Rectangle cellBounds = tableItem.getBounds(i);
                cellBounds.width += 1;
                cellBounds.height += 1;
                if (cellBounds.contains(clickPoint)) {
                    columnSelected = i;
                    break;
                }
            }
        }
        return new GridPos(columnSelected, rowSelected);
    }

    private List<GridPos> makeRowPositions(int rowSelected)
    {
        List<GridPos> rowPoses = new ArrayList<GridPos>();
        for (int i = 0; i < curColumns.size(); i++) {
            rowPoses.add(new GridPos(i, rowSelected));
        }
        return rowPoses;
    }

    public TableColumn getColumn(int index)
    {
        return curColumns.get(index);
    }

    public int getColumnsNum()
    {
        return curColumns.size();
    }

    private boolean isCellSelected(int col, int row)
    {
        GridPos pos = new GridPos(col, row);
        return selection.contains(pos) || (!tempSelection.isEmpty() && tempSelection.contains(pos));
    }

    public TableColumn addColumn(String text, String toolTipText, Image image)
    {
        TableColumn column = new TableColumn(table, SWT.NONE);
        column.setText(text);
        if (toolTipText != null) {
            column.setToolTipText(toolTipText);
        }
        if (image != null) {
            column.setImage(image);
        }
        curColumns.add(column);
        return column;
    }

    public void reinitState()
    {
        // Repack columns
        if (curColumns.size() == 1) {
            curColumns.get(0).setWidth(table.getSize().x - gridPanel.getPanelWidth());
        } else {
            for (TableColumn curColumn : curColumns) {
                curColumn.pack();
                if (curColumn.getWidth() > MAX_DEF_COLUMN_WIDTH) {
                    curColumn.setWidth(MAX_DEF_COLUMN_WIDTH);
                }
            }
        }
        // Reinit selection state
        selection.clear();
        tempSelection.clear();
        setCurrentRowNum(0);
        currentPosition = null;

        gridPanel.redraw();
    }

    private void clearColumns()
    {
        if (!curColumns.isEmpty()) {
            for (TableColumn column : curColumns) {
                column.dispose();
            }
            curColumns.clear();
        }
    }

    public int getVisibleRowsCount()
    {
        Rectangle clientArea = table.getClientArea();
        int itemHeight = table.getItemHeight();
        int count = (clientArea.height - table.getHeaderHeight() + itemHeight - 1) / itemHeight;
        if (count == 0) {
            count = 1;
        }
        return count;
    }

    public void clearGrid()
    {
        cancelInlineEditor();
        table.removeAll();
        this.clearColumns();
    }

    private void copySelectionToClipboard()
    {
        String lineSeparator = System.getProperty("line.separator");
        List<Integer> colsSelected = new ArrayList<Integer>();
        int firstCol = Integer.MAX_VALUE, lastCol = Integer.MIN_VALUE;
        int firstRow = Integer.MAX_VALUE;
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
            TableItem tableItem = table.getItem(pos.row);
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
        Menu menu = menuMgr.createContextMenu(table);
        menuMgr.addMenuListener(new IMenuListener()
        {
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
                        selectAllRows();
                    }
                };
                copyAction.setEnabled(!getSelection().isEmpty());
                manager.add(copyAction);
                manager.add(selectAllAction);
                manager.add(new GroupMarker(IWorkbenchActionConstants.MB_ADDITIONS));
            }
        });
        menuMgr.setRemoveAllWhenShown(true);
        table.setMenu(menu);
        site.registerContextMenu(menuMgr, selectionProvider);
    }

    void selectAllRows()
    {
        int rowCount = table.getItemCount();
        int colCount = curColumns.size();
        for (int i = 0; i < rowCount; i++) {
            for (int k = 0; k < colCount; k++) {
                selection.add(new GridPos(k, i));
            }
        }
        table.redraw();
    }

    void selectRow(int rowNum, boolean append)
    {
        List<GridPos> list = makeRowPositions(rowNum);
        List<GridPos> toRedraw;
        if (!append) {
            toRedraw = new ArrayList<GridPos>(selection);
            toRedraw.addAll(list);
            selection.clear();
        } else {
            toRedraw = list;
        }
        selection.addAll(list);
        this.redrawItems(toRedraw);
    }

    void openCellViewer(Event event, boolean inline)
    {
        if (dataProvider == null) {
            return;
        }
        // The control that will be the editor must be a child of the Table
        GridPos pos = getPosFromPoint(event.x, event.y);
        if (pos == null || pos.row < 0 || pos.col < 0) {
            return;
        }
        if (!dataProvider.isEditable() || !dataProvider.isCellEditable(pos.col, pos.row)) {
            return;
        }
        TableItem item = table.getItem(pos.row);

        Composite placeholder = null;
        if (inline) {
            cancelInlineEditor();

            placeholder = new Composite(table, SWT.BORDER);
            placeholder.setFont(table.getFont());
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
        lazyRow.index = pos.row;
        lazyRow.column = pos.col;
        lazyRow.item = item;
        boolean editSuccess = dataProvider.showCellEditor(lazyRow, inline, placeholder);
        if (inline) {
            if (editSuccess) {
                int minHeight = 0, minWidth = 50;
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
                tableEditor.setEditor(placeholder, item, pos.col);
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
        return table.getItemCount();
    }

    public void setItemCount(int count)
    {
        table.setItemCount(count);
    }

    public int getColumnsCount()
    {
        return table.getColumnCount();
    }

    public void refreshCell(int col, int row)
    {
        
    }

    private void registerActions(boolean register)
    {
        IHandlerService service = (IHandlerService)site.getService(IHandlerService.class);
        for (ActionInfo actionInfo : actionsInfo) {
            if (register) {
                assert(actionInfo.handlerActivation == null);
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

    public IGridRowInfo getRowInfo(int rowNum)
    {
        rowInfo.clear();
        if (dataProvider != null) {
            dataProvider.fillRowInfo(rowNum, rowInfo);
        } else {
            rowInfo.setText(String.valueOf(rowNum));
        }
        return rowInfo;
    }

    public void setPanelWidth(int width)
    {
        gridPanel.setPanelWidth(width);
        this.redraw();
    }

    public GridPos getCurrentPos()
    {
        return cursorPosition;
    }

    private void setCurrentRowNum(int rowNum)
    {
        cursorPosition = new GridPos(0, rowNum);
        selection.clear();
        if (!curColumns.isEmpty() && this.getItemCount() > 0) {
            selection.add(cursorPosition);
        }
    }

    public void redrawGrid()
    {
        Rectangle bounds = table.getBounds();
        table.redraw(bounds.x, bounds.y, bounds.width, bounds.height, true);
    }

    private static class ActionInfo {
        IAction action;
        IHandlerActivation handlerActivation;

        private ActionInfo(IAction action)
        {
            this.action = action;
        }
    }

    private class CopyAction extends Action
    {
        private CopyAction()
        {
            setActionDefinitionId(IWorkbenchActionDefinitionIds.COPY);
        }

        public void run()
        {
            copySelectionToClipboard();
        }
    }

    private class CursorMoveAction extends Action
    {
        private CursorMoveAction(String actionId)
        {
            setActionDefinitionId(actionId);
        }

        public void run()
        {
            Event event = new Event();
            String actionId = getActionDefinitionId();
            if (actionId.equals(ITextEditorActionDefinitionIds.LINE_START)) {
                event.keyCode = SWT.HOME;
            } else if (actionId.equals(ITextEditorActionDefinitionIds.LINE_END)) {
                event.keyCode = SWT.END;
            } else if (actionId.equals(ITextEditorActionDefinitionIds.TEXT_START)) {
                event.keyCode = SWT.HOME;
                event.stateMask |= SWT.ALT;
            } else if (actionId.equals(ITextEditorActionDefinitionIds.TEXT_END)) {
                event.keyCode = SWT.END;
                event.stateMask |= SWT.ALT;
            }
            processKeyDown(event);
            //event.stateMask = 0;
            //processKeyUp(event);
        }
    }

    private static class LazyGridRow implements IGridRowData {

        private TableItem item;
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

        public Object getData() {
            return item.getData();
        }

        public void setData(Object data)
        {
            item.setData(data);
        }

    }

    private static class GridRowInfo implements IGridRowInfo {
        private String text;
        private String toolTip;
        private Image image;

        public String getText() {
            return text;
        }

        public void setText(String text) {
            this.text = text;
        }

        public String getToolTip() {
            return toolTip;
        }

        public void setToolTip(String toolTip) {
            this.toolTip = toolTip;
        }

        public Image getImage() {
            return image;
        }

        public void setImage(Image image) {
            this.image = image;
        }

        void clear()
        {
            this.text = null;
            this.toolTip = null;
            this.image = null;
        }
    }
}
