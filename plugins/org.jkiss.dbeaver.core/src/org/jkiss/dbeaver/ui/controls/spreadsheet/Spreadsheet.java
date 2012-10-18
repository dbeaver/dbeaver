/*
 * Copyright (C) 2010-2012 Serge Rieder
 * serge@jkiss.org
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package org.jkiss.dbeaver.ui.controls.spreadsheet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.jface.action.GroupMarker;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.IWorkbenchPartSite;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.lightgrid.*;
import org.jkiss.dbeaver.utils.ContentUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * ResultSetControl
 */
public class Spreadsheet extends Composite implements Listener {
    static final Log log = LogFactory.getLog(Spreadsheet.class);

    private static final String SPREADSHEET_CONTROL_ID = "org.jkiss.dbeaver.ui.spreadsheet";
    public static final int MAX_DEF_COLUMN_WIDTH = 300;
    public static final int MAX_INLINE_EDIT_WITH = 300;

    private static final int Event_ChangeCursor = 1000;

    private LightGrid grid;
    private GridEditor tableEditor;

    private IWorkbenchPartSite site;
    private ISpreadsheetController spreadsheetController;
    private IGridContentProvider contentProvider;
    private ILabelProvider contentLabelProvider;
    private ILabelProvider columnLabelProvider;
    private ILabelProvider rowLabelProvider;

    private SpreadsheetSelectionProvider selectionProvider;

    private Clipboard clipboard;

    private Color foregroundNormal;
    private Color foregroundLines;
    private Color foregroundSelected;
    private Color backgroundNormal;
    private Color backgroundControl;
    private Color backgroundSelected;

    private SelectionListener gridSelectionListener;

    public Spreadsheet(
        Composite parent,
        int style,
        IWorkbenchPartSite site,
        ISpreadsheetController spreadsheetController,
        IGridContentProvider contentProvider,
        ILabelProvider contentLabelProvider,
        ILabelProvider columnLabelProvider,
        ILabelProvider rowLabelProvider
        )
    {
        super(parent, SWT.NONE);
        GridLayout layout = new GridLayout(1, true);
        layout.numColumns = 1;
        layout.makeColumnsEqualWidth = false;
        layout.marginWidth = 0;
        layout.marginHeight = 0;
        this.setLayout(layout);

        this.site = site;
        this.spreadsheetController = spreadsheetController;
        this.contentProvider = contentProvider;
        this.contentLabelProvider = contentLabelProvider;
        this.columnLabelProvider = columnLabelProvider;
        this.rowLabelProvider = rowLabelProvider;
        this.selectionProvider = new SpreadsheetSelectionProvider(this);

        this.foregroundNormal = getDisplay().getSystemColor(SWT.COLOR_LIST_FOREGROUND);
        this.foregroundLines = getDisplay().getSystemColor(SWT.COLOR_GRAY);
        this.foregroundSelected = getDisplay().getSystemColor(SWT.COLOR_LIST_SELECTION_TEXT);
        this.backgroundNormal = getDisplay().getSystemColor(SWT.COLOR_LIST_BACKGROUND);
        this.backgroundSelected = getDisplay().getSystemColor(SWT.COLOR_LIST_SELECTION);
        this.backgroundControl = getDisplay().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND);

        this.clipboard = new Clipboard(getDisplay());

