/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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
import org.eclipse.jface.fieldassist.IContentProposal;
import org.eclipse.jface.fieldassist.IContentProposalProvider;
import org.eclipse.jface.resource.FontDescriptor;
import org.eclipse.jface.resource.FontRegistry;
import org.eclipse.jface.resource.StringConverter;
import org.eclipse.jface.text.IFindReplaceTarget;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.IWorkbenchCommandConstants;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.commands.IElementUpdater;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.menus.UIElement;
import org.eclipse.ui.texteditor.ITextEditorActionDefinitionIds;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.*;
import org.jkiss.dbeaver.model.data.DBDAttributeBinding;
import org.jkiss.dbeaver.model.data.DBDDisplayFormat;
import org.jkiss.dbeaver.model.data.DBDValueDefaultGenerator;
import org.jkiss.dbeaver.model.data.DBDValueHandler;
import org.jkiss.dbeaver.model.exec.DBCExecutionPurpose;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.exec.DBExecUtils;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.model.runtime.AbstractJob;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSDataContainer;
import org.jkiss.dbeaver.model.virtual.DBVEntity;
import org.jkiss.dbeaver.model.virtual.DBVUtils;
import org.jkiss.dbeaver.registry.BasePolicyDataProvider;
import org.jkiss.dbeaver.tools.transfer.DTConstants;
import org.jkiss.dbeaver.tools.transfer.database.DatabaseTransferProducer;
import org.jkiss.dbeaver.tools.transfer.registry.DataTransferProcessorDescriptor;
import org.jkiss.dbeaver.tools.transfer.ui.wizard.DataTransferWizard;
import org.jkiss.dbeaver.ui.ActionUtils;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.actions.ConnectionCommands;
import org.jkiss.dbeaver.ui.contentassist.ContentAssistUtils;
import org.jkiss.dbeaver.ui.contentassist.ContentProposalExt;
import org.jkiss.dbeaver.ui.contentassist.SmartTextContentAdapter;
import org.jkiss.dbeaver.ui.controls.resultset.*;
import org.jkiss.dbeaver.ui.controls.resultset.IResultSetController.RowPlacement;
import org.jkiss.dbeaver.ui.controls.resultset.internal.ResultSetMessages;
import org.jkiss.dbeaver.ui.controls.resultset.spreadsheet.SpreadsheetPresentation;
import org.jkiss.dbeaver.ui.data.IValueController;
import org.jkiss.dbeaver.ui.data.managers.BaseValueManager;
import org.jkiss.dbeaver.ui.editors.MultiPageAbstractEditor;
import org.jkiss.dbeaver.ui.editors.TextEditorUtils;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.utils.CommonUtils;
import org.jkiss.utils.Pair;

import java.util.List;
import java.util.*;
import java.util.stream.Collectors;

/**
 * ResultSetHandlerMain
 */
public class ResultSetHandlerMain extends AbstractHandler implements IElementUpdater {

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
    public static final String CMD_ROW_COPY_FROM_ABOVE = "org.jkiss.dbeaver.core.resultset.row.copy.from.above";
    public static final String CMD_ROW_COPY_FROM_BELOW = "org.jkiss.dbeaver.core.resultset.row.copy.from.below";
    public static final String CMD_ROW_DELETE = "org.jkiss.dbeaver.core.resultset.row.delete";
    public static final String CMD_CELL_SET_NULL = "org.jkiss.dbeaver.core.resultset.cell.setNull";
    public static final String CMD_CELL_SET_DEFAULT = "org.jkiss.dbeaver.core.resultset.cell.setDefault";
    public static final String CMD_CELL_RESET = "org.jkiss.dbeaver.core.resultset.cell.reset";
    public static final String CMD_APPLY_CHANGES = "org.jkiss.dbeaver.core.resultset.applyChanges";
    public static final String CMD_APPLY_AND_COMMIT_CHANGES = "org.jkiss.dbeaver.core.resultset.applyAndCommitChanges";
    public static final String CMD_REJECT_CHANGES = "org.jkiss.dbeaver.core.resultset.rejectChanges";
    public static final String CMD_GENERATE_SCRIPT = "org.jkiss.dbeaver.core.resultset.generateScript";
    public static final String CMD_TOGGLE_CONFIRM_SAVE = "org.jkiss.dbeaver.core.resultset.toggleConfirmSave";
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

    public static final String CMD_ZOOM_IN = "org.jkiss.dbeaver.core.resultset.zoomIn";
    public static final String CMD_ZOOM_OUT = "org.jkiss.dbeaver.core.resultset.zoomOut";

    public static final String CMD_TOGGLE_ORDER = "org.jkiss.dbeaver.core.resultset.toggleOrder";
    public static final String CMD_SELECT_ROW_COLOR = "org.jkiss.dbeaver.core.resultset.grid.selectRowColor";

