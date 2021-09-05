/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2021 DBeaver Corp and others
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
package org.jkiss.dbeaver.ui.controls.resultset.handler;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.IInputValidator;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.resource.FontDescriptor;
import org.eclipse.jface.resource.FontRegistry;
import org.eclipse.jface.text.IFindReplaceTarget;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchCommandConstants;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.internal.Workbench;
import org.eclipse.ui.texteditor.ITextEditorActionDefinitionIds;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.data.DBDAttributeBinding;
import org.jkiss.dbeaver.model.data.DBDDisplayFormat;
import org.jkiss.dbeaver.model.data.DBDValueDefaultGenerator;
import org.jkiss.dbeaver.model.data.DBDValueHandler;
import org.jkiss.dbeaver.model.exec.DBCExecutionPurpose;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.exec.DBExecUtils;
import org.jkiss.dbeaver.model.runtime.AbstractJob;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.tools.transfer.database.DatabaseTransferProducer;
import org.jkiss.dbeaver.tools.transfer.ui.wizard.DataTransferWizard;
import org.jkiss.dbeaver.ui.IActionConstants;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.resultset.*;
import org.jkiss.dbeaver.ui.data.IValueController;
import org.jkiss.dbeaver.ui.data.managers.BaseValueManager;
import org.jkiss.dbeaver.ui.editors.MultiPageAbstractEditor;
import org.jkiss.dbeaver.ui.editors.TextEditorUtils;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.utils.CommonUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * ResultSetHandlerMain
 */
public class ResultSetHandlerMain extends AbstractHandler {

    public static final String CMD_TOGGLE_PANELS = "org.jkiss.dbeaver.core.resultset.grid.togglePreview";
    public static final String CMD_ACTIVATE_PANELS = "org.jkiss.dbeaver.core.resultset.grid.activatePreview";
    public static final String CMD_TOGGLE_MAXIMIZE = "org.jkiss.dbeaver.core.resultset.grid.togglePanelMaximize";
    public static final String CMD_TOGGLE_LAYOUT = "org.jkiss.dbeaver.core.resultset.grid.toggleLayout";
    public static final String CMD_TOGGLE_MODE = "org.jkiss.dbeaver.core.resultset.toggleMode";
    public static final String CMD_FOCUS_FILTER = "org.jkiss.dbeaver.core.resultset.focus.filter";
    public static final String CMD_SWITCH_PRESENTATION = "org.jkiss.dbeaver.core.resultset.switchPresentation";
    public static final String CMD_ROW_FIRST = "org.jkiss.dbeaver.core.resultset.row.first";
    public static final String CMD_ROW_PREVIOUS = "org.jkiss.dbeaver.core.resultset.row.previous";
    public static final String CMD_ROW_NEXT = "org.jkiss.dbeaver.core.resultset.row.next";
    public static final String CMD_ROW_LAST = "org.jkiss.dbeaver.core.resultset.row.last";
    public static final String CMD_FETCH_PAGE = "org.jkiss.dbeaver.core.resultset.fetch.page";
    public static final String CMD_FETCH_ALL = "org.jkiss.dbeaver.core.resultset.fetch.all";
    public static final String CMD_COUNT = "org.jkiss.dbeaver.core.resultset.count";
    public static final String CMD_ROW_EDIT = "org.jkiss.dbeaver.core.resultset.row.edit";
    public static final String CMD_ROW_EDIT_INLINE = "org.jkiss.dbeaver.core.resultset.row.edit.inline";
    public static final String CMD_ROW_ADD = "org.jkiss.dbeaver.core.resultset.row.add";
    public static final String CMD_ROW_COPY = "org.jkiss.dbeaver.core.resultset.row.copy";
    public static final String CMD_ROW_DELETE = "org.jkiss.dbeaver.core.resultset.row.delete";
    public static final String CMD_CELL_SET_NULL = "org.jkiss.dbeaver.core.resultset.cell.setNull";
    public static final String CMD_CELL_SET_DEFAULT = "org.jkiss.dbeaver.core.resultset.cell.setDefault";
    public static final String CMD_CELL_RESET = "org.jkiss.dbeaver.core.resultset.cell.reset";
    public static final String CMD_APPLY_CHANGES = "org.jkiss.dbeaver.core.resultset.applyChanges";
    public static final String CMD_REJECT_CHANGES = "org.jkiss.dbeaver.core.resultset.rejectChanges";
    public static final String CMD_GENERATE_SCRIPT = "org.jkiss.dbeaver.core.resultset.generateScript";
    public static final String CMD_NAVIGATE_LINK = "org.jkiss.dbeaver.core.resultset.navigateLink";
    public static final String CMD_FILTER_MENU = "org.jkiss.dbeaver.core.resultset.filterMenu";
    public static final String CMD_FILTER_MENU_DISTINCT = "org.jkiss.dbeaver.core.resultset.filterMenu.distinct";
    public static final String CMD_FILTER_EDIT_SETTINGS = "org.jkiss.dbeaver.core.resultset.filterSettings";
    public static final String CMD_FILTER_SAVE_SETTING = "org.jkiss.dbeaver.core.resultset.filterSave";
    public static final String CMD_FILTER_CLEAR_SETTING = "org.jkiss.dbeaver.core.resultset.filterClear";
    public static final String CMD_REFERENCES_MENU = "org.jkiss.dbeaver.core.resultset.referencesMenu";
    public static final String CMD_COPY_COLUMN_NAMES = "org.jkiss.dbeaver.core.resultset.grid.copyColumnNames";
    public static final String CMD_COPY_ROW_NAMES = "org.jkiss.dbeaver.core.resultset.grid.copyRowNames";
    public static final String CMD_EXPORT = "org.jkiss.dbeaver.core.resultset.export";

