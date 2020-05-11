package org.jkiss.dbeaver.ext.exasol.ui.config;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.jkiss.dbeaver.ext.exasol.model.ExasolConnection;
import org.jkiss.dbeaver.ext.exasol.model.ExasolDataSource;
import org.jkiss.dbeaver.model.edit.DBEObjectConfigurator;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.ui.UITask;
import org.jkiss.dbeaver.ui.UIUtils;

public class ExasolConnectionConfigurator implements DBEObjectConfigurator<ExasolConnection> {
    @Override
    public ExasolConnection configureObject(DBRProgressMonitor monitor, Object container, ExasolConnection con) {
        return new UITask<ExasolConnection>() {
            @Override
            protected ExasolConnection runTask()
            {
                ExasolConnectionDialog dialog = new ExasolConnectionDialog(UIUtils.getActiveWorkbenchShell(), (ExasolDataSource) container);
                if (dialog.open() != IDialogConstants.OK_ID)
                {
                    return null;
                }
                con.setName(dialog.getName());
                con.setConnectionString(dialog.getUrl());
                con.setDescription(dialog.getComment());
                con.setUserName(dialog.getUser());
                con.setPassword(dialog.getPassword());
                return con;
            }
        }.execute();
    }
}
