package org.jkiss.dbeaver.ext.exasol.ui.config;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.jkiss.dbeaver.ext.exasol.model.ExasolDataSource;
import org.jkiss.dbeaver.ext.exasol.model.security.ExasolUser;
import org.jkiss.dbeaver.model.edit.DBEObjectConfigurator;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.ui.UITask;
import org.jkiss.dbeaver.ui.UIUtils;

public class ExasolUserConfigurator implements DBEObjectConfigurator<ExasolUser> {
    @Override
    public ExasolUser configureObject(DBRProgressMonitor monitor, Object container, ExasolUser user) {
        return new UITask<ExasolUser>() {
            @Override
            protected ExasolUser runTask() {
                ExasolUserDialog dialog = new ExasolUserDialog(UIUtils.getActiveWorkbenchShell(), (ExasolDataSource) container);
                if (dialog.open() != IDialogConstants.OK_ID) {
                    return null;
                }
                user.setName(dialog.getName());
                user.setDescription(dialog.getComment());
                switch (dialog.getUserType()) {
                    case KERBEROS:
                        user.setKerberosPrincipal(dialog.getKerberosPrincipal());
                        break;
                    case LDAP:
                        user.setDN(dialog.getLDAPDN());
                        break;
                    case LOCAL:
                        user.setPassword(dialog.getPassword());
                        break;
                }
                return user;
            }
        }.execute();
    }
}