        this.createControl(style);
    }

    public static Spreadsheet getFromGrid(LightGrid grid)
    {
        return grid != null && !grid.isDisposed() && grid.getParent() instanceof Spreadsheet ? (Spreadsheet)grid.getParent() : null;
    }

    public LightGrid getGrid()
    {
        return grid;
    }

    public ISpreadsheetController getController()
    {
        return spreadsheetController;
    }

    public Clipboard getClipboard()
    {
        return clipboard;
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

    @Override
    public void setFont(Font font)
    {
        grid.setFont(font);
        //gridPanel.setFont(font);
    }

    public Collection<GridPos> getSelection()
    {
        return grid.getCellSelection();
    }

    public int getCurrentRow()
    {
        return grid.getFocusItem();
    }

    public GridPos getCursorPosition()
    {
        if (grid.isDisposed()) {
            return new GridPos(-1, -1);
        }
        return grid.getFocusCell();
    }

    public void setRowHeaderWidth(int width)
    {
        grid.setItemHeaderWidth(width);
    }

    public boolean shiftCursor(int xOffset, int yOffset, boolean keepSelection)
    {
        if (xOffset == 0 && yOffset == 0) {
            return false;
        }
        GridPos curPos = getCursorPosition();
        if (curPos == null) {
            return false;
        }
        GridPos newPos = new GridPos(curPos.col, curPos.row);
        // Move row
        if (yOffset != 0) {
            int newRow = curPos.row + yOffset;
            if (newRow < 0) {
                newRow = 0;
            }
            if (newRow >= getItemCount()) {
                newRow = getItemCount() - 1;
            }
            newPos.row = newRow;
        }
        // Move column
        if (xOffset != 0) {
            int newCol = curPos.col + xOffset;
            if (newCol < 0) {
                newCol = 0;
            }
            if (newCol >= getColumnsCount()) {
                newCol = getColumnsCount() - 1;
            }
            newPos.col = newCol;
        }

        setCursor(newPos, keepSelection);
        return true;
    }

    public void setCursor(GridPos newPos, boolean keepSelection)
    {
        Event fakeEvent = new Event();
        fakeEvent.widget = grid;
        SelectionEvent selectionEvent = new SelectionEvent(fakeEvent);
        // Move row
        if (newPos.row >= 0 && newPos.row < getItemCount()) {
            selectionEvent.data = newPos.row;
            grid.setFocusItem(newPos.row);
            grid.showItem(newPos.row);
        }
        // Move column
        if (newPos.col >= 0 && newPos.col < getColumnsCount()) {
            grid.setFocusColumn(newPos.col);
            grid.showColumn(newPos.col);
        }
        if (!keepSelection) {
            grid.deselectAll();
        }
        grid.selectCell(newPos);
        //spreadsheet.s
        grid.redraw();

        // Change selection event
        selectionEvent.data = new GridPos(newPos.col, newPos.row);
        gridSelectionListener.widgetSelected(selectionEvent);
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
        grid = new LightGrid(this, style);
        grid.setRowHeaderVisible(true);
        //grid.setFooterVisible(true);
        //spreadsheet.set
        //spreadsheet.setRowHeaderRenderer(new IGridRenderer() {
        //});

        grid.setLinesVisible(true);
        grid.setHeaderVisible(true);
        grid.setMaxColumnDefWidth(MAX_DEF_COLUMN_WIDTH);

        GridData gd = new GridData(GridData.FILL_BOTH);
        grid.setLayoutData(gd);

        grid.addListener(SWT.MouseDoubleClick, this);
        grid.addListener(SWT.MouseDown, this);
        grid.addListener(SWT.KeyDown, this);
        grid.addListener(LightGrid.Event_ChangeSort, this);
        //Event_ChangeSort
        gridSelectionListener = new SelectionListener() {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                //Integer row = (Integer) e.data;
                GridPos pos = (GridPos) e.data;
                //GridPos focusCell = grid.getFocusCell();
                //if (focusCell != null) {
                    Event event = new Event();
                    //event.data = row;
                    //event.data = e.data;
                    event.x = pos.col;
                    event.y = pos.row;
                    notifyListeners(Event_ChangeCursor, event);
                //}
            }

            @Override
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

        grid.setContentProvider(contentProvider);
        grid.setContentLabelProvider(contentLabelProvider);
        grid.setColumnLabelProvider(columnLabelProvider);
        grid.setRowLabelProvider(rowLabelProvider);

        {
            UIUtils.addFocusTracker(site, SPREADSHEET_CONTROL_ID, grid);
            grid.addDisposeListener(new DisposeListener() {
                @Override
                public void widgetDisposed(DisposeEvent e) {
                    UIUtils.removeFocusTracker(site, grid);
                }
            });
        }

    }


/*
    private void initContextHandling() {
        ((IContextService) site.getService(IContextService.class)).activateContext("org.jkiss.dbeaver.ui.spreadsheet.grid.context", new Expression() {
            @Override
            public void collectExpressionInfo(ExpressionInfo info) {
                super.collectExpressionInfo(info);
                info.addVariableNameAccess(ISources.ACTIVE_FOCUS_CONTROL_NAME);
            }

            @Override
            public EvaluationResult evaluate(IEvaluationContext context)
                throws CoreException {
                if (context.getVariable(ISources.ACTIVE_FOCUS_CONTROL_NAME) == grid) {
                    return EvaluationResult.TRUE;
                }
                return EvaluationResult.FALSE;
            }

        });
    }
*/


    @Override
    public void dispose()
    {
        this.clearGrid();
        if (clipboard != null && !clipboard.isDisposed()) {
            clipboard.dispose();
        }
        super.dispose();
    }

    @Override
    public void handleEvent(Event event)
    {
        switch (event.type) {
/*
            case SWT.SetData: {
                lazyRow.item = (GridItem) event.data;
                lazyRow.index = event.index;
                if (dataProvider != null) {
                    dataProvider.fillRowData(lazyRow);
                }
                break;
            }
*/
            case SWT.KeyDown:
                if (event.keyCode == SWT.CR ||
                    (event.keyCode >= 'a' && event.keyCode <= 'z') ||
                    (event.keyCode >= '0' && event.keyCode <= '9'))
                {
                    openCellViewer(true);
                } else if (event.keyCode == SWT.ESC) {
                    // Reset cell value
                    if (spreadsheetController != null) {
                        spreadsheetController.resetCellValue(grid.getFocusCell(), false);
                    }
                }
                break;
            case SWT.MouseDoubleClick:
                GridPos pos = grid.getCell(new Point(event.x, event.y));
                GridPos focusPos = grid.getFocusCell();
                if (pos != null && focusPos != null && pos.equals(grid.getFocusCell())) {
                    openCellViewer(false);
                }
                break;
            case SWT.MouseDown:
                if (event.button == 2) {
                    openCellViewer(true);
                }
                break;
            case LightGrid.Event_ChangeSort:
                spreadsheetController.changeSorting((GridColumn) event.data, event.stateMask);
                break;
        }
    }

/*
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
*/

    public void reinitState()
    {
        cancelInlineEditor();
        // Repack columns
        grid.refreshData();

        setCursor(new GridPos(-1, -1), false);
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
    }

    public void copySelectionToClipboard(boolean copyHeader)
    {
        String lineSeparator = ContentUtils.getDefaultLineSeparator();
        List<Integer> colsSelected = new ArrayList<Integer>();
        int firstCol = Integer.MAX_VALUE, lastCol = Integer.MIN_VALUE;
        int firstRow = Integer.MAX_VALUE;
        Collection<GridPos> selection = getSelection();
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
        if (copyHeader) {
            for (int colIndex : colsSelected) {
                GridColumn column = grid.getColumn(colIndex);
                if (tdt.length() > 0) {
                    tdt.append('\t');
                }
                tdt.append(column.getText());
            }
            tdt.append(lineSeparator);
        }
        int prevRow = firstRow;
        int prevCol = firstCol;
        for (GridPos pos : selection) {
            if (pos.row > prevRow) {
                if (prevCol < lastCol) {
                    for (int i = prevCol; i < lastCol; i++) {
                        if (colsSelected.contains(i)) {
                            tdt.append('\t');
                        }
                    }
                }
                tdt.append(lineSeparator);
                prevRow = pos.row;
                prevCol = firstCol;
            }
            if (pos.col > prevCol) {
                for (int i = prevCol; i < pos.col; i++) {
                    if (colsSelected.contains(i)) {
                        tdt.append('\t');
                    }
                }
                prevCol = pos.col;
            }
            // Make some formatting
            // We don't want to use locale-specific formatter because numbers and dates
            // become not valid for SQL queries
            Object element = contentProvider.getElement(pos);
            String text;
            if (element instanceof Number) {
                text = element.toString();
            } else {
                text = contentLabelProvider.getText(pos);
            }
            tdt.append(text == null ? "" : text);
        }
        if (tdt.length() > 0) {
            TextTransfer textTransfer = TextTransfer.getInstance();
            clipboard.setContents(
                new Object[]{tdt.toString()},
                new Transfer[]{textTransfer});
        }
    }

    private void hookContextMenu()
    {
        MenuManager menuMgr = new MenuManager();
        Menu menu = menuMgr.createContextMenu(grid);
        menuMgr.addMenuListener(new IMenuListener() {
            @Override
            public void menuAboutToShow(IMenuManager manager)
            {
/*
                IAction copyAction = new Action("Copy selection") {
                    public void run()
                    {
                        copySelectionToClipboard(false);
                    }
                };
                copyAction.setEnabled(grid.getCellSelectionCount() > 0);
                copyAction.setActionDefinitionId(IWorkbenchCommandConstants.EDIT_COPY);

                IAction copySpecialAction = new Action("Copy selection with header") {
                    public void run()
                    {
                        copySelectionToClipboard(true);
                    }
                };
                copySpecialAction.setEnabled(grid.getCellSelectionCount() > 0);
                copySpecialAction.setActionDefinitionId(ICommandIds.CMD_COPY_SPECIAL);

                IAction selectAllAction = new Action("Select All") {
                    public void run()
                    {
                        grid.selectAll();
                    }
                };
                selectAllAction.setEnabled(grid.getCellSelectionCount() > 0);
                selectAllAction.setActionDefinitionId(IWorkbenchCommandConstants.EDIT_SELECT_ALL);

                manager.add(copyAction);
                manager.add(copySpecialAction);
                manager.add(selectAllAction);
*/
                manager.add(new GroupMarker(IWorkbenchActionConstants.MB_ADDITIONS));

                // Let controlles to provide it's own menu items
                spreadsheetController.fillContextMenu(grid.getFocusCell(), manager);
            }
        });
        menuMgr.setRemoveAllWhenShown(true);
        grid.setMenu(menu);
        site.registerContextMenu(menuMgr, selectionProvider);
    }

    public void openCellViewer(final boolean inline)
    {
        if (spreadsheetController == null) {
            return;
        }
        // The control that will be the editor must be a child of the Table
        final GridPos focusCell = grid.getFocusCell();
        //GridPos pos = getPosFromPoint(event.x, event.y);
        if (focusCell == null || focusCell.row < 0 || focusCell.col < 0) {
            return;
        }
        if (!spreadsheetController.isValidCell(focusCell)) {
            return;
        }
        //GridItem item = grid.getItem(focusCell.y);

        Composite placeholder = null;
        if (inline) {
            if (spreadsheetController.isReadOnly()) {
                return;
            }
            cancelInlineEditor();

            placeholder = new Composite(grid, SWT.NONE);
            placeholder.setFont(grid.getFont());
            placeholder.setLayout(new FillLayout());

            GridData gd = new GridData(GridData.FILL_BOTH);
            gd.horizontalIndent = 0;
            gd.verticalIndent = 0;
            gd.grabExcessHorizontalSpace = true;
            gd.grabExcessVerticalSpace = true;
            placeholder.setLayoutData(gd);
        }
        boolean editSuccess = spreadsheetController.showCellEditor(focusCell, inline, placeholder);
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
                tableEditor.setEditor(placeholder, focusCell.col, focusCell.row);
            } else {
                // No editor was created so just drop placeholder
                placeholder.dispose();
            }
        }
    }

    public void cancelInlineEditor()
    {
        Control oldEditor = tableEditor.getEditor();
        if (oldEditor != null) {
            oldEditor.dispose();
            tableEditor.setEditor(null);
            this.getGrid().setFocus();
        }
    }

    public int getItemCount()
    {
        return grid.getItemCount();
    }

    public int getColumnsCount()
    {
        return grid.getColumnCount();
    }

    public void redrawGrid()
    {
        Rectangle bounds = grid.getBounds();
        grid.redraw(bounds.x, bounds.y, bounds.width, bounds.height, true);
    }

    public boolean isRowVisible(int rowNum)
    {
        return rowNum >= grid.getTopIndex() && rowNum <= grid.getBottomIndex();
    }

}
