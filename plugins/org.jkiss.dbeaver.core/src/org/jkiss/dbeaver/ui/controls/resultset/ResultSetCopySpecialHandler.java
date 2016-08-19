/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2016 Serge Rieder (serge@jkiss.org)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (version 2)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
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
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.core.CoreCommands;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.model.data.DBDDisplayFormat;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.utils.ArrayUtils;

import java.util.Map;

/**
 * Copy special handler
 */
public class ResultSetCopySpecialHandler extends ResultSetCommandHandler implements IElementUpdater {

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException
    {
        IResultSetController resultSet = getActiveResultSet(HandlerUtil.getActivePart(event));
        if (resultSet == null) {
            return null;
        }
        if (event.getCommand().getId().equals(CoreCommands.CMD_COPY_SPECIAL)) {
            ConfigDialog configDialog = new ConfigDialog(HandlerUtil.getActiveShell(event));
            if (configDialog.open() == IDialogConstants.OK_ID) {
                ResultSetUtils.copyToClipboard(resultSet.getActivePresentation().copySelectionToString(
                    configDialog.copySettings));
            }
        }
        return null;
    }

    @Override
    public void updateElement(UIElement element, Map parameters)
    {
        element.setText(CoreMessages.actions_spreadsheet_copy_special);
    }

    private class ConfigDialog extends Dialog {

        public static final String PARAM_COPY_HEADER = "copyHeader";
        public static final String PARAM_COPY_ROWS = "copyRows";
        public static final String PARAM_FORMAT = "format";
        public static final String PARAM_COL_DELIMITER = "delimiter";
        public static final String PARAM_ROW_DELIMITER = "rowDelimiter";

        private final IDialogSettings settings;

        private Button copyHeaderCheck;
        private Button copyRowsCheck;
        private Combo formatCombo;
        private Combo colDelimCombo;
        private Combo rowDelimCombo;

        private ResultSetCopySettings copySettings;

        protected ConfigDialog(Shell shell)
        {
            super(shell);
            settings = UIUtils.getDialogSettings("AdvanceCopySettings");
            copySettings = new ResultSetCopySettings();
            copySettings.setCopyHeader(true);
            copySettings.setCopyRowNumbers(false);
            copySettings.setFormat(DBDDisplayFormat.UI);
            copySettings.setColumnDelimiter("\t");
            copySettings.setRowDelimiter("\n");
            if (settings.get(PARAM_COPY_HEADER) != null) {
                copySettings.setCopyHeader(settings.getBoolean(PARAM_COPY_HEADER));
            }
            if (settings.get(PARAM_COPY_ROWS) != null) {
                copySettings.setCopyRowNumbers(settings.getBoolean(PARAM_COPY_ROWS));
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

            copyHeaderCheck = UIUtils.createLabelCheckbox(group, "Copy header", copySettings.isCopyHeader());
            copyRowsCheck = UIUtils.createLabelCheckbox(group, "Copy row numbers", copySettings.isCopyRowNumbers());

            UIUtils.createControlLabel(group, "Format");
            formatCombo = new Combo(group, SWT.BORDER | SWT.DROP_DOWN | SWT.READ_ONLY);
            formatCombo.add("Display (default)");
            formatCombo.add("Editable");
            formatCombo.add("Database native");
            formatCombo.select(copySettings.getFormat() == DBDDisplayFormat.UI ? 0 : copySettings.getFormat() == DBDDisplayFormat.EDIT ? 1 : 2);

            colDelimCombo = createDelimiterCombo(group, "Column Delimiter", new String[] {"\t", ";", ","}, copySettings.getColumnDelimiter());
            rowDelimCombo = createDelimiterCombo(group, "Row Delimiter", new String[] {"\n", "|", "^"}, copySettings.getRowDelimiter());
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
                String delim = convertDelimiterFromDisplay(items[i]);
                if (delim.equals(defDelimiter)) {
                    combo.select(i);
                    break;
                }
            }
            return combo;
        }

        @NotNull
        private String convertDelimiterFromDisplay(final String delim) {
            if (delim.equals("\\t")) return "\t";
            if (delim.equals("\\n")) return "\n";
            return delim;
        }

        @Override
        protected void okPressed() {
            copySettings.setCopyHeader(copyHeaderCheck.getSelection());
            copySettings.setCopyRowNumbers(copyRowsCheck.getSelection());
            DBDDisplayFormat format = DBDDisplayFormat.UI;
            switch (formatCombo.getSelectionIndex()) {
                case 0: format = DBDDisplayFormat.UI; break;
                case 1: format = DBDDisplayFormat.EDIT; break;
                case 2: format = DBDDisplayFormat.NATIVE; break;
            }
            copySettings.setFormat(format);
            copySettings.setColumnDelimiter(convertDelimiterFromDisplay(colDelimCombo.getText()));
            copySettings.setRowDelimiter(convertDelimiterFromDisplay(rowDelimCombo.getText()));

            settings.put(PARAM_COPY_HEADER, copySettings.isCopyHeader());
            settings.put(PARAM_COPY_ROWS, copySettings.isCopyRowNumbers());
            settings.put(PARAM_FORMAT, format.name());
            settings.put(PARAM_COL_DELIMITER, copySettings.getColumnDelimiter());
            settings.put(PARAM_ROW_DELIMITER, copySettings.getRowDelimiter());
            super.okPressed();
        }
    }

}
