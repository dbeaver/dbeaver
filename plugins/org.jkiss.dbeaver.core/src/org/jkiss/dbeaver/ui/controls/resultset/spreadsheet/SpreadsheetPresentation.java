/*
 * Copyright (C) 2010-2015 Serge Rieder
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
/*
 * Copyright (C) 2010-2015 Serge Rieder
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

package org.jkiss.dbeaver.ui.controls.resultset.spreadsheet;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.IFindReplaceTarget;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.commands.ICommandService;
import org.eclipse.ui.menus.CommandContributionItem;
import org.eclipse.ui.progress.UIJob;
import org.eclipse.ui.themes.ITheme;
import org.eclipse.ui.themes.IThemeManager;
import org.eclipse.ui.views.properties.IPropertySheetPage;
import org.eclipse.ui.views.properties.IPropertySource;
import org.eclipse.ui.views.properties.IPropertySourceProvider;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.DBeaverPreferences;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.core.DBeaverUI;
import org.jkiss.dbeaver.core.Log;
import org.jkiss.dbeaver.model.DBPDataKind;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.data.*;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRRunnableWithProgress;
import org.jkiss.dbeaver.model.struct.DBSDataContainer;
import org.jkiss.dbeaver.ui.ActionUtils;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.PropertyPageStandard;
import org.jkiss.dbeaver.ui.controls.lightgrid.GridCell;
import org.jkiss.dbeaver.ui.controls.lightgrid.GridPos;
import org.jkiss.dbeaver.ui.controls.lightgrid.IGridContentProvider;
import org.jkiss.dbeaver.ui.controls.lightgrid.IGridLabelProvider;
import org.jkiss.dbeaver.ui.controls.resultset.*;
import org.jkiss.dbeaver.ui.dialogs.ConfirmationDialog;
import org.jkiss.dbeaver.ui.properties.PropertyCollector;
import org.jkiss.dbeaver.utils.ContentUtils;
import org.jkiss.utils.CommonUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.List;

/**
 * Spreadsheet presentation.
 * Visualizes results as grid.
 */
public class SpreadsheetPresentation extends AbstractPresentation implements IResultSetEditor, ISelectionProvider, IStatefulControl, IAdaptable  {

    static final Log log = Log.getLog(SpreadsheetPresentation.class);

    private static final String VIEW_PANEL_VISIBLE = "viewPanelVisible";
    private static final String VIEW_PANEL_RATIO = "viewPanelRatio";

    private SashForm resultsSash;
    private Spreadsheet spreadsheet;
    private ViewValuePanel previewPane;
    private SpreadsheetValueController panelValueController;

    @Nullable
    private DBDAttributeBinding curAttribute;
    private int columnOrder = SWT.NONE;

    private final Map<SpreadsheetValueController, DBDValueEditorStandalone> openEditors = new HashMap<SpreadsheetValueController, DBDValueEditorStandalone>();

    private SpreadsheetFindReplaceTarget findReplaceTarget;
    private final List<ISelectionChangedListener> selectionChangedListenerList = new ArrayList<ISelectionChangedListener>();

    // UI modifiers
    @NotNull
    private IThemeManager themeManager;
    private IPropertyChangeListener themeChangeListener;

    private Color backgroundAdded;
    private Color backgroundDeleted;
    private Color backgroundModified;
    private Color backgroundNormal;
    private Color backgroundOdd;
    private Color backgroundReadOnly;
    private Color foregroundDefault;
    private Color foregroundNull;
    private Font boldFont;

    private boolean showOddRows = true;
    private boolean showCelIcons = true;

    public SpreadsheetPresentation() {
        findReplaceTarget = new SpreadsheetFindReplaceTarget(this);

    }

    public Spreadsheet getSpreadsheet() {
        return spreadsheet;
    }

    @Nullable
    DBPDataSource getDataSource() {
        DBSDataContainer dataContainer = controller.getDataContainer();
        return dataContainer == null ? null : dataContainer.getDataSource();
    }

