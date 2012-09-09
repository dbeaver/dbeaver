package org.jkiss.dbeaver.ui.controls.resultset;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialogWithToggle;
import org.eclipse.swt.widgets.Shell;
import org.jkiss.dbeaver.runtime.VoidProgressMonitor;

/**
 * Confirm virtual key usage dialog
 */
class ConfirmVirtualKeyUsageDialog extends MessageDialogWithToggle {

    private ResultSetViewer viewer;

    protected ConfirmVirtualKeyUsageDialog(ResultSetViewer viewer)
    {
        super(
            viewer.getControl().getShell(),
            "Confirm usage of virtual key",
            null,
            "You are about to persist changes. Virtual (set by user) unique key is used for the table '" +
                viewer.getVirtualEntityIdentifier().getReferrer().getParentObject().getName() +
                "'.\nIncorrect virtual key configuration may cause table data corruption.\nAre you sure you want to proceed?",
            WARNING,
            new String[]{IDialogConstants.OK_LABEL, IDialogConstants.CANCEL_LABEL, "Edit Virtual Key"},
            0,
            "Do not ask again for '" + viewer.getVirtualEntityIdentifier().getReferrer().getParentObject().getName() + "'",
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
        switch (buttonId)
        {
            case IDialogConstants.OK_ID:
            case IDialogConstants.CANCEL_ID:
                super.buttonPressed(buttonId);
                break;
            default:
                if (viewer.editEntityIdentifier(VoidProgressMonitor.INSTANCE)) {
                    // Update message?
                }
                break;
        }
    }
}
