package org.jkiss.dbeaver.ui.dialogs.connection;

import org.eclipse.ui.IWorkbenchWindow;
import org.jkiss.dbeaver.registry.DataSourceDescriptor;

/**
 * NewConnectionDialog
 */
public class EditConnectionDialog extends ConnectionDialog<EditConnectionDialog>
{
    private DataSourceDescriptor dataSource;

    public EditConnectionDialog(IWorkbenchWindow window, DataSourceDescriptor dataSource)
    {
        super(window, new EditConnectionWizard(dataSource));
        this.dataSource = dataSource;
    }

    public DataSourceDescriptor getDataSource()
    {
        return dataSource;
    }

    protected EditConnectionWizard getWizard()
    {
        return (EditConnectionWizard) super.getWizard();
    }

}
