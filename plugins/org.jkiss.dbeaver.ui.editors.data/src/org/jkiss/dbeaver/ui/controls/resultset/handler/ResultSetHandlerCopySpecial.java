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

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.commands.IElementUpdater;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.menus.UIElement;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.data.DBDDisplayFormat;
import org.jkiss.dbeaver.ui.ActionUtils;
import org.jkiss.dbeaver.ui.IActionConstants;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.UIWidgets;
import org.jkiss.dbeaver.ui.controls.ValueFormatSelector;
import org.jkiss.dbeaver.ui.controls.resultset.IResultSetController;
import org.jkiss.dbeaver.ui.controls.resultset.ResultSetCopySettings;
import org.jkiss.dbeaver.ui.controls.resultset.ResultSetUtils;
import org.jkiss.dbeaver.ui.controls.resultset.internal.ResultSetMessages;
import org.jkiss.dbeaver.ui.dialogs.BaseDialog;
import org.jkiss.utils.CommonUtils;

import java.util.Map;

/**
 * Copy special handler
 */
public class ResultSetHandlerCopySpecial extends ResultSetHandlerMain implements IElementUpdater {

    public static final Log log = Log.getLog(ResultSetHandlerCopySpecial.class);
    public static final String CMD_COPY_SPECIAL = IActionConstants.CMD_COPY_SPECIAL;
    public static final String CMD_COPY_SPECIAL_LAST = IActionConstants.CMD_COPY_SPECIAL_LAST;
    private static ResultSetCopySettings copySettingsLast = null;


    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {

        IResultSetController resultSet = getActiveResultSet(HandlerUtil.getActivePart(event));
        if (resultSet == null) {
            return null;
        }
        switch (event.getCommand().getId()) {
            case CMD_COPY_SPECIAL:
                showAdvancedCopyDialog(resultSet, HandlerUtil.getActiveShell(event));
                break;
            case CMD_COPY_SPECIAL_LAST:
                if (copySettingsLast == null) {
                    showAdvancedCopyDialog(resultSet, HandlerUtil.getActiveShell(event));
                } else {
                    ResultSetUtils.copyToClipboard(
                        resultSet.getActivePresentation().copySelection(copySettingsLast));
                }
                break;
            default:
                log.warn(String.format("Unexpected command id: %s",  event.getCommand().getId()));
                break;
        }
        return null;
    }

    public void showAdvancedCopyDialog(IResultSetController resultSet, Shell shell) {
        AdvancedCopyConfigDialog configDialog = new AdvancedCopyConfigDialog(shell);
        if (configDialog.open() == IDialogConstants.OK_ID) {
            copySettingsLast = configDialog.copySettings;
            ResultSetUtils.copyToClipboard(resultSet.getActivePresentation().copySelection(configDialog.copySettings));
        }
    }

    @Override
    public void updateElement(UIElement element, Map parameters)
    {
        element.setText(ResultSetMessages.actions_spreadsheet_copy_special);
    }

    public static class CopyConfigDialog extends BaseDialog {

        static final String PARAM_COL_DELIMITER = "delimiter";
        static final String PARAM_ROW_DELIMITER = "rowDelimiter";
        static final String PARAM_QUOTE_STRING = "quoteString";

        protected final IDialogSettings settings;

        private Combo colDelimCombo;
        private Combo rowDelimCombo;
        private Combo quoteStringCombo;

        protected ResultSetCopySettings copySettings;

        protected CopyConfigDialog(Shell shell, String dialogId)
        {
            super(shell, ResultSetMessages.copy_special_options, null);
            settings = UIUtils.getDialogSettings(dialogId);
            copySettings = new ResultSetCopySettings();
            copySettings.setColumnDelimiter("\t");
            copySettings.setRowDelimiter("\n");
            copySettings.setQuoteString("\"");
            if (settings.get(PARAM_COL_DELIMITER) != null) {
                copySettings.setColumnDelimiter(settings.get(PARAM_COL_DELIMITER));
            }
            if (settings.get(PARAM_ROW_DELIMITER) != null) {
                copySettings.setRowDelimiter(settings.get(PARAM_ROW_DELIMITER));
            }
            if (settings.get(PARAM_QUOTE_STRING) != null) {
                copySettings.setQuoteString(settings.get(PARAM_QUOTE_STRING));
            }
        }

        @Override
        protected void configureShell(Shell newShell) {
            super.configureShell(newShell);
        }

        @Override
        protected Composite createDialogArea(Composite parent) {
            Composite group = super.createDialogArea(parent);
            ((GridLayout)group.getLayout()).numColumns = 2;

            createControlsBefore(group);
            colDelimCombo = UIWidgets.createDelimiterCombo(group, ResultSetMessages.copy_special_column_delimiter, new String[] {"\t", ";", ","}, copySettings.getColumnDelimiter(), false);
            rowDelimCombo = UIWidgets.createDelimiterCombo(group, ResultSetMessages.copy_special_row_delimiter, new String[] {"\n", "|", "^", ","}, copySettings.getRowDelimiter(), false);
            quoteStringCombo = UIWidgets.createDelimiterCombo(group, ResultSetMessages.copy_special_quote_character, new String[] {"\"", "'"}, copySettings.getQuoteString(), false);

            Composite placeholder = UIUtils.createPlaceholder(group, 2);

            placeholder.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
            UIUtils.createLabel(placeholder, NLS.bind(ResultSetMessages.copy_special_hint_for_hotkey,
                ActionUtils.findCommandDescription(CMD_COPY_SPECIAL_LAST, PlatformUI.getWorkbench(), true)
            ));
            createControlsAfter(group);
            return group;
        }

