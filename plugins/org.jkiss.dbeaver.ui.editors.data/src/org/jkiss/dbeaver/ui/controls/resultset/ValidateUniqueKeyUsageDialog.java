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
package org.jkiss.dbeaver.ui.controls.resultset;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialogWithToggle;
import org.eclipse.swt.widgets.Shell;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.data.DBDAttributeBinding;
import org.jkiss.dbeaver.model.data.DBDRowIdentifier;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSEntityAttribute;
import org.jkiss.dbeaver.model.virtual.DBVEntityConstraint;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.utils.CommonUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Confirm virtual key usage dialog
 */
class ValidateUniqueKeyUsageDialog extends MessageDialogWithToggle {

    @NotNull
    private final ResultSetViewer viewer;
    @NotNull
    private final DBCExecutionContext executionContext;

    protected ValidateUniqueKeyUsageDialog(@NotNull ResultSetViewer viewer, @NotNull DBCExecutionContext executionContext)
    {
        super(
            viewer.getControl().getShell(),
            "No unique key - multiple rows modification possible",
            null,
            "There is no physical unique key defined for  '" + DBUtils.getObjectFullName(viewer.getVirtualEntityIdentifier().getUniqueKey().getParentObject(), DBPEvaluationContext.UI) +
                "'.\nDBeaver will use all columns as unique key. Possible multiple rows modification. \nAre you sure you want to proceed?",
            WARNING,
            new String[]{"Use All Columns", "Custom Unique Key", IDialogConstants.CANCEL_LABEL},
            0,
            "Do not ask again for '" + executionContext.getDataSource().getContainer().getName() + "'",
            false);
        this.viewer = viewer;
        this.executionContext = executionContext;
    }

    @Override
    protected void configureShell(Shell shell) {
        super.configureShell(shell);
    }

    @Override
    protected void buttonPressed(int buttonId)
    {
        executionContext.getDataSource().getContainer().getPreferenceStore().setValue(ResultSetPreferences.RS_EDIT_USE_ALL_COLUMNS, getToggleState());
        switch (buttonId)
        {
            case IDialogConstants.CANCEL_ID:
                super.buttonPressed(buttonId);
                break;
            case IDialogConstants.INTERNAL_ID:
                if (useAllColumns(viewer)) {
                    super.buttonPressed(IDialogConstants.OK_ID);
                }
                break;
            case IDialogConstants.INTERNAL_ID + 1:
                editCustomKey();

                break;
        }
        executionContext.getDataSource().getContainer().persistConfiguration();
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
            DBWorkbench.getPlatformUI().showError("Use All Columns", "No valid columns found for unique key");
            return false;
        }
        constraint.setAttributes(uniqueColumns);
        constraint.setUseAllColumns(true);

        try {
            identifier.reloadAttributes(
                new VoidProgressMonitor(),
                viewer.getModel().getAttributes());
        } catch (DBException e) {
            DBWorkbench.getPlatformUI().showError("Use All Columns", "Can't reload unique columns", e);
            return false;
        }

        return true;
    }

    public static boolean validateUniqueKey(@NotNull ResultSetViewer viewer, @NotNull DBCExecutionContext executionContext)
    {
        final DBDRowIdentifier identifier = viewer.getVirtualEntityIdentifier();
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

        ValidateUniqueKeyUsageDialog dialog = new ValidateUniqueKeyUsageDialog(viewer, executionContext);
        int result = dialog.open();
        return result == IDialogConstants.OK_ID;
    }

}
