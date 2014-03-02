/*
 * Copyright (C) 2010-2014 Serge Rieder
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

import org.eclipse.jface.action.GroupMarker;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.IWorkbenchPartSite;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBeaverPreferences;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.lightgrid.*;

/**
 * ResultSetControl
 */
public class Spreadsheet extends LightGrid implements Listener {
    //static final Log log = LogFactory.getLog(Spreadsheet.class);

    public enum DoubleClickBehavior {
        NONE,
        EDITOR,
        INLINE_EDITOR
    }

    private static final String SPREADSHEET_CONTROL_ID = "org.jkiss.dbeaver.ui.spreadsheet";
    public static final int MAX_DEF_COLUMN_WIDTH = 300;
    public static final int MAX_INLINE_EDIT_WITH = 300;

    private static final int Event_ChangeCursor = 2000;

    private GridEditor tableEditor;

    @NotNull
    private final IWorkbenchPartSite site;
    @NotNull
    private final ISpreadsheetController spreadsheetController;
    @NotNull
    private final IGridContentProvider contentProvider;
    @NotNull
    private final IGridLabelProvider columnLabelProvider;
    @NotNull
    private final IGridLabelProvider rowLabelProvider;

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
        @NotNull final Composite parent,
        final int style,
        @NotNull final IWorkbenchPartSite site,
        @NotNull final ISpreadsheetController spreadsheetController,
        @NotNull final IGridContentProvider contentProvider,
        @NotNull final IGridLabelProvider columnLabelProvider,
        @NotNull final IGridLabelProvider rowLabelProvider
        )
    {
        super(parent, style);
        GridLayout layout = new GridLayout(1, true);
        layout.numColumns = 1;
        layout.makeColumnsEqualWidth = false;
        layout.marginWidth = 0;
        layout.marginHeight = 0;
        this.setLayout(layout);

        this.site = site;
        this.spreadsheetController = spreadsheetController;
        this.contentProvider = contentProvider;
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

        super.setRowHeaderVisible(true);
        super.setLinesVisible(true);
        super.setHeaderVisible(true);
        super.setMaxColumnDefWidth(MAX_DEF_COLUMN_WIDTH);

        GridData gd = new GridData(GridData.FILL_BOTH);
        super.setLayoutData(gd);

        super.addListener(SWT.MouseDoubleClick, this);
        super.addListener(SWT.MouseDown, this);
        super.addListener(SWT.KeyDown, this);
        super.addListener(LightGrid.Event_ChangeSort, this);
        //Event_ChangeSort
        gridSelectionListener = new SelectionListener() {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                //Integer row = (Integer) e.data;
                GridPos pos = (GridPos) e.data;
                Event event = new Event();
                event.x = pos.col;
                event.y = pos.row;
                notifyListeners(Event_ChangeCursor, event);
            }

            @Override
            public void widgetDefaultSelected(SelectionEvent e)
            {
            }
        };
        super.addSelectionListener(gridSelectionListener);

        tableEditor = new GridEditor(this);
        tableEditor.horizontalAlignment = SWT.LEFT;
        tableEditor.verticalAlignment = SWT.TOP;
        tableEditor.grabHorizontal = true;
        tableEditor.grabVertical = true;
        tableEditor.minimumWidth = 50;

        hookContextMenu();

        {
            UIUtils.addFocusTracker(site, SPREADSHEET_CONTROL_ID, this);

            super.addDisposeListener(new DisposeListener() {
                @Override
                public void widgetDisposed(DisposeEvent e) {
                    UIUtils.removeFocusTracker(site, Spreadsheet.this);
                    if (clipboard != null && !clipboard.isDisposed()) {
                        clipboard.dispose();
                    }
                }
            });
        }
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
        super.redraw();
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
        super.redraw();
    }

    public int getCurrentRow()
    {
        if (super.isDisposed()) {
            return -1;
        }
        return super.getFocusItem();
    }

    /**
     * Returns current cursor position
     * Note: returned object is not immutable and will be changed if user will change focus cell
     * @return cursor position.
     */
    public GridPos getCursorPosition()
    {
        if (super.isDisposed()) {
            return new GridPos(-1, -1);
        }
        return super.getFocusCell();
    }

    public void setRowHeaderWidth(int width)
    {
        super.setItemHeaderWidth(width);
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
        fakeEvent.widget = this;
        SelectionEvent selectionEvent = new SelectionEvent(fakeEvent);
        // Move row
        if (newPos.row >= 0 && newPos.row < getItemCount()) {
            selectionEvent.data = newPos.row;
            super.setFocusItem(newPos.row);
            super.showItem(newPos.row);
        }
        // Move column
        if (newPos.col >= 0 && newPos.col < getColumnsCount()) {
            super.setFocusColumn(newPos.col);
            super.showColumn(newPos.col);
        }
        if (!keepSelection) {
            super.deselectAll();
        }
        super.selectCell(newPos);

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


/*
    private void initContextHandling() {
        ((IContextService) site.getService(IContextService.class)).activateContext("org.jkiss.dbeaver.ui.spreadsheet.super.context", new Expression() {
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
                    (event.keyCode >= SWT.KEYPAD_0 && event.keyCode <= SWT.KEYPAD_9) ||
                    (event.keyCode >= 'a' && event.keyCode <= 'z') ||
                    (event.keyCode >= '0' && event.keyCode <= '9'))
                {
                    final Control editorControl = spreadsheetController.showCellEditor(true);
                    if (editorControl != null && event.keyCode != SWT.CR) {
                        // Forward the same key event to just created control
                        final Event fwdEvent = new Event();
                        fwdEvent.type = SWT.KeyDown;
                        fwdEvent.character = event.character;
                        fwdEvent.keyCode = event.keyCode;
                        final Display display = editorControl.getDisplay();
                        display.asyncExec(new Runnable() {
                            @Override
                            public void run() {
                                display.post(fwdEvent);
                            }
                        });
                    }
                } else if (event.keyCode == SWT.ESC) {
                    // Reset cell value
                    if (spreadsheetController != null) {
                        spreadsheetController.resetCellValue(super.getFocusCell(), false);
                    }
                }
                break;
            case SWT.MouseDoubleClick:
                GridPos pos = super.getCell(new Point(event.x, event.y));
                GridPos focusPos = super.getFocusCell();
                if (pos != null && focusPos != null && pos.equals(super.getFocusCell())) {
                    DoubleClickBehavior doubleClickBehavior = DoubleClickBehavior.valueOf(
                        getController().getPreferenceStore().getString(DBeaverPreferences.RESULT_SET_DOUBLE_CLICK));

                    switch (doubleClickBehavior) {
                        case NONE:
                            return;
                        case EDITOR:
                            spreadsheetController.showCellEditor(false);
                            break;
                        case INLINE_EDITOR:
                            spreadsheetController.showCellEditor(true);
                            break;
                    }
                }
                break;
            case SWT.MouseDown:
                if (event.button == 2) {
                    spreadsheetController.showCellEditor(true);
                }
                break;
            case LightGrid.Event_ChangeSort:
                spreadsheetController.changeSorting(((GridColumn) event.data).getIndex(), event.stateMask);
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

    public void reinitState(boolean clearData)
    {
        cancelInlineEditor();
        // Repack columns
        super.refreshData(clearData);

        //setCursor(new GridPos(-1, -1), false);
    }

    public int getVisibleRowsCount()
    {
        Rectangle clientArea = super.getClientArea();
        int itemHeight = super.getItemHeight();
        int count = (clientArea.height - super.getHeaderHeight() + itemHeight - 1) / itemHeight;
        if (count == 0) {
            count = 1;
        }
        return count;
    }

    public void clearGrid()
    {
        //spreadsheet.setSelection(new int[0]);
        cancelInlineEditor();
        super.removeAll();
    }

    private void hookContextMenu()
    {
        MenuManager menuMgr = new MenuManager();
        Menu menu = menuMgr.createContextMenu(this);
        menuMgr.addMenuListener(new IMenuListener() {
            @Override
            public void menuAboutToShow(IMenuManager manager)
            {
                manager.add(new GroupMarker(IWorkbenchActionConstants.MB_ADDITIONS));

                // Let controller to provide it's own menu items
                spreadsheetController.fillContextMenu(getFocusCell(), manager);
            }
        });
        menuMgr.setRemoveAllWhenShown(true);
        super.setMenu(menu);
        site.registerContextMenu(menuMgr, selectionProvider);
    }

    public void cancelInlineEditor()
    {
        Control oldEditor = tableEditor.getEditor();
        if (oldEditor != null) {
            oldEditor.dispose();
            tableEditor.setEditor(null);
            this.setFocus();
        }
    }

    @NotNull
    @Override
    public IGridContentProvider getContentProvider()
    {
        return contentProvider;
    }

    @NotNull
    @Override
    public IGridLabelProvider getColumnLabelProvider()
    {
        return columnLabelProvider;
    }

    @NotNull
    @Override
    public IGridLabelProvider getRowLabelProvider()
    {
        return rowLabelProvider;
    }

    public int getColumnsCount()
    {
        return super.getColumnCount();
    }

    public void redrawGrid()
    {
        Rectangle bounds = super.getBounds();
        super.redraw(bounds.x, bounds.y, bounds.width, bounds.height, true);
    }

    public boolean isRowVisible(int rowNum)
    {
        return rowNum >= super.getTopIndex() && rowNum <= super.getBottomIndex();
    }

    public void showCellEditor(GridPos cell, Composite editor)
    {
        int minHeight, minWidth;
        Point editorSize = editor.computeSize(SWT.DEFAULT, SWT.DEFAULT);
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
        tableEditor.setEditor(editor, cell.col, cell.row);
    }

}
