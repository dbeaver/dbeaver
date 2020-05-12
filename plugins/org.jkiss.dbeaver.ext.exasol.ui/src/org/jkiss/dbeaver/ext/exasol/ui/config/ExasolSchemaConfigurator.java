package org.jkiss.dbeaver.ext.exasol.ui.config;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.jkiss.dbeaver.ext.exasol.model.ExasolDataSource;
import org.jkiss.dbeaver.ext.exasol.model.ExasolSchema;
import org.jkiss.dbeaver.model.edit.DBEObjectConfigurator;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.ui.UITask;
import org.jkiss.dbeaver.ui.UIUtils;

public class ExasolSchemaConfigurator implements DBEObjectConfigurator<ExasolSchema> {
    @Override
    public ExasolSchema configureObject(DBRProgressMonitor monitor, Object container, ExasolSchema schema) {
        return new UITask<ExasolSchema>() {
            @Override
            protected ExasolSchema runTask() {
                ExasolCreateSchemaDialog dialog = new ExasolCreateSchemaDialog(UIUtils.getActiveWorkbenchShell(), (ExasolDataSource) container);
                if (dialog.open() != IDialogConstants.OK_ID) {
                    return null;
                }
                schema.setName(dialog.getName());
                schema.setOwner(dialog.getOwner());
                return schema;
            }
        }.execute();
    }
}
