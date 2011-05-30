package org.jkiss.dbeaver.ui.dialogs;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.TrayDialog;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.jkiss.dbeaver.ui.UIUtils;

/**
 * Dialog with enabled help
 */
public abstract class HelpEnabledDialog extends TrayDialog {

    protected final String helpContextID;

    protected HelpEnabledDialog(Shell shell, String helpContextID)
    {
        super(shell);
        this.helpContextID = helpContextID;
        setHelpAvailable(true);
    }

    @Override
    protected Control createContents(Composite parent)
    {
        final Control contents = super.createContents(parent);
        UIUtils.setHelp(contents, helpContextID);
        return contents;
    }

    @Override
    protected void buttonPressed(int buttonId)
    {
        if (buttonId == IDialogConstants.HELP_ID) {
            if (getTray() != null) {
                closeTray();
            } else {
                PlatformUI.getWorkbench().getHelpSystem().displayHelp(helpContextID);
            }
        }
        super.buttonPressed(buttonId);
    }

//    protected Button createHelpButton(Composite parent)
//    {
//        final Button button = createButton(parent, IDialogConstants.HELP_ID, IDialogConstants.HELP_LABEL, false);
//        button.setImage(
//            PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_LCL_LINKTO_HELP)
//        );
//        return button;
//    }

}
