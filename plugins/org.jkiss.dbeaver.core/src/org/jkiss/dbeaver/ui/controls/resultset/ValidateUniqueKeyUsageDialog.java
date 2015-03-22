/*
 * Copyright (C) 2010-2015 Serge Rieder
 * serge@jkiss.org
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package org.jkiss.dbeaver.ui.controls.resultset;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialogWithToggle;
import org.eclipse.swt.widgets.Shell;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.DBeaverPreferences;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.data.DBDAttributeBinding;
import org.jkiss.dbeaver.model.data.DBDRowIdentifier;
import org.jkiss.dbeaver.model.struct.DBSEntityAttribute;
import org.jkiss.dbeaver.model.virtual.DBVEntityConstraint;
import org.jkiss.dbeaver.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.utils.CommonUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Confirm virtual key usage dialog
 */
class ValidateUniqueKeyUsageDialog extends MessageDialogWithToggle {

    private ResultSetViewer viewer;

    protected ValidateUniqueKeyUsageDialog(ResultSetViewer viewer)
    {
        super(
            viewer.getControl().getShell(),
            "Possible multiple rows modification",
            null,
            "There is no physical unique key defined for  '" + DBUtils.getObjectFullName(viewer.getVirtualEntityIdentifier().getUniqueKey().getParentObject()) +
                "'.\nDBeaver will use all columns as unique key. Possible multiple rows modification. \nAre you sure you want to proceed?",
            WARNING,
            new String[]{"Use All Columns", "Custom Unique Key", IDialogConstants.CANCEL_LABEL},
            0,
            "Do not ask again for '" + viewer.getDataSource().getContainer().getName() + "'",
            false);
        this.viewer = viewer;
    }

    @Override
    protected void configureShell(Shell shell) {
        super.configureShell(shell);
    }

    @Override
    protected void buttonPressed(int buttonId)
    {
        viewer.getDataSource().getContainer().getPreferenceStore().setValue(DBeaverPreferences.RS_EDIT_USE_ALL_COLUMNS, getToggleState());
        switch (buttonId)
        {
            case IDialogConstants.CANCEL_ID:
                super.buttonPressed(buttonId);
                break;
            case IDialogConstants.INTERNAL_ID:
                if (useAllColumns(getShell(), viewer)) {
                    super.buttonPressed(IDialogConstants.OK_ID);
                }

                break;
            case IDialogConstants.INTERNAL_ID + 1:
                editCustomKey();

                break;
        }
        viewer.getDataSource().getContainer().persistConfiguration();
    }

    private void editCustomKey()
    {
        // Edit custom key
        try {
            if (viewer.editEntityIdentifier(VoidProgressMonitor.INSTANCE)) {
                super.buttonPressed(IDialogConstants.OK_ID);
            }
        } catch (DBException e) {
            UIUtils.showErrorDialog(getShell(), "Virtual key edit", "Error editing virtual key", e);
        }
    }

    private static boolean useAllColumns(Shell shell, ResultSetViewer viewer)
    {
        // Use all columns
        final DBDRowIdentifier identifier = viewer.getVirtualEntityIdentifier();
        DBVEntityConstraint constraint = (DBVEntityConstraint) identifier.getUniqueKey();
        List<DBSEntityAttribute> uniqueColumns = new ArrayList<DBSEntityAttribute>();
        for (DBDAttributeBinding binding : viewer.getModel().getAttributes()) {
            if (binding.getEntityAttribute() != null) {
                uniqueColumns.add(binding.getEntityAttribute());
            }
        }
        if (uniqueColumns.isEmpty()) {
            UIUtils.showErrorDialog(shell, "Use All Columns", "No valid columns found for unique key");
            return false;
        }
        constraint.setAttributes(uniqueColumns);

        try {
            identifier.reloadAttributes(
                VoidProgressMonitor.INSTANCE,
                viewer.getModel().getAttributes());
        } catch (DBException e) {
            UIUtils.showErrorDialog(shell, "Use All Columns", "Can't reload unique columns", e);
            return false;
        }

        return true;
    }

    public static boolean validateUniqueKey(ResultSetViewer viewer)
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

        if (viewer.getDataSource().getContainer().getPreferenceStore().getBoolean(DBeaverPreferences.RS_EDIT_USE_ALL_COLUMNS)) {
            if (useAllColumns(viewer.getControl().getShell(), viewer)) {
                return true;
            }
        }

        ValidateUniqueKeyUsageDialog dialog = new ValidateUniqueKeyUsageDialog(viewer);
        int result = dialog.open();
        return result == IDialogConstants.OK_ID;
    }

}
