package org.jkiss.dbeaver.ui.dialogs.connection;

import org.eclipse.jface.dialogs.IPageChangingListener;
import org.eclipse.jface.dialogs.PageChangingEvent;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.ui.IWorkbenchWindow;

/**
 * NewConnectionDialog
 */
public class ConnectionDialog extends WizardDialog
{
    private IWorkbenchWindow window;

    public ConnectionDialog(IWorkbenchWindow window, ConnectionWizard wizard)
    {
        super(window.getShell(), wizard);
        this.window = window;

        getWizard().init(this.window.getWorkbench(), null);
        this.addPageChangingListener(new IPageChangingListener()
        {
            public void handlePageChanging(PageChangingEvent event)
            {
                getWizard().changePage(event.getCurrentPage(), event.getTargetPage());
            }
        });
    }

    protected ConnectionWizard getWizard()
    {
        return (ConnectionWizard) super.getWizard();
    }

    public IWorkbenchWindow getWindow()
    {
        return window;
    }
}