        protected void createControlsAfter(Composite group) {

        }

        protected void createControlsBefore(Composite group) {

        }

        @Override
        protected void okPressed() {
            copySettings.setColumnDelimiter(CommonUtils.unescapeDisplayString(colDelimCombo.getText()));
            copySettings.setRowDelimiter(CommonUtils.unescapeDisplayString(rowDelimCombo.getText()));
            copySettings.setQuoteString(CommonUtils.unescapeDisplayString(quoteStringCombo.getText()));

            settings.put(PARAM_COL_DELIMITER, copySettings.getColumnDelimiter());
            settings.put(PARAM_ROW_DELIMITER, copySettings.getRowDelimiter());
            settings.put(PARAM_QUOTE_STRING, copySettings.getQuoteString());
            super.okPressed();
        }
    }

    private static class AdvancedCopyConfigDialog extends CopyConfigDialog {

        static final String PARAM_COPY_HEADER = "copyHeader";
        static final String PARAM_COPY_ROWS = "copyRows";
        static final String PARAM_QUOTE_CELLS = "quoteCells";
        static final String PARAM_FORCE_QUOTES = "forceQuotes";
        static final String PARAM_FORMAT = "format";
        static final String PARAM_COPY_HTML = "copyHTML";

        private Button copyHeaderCheck;
        private Button copyRowsCheck;
        private Button quoteCellsCheck;
        private Button forceQuoteCheck;
        private Button copyHtmlCheck;
        private ValueFormatSelector formatSelector;

        protected AdvancedCopyConfigDialog(Shell shell)
        {
            super(shell, "AdvanceCopySettings");
            copySettings.setQuoteCells(true);
            copySettings.setCopyHeader(true);
            copySettings.setCopyRowNumbers(false);
            copySettings.setFormat(DBDDisplayFormat.UI);
            if (settings.get(PARAM_COPY_HEADER) != null) {
                copySettings.setCopyHeader(settings.getBoolean(PARAM_COPY_HEADER));
            }
            if (settings.get(PARAM_COPY_ROWS) != null) {
                copySettings.setCopyRowNumbers(settings.getBoolean(PARAM_COPY_ROWS));
            }
            if (settings.get(PARAM_QUOTE_CELLS) != null) {
                copySettings.setQuoteCells(settings.getBoolean(PARAM_QUOTE_CELLS));
            }
            if (settings.get(PARAM_FORCE_QUOTES) != null) {
                copySettings.setForceQuotes(settings.getBoolean(PARAM_FORCE_QUOTES));
            }
            if (settings.get(PARAM_FORMAT) != null) {
                copySettings.setFormat(DBDDisplayFormat.valueOf(settings.get(PARAM_FORMAT)));
            }
            if (settings.get(PARAM_COPY_HTML) != null) {
                copySettings.setCopyHTML(settings.getBoolean(PARAM_COPY_HTML));
            }
        }

        @Override
        protected void createControlsBefore(Composite group) {
            copyHeaderCheck = UIUtils.createCheckbox(group, ResultSetMessages.copy_special_copy_header_text, null, copySettings.isCopyHeader(), 2);
            copyRowsCheck = UIUtils.createCheckbox(group, ResultSetMessages.copy_special_copy_row_numbers_text, null, copySettings.isCopyRowNumbers(), 2);
            quoteCellsCheck = UIUtils.createCheckbox(group, ResultSetMessages.copy_special_quote_cell_values_text, ResultSetMessages.copy_special_quote_cell_values_tip, copySettings.isQuoteCells(), 2);
            forceQuoteCheck = UIUtils.createCheckbox(group, ResultSetMessages.copy_special_force_quote_cell_values_text, ResultSetMessages.copy_special_force_quote_cell_values_tip, copySettings.isForceQuotes(), 2);
            copyHtmlCheck = UIUtils.createCheckbox(group, ResultSetMessages.copy_special_copy_as_html_text, ResultSetMessages.copy_special_copy_as_html_tip, copySettings.isCopyHTML(), 2);

            formatSelector = new ValueFormatSelector(group);
            formatSelector.select(copySettings.getFormat());
        }

        @Override
        protected void okPressed() {
            copySettings.setCopyHeader(copyHeaderCheck.getSelection());
            copySettings.setCopyRowNumbers(copyRowsCheck.getSelection());
            copySettings.setQuoteCells(quoteCellsCheck.getSelection());
            copySettings.setForceQuotes(forceQuoteCheck.getSelection());
            copySettings.setCopyHTML(copyHtmlCheck.getSelection());
            copySettings.setFormat(formatSelector.getSelection());

            settings.put(PARAM_COPY_HEADER, copySettings.isCopyHeader());
            settings.put(PARAM_COPY_ROWS, copySettings.isCopyRowNumbers());
            settings.put(PARAM_QUOTE_CELLS, copySettings.isQuoteCells());
            settings.put(PARAM_FORCE_QUOTES, copySettings.isForceQuotes());
            settings.put(PARAM_COPY_HTML, copySettings.isCopyHTML());
            settings.put(PARAM_FORMAT, copySettings.getFormat().name());

            super.okPressed();
        }
    }
}
