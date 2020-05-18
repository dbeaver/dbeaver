/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
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
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.commands.IElementUpdater;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.menus.UIElement;
import org.jkiss.dbeaver.model.data.DBDDisplayFormat;
import org.jkiss.dbeaver.ui.IActionConstants;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.resultset.IResultSetController;
import org.jkiss.dbeaver.ui.controls.resultset.ResultSetCopySettings;
import org.jkiss.dbeaver.ui.controls.resultset.ResultSetUtils;
import org.jkiss.dbeaver.ui.controls.resultset.ValueFormatSelector;
import org.jkiss.dbeaver.ui.controls.resultset.internal.ResultSetMessages;
import org.jkiss.utils.CommonUtils;

import java.util.Map;

/**
 * Copy special handler
 */
public class ResultSetHandlerCopySpecial extends ResultSetHandlerMain implements IElementUpdater {

    public static final String CMD_COPY_SPECIAL = IActionConstants.CMD_COPY_SPECIAL;

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException
    {
        IResultSetController resultSet = getActiveResultSet(HandlerUtil.getActivePart(event));
        if (resultSet == null) {
            return null;
        }
        switch (event.getCommand().getId()) {
            case CMD_COPY_SPECIAL:
                showAdvancedCopyDialog(resultSet, HandlerUtil.getActiveShell(event));
                break;
        }
        return null;
    }

    public static void showAdvancedCopyDialog(IResultSetController resultSet, Shell shell) {
        AdvancedCopyConfigDialog configDialog = new AdvancedCopyConfigDialog(shell);
        if (configDialog.open() == IDialogConstants.OK_ID) {
            ResultSetUtils.copyToClipboard(resultSet.getActivePresentation().copySelectionToString(
                configDialog.copySettings));
        }
    }

    @Override
    public void updateElement(UIElement element, Map parameters)
    {
        element.setText(ResultSetMessages.actions_spreadsheet_copy_special);
    }

    public static class CopyConfigDialog extends Dialog {

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
            super(shell);
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
            newShell.setText("Options");
        }

        @Override
        protected Control createDialogArea(Composite parent)
        {
            Composite group = (Composite)super.createDialogArea(parent);
            ((GridLayout)group.getLayout()).numColumns = 2;

            createControlsBefore(group);
            colDelimCombo = UIUtils.createDelimiterCombo(group, "Column Delimiter", new String[] {"\t", ";", ","}, copySettings.getColumnDelimiter(), false);
            rowDelimCombo = UIUtils.createDelimiterCombo(group, "Row Delimiter", new String[] {"\n", "|", "^"}, copySettings.getRowDelimiter(), false);
            quoteStringCombo = UIUtils.createDelimiterCombo(group, "Quote Character", new String[] {"\"", "'"}, copySettings.getQuoteString(), false);
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

        private Button copyHeaderCheck;
        private Button copyRowsCheck;
        private Button quoteCellsCheck;
        private Button forceQuoteCheck;
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
        }

        @Override
        protected void createControlsBefore(Composite group) {
            copyHeaderCheck = UIUtils.createCheckbox(group, "Copy header", null, copySettings.isCopyHeader(), 2);
            copyRowsCheck = UIUtils.createCheckbox(group, "Copy row numbers", null, copySettings.isCopyRowNumbers(), 2);
            quoteCellsCheck = UIUtils.createCheckbox(group, "Quote cell values", "Place cell value in quotes if it contains column or row delimiter", copySettings.isQuoteCells(), 2);
            forceQuoteCheck = UIUtils.createCheckbox(group, "Always quote values", "Place all cell values in quotes", copySettings.isForceQuotes(), 2);

            formatSelector = new ValueFormatSelector(group);
            formatSelector.select(copySettings.getFormat());
        }

        @Override
        protected void okPressed() {
            copySettings.setCopyHeader(copyHeaderCheck.getSelection());
            copySettings.setCopyRowNumbers(copyRowsCheck.getSelection());
            copySettings.setQuoteCells(quoteCellsCheck.getSelection());
            copySettings.setForceQuotes(forceQuoteCheck.getSelection());
            copySettings.setFormat(formatSelector.getSelection());

            settings.put(PARAM_COPY_HEADER, copySettings.isCopyHeader());
            settings.put(PARAM_COPY_ROWS, copySettings.isCopyRowNumbers());
            settings.put(PARAM_QUOTE_CELLS, copySettings.isQuoteCells());
            settings.put(PARAM_FORCE_QUOTES, copySettings.isForceQuotes());
            settings.put(PARAM_FORMAT, copySettings.getFormat().name());

            super.okPressed();
        }
    }

}
