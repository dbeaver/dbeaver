/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2015 Serge Rieder (serge@jkiss.org)
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
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.model.data.DBDDisplayFormat;
import org.jkiss.dbeaver.ui.ICommandIds;
import org.jkiss.dbeaver.ui.UIUtils;

import java.util.Map;

/**
 * Copy special handler
 */
public class ResultSetCopySpecialHandler extends ResultSetCommandHandler implements IElementUpdater {

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException
    {
        ResultSetViewer resultSet = getActiveResultSet(HandlerUtil.getActivePart(event));
        if (resultSet == null) {
            return null;
        }
        if (event.getCommand().getId().equals(ICommandIds.CMD_COPY_SPECIAL)) {
            ConfigDialog configDialog = new ConfigDialog(HandlerUtil.getActiveShell(event));
            if (configDialog.open() == IDialogConstants.OK_ID) {
                ResultSetUtils.copyToClipboard(resultSet.getActivePresentation().copySelectionToString(
                    configDialog.copyHeader,
                    configDialog.copyRows,
                    false,
                    configDialog.delimiter,
                    configDialog.format));
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
        public static final String PARAM_DELIMITER = "delimiter";
        private final IDialogSettings settings;

        private Button copyHeaderCheck;
        private Button copyRowsCheck;
        private Combo formatCombo;
        private Combo delimCombo;

        private boolean copyHeader = true;
        private boolean copyRows = false;
        private DBDDisplayFormat format = DBDDisplayFormat.UI;
        private String delimiter = "\t";

        protected ConfigDialog(Shell shell)
        {
            super(shell);
            settings = UIUtils.getDialogSettings("AdvanceCopySettings");
            if (settings.get(PARAM_COPY_HEADER) != null) {
                copyHeader = settings.getBoolean(PARAM_COPY_HEADER);
            }
            if (settings.get(PARAM_COPY_ROWS) != null) {
                copyRows = settings.getBoolean("copyRows");
            }
            if (settings.get(PARAM_FORMAT) != null) {
                format = DBDDisplayFormat.valueOf(settings.get("format"));
            }
            if (settings.get(PARAM_DELIMITER) != null) {
                delimiter = settings.get("delimiter");
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

            copyHeaderCheck = UIUtils.createLabelCheckbox(group, "Copy header", copyHeader);
            copyRowsCheck = UIUtils.createLabelCheckbox(group, "Copy row numbers", copyRows);

            UIUtils.createControlLabel(group, "Format");
            formatCombo = new Combo(group, SWT.BORDER | SWT.DROP_DOWN | SWT.READ_ONLY);
            formatCombo.add("Display (default)");
            formatCombo.add("Editable");
            formatCombo.add("Database native");
            formatCombo.select(format == DBDDisplayFormat.UI ? 0 : format == DBDDisplayFormat.EDIT ? 1 : 2);

            UIUtils.createControlLabel(group, "Delimiter");
            delimCombo = new Combo(group, SWT.BORDER | SWT.DROP_DOWN);
            delimCombo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
            delimCombo.add("\t");
            delimCombo.add(";");
            delimCombo.add(",");
            if (!delimiter.equals("\t") && !delimiter.equals(";") && !delimiter.equals(",")) {
                delimCombo.add(delimiter);
            }
            String[] items = delimCombo.getItems();
            for (int i = 0, itemsLength = items.length; i < itemsLength; i++) {
                String delim = items[i];
                if (delim.equals(delimiter)) {
                    delimCombo.select(i);
                    break;
                }
            }
            return group;
        }

        @Override
        protected void okPressed()
        {
            copyHeader = copyHeaderCheck.getSelection();
            copyRows = copyRowsCheck.getSelection();
            switch (formatCombo.getSelectionIndex()) {
                case 0: format = DBDDisplayFormat.UI; break;
                case 1: format = DBDDisplayFormat.EDIT; break;
                case 2: format = DBDDisplayFormat.NATIVE; break;
            }
            delimiter = delimCombo.getText();

            settings.put(PARAM_COPY_HEADER, copyHeader);
            settings.put(PARAM_COPY_ROWS, copyRows);
            settings.put(PARAM_FORMAT, format.name());
            settings.put(PARAM_DELIMITER, delimiter);
            super.okPressed();
        }
    }

}
