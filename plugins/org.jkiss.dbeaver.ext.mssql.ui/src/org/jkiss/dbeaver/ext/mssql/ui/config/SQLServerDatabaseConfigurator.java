package org.jkiss.dbeaver.ext.mssql.ui.config;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.jkiss.dbeaver.ext.mssql.model.SQLServerDatabase;
import org.jkiss.dbeaver.ext.mssql.ui.SQLServerCreateDatabaseDialog;
import org.jkiss.dbeaver.model.edit.DBEObjectConfigurator;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.ui.UITask;
import org.jkiss.dbeaver.ui.UIUtils;

public class SQLServerDatabaseConfigurator implements DBEObjectConfigurator<SQLServerDatabase> {

    @Override
    public SQLServerDatabase configureObject(DBRProgressMonitor monitor, Object container, SQLServerDatabase database) {
        return UITask.run(() -> {
            SQLServerCreateDatabaseDialog dialog = new SQLServerCreateDatabaseDialog(UIUtils.getActiveWorkbenchShell(), database.getDataSource());

            if (dialog.open() != IDialogConstants.OK_ID) {
                return null;
            }

            database.setName(dialog.getName());
            return database;
        });
    }
}
