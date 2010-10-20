package org.jkiss.dbeaver.ui.dialogs;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialogWithToggle;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Shell;
import org.jkiss.dbeaver.core.DBeaverCore;

/**
 * Standard confirmation dialog
 */
public class ConfirmationDialog extends MessageDialogWithToggle {

    public ConfirmationDialog(
        Shell parentShell,
        String dialogTitle,
        Image image,
        String message,
        int dialogImageType,
        String[] dialogButtonLabels,
        int defaultIndex,
        String toggleMessage,
        boolean toggleState)
    {
        super(parentShell, dialogTitle, image, message, dialogImageType, dialogButtonLabels, defaultIndex, toggleMessage, toggleState);
    }

    public static int open(
        int kind,
        Shell parent,
        String title,
        String message,
        String toggleMessage,
        boolean toggleState,
        String key)
    {
        ConfirmationDialog dialog = new ConfirmationDialog(
            parent,
            title,
            null, // accept the default window icon
            message,
            kind,
            getButtonLabels(kind),
            0,
            toggleMessage,
            toggleState);
        dialog.setPrefStore(DBeaverCore.getInstance().getGlobalPreferenceStore());
        dialog.setPrefKey(key);
        return dialog.open();
    }

    static String[] getButtonLabels(int kind) {
        switch (kind) {
        case ERROR:
        case INFORMATION:
        case WARNING:
            return new String[] { IDialogConstants.OK_LABEL };
        case CONFIRM:
            return new String[] { IDialogConstants.OK_LABEL, IDialogConstants.CANCEL_LABEL };
        case QUESTION:
            return new String[] { IDialogConstants.YES_LABEL, IDialogConstants.NO_LABEL };
        case QUESTION_WITH_CANCEL: {
            return new String[] { IDialogConstants.YES_LABEL, IDialogConstants.NO_LABEL, IDialogConstants.CANCEL_LABEL };
        }
        default:
            throw new IllegalArgumentException(
                    "Illegal value for kind in MessageDialog.open()"); //$NON-NLS-1$
        }
    }

}