    public static final String CMD_ZOOM_IN = "org.eclipse.ui.edit.text.zoomIn";
    public static final String CMD_ZOOM_OUT = "org.eclipse.ui.edit.text.zoomOut";

    public static final String CMD_TOGGLE_ORDER = "org.jkiss.dbeaver.core.resultset.toggleOrder";

    public static IResultSetController getActiveResultSet(IWorkbenchPart activePart) {
        if (activePart != null) {
            IWorkbenchPartSite site = activePart.getSite();
            if (site != null && !Workbench.getInstance().isClosing()) {
                Shell shell = site.getShell();
                if (shell != null) {
                    for (Control focusControl = shell.getDisplay().getFocusControl(); focusControl != null; focusControl = focusControl.getParent()) {
                        ResultSetViewer viewer = (ResultSetViewer) focusControl.getData(ResultSetViewer.CONTROL_ID);
                        if (viewer != null) {
                            return viewer;
                        }
                    }
                }
            }
        }


        if (activePart instanceof IResultSetContainer) {
            return ((IResultSetContainer) activePart).getResultSetController();
        } else if (activePart instanceof MultiPageAbstractEditor) {
            return getActiveResultSet(((MultiPageAbstractEditor) activePart).getActiveEditor());
        } else if (activePart != null) {
            return activePart.getAdapter(ResultSetViewer.class);
        } else {
            return null;
        }
    }

    @Nullable
    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        IWorkbenchPart activePart = HandlerUtil.getActivePart(event);
        if (activePart == null) {
            return null;
        }
        final ResultSetViewer rsv = (ResultSetViewer) getActiveResultSet(activePart);
        if (rsv == null) {
            return null;
        }

