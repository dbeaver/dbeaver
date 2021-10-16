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

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.handlers.HandlerUtil;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.resultset.IResultSetController;
import org.jkiss.dbeaver.ui.controls.resultset.IResultSetEditor;
import org.jkiss.dbeaver.ui.controls.resultset.IResultSetPresentation;
import org.jkiss.dbeaver.ui.controls.resultset.ResultSetPasteSettings;
import org.jkiss.dbeaver.ui.controls.resultset.internal.ResultSetMessages;
import org.jkiss.dbeaver.ui.dialogs.BaseDialog;

public class ResultSetHandlerPasteSpecial extends ResultSetHandlerMain {
    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        final IResultSetController resultSet = getActiveResultSet(HandlerUtil.getActivePart(event));
        if (resultSet == null) {
            return null;
        }
        final IResultSetPresentation presentation = resultSet.getActivePresentation();
        if (!(presentation instanceof IResultSetEditor)) {
            return null;
        }
        final AdvancedPasteConfigDialog configDialog = new AdvancedPasteConfigDialog(HandlerUtil.getActiveShell(event));
        if (configDialog.open() == IDialogConstants.OK_ID) {
            ((IResultSetEditor) presentation).pasteFromClipboard(configDialog.pasteSettings);
        }
        return null;
    }

    private static class AdvancedPasteConfigDialog extends BaseDialog {
        private static final String DIALOG_ID = "AdvancedPasteOptions";
        private static final String PROP_INSERT_MULTIPLE_ROWS = "insertMultipleRows";
        private static final String PROP_INSERT_NULLS = "insertNulls";
        private static final String PROP_NULL_VALUE_MARK = "nullValueMark";

        private final IDialogSettings dialogSettings;
        private final ResultSetPasteSettings pasteSettings;

        private Button insertMultipleRowsCheck;
        private Button insertNullsCheck;
        private Combo nullValueMarkCombo;

        public AdvancedPasteConfigDialog(@NotNull Shell shell) {
            super(shell, ResultSetMessages.dialog_paste_as_title, null);
            setShellStyle(SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL);

            this.dialogSettings = UIUtils.getDialogSettings(DIALOG_ID);
            this.pasteSettings = new ResultSetPasteSettings();
            if (dialogSettings.get(PROP_INSERT_MULTIPLE_ROWS) != null) {
                pasteSettings.setInsertMultipleRows(dialogSettings.getBoolean(PROP_INSERT_MULTIPLE_ROWS));
            }
            if (dialogSettings.get(PROP_INSERT_NULLS) != null) {
                pasteSettings.setInsertNulls(dialogSettings.getBoolean(PROP_INSERT_NULLS));
            }
            if (dialogSettings.get(PROP_NULL_VALUE_MARK) != null) {
                pasteSettings.setNullValueMark(dialogSettings.get(PROP_NULL_VALUE_MARK));
            }
        }

        @Override
        protected Composite createDialogArea(Composite parent) {
            final Composite composite = super.createDialogArea(parent);

            insertMultipleRowsCheck = UIUtils.createCheckbox(
                composite,
                ResultSetMessages.dialog_paste_as_insert_multiple_rows_text,
                ResultSetMessages.dialog_paste_as_insert_multiple_rows_tip,
                pasteSettings.isInsertMultipleRows(),
                1
            );

            insertNullsCheck = UIUtils.createCheckbox(
                composite,
                ResultSetMessages.dialog_paste_as_insert_nulls_text,
                ResultSetMessages.dialog_paste_as_insert_nulls_tip,
                pasteSettings.isInsertNulls(),
                1
            );
            insertNullsCheck.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    nullValueMarkCombo.setEnabled(insertNullsCheck.getSelection());
                }
            });

            nullValueMarkCombo = UIUtils.createLabelCombo(
                UIUtils.createPlaceholder(composite, 2),
                ResultSetMessages.dialog_paste_as_null_value_mark_text,
                ResultSetMessages.dialog_paste_as_null_value_mark_tip,
                SWT.NONE
            );
            nullValueMarkCombo.add("NULL");
            nullValueMarkCombo.add("");
            nullValueMarkCombo.setText(pasteSettings.getNullValueMark());
            nullValueMarkCombo.setEnabled(pasteSettings.isInsertNulls());

            return composite;
        }

        @Override
        protected void okPressed() {
            pasteSettings.setInsertMultipleRows(insertMultipleRowsCheck.getSelection());
            pasteSettings.setInsertNulls(insertNullsCheck.getSelection());
            pasteSettings.setNullValueMark(nullValueMarkCombo.getText());

            dialogSettings.put(PROP_INSERT_MULTIPLE_ROWS, pasteSettings.isInsertMultipleRows());
            dialogSettings.put(PROP_INSERT_NULLS, pasteSettings.isInsertNulls());
            dialogSettings.put(PROP_NULL_VALUE_MARK, pasteSettings.getNullValueMark());

            super.okPressed();
        }
    }
}
