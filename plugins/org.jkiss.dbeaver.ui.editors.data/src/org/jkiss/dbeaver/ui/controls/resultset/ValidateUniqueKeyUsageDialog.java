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
package org.jkiss.dbeaver.ui.controls.resultset;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Shell;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.data.DBDAttributeBinding;
import org.jkiss.dbeaver.model.data.DBDRowIdentifier;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSEntityAttribute;
import org.jkiss.dbeaver.model.virtual.DBVEntityConstraint;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.controls.resultset.internal.ResultSetMessages;
import org.jkiss.utils.CommonUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Confirm virtual key usage dialog
 */
final class ValidateUniqueKeyUsageDialog extends MessageDialog {
    @NotNull
    private final ResultSetViewer viewer;

    private ValidateUniqueKeyUsageDialog(@NotNull ResultSetViewer viewer) {
        super(
            viewer.getControl().getShell(),
            ResultSetMessages.validate_unique_key_usage_dialog_title,
            null,
            ResultSetMessages.validate_unique_key_usage_dialog_main_question,
            WARNING,
            new String[]{
                ResultSetMessages.validate_unique_key_usage_dialog_use_all_columns,
                ResultSetMessages.validate_unique_key_usage_dialog_custom_unique_key,
                IDialogConstants.CANCEL_LABEL
            },
            2
        );
        this.viewer = viewer;
    }

    @Override
    protected void configureShell(Shell shell) {
        super.configureShell(shell);
    }

    @Override
    protected void buttonPressed(int buttonId) {
        switch (buttonId) {
            case 0:
                if (useAllColumns(viewer)) {
                    super.buttonPressed(IDialogConstants.OK_ID);
                }
                break;
            case 1:
                editCustomKey();
                break;
            case 2:
                super.buttonPressed(IDialogConstants.CANCEL_ID);
                break;
        }
    }

    private void editCustomKey()
    {
        // Edit custom key
        if (viewer.editEntityIdentifier()) {
            super.buttonPressed(IDialogConstants.OK_ID);
        }
    }

    private static boolean useAllColumns(ResultSetViewer viewer)
    {
        // Use all columns
        final DBDRowIdentifier identifier = viewer.getVirtualEntityIdentifier();
        DBVEntityConstraint constraint = (DBVEntityConstraint) identifier.getUniqueKey();
        List<DBSEntityAttribute> uniqueColumns = new ArrayList<>();
        for (DBDAttributeBinding binding : viewer.getModel().getAttributes()) {
            if (binding.getEntityAttribute() != null) {
                uniqueColumns.add(binding.getEntityAttribute());
            }
        }
        if (uniqueColumns.isEmpty()) {
            DBWorkbench.getPlatformUI().showError(
                ResultSetMessages.validate_unique_key_usage_dialog_use_all_columns,
                ResultSetMessages.validate_unique_key_usage_dialog_use_all_columns_no_valid_columns_found
            );
            return false;
        }
        constraint.setAttributes(uniqueColumns);
        constraint.setUseAllColumns(true);

        try {
            identifier.reloadAttributes(
                new VoidProgressMonitor(),
                viewer.getModel().getAttributes());
        } catch (DBException e) {
            DBWorkbench.getPlatformUI().showError(
                ResultSetMessages.validate_unique_key_usage_dialog_use_all_columns,
                ResultSetMessages.validate_unique_key_usage_dialog_use_all_columns_cannot_reload_unique_columns,
                e
            );
            return false;
        }

        return true;
    }

    static boolean validateUniqueKey(@NotNull ResultSetViewer viewer, @NotNull DBCExecutionContext executionContext) {
        DBDRowIdentifier identifier = viewer.getVirtualEntityIdentifier();
        if (identifier == null) {
            // No key
            return false;
        }
        if (!CommonUtils.isEmpty(identifier.getAttributes())) {
            // Key already defined
            return true;
        }
        if (executionContext.getDataSource().getContainer().getPreferenceStore().getBoolean(ResultSetPreferences.RS_EDIT_USE_ALL_COLUMNS)) {
            if (useAllColumns(viewer)) {
                return true;
            }
        }
        ValidateUniqueKeyUsageDialog dialog = new ValidateUniqueKeyUsageDialog(viewer);
        int result = dialog.open();
        return result == IDialogConstants.OK_ID;
    }
}