        Shell activeShell = HandlerUtil.getActiveShell(event);
        String actionId = event.getCommand().getId();
        IResultSetPresentation presentation = rsv.getActivePresentation();
        DBPDataSource dataSource = rsv.getDataSource();
        switch (actionId) {
            case IWorkbenchCommandConstants.FILE_REFRESH:
                rsv.refreshData(null);
                break;
            case CMD_TOGGLE_MODE:
                rsv.toggleMode();
                break;
            case CMD_TOGGLE_PANELS:
                rsv.showPanels(!rsv.isPanelsVisible(), true, true);
                break;
            case CMD_ACTIVATE_PANELS:
                rsv.togglePanelsFocus();
                break;
            case CMD_TOGGLE_MAXIMIZE:
                rsv.togglePanelsMaximize();
                break;
            case CMD_TOGGLE_LAYOUT:
                rsv.toggleVerticalLayout();
                break;
            case CMD_FOCUS_FILTER:
                rsv.switchFilterFocus();
                break;
            case CMD_SWITCH_PRESENTATION:
                rsv.switchPresentation();
                break;
            case CMD_ROW_PREVIOUS:
            case ITextEditorActionDefinitionIds.WORD_PREVIOUS:
                presentation.scrollToRow(IResultSetPresentation.RowPosition.PREVIOUS);
                break;
            case CMD_ROW_NEXT:
            case ITextEditorActionDefinitionIds.WORD_NEXT:
                presentation.scrollToRow(IResultSetPresentation.RowPosition.NEXT);
                break;
            case CMD_ROW_FIRST:
            case ITextEditorActionDefinitionIds.SELECT_WORD_PREVIOUS:
                presentation.scrollToRow(IResultSetPresentation.RowPosition.FIRST);
                break;
            case CMD_ROW_LAST:
            case ITextEditorActionDefinitionIds.SELECT_WORD_NEXT:
                presentation.scrollToRow(IResultSetPresentation.RowPosition.LAST);
                break;
            case CMD_FETCH_PAGE:
                rsv.readNextSegment();
                break;
            case CMD_FETCH_ALL:
                rsv.readAllData();
                break;
            case CMD_ROW_EDIT:
                if (presentation instanceof IResultSetEditor) {
                    ((IResultSetEditor) presentation).openValueEditor(false);
                }
                break;
            case CMD_ROW_EDIT_INLINE:
                if (presentation instanceof IResultSetEditor) {
                    ((IResultSetEditor) presentation).openValueEditor(true);
                }
                break;
            case CMD_ROW_ADD:
            case CMD_ROW_COPY: {
                boolean copy = actionId.equals(CMD_ROW_COPY);
                boolean shiftPressed = event.getTrigger() instanceof Event && ((((Event) event.getTrigger()).stateMask & SWT.SHIFT) == SWT.SHIFT);
                boolean insertAfter = rsv.getPreferenceStore().getBoolean(ResultSetPreferences.RS_EDIT_NEW_ROWS_AFTER);
                if (shiftPressed) insertAfter = !insertAfter;
                rsv.addNewRow(copy, insertAfter, true);
                break;
            }
            case CMD_ROW_DELETE:
            case IWorkbenchCommandConstants.EDIT_DELETE:
                // Execute in async mode. Otherwise if user holds DEL button pressed then all keyboard
                // events are processed in sync mode and first pain event after all keyboard events.
                // Bad UIX.
                UIUtils.asyncExec(rsv::deleteSelectedRows);
                break;
            case CMD_CELL_SET_NULL:
            case CMD_CELL_SET_DEFAULT:
            case CMD_CELL_RESET: {
                IResultSetSelection selection = rsv.getSelection();
                for (Object cell : selection.toArray()) {
                    DBDAttributeBinding attr = selection.getElementAttribute(cell);
                    ResultSetRow row = selection.getElementRow(cell);
                    if (row != null && attr != null && !DBExecUtils.isAttributeReadOnly(attr)) {
                        ResultSetValueController valueController = new ResultSetValueController(
                            rsv,
                            attr,
                            row,
                            IValueController.EditType.NONE,
                            null);
                        if (actionId.equals(CMD_CELL_SET_NULL)) {
                            valueController.updateValue(
                                BaseValueManager.makeNullValue(valueController), false);
                        } else if (actionId.equals(CMD_CELL_SET_DEFAULT)) {
                            DBDValueHandler valueHandler = valueController.getValueHandler();
                            if (valueHandler instanceof DBDValueDefaultGenerator) {
                                try (DBCSession session = rsv.getExecutionContext().openSession(new VoidProgressMonitor(), DBCExecutionPurpose.UTIL, "Generate default value")) {
                                    Object defValue = ((DBDValueDefaultGenerator) valueHandler).generateDefaultValue(session, valueController.getValueType());
                                    valueController.updateValue(defValue, false);
                                }
                            }
                        } else {
                            rsv.getModel().resetCellValue(attr, row);
                        }
                    }
                }
                rsv.redrawData(false, false);
                rsv.updatePanelsContent(false);
                break;
            }
            case CMD_APPLY_CHANGES: {
                if (dataSource == null) {
                    return null;
                }
                ResultSetSaveSettings saveSettings = new ResultSetSaveSettings();

                rsv.applyChanges(null, saveSettings);

                break;
            }
            case CMD_REJECT_CHANGES:
                rsv.rejectChanges();
                break;
            case CMD_GENERATE_SCRIPT: {
                ResultSetSaveReport saveReport = rsv.generateChangesReport();
                if (saveReport == null) {
                    return null;
                }

                ResultSetSaveSettings saveSettings = showPreviewScript(rsv, saveReport);
                if (saveSettings != null) {
                    rsv.applyChanges(null, saveSettings);
                }
                break;
            }
            case CMD_COPY_COLUMN_NAMES: {
                StringBuilder buffer = new StringBuilder();
                String columnNames = event.getParameter("columns");

                IResultSetSelection selection = rsv.getSelection();
                List<DBDAttributeBinding> attrs = selection.isEmpty() ? rsv.getModel().getVisibleAttributes() : selection.getSelectedAttributes();
                if (!CommonUtils.isEmpty(columnNames) && attrs.size() == 1) {
                    attrs = new ArrayList<>();
                    for (String colName : columnNames.split(",")) {
                        for (DBDAttributeBinding attr : rsv.getModel().getVisibleAttributes()) {
                            if (colName.equals(attr.getName())) {
                                attrs.add(attr);
                            }
                        }
                    }
                }

                ResultSetCopySettings settings = new ResultSetCopySettings();
                if (attrs.size() > 1) {
                    ResultSetHandlerCopySpecial.CopyConfigDialog configDialog = new ResultSetHandlerCopySpecial.CopyConfigDialog(activeShell, "CopyGridNamesOptionsDialog");
                    if (configDialog.open() != IDialogConstants.OK_ID) {
                        return null;
                    }
                    settings = configDialog.copySettings;
                }

                for (DBDAttributeBinding attr : attrs) {
                    if (buffer.length() > 0) {
                        buffer.append(settings.getColumnDelimiter());
                    }
                    String colName = attr.getLabel();
                    if (CommonUtils.isEmpty(colName)) {
                        colName = attr.getName();
                    }
                    if (dataSource == null) {
                        buffer.append(colName);
                    } else {
                        buffer.append(DBUtils.getQuotedIdentifier(dataSource, colName));
                    }
                }

                ResultSetUtils.copyToClipboard(buffer.toString());
                break;
            }
            case CMD_COPY_ROW_NAMES: {
                StringBuilder buffer = new StringBuilder();
                List<ResultSetRow> selectedRows = rsv.getSelection().getSelectedRows();
                ResultSetCopySettings settings = new ResultSetCopySettings();
                if (selectedRows.size() > 1) {
                    ResultSetHandlerCopySpecial.CopyConfigDialog configDialog = new ResultSetHandlerCopySpecial.CopyConfigDialog(activeShell, "CopyGridNamesOptionsDialog");
                    if (configDialog.open() != IDialogConstants.OK_ID) {
                        return null;
                    }
                    settings = configDialog.copySettings;
                }
                for (ResultSetRow row : selectedRows) {
                    if (buffer.length() > 0) {
                        buffer.append(settings.getRowDelimiter());
                    }
                    buffer.append(row.getVisualNumber() + 1);
                }
                ResultSetUtils.copyToClipboard(buffer.toString());
                break;
            }
            case IWorkbenchCommandConstants.EDIT_COPY:
                ResultSetUtils.copyToClipboard(
                    presentation.copySelection(
                        new ResultSetCopySettings(false, false, false, true, false, null, null, null, DBDDisplayFormat.EDIT)));
                break;
            case IWorkbenchCommandConstants.EDIT_PASTE:
                if (presentation instanceof IResultSetEditor) {
                    ((IResultSetEditor) presentation).pasteFromClipboard(null);
                }
                break;
            case IWorkbenchCommandConstants.EDIT_CUT:
                ResultSetUtils.copyToClipboard(
                    presentation.copySelection(
                        new ResultSetCopySettings(false, false, true, true, false, null, null, null, DBDDisplayFormat.EDIT))
                );
                break;
            case IWorkbenchCommandConstants.FILE_PRINT:
                presentation.printResultSet();
                break;
            case ITextEditorActionDefinitionIds.SMART_ENTER:
                if (presentation instanceof IResultSetEditor) {
                    ((IResultSetEditor) presentation).openValueEditor(false);
                }
                break;
            case IWorkbenchCommandConstants.EDIT_FIND_AND_REPLACE:
                IAction action = TextEditorUtils.createFindReplaceAction(
                    activeShell,
                    rsv.getAdapter(IFindReplaceTarget.class));
                action.run();
                break;
            case CMD_NAVIGATE_LINK: {
                final DBDAttributeBinding attr = rsv.getActivePresentation().getCurrentAttribute();
                if (attr != null) {
                    new AbstractJob("Navigate association") {
                        @Override
                        protected IStatus run(DBRProgressMonitor monitor) {
                            try {
                                rsv.navigateAssociation(monitor, rsv.getModel(), DBExecUtils.getAssociationByAttribute(attr), rsv.getSelection().getSelectedRows(), false);
                            } catch (DBException e) {
                                return GeneralUtils.makeExceptionStatus(e);
                            }
                            return Status.OK_STATUS;
                        }
                    }.schedule();
                }
                break;
            }
            case CMD_COUNT:
                rsv.updateRowCount();
                break;
            case IWorkbenchCommandConstants.NAVIGATE_BACKWARD_HISTORY: {
                final int hp = rsv.getHistoryPosition();
                if (hp > 0) {
                    rsv.navigateHistory(hp - 1);
                }
                break;
            }
            case IWorkbenchCommandConstants.NAVIGATE_FORWARD_HISTORY: {
                final int hp = rsv.getHistoryPosition();
                if (hp < rsv.getHistorySize() - 1) {
                    rsv.navigateHistory(hp + 1);
                }
                break;
            }
            case ITextEditorActionDefinitionIds.LINE_GOTO: {
                ResultSetRow currentRow = rsv.getCurrentRow();
                final int rowCount = rsv.getModel().getRowCount();
                if (rowCount <= 0) {
                    break;
                }
                GotoLineDialog d = new GotoLineDialog(
                    activeShell,
                    "Go to Row",
                    "Enter row number (1.." + rowCount + ")",
                    String.valueOf(currentRow == null ? 1 : currentRow.getVisualNumber() + 1),
                    input -> {
                        try {
                            int i = Integer.parseInt(input);
                            if (i <= 0 || rowCount < i) {
                                return "Row number is out of range";
                            }
                        } catch (NumberFormatException x) {
                            return "Not a number";
                        }

                        return null;
                    });
                if (d.open() == Window.OK) {
                    int line = Integer.parseInt(d.getValue());
                    rsv.setCurrentRow(rsv.getModel().getRow(line - 1));
                    rsv.getActivePresentation().scrollToRow(IResultSetPresentation.RowPosition.CURRENT);
                }
                break;
            }
            case CMD_FILTER_MENU: {
                rsv.showFiltersMenu();
                break;
            }
            case CMD_FILTER_MENU_DISTINCT: {
                DBDAttributeBinding curAttribute = rsv.getActivePresentation().getCurrentAttribute();
                if (curAttribute != null) {
                    rsv.showFiltersDistinctMenu(curAttribute, true);
                }
                break;
            }
            case CMD_FILTER_EDIT_SETTINGS: {
                rsv.showFilterSettingsDialog();
                break;
            }
            case CMD_FILTER_SAVE_SETTING: {
                rsv.saveDataFilter();
                break;
            }
            case CMD_FILTER_CLEAR_SETTING: {
                rsv.clearDataFilter(true);
            }
            case CMD_REFERENCES_MENU: {
                boolean shiftPressed = event.getTrigger() instanceof Event && ((((Event) event.getTrigger()).stateMask & SWT.SHIFT) == SWT.SHIFT);
                rsv.showReferencesMenu(shiftPressed);
                break;
            }
            case CMD_EXPORT: {
                List<Long> selectedRows = new ArrayList<>();
                for (ResultSetRow selectedRow : rsv.getSelection().getSelectedRows()) {
                    selectedRows.add(Long.valueOf(selectedRow.getRowNumber()));
                }

                ResultSetDataContainerOptions options = new ResultSetDataContainerOptions();
                options.setSelectedRows(selectedRows);
                options.setSelectedColumns(rsv.getSelection().getSelectedAttributes());

                ResultSetDataContainer dataContainer = new ResultSetDataContainer(rsv, options);
                DataTransferWizard.openWizard(
                    HandlerUtil.getActiveWorkbenchWindow(event),
                    Collections.singletonList(
                        new DatabaseTransferProducer(dataContainer, rsv.getModel().getDataFilter())),
                    null,
                    rsv.getSelection());
                break;
            }
            case CMD_ZOOM_IN:
            case CMD_ZOOM_OUT: {
                FontRegistry fontRegistry= rsv.getSite().getWorkbenchWindow().getWorkbench().getThemeManager().getCurrentTheme().getFontRegistry();
                Font font = fontRegistry.get(ThemeConstants.FONT_SQL_RESULT_SET);
                if (font != null) {
                    FontData[] fondData= font.getFontData();
                    if (fondData != null) {
                        int zoomFactor = actionId.equals(CMD_ZOOM_IN) ? 1 : -1;
                        FontDescriptor fd = createFontDescriptor(fondData, zoomFactor);
                        fontRegistry.put(ThemeConstants.FONT_SQL_RESULT_SET, fd.getFontData());
                    }
                }

                break;
            }

            case CMD_TOGGLE_ORDER: {
                final DBDAttributeBinding attr = rsv.getActivePresentation().getCurrentAttribute();
                if (attr != null) {
                    rsv.toggleSortOrder(attr, null);
                }
                break;
            }
        }