    @Override
    public void createPresentation(@NotNull IResultSetController controller, @NotNull Composite parent) {
        super.createPresentation(controller, parent);

        this.boldFont = UIUtils.makeBoldFont(parent.getFont());
        this.foregroundNull = parent.getShell().getDisplay().getSystemColor(SWT.COLOR_GRAY);

        {
            resultsSash = new SashForm(parent, SWT.HORIZONTAL | SWT.SMOOTH);
            resultsSash.setBackgroundMode(SWT.INHERIT_FORCE);
            resultsSash.setLayoutData(new GridData(GridData.FILL_BOTH));
            resultsSash.setSashWidth(5);
            //resultsSash.setBackground(resultsSash.getDisplay().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND));

            this.spreadsheet = new Spreadsheet(
                resultsSash,
                SWT.MULTI | SWT.VIRTUAL | SWT.H_SCROLL | SWT.V_SCROLL,
                controller.getSite(),
                this,
                new ContentProvider(),
                new GridLabelProvider());
            this.spreadsheet.setLayoutData(new GridData(GridData.FILL_BOTH));

            this.previewPane = new ViewValuePanel(resultsSash) {
                @Override
                protected void hidePanel()
                {
                    togglePreview();
                }
            };

            final IPreferenceStore preferences = getPreferenceStore();
            int ratio = preferences.getInt(VIEW_PANEL_RATIO);
            boolean viewPanelVisible = preferences.getBoolean(VIEW_PANEL_VISIBLE);
            if (ratio <= 0) {
                ratio = 750;
            }
            resultsSash.setWeights(new int[]{ratio, 1000 - ratio});
            if (!viewPanelVisible) {
                resultsSash.setMaximizedControl(spreadsheet);
            }
            previewPane.addListener(SWT.Resize, new Listener() {
                @Override
                public void handleEvent(Event event)
                {
                    DBPDataSource dataSource = getDataSource();
                    if (dataSource != null) {
                        if (!resultsSash.isDisposed()) {
                            int[] weights = resultsSash.getWeights();
                            int ratio = weights[0];
                            DBeaverCore.getGlobalPreferenceStore().setValue(VIEW_PANEL_RATIO, ratio);
                        }
                    }
                }
            });
        }

        this.spreadsheet.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                fireSelectionChanged(new SpreadsheetSelectionImpl());
            }
        });
        this.spreadsheet.addCursorChangeListener(new Listener() {
            @Override
            public void handleEvent(Event event) {
                if (event.detail != SWT.DRAG && event.detail != SWT.DROP_DOWN) {
                    updateGridCursor((GridCell) event.data);
                }
            }
        });

        spreadsheet.addFocusListener(new FocusListener() {
            @Override
            public void focusGained(FocusEvent e) {
                SpreadsheetPresentation.this.controller.updateEditControls();
            }

            @Override
            public void focusLost(FocusEvent e) {
                SpreadsheetPresentation.this.controller.updateEditControls();
            }
        });

        this.themeManager = controller.getSite().getWorkbenchWindow().getWorkbench().getThemeManager();
        this.themeChangeListener = new IPropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent event) {
                if (event.getProperty().startsWith(ThemeConstants.RESULTS_PROP_PREFIX)) {
                    applyThemeSettings();
                }
            }
        };
        this.themeManager.addPropertyChangeListener(themeChangeListener);

        applyThemeSettings();

        this.spreadsheet.addDisposeListener(new DisposeListener() {
            @Override
            public void widgetDisposed(DisposeEvent e) {
                dispose();
            }
        });

        trackPresentationControl();
        UIUtils.enableHostEditorKeyBindingsSupport(controller.getSite(), spreadsheet);
    }

    private void dispose()
    {
        closeEditors();
        clearMetaData();

        themeManager.removePropertyChangeListener(themeChangeListener);

        UIUtils.dispose(this.boldFont);
    }

    public void scrollToRow(@NotNull RowPosition position)
    {
        boolean recordMode = controller.isRecordMode();
        ResultSetRow curRow = controller.getCurrentRow();
        ResultSetModel model = controller.getModel();

        switch (position) {
            case FIRST:
                if (recordMode) {
                    if (model.getRowCount() > 0) {
                        controller.setCurrentRow(model.getRow(0));
                    } else {
                        controller.setCurrentRow(null);
                    }
                } else {
                    spreadsheet.shiftCursor(0, -spreadsheet.getItemCount(), false);
                }
                break;
            case PREVIOUS:
                if (recordMode && curRow != null && curRow.getVisualNumber() > 0) {
                    controller.setCurrentRow(model.getRow(curRow.getVisualNumber() - 1));
                } else {
                    spreadsheet.shiftCursor(0, -1, false);
                }
                break;
            case NEXT:
                if (recordMode && curRow != null && curRow.getVisualNumber() < model.getRowCount() - 1) {
                    controller.setCurrentRow(model.getRow(curRow.getVisualNumber() + 1));
                } else {
                    spreadsheet.shiftCursor(0, 1, false);
                }
                break;
            case LAST:
                if (recordMode && model.getRowCount() > 0) {
                    controller.setCurrentRow(model.getRow(model.getRowCount() - 1));
                } else {
                    spreadsheet.shiftCursor(0, spreadsheet.getItemCount(), false);
                }
                break;
        }
        if (controller.isRecordMode()) {
            // Update focus cell
            restoreState(curAttribute);
        }
        // Update controls
        controller.updateEditControls();
        controller.updateStatusMessage();
        // Refresh meta if we are in record mode
        refreshData(recordMode, false);
    }

    @Nullable
    @Override
    public DBDAttributeBinding getCurrentAttribute() {
        return curAttribute;
    }

    @Override
    public Object saveState() {
        return curAttribute;
    }

    @Override
    public void restoreState(Object state) {
        this.curAttribute = (DBDAttributeBinding) state;
        ResultSetRow curRow = controller.getCurrentRow();
        if (curRow != null && this.curAttribute != null) {
            GridCell cell = controller.isRecordMode() ?
                new GridCell(curRow, this.curAttribute) :
                new GridCell(this.curAttribute, curRow);
            //spreadsheet.selectCell(cell);
            spreadsheet.setCursor(cell, false);
        }
    }

    private void updateGridCursor(GridCell cell)
    {
        boolean changed;
        Object newCol = cell == null ? null : cell.col;
        Object newRow = cell == null ? null : cell.row;
        ResultSetRow curRow = controller.getCurrentRow();
        if (!controller.isRecordMode()) {
            changed = curRow != newRow || curAttribute != newCol;
            if (newRow instanceof ResultSetRow && newCol instanceof DBDAttributeBinding) {
                curRow = (ResultSetRow) newRow;
                curAttribute = (DBDAttributeBinding) newCol;
            }
            controller.setCurrentRow(curRow);
        } else {
            changed = curAttribute != newRow;
            if (newRow instanceof DBDAttributeBinding) {
                curAttribute = (DBDAttributeBinding) newRow;
            }
        }
        if (changed) {
            ResultSetPropertyTester.firePropertyChange(ResultSetPropertyTester.PROP_CAN_MOVE);
            ResultSetPropertyTester.firePropertyChange(ResultSetPropertyTester.PROP_EDITABLE);
            updateValueView();
        }
    }

    @Nullable
    public String copySelectionToString(
        boolean copyHeader,
        boolean copyRowNumbers,
        boolean cut,
        String delimiter,
        DBDDisplayFormat format)
    {
        if (delimiter == null) {
            delimiter = "\t";
        }
        String lineSeparator = ContentUtils.getDefaultLineSeparator();
        List<Object> selectedColumns = spreadsheet.getColumnSelection();
        IGridLabelProvider labelProvider = spreadsheet.getLabelProvider();
        StringBuilder tdt = new StringBuilder();
        if (copyHeader) {
            if (copyRowNumbers) {
                tdt.append("#");
            }
            for (Object column : selectedColumns) {
                if (tdt.length() > 0) {
                    tdt.append(delimiter);
                }
                tdt.append(labelProvider.getText(column));
            }
            tdt.append(lineSeparator);
        }

        List<GridCell> selectedCells = spreadsheet.getCellSelection();

        GridCell prevCell = null;
        for (GridCell cell : selectedCells) {
            if (prevCell == null || cell.row != prevCell.row) {
                // Next row
                if (prevCell != null && prevCell.col != cell.col) {
                    // Fill empty row tail
                    int prevColIndex = selectedColumns.indexOf(prevCell.col);
                    for (int i = prevColIndex; i < selectedColumns.size() - 1; i++) {
                        tdt.append(delimiter);
                    }
                }
                if (prevCell != null) {
                    tdt.append(lineSeparator);
                }
                if (copyRowNumbers) {
                    tdt.append(labelProvider.getText(cell.row)).append(delimiter);
                }
            }
            if (prevCell != null && prevCell.col != cell.col) {
                int prevColIndex = selectedColumns.indexOf(prevCell.col);
                int curColIndex = selectedColumns.indexOf(cell.col);
                for (int i = prevColIndex; i < curColIndex; i++) {
                    tdt.append(delimiter);
                }
            }

            boolean recordMode = controller.isRecordMode();
            DBDAttributeBinding column = (DBDAttributeBinding)(!recordMode ?  cell.col : cell.row);
            ResultSetRow row = (ResultSetRow) (!recordMode ?  cell.row : cell.col);
            Object value = controller.getModel().getCellValue(column, row);
            String cellText = column.getValueHandler().getValueDisplayString(
                column.getAttribute(),
                value,
                format);
            tdt.append(cellText);

            if (cut) {
                DBDValueController valueController = new SpreadsheetValueController(
                    controller, column, row, DBDValueController.EditType.NONE, null);
                if (!valueController.isReadOnly()) {
                    valueController.updateValue(DBUtils.makeNullValue(valueController));
                }
            }

            prevCell = cell;
        }

        return tdt.toString();
    }

    @Override
    public void pasteFromClipboard()
    {
        DBDAttributeBinding attr = getFocusAttribute();
        ResultSetRow row = controller.getCurrentRow();
        if (attr == null || row == null) {
            return;
        }
        if (controller.isAttributeReadOnly(attr)) {
            // No inline editors for readonly columns
            return;
        }
        try {
            Object newValue = ResultSetUtils.getAttributeValueFromClipboard(attr);
            if (newValue == null) {
                return;
            }
            new SpreadsheetValueController(
                controller,
                attr,
                row,
                DBDValueController.EditType.NONE,
                null).updateValue(newValue);
        }
        catch (Exception e) {
            UIUtils.showErrorDialog(spreadsheet.getShell(), "Cannot replace cell value", null, e);
        }
    }

    @Override
    public Control getControl() {
        return spreadsheet;
    }

    @Override
    public void refreshData(boolean refreshMetadata, boolean append) {
        // Cache preferences
        IPreferenceStore preferenceStore = getPreferenceStore();
        showOddRows = preferenceStore.getBoolean(DBeaverPreferences.RESULT_SET_SHOW_ODD_ROWS);
        showCelIcons = preferenceStore.getBoolean(DBeaverPreferences.RESULT_SET_SHOW_CELL_ICONS);

        spreadsheet.refreshData(refreshMetadata);
    }

    @Override
    public void formatData(boolean refreshData) {
        reorderLocally();
        spreadsheet.refreshData(false);
    }

    @Override
    public void clearMetaData() {
        this.curAttribute = null;
        this.columnOrder = SWT.NONE;
    }

    @Override
    public void updateValueView() {
        spreadsheet.redrawGrid();
        previewValue();
    }

    @Override
    public void fillToolbar(@NotNull IToolBarManager toolBar) {
//        toolBar.insertAfter(PRES_TOOLS_BEGIN, ActionUtils.makeCommandContribution(
//            controller.getSite(),
//            SpreadsheetCommandHandler.CMD_TOGGLE_PREVIEW,
//            CommandContributionItem.STYLE_CHECK));
    }

    @Override
    public void fillMenu(@NotNull IMenuManager menu) {
        menu.add(ActionUtils.makeCommandContribution(
            controller.getSite(),
            SpreadsheetCommandHandler.CMD_TOGGLE_PREVIEW,
            CommandContributionItem.STYLE_CHECK));
    }

    @Override
    public void changeMode(boolean recordMode) {
        ResultSetRow oldRow = controller.getCurrentRow();
        DBDAttributeBinding oldAttribute = this.curAttribute;
        int rowCount = controller.getModel().getRowCount();
        if (rowCount > 0) {
            // Fix row number if needed
            if (oldRow == null) {
                oldRow = controller.getModel().getRow(0);
            } else if (oldRow.getVisualNumber() >= rowCount) {
                oldRow = controller.getModel().getRow(rowCount - 1);
            }
        }
        if (oldAttribute == null && controller.getModel().getVisibleAttributeCount() > 0) {
            oldAttribute = controller.getModel().getVisibleAttribute(0);
        }

        this.columnOrder = recordMode ? SWT.DEFAULT : SWT.NONE;
        if (oldRow != null && oldAttribute != null) {
            if (!recordMode) {
                spreadsheet.setCursor(new GridCell(oldAttribute, oldRow), false);
            } else {
                spreadsheet.setCursor(new GridCell(oldRow, oldAttribute), false);
            }
        }
        spreadsheet.layout(true, true);
        previewValue();
        //controller.setCurrentRow(oldRow);
    }

    public void fillContextMenu(@NotNull IMenuManager manager, @Nullable Object colObject, @Nullable Object rowObject) {
        final DBDAttributeBinding attr = (DBDAttributeBinding)(controller.isRecordMode() ? rowObject : colObject);
        final ResultSetRow row = (ResultSetRow)(controller.isRecordMode() ? colObject : rowObject);
        controller.fillContextMenu(manager, attr, row);

        if (attr != null && row != null) {
            final List<Object> selectedColumns = spreadsheet.getColumnSelection();
            if (!controller.isRecordMode() && !selectedColumns.isEmpty()) {
                String hideTitle;
                if (selectedColumns.size() == 1) {
                    DBDAttributeBinding columnToHide = (DBDAttributeBinding) selectedColumns.get(0);
                    hideTitle = "Hide column '" + columnToHide.getName() + "'";
                } else {
                    hideTitle = "Hide selected columns (" + selectedColumns.size() + ")";
                }
                manager.insertAfter(IResultSetController.MENU_GROUP_EDIT, new Action(hideTitle) {
                    @Override
                    public void run()
                    {
                        ResultSetModel model = controller.getModel();
                        if (selectedColumns.size() >= model.getVisibleAttributeCount()) {
                            UIUtils.showMessageBox(getControl().getShell(), "Hide columns", "Can't hide all result columns, at least one column must be visible", SWT.ERROR);
                        } else {
                            int[] columnIndexes = new int[selectedColumns.size()];
                            for (int i = 0, selectedColumnsSize = selectedColumns.size(); i < selectedColumnsSize; i++) {
                                columnIndexes[i] = model.getVisibleAttributeIndex((DBDAttributeBinding) selectedColumns.get(i));
                            }
                            Arrays.sort(columnIndexes);
                            for (int i = columnIndexes.length; i > 0; i--) {
                                model.setAttributeVisibility(model.getVisibleAttribute(columnIndexes[i - 1]), false);
                            }
                            refreshData(true, false);
                        }
                    }
                });
            }
        }
    }

    ////////////////////////////////////////////////////////////
    // Value preview

    public boolean isPreviewVisible()
    {
        return resultsSash.getMaximizedControl() == null;
    }

    public void togglePreview()
    {
        if (resultsSash.getMaximizedControl() == null) {
            resultsSash.setMaximizedControl(spreadsheet);
        } else {
            resultsSash.setMaximizedControl(null);
            previewValue();
        }
        DBeaverCore.getGlobalPreferenceStore().setValue(VIEW_PANEL_VISIBLE, isPreviewVisible());

        // Refresh elements
        ICommandService commandService = (ICommandService) controller.getSite().getService(ICommandService.class);
        if (commandService != null) {
            commandService.refreshElements(SpreadsheetCommandHandler.CMD_TOGGLE_PREVIEW, null);
        }
    }

    void previewValue()
    {
        DBDAttributeBinding attr = getFocusAttribute();
        ResultSetRow row = getFocusRow();
        if (!isPreviewVisible() || attr == null || row == null) {
            return;
        }
        if (panelValueController == null || panelValueController.getBinding() != attr) {
            panelValueController = new SpreadsheetValueController(
                controller,
                attr,
                row,
                DBDValueController.EditType.PANEL,
                previewPane.getViewPlaceholder());
        } else {
            panelValueController.setCurRow(row);
        }
        previewPane.viewValue(panelValueController);
    }

    /////////////////////////////////////////////////
    // Edit

    private void closeEditors() {
        List<DBDValueEditorStandalone> editors = new ArrayList<DBDValueEditorStandalone>(openEditors.values());
        for (DBDValueEditorStandalone editor : editors) {
            editor.closeValueEditor();
        }
        if (!openEditors.isEmpty()) {
            log.warn("Some value editors are still registered at result set: " + openEditors.size());
        }
        openEditors.clear();
    }

    @Override
    @Nullable
    public Control openValueEditor(final boolean inline)
    {
        // The control that will be the editor must be a child of the Table
        DBDAttributeBinding attr = getFocusAttribute();
        ResultSetRow row = getFocusRow();
        if (attr == null || row == null) {
            return null;
        }

        if (!inline) {
            for (SpreadsheetValueController valueController : openEditors.keySet()) {
                if (attr == valueController.getBinding() && row == valueController.getCurRow()) {
                    openEditors.get(valueController).showValueEditor();
                    return null;
                }
            }
        }
        final int handlerFeatures = attr.getValueHandler().getFeatures();
        if (handlerFeatures == DBDValueHandler.FEATURE_NONE) {
            return null;
        }
        if (inline &&
            ((handlerFeatures & DBDValueHandler.FEATURE_INLINE_EDITOR) == 0 || controller.isAttributeReadOnly(attr)) &&
            (handlerFeatures & DBDValueHandler.FEATURE_VIEWER) != 0)
        {
            // Inline editor isn't supported but panel viewer is
            // Enable panel
            if (!isPreviewVisible()) {
                togglePreview();
            }
            return null;
        }
        if (controller.isAttributeReadOnly(attr) && inline) {
            // No inline editors for readonly columns
            return null;
        }

        Composite placeholder = null;
        if (inline) {
            if (controller.isReadOnly()) {
                return null;
            }
            spreadsheet.cancelInlineEditor();

            placeholder = new Composite(spreadsheet, SWT.NONE);
            placeholder.setFont(spreadsheet.getFont());
            placeholder.setLayout(new FillLayout());

            GridData gd = new GridData(GridData.FILL_BOTH);
            gd.horizontalIndent = 0;
            gd.verticalIndent = 0;
            gd.grabExcessHorizontalSpace = true;
            gd.grabExcessVerticalSpace = true;
            placeholder.setLayoutData(gd);
        }

        SpreadsheetValueController valueController = new SpreadsheetValueController(
            controller,
            attr,
            row,
            inline ? DBDValueController.EditType.INLINE : DBDValueController.EditType.EDITOR,
            placeholder);
        final DBDValueEditor editor;
        try {
            editor = attr.getValueHandler().createEditor(valueController);
        }
        catch (Exception e) {
            UIUtils.showErrorDialog(spreadsheet.getShell(), "Cannot edit value", null, e);
            return null;
        }
        if (editor != null) {
            editor.createControl();
        }
        if (editor instanceof DBDValueEditorStandalone) {
            valueController.registerEditor((DBDValueEditorStandalone)editor);
            // show dialog in separate job to avoid block
            new UIJob("Open separate editor") {
                @Override
                public IStatus runInUIThread(IProgressMonitor monitor)
                {
                    ((DBDValueEditorStandalone)editor).showValueEditor();
                    return Status.OK_STATUS;
                }
            }.schedule();
            //((DBDValueEditorStandalone)editor).showValueEditor();
        } else {
            // Set editable value
            if (editor != null) {
                try {
                    editor.primeEditorValue(valueController.getValue());
                } catch (DBException e) {
                    log.error(e);
                }
            }
        }
        if (inline) {
            if (editor != null) {
                spreadsheet.showCellEditor(placeholder);
                return editor.getControl();
            } else {
                // No editor was created so just drop placeholder
                placeholder.dispose();
                // Probably we can just show preview panel
                if ((handlerFeatures & DBDValueHandler.FEATURE_VIEWER) != 0) {
                    // Inline editor isn't supported but panel viewer is
                    // Enable panel
                    if (!isPreviewVisible()) {
                        togglePreview();
                    }
                    return null;
                }
            }
        }
        return null;
    }

    public void resetCellValue(@NotNull Object colElement, @NotNull Object rowElement, boolean delete)
    {
        boolean recordMode = controller.isRecordMode();
        final DBDAttributeBinding attr = (DBDAttributeBinding)(recordMode ? rowElement : colElement);
        final ResultSetRow row = (ResultSetRow)(recordMode ? colElement : rowElement);
        controller.getModel().resetCellValue(attr, row);
        updateValueView();
    }

    ///////////////////////////////////////////////
    // Links

    public void navigateLink(@NotNull GridCell cell, int state) {
        boolean recordMode = controller.isRecordMode();
        final DBDAttributeBinding attr = (DBDAttributeBinding)(recordMode ? cell.row : cell.col);
        final ResultSetRow row = (ResultSetRow)(recordMode ? cell.col : cell.row);

        Object value = controller.getModel().getCellValue(attr, row);
        if (DBUtils.isNullValue(value)) {
            log.warn("Can't navigate to NULL value");
            return;
        }

        try {
            DBeaverUI.runInProgressService(new DBRRunnableWithProgress() {
                @Override
                public void run(DBRProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
                    try {
                        controller.navigateAssociation(monitor, attr, row);
                    } catch (DBException e) {
                        throw new InvocationTargetException(e);
                    }
                }
            });
        } catch (InvocationTargetException e) {
            UIUtils.showErrorDialog(spreadsheet.getShell(), "Cannot navigate to the reference", null, e.getTargetException());
        } catch (InterruptedException e) {
            // ignore
        }
    }

    ///////////////////////////////////////////////
    // Themes

    private void applyThemeSettings()
    {
        ITheme currentTheme = themeManager.getCurrentTheme();
        Font rsFont = currentTheme.getFontRegistry().get(ThemeConstants.FONT_SQL_RESULT_SET);
        if (rsFont != null) {
            this.spreadsheet.setFont(rsFont);
        }
        Color previewBack = currentTheme.getColorRegistry().get(ThemeConstants.COLOR_SQL_RESULT_SET_PREVIEW_BACK);
        if (previewBack != null) {
//            this.previewPane.getViewPlaceholder().setBackground(previewBack);
//            for (Control control : this.previewPane.getViewPlaceholder().getChildren()) {
//                control.setBackground(previewBack);
//            }
        }
        this.backgroundAdded = currentTheme.getColorRegistry().get(ThemeConstants.COLOR_SQL_RESULT_CELL_NEW_BACK);
        this.backgroundDeleted = currentTheme.getColorRegistry().get(ThemeConstants.COLOR_SQL_RESULT_CELL_DELETED_BACK);
        this.backgroundModified = currentTheme.getColorRegistry().get(ThemeConstants.COLOR_SQL_RESULT_CELL_MODIFIED_BACK);
        this.backgroundOdd = currentTheme.getColorRegistry().get(ThemeConstants.COLOR_SQL_RESULT_CELL_ODD_BACK);
        this.backgroundReadOnly = currentTheme.getColorRegistry().get(ThemeConstants.COLOR_SQL_RESULT_CELL_READ_ONLY);

        this.spreadsheet.recalculateSizes();
    }

    ///////////////////////////////////////////////
    // Ordering

    private boolean supportsDataFilter()
    {
        DBSDataContainer dataContainer = controller.getDataContainer();
        return dataContainer != null &&
            (dataContainer.getSupportedFeatures() & DBSDataContainer.DATA_FILTER) == DBSDataContainer.DATA_FILTER;
    }

    private void reorderLocally()
    {
        controller.rejectChanges();
        controller.getModel().resetOrdering();
        refreshData(false, false);
    }

    public void changeSorting(Object columnElement, final int state)
    {
        if (columnElement == null) {
            columnOrder = columnOrder == SWT.DEFAULT ? SWT.DOWN : (columnOrder == SWT.DOWN ? SWT.UP : SWT.DEFAULT);
            spreadsheet.refreshData(false);
            spreadsheet.redrawGrid();
            return;
        }
        DBDDataFilter dataFilter = controller.getModel().getDataFilter();
        boolean ctrlPressed = (state & SWT.CTRL) == SWT.CTRL;
        boolean altPressed = (state & SWT.ALT) == SWT.ALT;
        if (ctrlPressed) {
            dataFilter.resetOrderBy();
        }
        DBDAttributeBinding metaColumn = (DBDAttributeBinding)columnElement;
        DBDAttributeConstraint constraint = dataFilter.getConstraint(metaColumn);
        assert constraint != null;
        //int newSort;
        if (constraint.getOrderPosition() == 0) {
            if (ResultSetUtils.isServerSideFiltering(controller) && supportsDataFilter()) {
                if (!ConfirmationDialog.confirmActionWithParams(
                    spreadsheet.getShell(),
                    DBeaverPreferences.CONFIRM_ORDER_RESULTSET,
                    metaColumn.getName()))
                {
                    return;
                }
            }
            constraint.setOrderPosition(dataFilter.getMaxOrderingPosition() + 1);
            constraint.setOrderDescending(altPressed);
        } else if (!constraint.isOrderDescending()) {
            constraint.setOrderDescending(true);
        } else {
            for (DBDAttributeConstraint con2 : dataFilter.getConstraints()) {
                if (con2.getOrderPosition() > constraint.getOrderPosition()) {
                    con2.setOrderPosition(con2.getOrderPosition() - 1);
                }
            }
            constraint.setOrderPosition(0);
            constraint.setOrderDescending(false);
        }

        if (!ResultSetUtils.isServerSideFiltering(controller) && !controller.isHasMoreData()) {
            reorderLocally();
        } else {
            controller.refreshData(null);
        }
    }


    ///////////////////////////////////////////////
    // Misc

    public IPreferenceStore getPreferenceStore() {
        return controller.getPreferenceStore();
    }

    @Override
    public Object getAdapter(Class adapter) {
        if (adapter == IPropertySheetPage.class) {
            // Show cell properties
            PropertyPageStandard page = new PropertyPageStandard();
            page.setPropertySourceProvider(new IPropertySourceProvider() {
                @Nullable
                @Override
                public IPropertySource getPropertySource(Object object)
                {
                    if (object instanceof GridCell) {
                        GridCell cell = (GridCell) object;
                        boolean recordMode = controller.isRecordMode();
                        final DBDAttributeBinding attr = (DBDAttributeBinding)(recordMode ? cell.row : cell.col);
                        final ResultSetRow row = (ResultSetRow)(recordMode ? cell.col : cell.row);
                        final SpreadsheetValueController valueController = new SpreadsheetValueController(
                            controller,
                            attr,
                            row,
                            DBDValueController.EditType.NONE,
                            null);
                        PropertyCollector props = new PropertyCollector(valueController.getBinding().getAttribute(), false);
                        props.collectProperties();
                        valueController.getValueHandler().contributeProperties(props, valueController);
                        return props;
                    }
                    return null;
                }
            });
            return page;
        } else if (adapter == IFindReplaceTarget.class) {
            return findReplaceTarget;
        }
        return null;
    }

    @Nullable
    public DBDAttributeBinding getFocusAttribute()
    {
        return controller.isRecordMode() ?
            (DBDAttributeBinding) spreadsheet.getFocusRowElement() :
            (DBDAttributeBinding) spreadsheet.getFocusColumnElement();
    }

    @Nullable
    public ResultSetRow getFocusRow()
    {
        return controller.isRecordMode() ?
            (ResultSetRow) spreadsheet.getFocusColumnElement() :
            (ResultSetRow) spreadsheet.getFocusRowElement();
    }

    ///////////////////////////////////////////////
    // Selection provider

    @Override
    public IResultSetSelection getSelection() {
        return new SpreadsheetSelectionImpl();
    }

    @Override
    public void setSelection(ISelection selection) {
        if (selection instanceof IResultSetSelection && ((IResultSetSelection) selection).getController() == getController()) {
            // It may occur on simple focus change so we won't do anything
            return;
        }
        spreadsheet.deselectAll();
        if (!selection.isEmpty() && selection instanceof IStructuredSelection) {
            List<GridPos> cellSelection = new ArrayList<GridPos>();
            for (Iterator iter = ((IStructuredSelection) selection).iterator(); iter.hasNext(); ) {
                Object cell = iter.next();
                if (cell instanceof GridPos) {
                    cellSelection.add((GridPos) cell);
                } else {
                    log.warn("Bad selection object: " + cell);
                }
            }
            spreadsheet.selectCells(cellSelection);
            spreadsheet.showSelection();
        }
        fireSelectionChanged(selection);
    }

    private void fireSelectionChanged(ISelection selection) {
        SelectionChangedEvent event = new SelectionChangedEvent(this, selection);
        for (ISelectionChangedListener listener : selectionChangedListenerList) {
            listener.selectionChanged(event);
        }
    }

    @Override
    public void addSelectionChangedListener(ISelectionChangedListener listener) {
        selectionChangedListenerList.add(listener);
    }

    @Override
    public void removeSelectionChangedListener(ISelectionChangedListener listener) {
        selectionChangedListenerList.remove(listener);
    }

    private class SpreadsheetSelectionImpl implements IResultSetSelection {

        @Nullable
        @Override
        public GridPos getFirstElement()
        {
            Collection<GridPos> ssSelection = spreadsheet.getSelection();
            if (ssSelection.isEmpty()) {
                return null;
            }
            return ssSelection.iterator().next();
        }

        @Override
        public Iterator<GridPos> iterator()
        {
            return spreadsheet.getSelection().iterator();
        }

        @Override
        public int size()
        {
            return spreadsheet.getSelection().size();
        }

        @Override
        public Object[] toArray()
        {
            return spreadsheet.getSelection().toArray();
        }

        @Override
        public List toList()
        {
            return new ArrayList<GridPos>(spreadsheet.getSelection());
        }

        @Override
        public boolean isEmpty()
        {
            return spreadsheet.getSelection().isEmpty();
        }

        @Override
        public IResultSetController getController()
        {
            return SpreadsheetPresentation.this.getController();
        }

        @Override
        public Collection<ResultSetRow> getSelectedRows()
        {
            if (controller.isRecordMode()) {
                ResultSetRow currentRow = controller.getCurrentRow();
                if (currentRow == null) {
                    return Collections.emptyList();
                }
                return Collections.singletonList(currentRow);
            } else {
                List<ResultSetRow> rows = new ArrayList<ResultSetRow>();
                Collection<Integer> rowSelection = spreadsheet.getRowSelection();
                for (Integer row : rowSelection) {
                    rows.add(controller.getModel().getRow(row));
                }
                return rows;
            }
        }
    }

    private class ContentProvider implements IGridContentProvider {

        @NotNull
        @Override
        public Object[] getElements(boolean horizontal) {
            boolean recordMode = controller.isRecordMode();
            ResultSetModel model = controller.getModel();
            if (horizontal) {
                // columns
                if (!recordMode) {
                    return model.getVisibleAttributes().toArray();
                } else {
                    Object curRow = controller.getCurrentRow();
                    return curRow == null ? new Object[0] : new Object[] {curRow};
                }
            } else {
                // rows
                if (!recordMode) {
                    return model.getAllRows().toArray();
                } else {
                    DBDAttributeBinding[] columns = model.getVisibleAttributes().toArray(new DBDAttributeBinding[model.getVisibleAttributeCount()]);
                    if (columnOrder != SWT.NONE && columnOrder != SWT.DEFAULT) {
                        Arrays.sort(columns, new Comparator<DBDAttributeBinding>() {
                            @Override
                            public int compare(DBDAttributeBinding o1, DBDAttributeBinding o2) {
                                return o1.getName().compareTo(o2.getName()) * (columnOrder == SWT.DOWN ? 1 : -1);
                            }
                        });
                    }
                    return columns;
                }
            }
        }

        @Nullable
        @Override
        public Object[] getChildren(Object element) {
            if (element instanceof DBDAttributeBinding) {
                DBDAttributeBinding binding = (DBDAttributeBinding) element;
                switch (binding.getDataKind()) {
                    case ARRAY:
                        if (controller.isRecordMode()) {
                            ResultSetRow curRow = controller.getCurrentRow();
                            if (curRow != null) {
                                Object value = controller.getModel().getCellValue(binding, curRow);
                                if (value instanceof DBDCollection) {
                                    return curRow.getCollectionData(
                                        binding,
                                        ((DBDCollection)value)).getElements();
                                }
                            }
                            return null;
                        }
                    case STRUCT:
                    case DOCUMENT:
                    case ANY:
                        if (binding.getNestedBindings() != null) {
                            return binding.getNestedBindings().toArray();
                        }
                        break;
                }
            }

            return null;
        }

        @Override
        public int getSortOrder(@Nullable Object column)
        {
            if (column instanceof DBDAttributeBinding) {
                DBDAttributeBinding binding = (DBDAttributeBinding) column;
                if (!binding.hasNestedBindings()) {
                    DBDAttributeConstraint co = controller.getModel().getDataFilter().getConstraint(binding);
                    if (co != null && co.getOrderPosition() > 0) {
                        return co.isOrderDescending() ? SWT.UP : SWT.DOWN;
                    }
                    return SWT.DEFAULT;
                }
            } else if (column == null && controller.isRecordMode()) {
                // Columns order in record mode
                return columnOrder;
            }
            return SWT.NONE;
        }

        @Override
        public ElementState getDefaultState(@NotNull Object element) {
            if (element instanceof DBDAttributeBinding) {
                DBDAttributeBinding binding = (DBDAttributeBinding) element;
                switch (binding.getAttribute().getDataKind()) {
                    case STRUCT:
                    case DOCUMENT:
                        return ElementState.EXPANDED;
                    case ARRAY:
                        ResultSetRow curRow = controller.getCurrentRow();
                        if (curRow != null) {
                            Object cellValue = controller.getModel().getCellValue(binding, curRow);
                            if (cellValue instanceof DBDCollection && ((DBDCollection) cellValue).getItemCount() < 3) {
                                return ElementState.EXPANDED;
                            }
                        }
                        return ElementState.COLLAPSED;
                    default:
                        break;
                }
            }
            return ElementState.NONE;
        }

        @Override
        public int getCellState(Object colElement, Object rowElement) {
            int state = STATE_NONE;
            boolean recordMode = controller.isRecordMode();
            DBDAttributeBinding attr = (DBDAttributeBinding)(recordMode ? rowElement : colElement);
            ResultSetRow row = (ResultSetRow)(recordMode ? colElement : rowElement);
            Object value = controller.getModel().getCellValue(attr, row);
            if (!CommonUtils.isEmpty(attr.getReferrers()) && !DBUtils.isNullValue(value)) {
                state |= STATE_LINK;
            }
            return state;
        }

        @Override
        public void dispose()
        {
        }

        @Override
        public void inputChanged(Viewer viewer, Object oldInput, Object newInput)
        {
        }

        @Nullable
        @Override
        public Object getCellValue(Object colElement, Object rowElement, boolean formatString)
        {
            DBDAttributeBinding attr = (DBDAttributeBinding)(rowElement instanceof DBDAttributeBinding ? rowElement : colElement);
            ResultSetRow row = (ResultSetRow)(colElement instanceof ResultSetRow ? colElement : rowElement);
            int rowNum = row.getVisualNumber();
            Object value = controller.getModel().getCellValue(attr, row);

            boolean recordMode = controller.isRecordMode();
            if (rowNum > 0 && rowNum == controller.getModel().getRowCount() - 1 && (recordMode || spreadsheet.isRowVisible(rowNum)) && controller.isHasMoreData()) {
                controller.readNextSegment();
            }

            if (formatString) {
                if (recordMode) {
                    if (attr.getDataKind() == DBPDataKind.ARRAY && value instanceof DBDCollection) {
                        return "[" + ((DBDCollection) value).getItemCount() + "]";
                    } else if (attr.getDataKind() == DBPDataKind.STRUCT && value instanceof DBDStructure) {
                        return "[" + ((DBDStructure) value).getDataType().getName() + "]";
                    }
                }
                return attr.getValueHandler().getValueDisplayString(
                    attr.getAttribute(),
                    value,
                    DBDDisplayFormat.UI);
            } else {
                return value;
            }
        }

        @Nullable
        @Override
        public Image getCellImage(Object colElement, Object rowElement)
        {
            if (!showCelIcons) {
                return null;
            }
            DBDAttributeBinding attr = (DBDAttributeBinding)(controller.isRecordMode() ? rowElement : colElement);
            if ((attr.getValueHandler().getFeatures() & DBDValueHandler.FEATURE_SHOW_ICON) != 0) {
                return DBUtils.getTypeImage(attr.getMetaAttribute());
            } else {
                return null;
            }
        }

        @NotNull
        @Override
        public String getCellText(Object colElement, Object rowElement)
        {
            return String.valueOf(getCellValue(colElement, rowElement, true));
        }

        @Nullable
        @Override
        public Color getCellForeground(Object colElement, Object rowElement)
        {
            Object value = getCellValue(colElement, rowElement, false);
            if (DBUtils.isNullValue(value)) {
                return foregroundNull;
            } else {
                if (foregroundDefault == null) {
                    foregroundDefault = controller.getDefaultForeground();
                }
                return foregroundDefault;
            }
        }

        @Nullable
        @Override
        public Color getCellBackground(Object colElement, Object rowElement)
        {
            boolean recordMode = controller.isRecordMode();
            ResultSetRow row = (ResultSetRow) (!recordMode ?  rowElement : colElement);
            DBDAttributeBinding attribute = (DBDAttributeBinding)(!recordMode ?  colElement : rowElement);
            boolean odd = row.getVisualNumber() % 2 == 0;

            if (row.getState() == ResultSetRow.STATE_ADDED) {
                return backgroundAdded;
            }
            if (row.getState() == ResultSetRow.STATE_REMOVED) {
                return backgroundDeleted;
            }
            if (row.changes != null && row.changes.containsKey(attribute)) {
                return backgroundModified;
            }
            if ((attribute.getValueHandler().getFeatures() & DBDValueHandler.FEATURE_COMPOSITE) != 0) {
                return backgroundReadOnly;
            }
            if (!recordMode && odd && showOddRows) {
                return backgroundOdd;
            }

            if (backgroundNormal == null) {
                backgroundNormal = controller.getDefaultBackground();
            }
            return backgroundNormal;
        }

        @Override
        public void resetColors() {
            backgroundNormal = null;
            foregroundDefault = null;
        }
    }

    private class GridLabelProvider implements IGridLabelProvider {
        @Nullable
        @Override
        public Image getImage(Object element)
        {
            if (element instanceof DBDAttributeBinding/* && (!isRecordMode() || !model.isDynamicMetadata())*/) {
                return DBUtils.getTypeImage(((DBDAttributeBinding) element).getMetaAttribute());
            }
            return null;
        }

        @Nullable
        @Override
        public Color getForeground(Object element) {
            if (element == null) {
                if (foregroundDefault == null) {
                    foregroundDefault = controller.getDefaultForeground();
                }
                return foregroundDefault;
            }
            return null;
        }

        @Nullable
        @Override
        public Color getBackground(Object element) {
            if (backgroundNormal == null) {
                backgroundNormal = controller.getDefaultBackground();
            }
            if (element == null) {
                return backgroundNormal;
            }

/*
            ResultSetRow row = (ResultSetRow) (!recordMode ?  element : curRow);
            boolean odd = row != null && row.getVisualNumber() % 2 == 0;
            if (!recordMode && odd && showOddRows) {
                return backgroundOdd;
            }
            return backgroundNormal;
*/
            return null;
        }

        @NotNull
        @Override
        public String getText(Object element)
        {
            if (element instanceof DBDAttributeBinding) {
                DBDAttributeBinding attributeBinding = (DBDAttributeBinding) element;
                if (CommonUtils.isEmpty(attributeBinding.getLabel())) {
                    return attributeBinding.getName();
                } else {
                    return attributeBinding.getLabel();
                }
            } else {
                if (!controller.isRecordMode()) {
                    return String.valueOf(((ResultSetRow)element).getVisualNumber() + 1);
                } else {
                    return CoreMessages.controls_resultset_viewer_value;
                }
            }
        }

        @Nullable
        @Override
        public Font getFont(Object element)
        {
            if (element instanceof DBDAttributeBinding) {
                DBDAttributeBinding attributeBinding = (DBDAttributeBinding) element;
                DBDAttributeConstraint constraint = controller.getModel().getDataFilter().getConstraint(attributeBinding);
                if (constraint != null && constraint.hasFilter()) {
                    return boldFont;
                }
            }
            return null;
        }

        @Nullable
        @Override
        public String getTooltip(Object element)
        {
            if (element instanceof DBDAttributeBinding) {
                DBDAttributeBinding attributeBinding = (DBDAttributeBinding) element;
                String name = attributeBinding.getName();
                String typeName = DBUtils.getFullTypeName(attributeBinding.getAttribute());
                return name + ": " + typeName;
            }
            return null;
        }
    }

    /////////////////////////////
    // Value controller

    public class SpreadsheetValueController extends ResultSetValueController {

        public SpreadsheetValueController(@NotNull IResultSetController controller, @NotNull DBDAttributeBinding binding, @NotNull ResultSetRow row, @NotNull EditType editType, @Nullable Composite inlinePlaceholder) {
            super(controller, binding, row, editType, inlinePlaceholder);
        }

        @Override
        public Object getValue()
        {
            return spreadsheet.getContentProvider().getCellValue(curRow, binding, false);
        }

        @Nullable
        @Override
        public org.eclipse.jface.action.IContributionManager getEditBar()
        {
            return isPreviewVisible() ? previewPane.getToolBar() : null;
        }

        @Override
        public void closeInlineEditor()
        {
            spreadsheet.cancelInlineEditor();
        }

        @Override
        public void nextInlineEditor(boolean next) {
            spreadsheet.cancelInlineEditor();
            int colOffset = next ? 1 : -1;
            int rowOffset = 0;
            //final int rowCount = spreadsheet.getItemCount();
            final int colCount = spreadsheet.getColumnCount();
            final GridPos curPosition = spreadsheet.getCursorPosition();
            if (colOffset > 0 && curPosition.col + colOffset >= colCount) {
                colOffset = -colCount;
                rowOffset = 1;
            } else if (colOffset < 0 && curPosition.col + colOffset < 0) {
                colOffset = colCount;
                rowOffset = -1;
            }
            spreadsheet.shiftCursor(colOffset, rowOffset, false);
            openValueEditor(true);
        }

        public void registerEditor(DBDValueEditorStandalone editor) {
            openEditors.put(this, editor);
        }

        @Override
        public void unregisterEditor(DBDValueEditorStandalone editor) {
            openEditors.remove(this);
        }

    }

}
