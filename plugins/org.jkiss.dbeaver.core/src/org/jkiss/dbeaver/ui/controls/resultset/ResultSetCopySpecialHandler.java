/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
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
package org.jkiss.dbeaver.ui.controls.resultset;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.commands.IElementUpdater;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.menus.UIElement;
import org.jkiss.dbeaver.core.CoreCommands;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.model.data.DBDDisplayFormat;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.utils.ArrayUtils;
import org.jkiss.utils.CommonUtils;

import java.util.Map;

/**
 * Copy special handler
 */
public class ResultSetCopySpecialHandler extends ResultSetCommandHandler implements IElementUpdater {

    public static final String CMD_COPY_SPECIAL = CoreCommands.CMD_COPY_SPECIAL;

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException
    {
        IResultSetController resultSet = getActiveResultSet(HandlerUtil.getActivePart(event));
        if (resultSet == null) {
            return null;
        }
        switch (event.getCommand().getId()) {
            case CoreCommands.CMD_COPY_SPECIAL:
                ConfigDialog configDialog = new ConfigDialog(HandlerUtil.getActiveShell(event));
                if (configDialog.open() == IDialogConstants.OK_ID) {
                    ResultSetUtils.copyToClipboard(resultSet.getActivePresentation().copySelectionToString(
                        configDialog.copySettings));
                }
                break;
        }
        return null;
    }

    @Override
    public void updateElement(UIElement element, Map parameters)
    {
        element.setText(CoreMessages.actions_spreadsheet_copy_special);
    }

    private class ConfigDialog extends Dialog {

        static final String PARAM_COPY_HEADER = "copyHeader";
        static final String PARAM_COPY_ROWS = "copyRows";
        static final String PARAM_QUOTE_CELLS = "quoteCells";
        static final String PARAM_FORCE_QUOTES = "forceQuotes";
        static final String PARAM_FORMAT = "format";
        static final String PARAM_COL_DELIMITER = "delimiter";
        static final String PARAM_ROW_DELIMITER = "rowDelimiter";
        static final String PARAM_QUOTE_STRING = "quoteString";

        private final IDialogSettings settings;

        private Button copyHeaderCheck;
        private Button copyRowsCheck;
        private Button quoteCellsCheck;
        private Button forceQuoteCheck;
        private ValueFormatSelector formatSelector;
        private Combo colDelimCombo;
        private Combo rowDelimCombo;
        private Combo quoteStringCombo;

        private ResultSetCopySettings copySettings;

        protected ConfigDialog(Shell shell)
        {
            super(shell);
            settings = UIUtils.getDialogSettings("AdvanceCopySettings");
            copySettings = new ResultSetCopySettings();
            copySettings.setQuoteCells(true);
            copySettings.setCopyHeader(true);
            copySettings.setCopyRowNumbers(false);
            copySettings.setFormat(DBDDisplayFormat.UI);
            copySettings.setColumnDelimiter("\t");
            copySettings.setRowDelimiter("\n");
            copySettings.setQuoteString("\"");
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

            copyHeaderCheck = UIUtils.createCheckbox(group, "Copy header", null, copySettings.isCopyHeader(), 2);
            copyRowsCheck = UIUtils.createCheckbox(group, "Copy row numbers", null, copySettings.isCopyRowNumbers(), 2);
            quoteCellsCheck = UIUtils.createCheckbox(group, "Quote cell values", "Place cell value in quotes if it contains column or row delimiter", copySettings.isQuoteCells(), 2);
            forceQuoteCheck = UIUtils.createCheckbox(group, "Always quote values", "Place all cell values in quotes", copySettings.isForceQuotes(), 2);

            formatSelector = new ValueFormatSelector(group);
            formatSelector.select(copySettings.getFormat());

            colDelimCombo = createDelimiterCombo(group, "Column Delimiter", new String[] {"\t", ";", ","}, copySettings.getColumnDelimiter());
            rowDelimCombo = createDelimiterCombo(group, "Row Delimiter", new String[] {"\n", "|", "^"}, copySettings.getRowDelimiter());
            quoteStringCombo = createDelimiterCombo(group, "Quote Character", new String[] {"\"", "'"}, copySettings.getQuoteString());
            return group;
        }

        private Combo createDelimiterCombo(Composite group, String label, String[] options, String defDelimiter) {
            UIUtils.createControlLabel(group, label);
            Combo combo = new Combo(group, SWT.BORDER | SWT.DROP_DOWN);
            combo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
            for (String option : options) {
                if (option.equals("\t")) option = "\\t";
                if (option.equals("\n")) option = "\\n";
                combo.add(option);
            }
            if (!ArrayUtils.contains(options, defDelimiter)) {
                combo.add(defDelimiter);
            }
            String[] items = combo.getItems();
            for (int i = 0, itemsLength = items.length; i < itemsLength; i++) {
                String delim = CommonUtils.unescapeDisplayString(items[i]);
                if (delim.equals(defDelimiter)) {
                    combo.select(i);
                    break;
                }
            }
            return combo;
        }

        @Override
        protected void okPressed() {
            copySettings.setCopyHeader(copyHeaderCheck.getSelection());
            copySettings.setCopyRowNumbers(copyRowsCheck.getSelection());
            copySettings.setQuoteCells(quoteCellsCheck.getSelection());
            copySettings.setForceQuotes(forceQuoteCheck.getSelection());
            copySettings.setFormat(formatSelector.getSelection());
            copySettings.setColumnDelimiter(CommonUtils.unescapeDisplayString(colDelimCombo.getText()));
            copySettings.setRowDelimiter(CommonUtils.unescapeDisplayString(rowDelimCombo.getText()));
            copySettings.setQuoteString(CommonUtils.unescapeDisplayString(quoteStringCombo.getText()));

            settings.put(PARAM_COPY_HEADER, copySettings.isCopyHeader());
            settings.put(PARAM_COPY_ROWS, copySettings.isCopyRowNumbers());
            settings.put(PARAM_QUOTE_CELLS, copySettings.isQuoteCells());
            settings.put(PARAM_FORCE_QUOTES, copySettings.isForceQuotes());
            settings.put(PARAM_FORMAT, copySettings.getFormat().name());
            settings.put(PARAM_COL_DELIMITER, copySettings.getColumnDelimiter());
            settings.put(PARAM_ROW_DELIMITER, copySettings.getRowDelimiter());
            settings.put(PARAM_QUOTE_STRING, copySettings.getQuoteString());
            super.okPressed();
        }
    }

}