        return null;
    }

    private ResultSetSaveSettings showPreviewScript(ResultSetViewer rsv, ResultSetSaveReport saveReport) {
        SaveScriptDialog scriptDialog = new SaveScriptDialog(rsv, saveReport);
        if (scriptDialog.open() == IDialogConstants.OK_ID) {
            return scriptDialog.getSaveSettings();
        }
/*
        try {
            final List<DBEPersistAction> sqlScript = new ArrayList<>();
            try {
                UIUtils.runInProgressService(monitor -> {
                    List<DBEPersistAction> script = rsv.generateChangesScript(monitor, new ResultSetSaveSettings());
                    if (script != null) {
                        sqlScript.addAll(script);
                    }
                });
            } catch (InterruptedException e) {
                // ignore
            }
            if (!sqlScript.isEmpty()) {
                String scriptText = SQLUtils.generateScript(
                    rsv.getDataSource(),
                    sqlScript.toArray(new DBEPersistAction[0]),
                    false);
                scriptText =
                    SQLUtils.generateCommentLine(
                        rsv.getExecutionContext() == null ? null : rsv.getExecutionContext().getDataSource(),
                        "Auto-generated SQL script. Actual values for binary/complex data types may differ - what you see is the default string representation of values.") +
                    scriptText;
                UIServiceSQL serviceSQL = DBWorkbench.getService(UIServiceSQL.class);
                if (serviceSQL != null) {
                    int resID = serviceSQL.openSQLViewer(
                        rsv.getExecutionContext(),
                        UINavigatorMessages.editors_entity_dialog_preview_title,
                        UIIcon.SQL_PREVIEW,
                        scriptText,
                        showSave);
                    return resID == IDialogConstants.OK_ID;
                }
            }

        } catch (InvocationTargetException e) {
            DBWorkbench.getPlatformUI().showError("Script generation", "Can't generate changes script", e.getTargetException());
        }
*/
        return null;
    }

    private FontDescriptor createFontDescriptor(FontData[] initialFontData, int fFontSizeOffset) {
        int destFontSize= initialFontData[0].getHeight() + fFontSizeOffset;
        if (destFontSize <= 0) {
            return FontDescriptor.createFrom(initialFontData);
        }
        return FontDescriptor.createFrom(initialFontData).setHeight(destFontSize);
    }

    static class GotoLineDialog extends InputDialog {
        private static final String DIALOG_ID = "ResultSetHandlerMain.GotoLineDialog";

        public GotoLineDialog(Shell parent, String title, String message, String initialValue, IInputValidator validator) {
            super(parent, title, message, initialValue, validator);
        }

        protected IDialogSettings getDialogBoundsSettings() {
            return UIUtils.getDialogSettings(DIALOG_ID);
        }
    }

}