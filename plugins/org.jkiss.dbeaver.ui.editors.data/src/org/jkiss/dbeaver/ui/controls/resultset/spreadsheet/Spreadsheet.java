/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2023 DBeaver Corp and others
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
package org.jkiss.dbeaver.ui.controls.resultset.spreadsheet;

import org.eclipse.jface.action.MenuManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.accessibility.ACC;
import org.eclipse.swt.accessibility.Accessible;
import org.eclipse.swt.accessibility.AccessibleControlAdapter;
import org.eclipse.swt.accessibility.AccessibleControlEvent;
import org.eclipse.swt.accessibility.AccessibleEvent;
import org.eclipse.swt.accessibility.AccessibleListener;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.MenuDetectEvent;
import org.eclipse.swt.events.MenuDetectListener;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IWorkbenchPartSite;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.data.DBDAttributeBinding;
import org.jkiss.dbeaver.model.data.DBDCollection;
import org.jkiss.dbeaver.model.data.DBDContent;
import org.jkiss.dbeaver.model.data.DBDDisplayFormat;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.lightgrid.*;
import org.jkiss.dbeaver.ui.controls.resultset.*;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.dbeaver.utils.MimeTypes;
import org.jkiss.utils.CommonUtils;

import java.util.Map;

/**
 * ResultSetControl
 */
public class Spreadsheet extends LightGrid implements Listener {
    //static final Log log = Log.getLog(Spreadsheet.class);

    public enum DoubleClickBehavior {
        NONE,
        EDITOR,
        INLINE_EDITOR,
        COPY_VALUE,
        COPY_PASTE_VALUE
    }

    public static final int MAX_INLINE_EDIT_WITH = 300;

    @NotNull
    private final SpreadsheetCellEditor tableEditor;

    @NotNull
    private final IWorkbenchPartSite site;
    @NotNull
    private final SpreadsheetPresentation presentation;
    @NotNull
    private final IGridContentProvider contentProvider;
    @NotNull
    private final IGridLabelProvider labelProvider;
    @Nullable
    private final IGridController gridController;

    private Clipboard clipboard;

