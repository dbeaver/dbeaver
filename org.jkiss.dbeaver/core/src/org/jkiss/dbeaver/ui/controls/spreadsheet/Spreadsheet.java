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
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
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
import org.eclipse.ui.IWorkbenchCommandConstants;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.handlers.IHandlerActivation;
import org.eclipse.ui.handlers.IHandlerService;
import org.eclipse.ui.texteditor.ITextEditorActionDefinitionIds;
import org.jkiss.dbeaver.ui.controls.lightgrid.GridEditor;
import org.jkiss.dbeaver.ui.controls.lightgrid.GridPos;
import org.jkiss.dbeaver.ui.controls.lightgrid.IGridContentProvider;
import org.jkiss.dbeaver.ui.controls.lightgrid.LightGrid;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * ResultSetControl
 */
public class Spreadsheet extends Composite implements Listener {
    static Log log = LogFactory.getLog(Spreadsheet.class);

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
    private ActionInfo[] actionsInfo;

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
        this.setLayout(layout);

        this.site = site;
        this.spreadsheetController = spreadsheetController;
        this.contentProvider = contentProvider;
        this.contentLabelProvider = contentLabelProvider;
        this.columnLabelProvider = columnLabelProvider;
        this.rowLabelProvider = rowLabelProvider;
        this.selectionProvider = new SpreadsheetSelectionProvider(this);