    public static final String CMD_GO_TO_COLUMN = "org.jkiss.dbeaver.core.resultset.grid.gotoColumn";
    public static final String CMD_GO_TO_ROW = "org.jkiss.dbeaver.core.resultset.grid.gotoRow";
    public static final String PARAM_EXPORT_WITH_PARAM = "exportWithParameter";

    @Nullable
    public static IResultSetController getActiveResultSet(@Nullable IWorkbenchPart activePart) {
        return getActiveResultSet(activePart, false);
    }
    
    public static IResultSetController getActiveResultSet(@Nullable IWorkbenchPart activePart, boolean underMouseCursor) {
        if (activePart != null) {
            IWorkbenchPartSite site = activePart.getSite();
            if (site != null && site.getPart() != null && PlatformUI.isWorkbenchRunning()) {
                Shell shell = site.getShell();
                if (shell != null) {
                    Display display = shell.getDisplay();
                    for (Control focusControl = underMouseCursor ? display.getCursorControl() : display.getFocusControl();
                         focusControl != null;
                         focusControl = focusControl.getParent()
                    ) {
                        ResultSetViewer viewer = (ResultSetViewer) focusControl.getData(ResultSetViewer.CONTROL_ID);
                        if (viewer != null) {
                            return viewer;
                        }
                    }
                }
            }
        }

        if (activePart instanceof IResultSetProvider) {
            return ((IResultSetProvider) activePart).getResultSetController();
        } else if (activePart instanceof MultiPageAbstractEditor) {
            return getActiveResultSet(((MultiPageAbstractEditor) activePart).getActiveEditor());
        } else if (activePart != null) {
            return activePart.getAdapter(IResultSetController.class);
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
                final RowPlacement placement;

                if (rsv.getPreferenceStore().getBoolean(ResultSetPreferences.RS_EDIT_NEW_ROWS_AFTER) ^ shiftPressed) {
                    placement = RowPlacement.AFTER_SELECTION;
                } else {
                    placement = RowPlacement.BEFORE_SELECTION;
                }

                rsv.addNewRow(placement, copy, true);
                rsv.getActivePresentation().getControl().setFocus();
                break;
            }
            case CMD_ROW_COPY_FROM_ABOVE:
            case CMD_ROW_COPY_FROM_BELOW: {
                rsv.copyRowValues(actionId.equals(CMD_ROW_COPY_FROM_ABOVE), true);
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
                            new ResultSetCellLocation(attr, row, selection.getElementRowIndexes(cell)),
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
                            rsv.resetCellValue(attr, row, selection.getElementRowIndexes(cell));
                        }
                    }
                }
                rsv.redrawData(false, false);
                rsv.updatePanelsContent(false);
                break;
            }
            case CMD_APPLY_CHANGES:
            case CMD_APPLY_AND_COMMIT_CHANGES: {
                if (dataSource == null) {
                    return null;
                }
                ResultSetSaveSettings saveSettings = new ResultSetSaveSettings();

                rsv.applyChanges(null, saveSettings);

                if (actionId.equals(CMD_APPLY_AND_COMMIT_CHANGES)) {
                    // Do commit
                    ActionUtils.runCommand(ConnectionCommands.CMD_COMMIT, rsv.getSite());
                }

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
            case CMD_TOGGLE_CONFIRM_SAVE: {
                DBPPreferenceStore store = rsv.getPreferenceStore();
                store.setValue(ResultSetPreferences.RESULT_SET_CONFIRM_BEFORE_SAVE, !store.getBoolean(ResultSetPreferences.RESULT_SET_CONFIRM_BEFORE_SAVE));
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
            case CMD_GO_TO_ROW: {
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
            case CMD_GO_TO_COLUMN: {
                var currentAttribute = rsv.getActivePresentation().getCurrentAttribute();
                DBDAttributeBinding targetAttribute = askForColumnToGo(activeShell, currentAttribute, rsv.getModel().getAttributes());
                if (targetAttribute != null) {
                    rsv.getActivePresentation().setCurrentAttribute(targetAttribute);
                    rsv.getActivePresentation().scrollToRow(IResultSetPresentation.RowPosition.CURRENT);
                }
                break;
            }
            case CMD_FILTER_MENU: {
                rsv.showFiltersMenu();
                break;
            }
            case CMD_FILTER_MENU_DISTINCT: {
                DBDAttributeBinding curAttribute = rsv.getActivePresentation().getFocusAttribute();
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
                break;
            }
            case CMD_REFERENCES_MENU: {
                boolean shiftPressed = event.getTrigger() instanceof Event && ((((Event) event.getTrigger()).stateMask & SWT.SHIFT) == SWT.SHIFT);
                rsv.showReferencesMenu(shiftPressed);
                break;
            }
            case CMD_EXPORT: {
                if (BasePolicyDataProvider.getInstance().isPolicyEnabled(DTConstants.POLICY_DATA_EXPORT)) {
                    UIUtils.showMessageBox(HandlerUtil.getActiveShell(event),
                        ResultSetMessages.dialog_policy_data_export_title,
                        ResultSetMessages.dialog_policy_data_export_msg,
                        SWT.ICON_WARNING);
                } else {
                    if (event.getParameter(PARAM_EXPORT_WITH_PARAM) != null) {
                        String defProc = ResultSetHandlerOpenWith.getDefaultOpenWithProcessor();
                        if (!CommonUtils.isEmpty(defProc)) {
                            // Run "open with"
                            ActionUtils.runCommand(
                                ResultSetHandlerOpenWith.CMD_OPEN_WITH, null, Map.of(ResultSetHandlerOpenWith.PARAM_PROCESSOR_ID, defProc),
                                rsv.getSite());
                            return null;
                        }
                    }
                    List<Integer> selectedRows = new ArrayList<>();
                    for (ResultSetRow selectedRow : rsv.getSelection().getSelectedRows()) {
                        selectedRows.add(selectedRow.getRowNumber());
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
                }
                break;
            }
            case CMD_ZOOM_IN:
            case CMD_ZOOM_OUT: {
                FontRegistry fontRegistry= rsv.getSite().getWorkbenchWindow().getWorkbench().getThemeManager().getCurrentTheme().getFontRegistry();
                Font font = fontRegistry.get(presentation.getFontId());
                if (font != null) {
                    FontData[] fondData= font.getFontData();
                    if (fondData != null) {
                        int zoomFactor = actionId.equals(CMD_ZOOM_IN) ? 1 : -1;
                        FontDescriptor fd = createFontDescriptor(fondData, zoomFactor);
                        fontRegistry.put(presentation.getFontId(), fd.getFontData());
                    }
                }

                break;
            }
            case CMD_TOGGLE_ORDER: {
                final DBDAttributeBinding attr = rsv.getActivePresentation().getFocusAttribute();
                if (attr != null) {
                    rsv.toggleSortOrder(attr, null);
                }
                break;
            }
            case CMD_SELECT_ROW_COLOR: {
                if (activePart != null) {
                    ResultSetViewer resultSetViewer = activePart.getAdapter(ResultSetViewer.class);
                    if (presentation instanceof SpreadsheetPresentation) {
                        SpreadsheetPresentation ssp = (SpreadsheetPresentation) presentation;
                        UIUtils.asyncExec(() -> {
                            ColorDialog dialog = new ColorDialog(UIUtils.createCenteredShell(resultSetViewer.getControl().getShell()));
                            RGB color = dialog.open();
                            if (color != null) {
                                final DBVEntity vEntity = getColorsVirtualEntity(resultSetViewer);
                                final DBDAttributeBinding attr = rsv.getActivePresentation().getCurrentAttribute();
                                ResultSetCellLocation cellLocation = ssp.getCurrentCellLocation();
                                Object cellValue = resultSetViewer.getContainer().getResultSetController().getModel()
                                    .getCellValue(cellLocation);
                                vEntity.setColorOverride(attr, cellValue, null, StringConverter.asString(color));
                                updateColors(resultSetViewer, vEntity, true);
                            }
                        });
                    }
                }
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

    @Override
    public void updateElement(UIElement element, Map parameters) {
        if (parameters.get(PARAM_EXPORT_WITH_PARAM) != null) {
            final String processorId = ResultSetHandlerOpenWith.getDefaultOpenWithProcessor();
            final DataTransferProcessorDescriptor descriptor = ResultSetHandlerCopyAs.getActiveProcessor(processorId);

            if (descriptor == null) {
                element.setText(ActionUtils.findCommandName(CMD_EXPORT));
                element.setIcon(ActionUtils.findCommandImage(CMD_EXPORT));
                element.setTooltip(ActionUtils.findCommandDescription(CMD_EXPORT, element.getServiceLocator(), false));
            } else {
                element.setText(descriptor.getAppName());
                element.setIcon(DBeaverIcons.getImageDescriptor(descriptor.getIcon()));
                element.setTooltip(descriptor.getDescription());
            }
        }
    }
    
    private void updateColors(ResultSetViewer resultSetViewer, DBVEntity entity, boolean refresh) {
        resultSetViewer.getModel().updateColorMapping(entity, true);
        entity.persistConfiguration();
        if (refresh) {
            resultSetViewer.redrawData(false, false);
        }
    }

    @NotNull
    private DBVEntity getColorsVirtualEntity(ResultSetViewer resultSetViewer) throws IllegalStateException {
        DBSDataContainer dataContainer = resultSetViewer.getDataContainer();
        if (dataContainer == null) {
            throw new IllegalStateException("No data container");
        }
        return DBVUtils.getVirtualEntity(dataContainer, true);
    }

    static class GotoLineDialog extends InputDialog {
        private static final String DIALOG_ID = "ResultSetHandlerMain.GotoLineDialog"; //NON-NLS-1

        public GotoLineDialog(Shell parent, String title, String message, String initialValue, IInputValidator validator) {
            super(parent, title, message, initialValue, validator);
        }

        protected IDialogSettings getDialogBoundsSettings() {
            return UIUtils.getDialogSettings(DIALOG_ID);
        }
    }

    @NotNull
    private static DBPImage obtainAttributeIcon(@NotNull DBDAttributeBinding attr) {
        DBPImage image = DBValueFormatting.getObjectImage(attr.getAttribute());

        if (DBExecUtils.isAttributeReadOnly(attr)) {
            image = new DBIconComposite(image, false, null, null, null, DBIcon.OVER_LOCK);
        }

        return image;
    }

    @Nullable
    private static DBDAttributeBinding askForColumnToGo(
        @NotNull Shell activeShell,
        @Nullable DBDAttributeBinding currentAttribute,
        @NotNull DBDAttributeBinding[] attributes
    ) {
        final String DIALOG_ID = "ResultSetHandlerMain.GotoColumnDialog"; //NON-NLS-1

        Map<String, Pair<DBDAttributeBinding, IContentProposal>> attrsByName = Arrays.stream(attributes).collect(Collectors.toMap(
            a -> a.getName().toUpperCase(), 
            a -> new Pair<>(a, new ContentProposalExt(a.getName(), a.getLabel(), a.getDescription(), obtainAttributeIcon(a))), 
            (x, y) -> x
        ));

        String initialValue = currentAttribute != null ? currentAttribute.getName() : "";

        IInputValidator inputValidator = newText -> {
            if (attrsByName.containsKey(newText.toUpperCase())) {
                return null;
            } else {
                try {
                    int index = Integer.parseInt(newText);
                    if (index >= 1 && index <= attributes.length) {
                        return null;
                    }
                } catch (Throwable ex) {
                    // ignoring meaningless invalid input
                }
                return "Unknown column name"; //NON-NLS-1
            }
        };
        IContentProposalProvider proposalProvider = new IContentProposalProvider() {
            @Override
            public IContentProposal[] getProposals(String contents, int position) {
                String pattern = contents.substring(0, position).toUpperCase();
                // Our existing trie (used in SQL parsing) works fine but only for prefix-based lookups, but here we want suffix lookups.
                // Should use suffix tree instead of this for faster lookups on large attributes set:
                //     O(patternLength+resultsCount) for suffix tree VS O(patternLength*totalKnownAttrsCount) for present solution
                // See pretty dumb scale-irrelevant illustration
                //     at https://www.wolframalpha.com/input?i=Plot%5B%7Bn%2Bn*10%2Cn*n%7D%2C%7Bn%2C0%2C30%7D%5D
                // The same for MetaDataPanel::refresh(..)
                // Correctly used suffix tree also capable of giving results sorting for free, while here we are sorting explicitly.
                List<IContentProposal> entries = attrsByName.entrySet().stream()
                        .filter(kv -> kv.getKey().contains(pattern))
                        .map(kv -> kv.getValue().getSecond())
                        .collect(Collectors.toCollection(() -> new ArrayList<>(attrsByName.size())));
                entries.sort((a, b) -> { // list sorting works inplace instead of additional temporary buffer for streams
                    String x = a.getContent();
                    String y = b.getContent();
                    boolean xs = x.startsWith(pattern);
                    boolean ys = y.startsWith(pattern);
                    return (xs ^ ys) ? (xs ? -1 : 1) : x.compareToIgnoreCase(y);
                });
                return entries.toArray(new IContentProposal[entries.size()]);
            }
        };
        InputDialog dialog = new InputDialog(
            activeShell,
            ResultSetMessages.results_goto_column_dialog_title,
            ResultSetMessages.results_goto_column_dialog_message,
            initialValue,
            inputValidator
        ) {
            @Override
            protected Control createDialogArea(Composite parent) {
                Control result = super.createDialogArea(parent);
                ContentAssistUtils.installContentProposal(
                    this.getText(),
                    new SmartTextContentAdapter(),
                    proposalProvider,
                    null,
                    true,
                    true
                );
                return result;
            }

            @Override
            protected IDialogSettings getDialogBoundsSettings() {
                return UIUtils.getDialogSettings(DIALOG_ID);
            }
        };

        if (dialog.open() == Window.OK) {
            String value = dialog.getValue();
            Pair<DBDAttributeBinding, IContentProposal> selectedByName = attrsByName.get(value.toUpperCase());
            return selectedByName != null ?  selectedByName.getFirst() : attributes[Integer.parseInt(value) - 1];
        } else {
            return null;
        }
    }
}