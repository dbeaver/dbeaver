package org.jkiss.dbeaver.ui.dialogs.connection;

import org.eclipse.ui.IWorkbenchWindow;

/**
 * NewConnectionDialog
 */
public class NewConnectionDialog extends ConnectionDialog<NewConnectionDialog>
{
    public NewConnectionDialog(IWorkbenchWindow window)
    {
        super(window, new NewConnectionWizard());
    }

    protected NewConnectionWizard getWizard()
    {
        return (NewConnectionWizard) super.getWizard();
    }

}
