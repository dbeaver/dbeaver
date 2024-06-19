package org.jkiss.dbeaver.ext.gaussdb.ui.config;

import java.util.Map;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.gaussdb.model.GaussDBDatabase;
import org.jkiss.dbeaver.ext.gaussdb.ui.GaussDBCreateDatabaseDialog;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.DBEObjectConfigurator;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.ui.UITask;
import org.jkiss.dbeaver.ui.UIUtils;

public class GaussDBDatabaseConfigurator implements DBEObjectConfigurator<GaussDBDatabase> {

    @Override
    public GaussDBDatabase configureObject(@NotNull DBRProgressMonitor monitor,
                                           @Nullable DBECommandContext commandContext,
                                           @Nullable Object container,
                                           @NotNull GaussDBDatabase database,
                                           @NotNull Map<String, Object> options) {
        return new UITask<GaussDBDatabase>() {
            @Override
            protected GaussDBDatabase runTask() throws DBException {
                GaussDBCreateDatabaseDialog dialog = new GaussDBCreateDatabaseDialog(UIUtils.getActiveWorkbenchShell(),
                                                                                     database.getDataSource());
                if (dialog.open() != IDialogConstants.OK_ID) {
                    return null;
                }
                database.setName(dialog.getName());
                database.setInitialOwner(dialog.getOwner());
                database.setTemplateName(dialog.getTemplateName());
                database.setInitialTablespace(dialog.getTablespace());
                database.setInitialEncoding(dialog.getEncoding());
                return database;
            }
        }.execute();
    }
}