    public Spreadsheet(
        @NotNull final Composite parent,
        final int style,
        @NotNull final IWorkbenchPartSite site,
        @NotNull final SpreadsheetPresentation presentation,
        @NotNull final IGridContentProvider contentProvider,
        @NotNull final IGridLabelProvider labelProvider,
        @Nullable final IGridController gridController) {
        super(parent, style);
        GridLayout layout = new GridLayout(1, true);
        layout.numColumns = 1;
        layout.makeColumnsEqualWidth = false;
        layout.marginWidth = 0;
        layout.marginHeight = 0;
        this.setLayout(layout);

        this.site = site;
        this.presentation = presentation;
        this.contentProvider = contentProvider;
        this.labelProvider = labelProvider;
        this.gridController = gridController;

        this.clipboard = new Clipboard(getDisplay());

        super.setRowHeaderVisible(true);
        super.setLinesVisible(true);
        super.setHeaderVisible(true);
        super.setMaxColumnDefWidth(DBWorkbench.getPlatform().getPreferenceStore().getInt(ResultSetPreferences.RESULT_SET_MAX_COLUMN_DEF_WIDTH));

        super.addListener(SWT.MouseDoubleClick, this);
        super.addListener(SWT.MouseDown, this);
        super.addListener(SWT.KeyDown, this);
        super.addListener(SWT.KeyUp, this);
        super.addListener(LightGrid.Event_ChangeSort, this);
        super.addListener(LightGrid.Event_NavigateLink, this);
        super.addListener(LightGrid.Event_FilterColumn, this);

        tableEditor = new SpreadsheetCellEditor(this);
        tableEditor.horizontalAlignment = SWT.LEFT;
        tableEditor.verticalAlignment = SWT.TOP;
        tableEditor.grabHorizontal = true;
        tableEditor.grabVertical = true;
        tableEditor.minimumWidth = 50;

        hookContextMenu();
        hookAccessibility();
        this.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseDown(MouseEvent e) {
                Spreadsheet.this.forceFocus();
            }
       });
        
       {
            super.addDisposeListener(e -> {
                if (clipboard != null && !clipboard.isDisposed()) {
                    clipboard.dispose();
                }
            });
        }
    }

    @NotNull
    public SpreadsheetPresentation getPresentation() {
        return presentation;
    }
    
    @NotNull
    public IWorkbenchPartSite getSite() {
        return site;
    }

    public Clipboard getClipboard() {
        return clipboard;
    }

    /**
     * Returns current cursor position
     * Note: returned object is not immutable and will be changed if user will change focus cell
     *
     * @return cursor position.
     */
    public GridPos getCursorPosition() {
        if (super.isDisposed()) {
            return new GridPos(-1, -1);
        }
        return super.getFocusPos();
    }

    @Nullable
    public GridCell getCursorCell() {
        if (super.isDisposed()) {
            return null;
        }
        return super.getFocusCell();
    }

    public boolean shiftCursor(int xOffset, int yOffset, boolean keepSelection) {
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
            if (newCol >= getColumnCount()) {
                newCol = getColumnCount() - 1;
            }
            newPos.col = newCol;
        }

        GridCell newCell = posToCell(newPos);
        if (newCell != null) {
            setCursor(newCell, keepSelection, true, true);
        }
        return true;
    }

    void setCursor(@NotNull GridCell cell, boolean keepSelection, boolean showColumn, boolean notify) {
        Event selectionEvent = new Event();
        // Move row
        selectionEvent.data = cell;
        GridPos pos = cellToPos(cell);
        if (pos.row >= 0) {
            super.setFocusItem(pos.row);
            super.showItem(pos.row);
        }

        // Move column
        if (pos.col >= 0) {
            super.setFocusColumn(pos.col);
            if (showColumn) {
                super.showColumn(pos.col);
            }
        }

        if (!keepSelection) {
            super.deselectAll();
        }
        super.selectCell(pos);

        if (notify) {
            // Change selection event
            selectionEvent.data = cell;
            notifyListeners(SWT.Selection, selectionEvent);
        }
    }

    public void addCursorChangeListener(Listener listener) {
        super.addListener(SWT.Selection, listener);
    }

    @Override
    public void handleEvent(final Event event) {
        switch (event.type) {
//            case SWT.KeyUp:
            case SWT.KeyDown:
                boolean ctrlPressed = ((event.stateMask & SWT.CTRL) != 0);

                if (!ctrlPressed &&
                    (event.keyCode == SWT.CR ||
                        (event.keyCode >= SWT.KEYPAD_0 && event.keyCode <= SWT.KEYPAD_9) ||
                        (event.keyCode == '-' || event.keyCode == '+' || event.keyCode == SWT.KEYPAD_ADD || event.keyCode == SWT.KEYPAD_SUBTRACT) ||
                        (event.keyCode >= 'a' && event.keyCode <= 'z') ||
                        (event.keyCode >= '0' && event.keyCode <= '9')) ||
                    Character.isLetterOrDigit(event.character)) {
                    Control editorControl = tableEditor.getEditor();
                    if (editorControl == null || editorControl.isDisposed()) {
                        editorControl = presentation.openValueEditor(true);
                    }
                    final SpreadsheetPresentation presentation = getPresentation();
                    final DBDAttributeBinding attribute = presentation.getCurrentAttribute();
                    if (editorControl != null && attribute != null && presentation.getController().getAttributeReadOnlyStatus(attribute) == null && event.keyCode != SWT.CR) {
                        if (!editorControl.isDisposed()) {
                            // We used to forward key even to control but it worked poorly.
                            // So let's just insert first letter (it will remove old value which must be selected for inline controls)
                            String strValue = String.valueOf(event.character);
                            if (editorControl instanceof Text) {
                                ((Text) editorControl).insert(strValue);
                            } else if (editorControl instanceof StyledText) {
                                ((StyledText) editorControl).insert(strValue);
                            }
/*
                            // Set editor value
                            // Forward the same key event to just created control
                            final Event kdEvent = new Event();
                            kdEvent.type = event.type;
                            kdEvent.character = event.character;
                            kdEvent.keyCode = event.keyCode;
                            editorControl.setFocus();
                            editorControl.getDisplay().post(kdEvent);
*/
                        }
                    }
                }
                break;
            case SWT.MouseDoubleClick:
                if (event.button != 1) {
                    return;
                }
                GridPos pos = super.getCell(new Point(event.x, event.y));
                GridPos focusPos = super.getFocusPos();
                if (pos != null && focusPos != null && pos.equals(super.getFocusPos())) {
                    DoubleClickBehavior doubleClickBehavior = CommonUtils.valueOf(DoubleClickBehavior.class,
                        presentation.getPreferenceStore().getString(ResultSetPreferences.RESULT_SET_DOUBLE_CLICK),
                        DoubleClickBehavior.NONE);

                    switch (doubleClickBehavior) {
                        case NONE:
                            return;
                        case EDITOR:
                            presentation.openValueEditor(false);
                            break;
                        case INLINE_EDITOR:
                            presentation.openValueEditor(true);
                            break;
                        case COPY_VALUE: {
                            ResultSetCopySettings copySettings = new ResultSetCopySettings();
                            copySettings.setFormat(DBDDisplayFormat.EDIT);
                            ResultSetUtils.copyToClipboard(
                                presentation.copySelection(copySettings)
                            );
                            break;
                        }

                        case COPY_PASTE_VALUE: {
                            IResultSetValueReflector valueReflector = GeneralUtils.adapt(
                                presentation.getController().getContainer(),
                                IResultSetValueReflector.class);
                            if (valueReflector != null) {
                                ResultSetCellLocation currentCellLocation = presentation.getCurrentCellLocation();
                                if (currentCellLocation.getAttribute() != null && currentCellLocation.getRow() != null) {
                                    Object cellValue = presentation.getController().getModel().getCellValue(currentCellLocation);
                                    ResultSetCopySettings copySettings = new ResultSetCopySettings();
                                    Map<Transfer, Object> selFormats = presentation.copySelection(copySettings);
                                    Object textValue = selFormats.get(TextTransfer.getInstance());
                                    if (textValue != null) {
                                        valueReflector.insertCurrentCellValue(currentCellLocation.getAttribute(), cellValue, CommonUtils.toString(textValue));
                                    }
                                }
                            } else {
                                // No value reflector - open inline editor then
                                presentation.openValueEditor(true);
                            }
                            break;
                        }
                    }
                }
                break;
            case SWT.MouseDown:
                if (event.button == 2) {
//                    presentation.openValueEditor(true);
                }
                break;
            case LightGrid.Event_ChangeSort:
                presentation.changeSorting(event.data, event.stateMask);
                break;
            case LightGrid.Event_FilterColumn:
                IGridColumn columnByElement = getColumnByElement(event.data);
                if (columnByElement != null) {
                    setFocusColumn(columnByElement.getIndex());
                    redraw();
                }
                presentation.handleColumnIconClick(event.data);
                break;
            case LightGrid.Event_NavigateLink:
                // Perform navigation async because it may change grid content and
                // we don't want to mess current grid state
                UIUtils.asyncExec(() -> presentation.navigateLink((GridCell) event.data, event.stateMask));
                break;
        }
    }

    @Override
    public void refreshData(boolean refreshColumns, boolean keepState, boolean fitValue) {
        cancelInlineEditor();
        super.refreshData(refreshColumns, keepState, fitValue);
        super.redraw();
    }

    @Override
    protected void toggleCellValue(IGridColumn column, IGridRow row) {
        presentation.toggleCellValue(column, row);
    }

    private void hookContextMenu() {
        addMenuDetectListener(new MenuDetectListener() {
            @Override
            public void menuDetected(MenuDetectEvent e) {
                if (e.detail == SWT.MENU_KEYBOARD) {
                    GridCell focusCell = getFocusCell();
                    Object focusColumnElement = getFocusColumnElement();
                    if (focusCell != null) {
                        GridPos focusPos = cellToPos(focusCell);
                        patchEventWithLocalRect(e, getCellBounds(focusPos.col, focusPos.row));
                    } else if (focusColumnElement != null) {
                        IGridColumn focusColumn = getColumnByElement(focusColumnElement);
                        patchEventWithLocalRect(e, getColumnBounds(focusColumn.getIndex()));
                    }
                }
            }
            
            private void patchEventWithLocalRect(MenuDetectEvent e, Rectangle r) {
                Point p = toDisplay(new Point(r.x + r.width, r.y));
                e.x = p.x;
                e.y = p.y;
            }
        });
        
        MenuManager menuMgr = new MenuManager(null, AbstractPresentation.RESULT_SET_PRESENTATION_CONTEXT_MENU);
        Menu menu = menuMgr.createContextMenu(this);
        menuMgr.addMenuListener(manager -> {
            // Let controller to provide it's own menu items
            GridPos focusPos = getFocusPos();
            if (isColumnContextMenuShouldBeShown()) {
                boolean isRecordMode = presentation.getController().isRecordMode();
                presentation.fillContextMenu(
                    manager,
                    isRecordMode ? null : getColumnByPosition(focusPos),
                    isRecordMode ? getRowByPosition(focusPos) : null
                );
            } else {
                presentation.fillContextMenu(
                    manager,
                    isHoveringOnRowHeader() ? null : getColumnByPosition(focusPos),
                    isHoveringOnHeader() ? null : getRowByPosition(focusPos)
                );
            }
        });
        menuMgr.setRemoveAllWhenShown(true);
        super.setMenu(menu);
        if (site instanceof IEditorSite) {
            // Exclude editor input contributions from context menu
            ((IEditorSite) site).registerContextMenu("spreadsheet_menu", menuMgr, presentation, false);
        } else {
            site.registerContextMenu(menuMgr, presentation);
        }
    }

    /**
     * Returns grid column by grid position
     */
    @Nullable
    private GridColumn getColumnByPosition(GridPos focusPos) {
        return focusPos.col >= 0 && focusPos.col < getColumnCount() ? getColumn(focusPos.col) : null;
    }

    /**
     * Returns grid row by grid position
     */
    @Nullable
    private IGridRow getRowByPosition(GridPos focusPos) {
        return focusPos.row >= 0 && focusPos.row < gridRows.length ? gridRows[focusPos.row] : null;
    }

    public void cancelInlineEditor() {
        Control oldEditor = tableEditor.getEditor();
        if (oldEditor != null) {
            if (!oldEditor.isDisposed()) {
                oldEditor.dispose();
                UIUtils.asyncExec(() -> {
                    if (UIUtils.getDisplay().getFocusControl() == null) {
                        // Set focus to spreadsheet only i
                        // #5949
                        setFocus();
                    }
                });
            }
            tableEditor.setEditor(null);
        }
    }

    @NotNull
    @Override
    public IGridContentProvider getContentProvider() {
        return contentProvider;
    }

    @NotNull
    @Override
    public IGridLabelProvider getLabelProvider() {
        return labelProvider;
    }

    @Override
    public IGridController getGridController() {
        return gridController;
    }

    public void redrawGrid() {
        Rectangle bounds = super.getBounds();
        super.redraw(bounds.x, bounds.y, bounds.width, bounds.height, true);
    }

    public boolean isRowVisible(int rowNum) {
        return rowNum >= super.getTopIndex() && rowNum <= super.getBottomIndex();
    }

    public void showCellEditor(Composite editor) {
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
        GridPos pos = getFocusPos();
        tableEditor.setEditor(editor, pos.col, pos.row);
    }


    public void packColumns(boolean fitValue) {
        refreshData(true, false, fitValue);
    }

    ////////////////////////////////////////////////////////////
    // Accessibility support
    
    private static final Map<String, String> lobMimeTypeNames = Map.of(
        MimeTypes.TEXT_HTML, "html",
        MimeTypes.TEXT_XML, "xml",
        MimeTypes.TEXT_CSS, "css",
        MimeTypes.TEXT_JSON, "json",
        MimeTypes.APPLICATION_JSON, "json",
        MimeTypes.OCTET_STREAM, "blob",
        MimeTypes.MULTIPART_ANY, "multipart",
        MimeTypes.MULTIPART_RELATED, "multipart"
    );

    private void hookAccessibility() {
        final Accessible accessible = getAccessible();

        accessible.addAccessibleListener(new GridAccessibleListener());
        addCursorChangeListener(event -> {
            accessible.selectionChanged();

            GridCell cell = getFocusCell();
            if (cell != null) {
                accessible.sendEvent(ACC.EVENT_VALUE_CHANGED, new Object[] { null,  cell.getRow().getElement()});
            }
        });

        accessible.addAccessibleControlListener(new AccessibleControlAdapter() {
            @Override
            public void getValue(AccessibleControlEvent e) {
                int rowsCount = getRowSelectionSize();
                int colsCount = getColumnSelectionSize();
                int cellsCount = getCellSelectionSize();
               
                if (cellsCount == 1) {
                    GridCell cell = posToCell(getCursorPosition());
                    String rowLabel = getLabelProvider().getText(cell.getRow());
                    String columnLabel = getLabelProvider().getText(cell.getColumn());
                    boolean isReadOnly = getContentProvider().isElementReadOnly(cell.getColumn());
                    Object rawValue = getContentProvider().getCellValue(cell.getColumn(), cell.getRow(), false);
                    String valueStr;
                    String valuePrefix = "";
                    String lobContentType = rawValue instanceof DBDContent ? ((DBDContent) rawValue).getContentType() : null;
                    String collType = rawValue instanceof DBDCollection ? ((DBDCollection) rawValue).getComponentType().getName() : null;
                    if (lobContentType != null && lobMimeTypeNames.get(lobContentType) != null) {
                        valueStr = "object of type " + lobMimeTypeNames.get(lobContentType);
                    } else if (collType != null) {
                        valueStr = "collection of type " + collType;
                    } else if (rawValue instanceof Boolean) {
                        valuePrefix = " boolean";
                        valueStr = rawValue.toString();
                    } else {
                        if (rawValue instanceof String) {
                            valuePrefix = "string";
                        } else if (rawValue instanceof Number) {
                            valuePrefix = "numeric";
                        }
                        valueStr = getContentProvider().getCellValue(cell.getColumn(), cell.getRow(), true).toString();
                    }
                    if (valueStr.isEmpty()) {
                        valueStr = "empty string";
                    }
                    if (isReadOnly) {
                        valuePrefix = (isReadOnly ? " readonly" : "") + valuePrefix;
                    }
                    e.result = "at row " + rowLabel + " column " + columnLabel + valuePrefix + " value is " + valueStr;
                } else if (rowsCount == 1) {
                    e.result = colsCount + " columns selected";
                } else if (colsCount == 1) {
                    e.result = rowsCount + " rows selected";
                } else {
                    e.result = "a freeform range selected";
                }
            }
        });
    }

    private static class GridAccessibleListener implements AccessibleListener {
        @Override
        public void getName(AccessibleEvent e) {
            e.result = "Results grid";
        }

        @Override
        public void getHelp(AccessibleEvent e) {
        }

        @Override
        public void getKeyboardShortcut(AccessibleEvent e) {
        }

        @Override
        public void getDescription(AccessibleEvent e) {
            e.result = "Results grid";
        }
    }
}
