package org.jkiss.dbeaver.ext.exasol.ui.config;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.jkiss.dbeaver.ext.exasol.model.security.ExasolRole;
import org.jkiss.dbeaver.model.edit.DBEObjectConfigurator;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.ui.UITask;
import org.jkiss.dbeaver.ui.UIUtils;

public class ExasolRoleConfigurator implements DBEObjectConfigurator<ExasolRole> {
    @Override
    public ExasolRole configureObject(DBRProgressMonitor monitor, Object container, ExasolRole role) {
        return new UITask<ExasolRole>() {
            @Override
            protected ExasolRole runTask() {
                ExasolRoleDialog dialog = new ExasolRoleDialog(UIUtils.getActiveWorkbenchShell());
                if (dialog.open() != IDialogConstants.OK_ID) {
                    return null;
                }

                role.setName(dialog.getName());
                role.setDescription(dialog.getDescription());
                return role;
            }
        }.execute();
    }
}