        foregroundNormal = getDisplay().getSystemColor(SWT.COLOR_LIST_FOREGROUND);
        foregroundLines = getDisplay().getSystemColor(SWT.COLOR_GRAY);
        foregroundSelected = getDisplay().getSystemColor(SWT.COLOR_LIST_SELECTION_TEXT);
        backgroundNormal = getDisplay().getSystemColor(SWT.COLOR_LIST_BACKGROUND);
        backgroundSelected = getDisplay().getSystemColor(SWT.COLOR_LIST_SELECTION);
        backgroundControl = getDisplay().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND);

        clipboard = new Clipboard(getDisplay());

        actionsInfo = new ActionInfo[]{
            new ActionInfo(new GridAction(IWorkbenchCommandConstants.EDIT_COPY) {
                public void run()
                {
                    copySelectionToClipboard();
                }
            }),
            new ActionInfo(new CursorMoveAction(ITextEditorActionDefinitionIds.LINE_START)),
            new ActionInfo(new CursorMoveAction(ITextEditorActionDefinitionIds.LINE_END)),
            new ActionInfo(new CursorMoveAction(ITextEditorActionDefinitionIds.TEXT_START)),
            new ActionInfo(new CursorMoveAction(ITextEditorActionDefinitionIds.TEXT_END)),
            new ActionInfo(new CursorMoveAction(ITextEditorActionDefinitionIds.SELECT_LINE_START)),
            new ActionInfo(new CursorMoveAction(ITextEditorActionDefinitionIds.SELECT_LINE_END)),
            new ActionInfo(new CursorMoveAction(ITextEditorActionDefinitionIds.SELECT_TEXT_START)),
            new ActionInfo(new CursorMoveAction(ITextEditorActionDefinitionIds.SELECT_TEXT_END)),
            new ActionInfo(new GridAction(IWorkbenchCommandConstants.EDIT_SELECT_ALL) {
                public void run()
                {
                    grid.selectAll();
                }
            }),
        };

        this.createControl(style);
    }

    public LightGrid getGrid()
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

    public Collection<GridPos> getSelection()
    {
        return grid.getCellSelection();
    }

    public GridPos getCursorPosition()
    {
        if (grid.isDisposed() || grid.getItemCount() <= 0 || grid.getColumnCount() <= 0) {
            return new GridPos(-1, -1);
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
        GridPos curPos = getCursorPosition();
        if (curPos == null) {
            return;
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
    }

    public void setCursor(GridPos newPos, boolean keepSelection)
    {
        Event fakeEvent = new Event();
        fakeEvent.widget = grid;
        SelectionEvent selectionEvent = new SelectionEvent(fakeEvent);
        // Move row
        if (newPos.row >= 0) {
            selectionEvent.data = newPos.row;
            grid.setFocusItem(newPos.row);
            grid.showItem(newPos.row);
        }
        // Move column
        if (newPos.col >= 0) {
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
        selectionEvent.x = newPos.col;
        selectionEvent.y = newPos.row;
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

        grid = new LightGrid(group, style);
        grid.setRowHeaderVisible(true);
        //grid.setFooterVisible(true);
        //spreadsheet.set
        //spreadsheet.setRowHeaderRenderer(new IGridRenderer() {
        //});

        grid.setLinesVisible(true);
        grid.setHeaderVisible(true);
        grid.setMaxColumnDefWidth(MAX_DEF_COLUMN_WIDTH);

        gd = new GridData(GridData.FILL_BOTH);
        grid.setLayoutData(gd);

        grid.addListener(SWT.MouseDoubleClick, this);
        grid.addListener(SWT.MouseDown, this);
        grid.addListener(SWT.KeyDown, this);
        grid.addListener(SWT.FocusIn, this);
        grid.addListener(SWT.FocusOut, this);

        gridSelectionListener = new SelectionListener() {
            public void widgetSelected(SelectionEvent e)
            {
                Integer row = (Integer) e.data;
                GridPos focusCell = grid.getFocusCell();
                if (focusCell != null) {
                    Event event = new Event();
                    event.data = row;
                    event.data = e.data;
                    event.x = focusCell.col;
                    event.y = focusCell.row;
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

        grid.setContentProvider(contentProvider);
        grid.setContentLabelProvider(contentLabelProvider);
        grid.setColumnLabelProvider(columnLabelProvider);
        grid.setRowLabelProvider(rowLabelProvider);
    }

    public void dispose()
    {
        this.clearGrid();
        super.dispose();
    }

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

    private void copySelectionToClipboard()
    {
        String lineSeparator = System.getProperty("line.separator");
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
        int prevRow = firstRow;
        int prevCol = firstCol;
        for (GridPos pos : selection) {
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
            String text = contentLabelProvider.getText(pos);
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
                copyAction.setEnabled(grid.getCellSelectionCount() > 0);
                copyAction.setActionDefinitionId(IWorkbenchCommandConstants.EDIT_COPY);

                IAction selectAllAction = new Action("Select All") {
                    public void run()
                    {
                        grid.selectAll();
                    }
                };
                selectAllAction.setEnabled(grid.getCellSelectionCount() > 0);
                selectAllAction.setActionDefinitionId(IWorkbenchCommandConstants.EDIT_SELECT_ALL);

                manager.add(copyAction);
                manager.add(selectAllAction);
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
        if (!spreadsheetController.isEditable() || !spreadsheetController.isCellEditable(focusCell.col, focusCell.row)) {
            return;
        }
        //GridItem item = grid.getItem(focusCell.y);

        Composite placeholder = null;
        if (inline) {
            cancelInlineEditor();

            placeholder = new Composite(grid, SWT.NONE);
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
            this.setFocus();
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
            if (actionId.equals(ITextEditorActionDefinitionIds.LINE_START) || actionId.equals(ITextEditorActionDefinitionIds.SELECT_LINE_START)) {
                shiftCursor(-grid.getColumnCount(), 0, keepSelection);
            } else if (actionId.equals(ITextEditorActionDefinitionIds.LINE_END) || actionId.equals(ITextEditorActionDefinitionIds.SELECT_LINE_END)) {
                shiftCursor(grid.getColumnCount(), 0, keepSelection);
            } else if (actionId.equals(ITextEditorActionDefinitionIds.TEXT_START) || actionId.equals(ITextEditorActionDefinitionIds.SELECT_TEXT_START)) {
                shiftCursor(-grid.getColumnCount(), -grid.getItemCount(), keepSelection);
            } else if (actionId.equals(ITextEditorActionDefinitionIds.TEXT_END) || actionId.equals(ITextEditorActionDefinitionIds.SELECT_TEXT_END)) {
                shiftCursor(grid.getColumnCount(), grid.getItemCount(), keepSelection);
            }
        }
    }

}
