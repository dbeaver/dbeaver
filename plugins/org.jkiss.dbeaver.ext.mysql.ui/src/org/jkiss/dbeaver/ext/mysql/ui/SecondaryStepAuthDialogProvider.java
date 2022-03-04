package org.jkiss.dbeaver.ext.mysql.ui;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.jkiss.dbeaver.ext.mysql.SecondaryStepAuthProvider;
import org.jkiss.dbeaver.ext.mysql.ui.internal.MySQLUIMessages;
import org.jkiss.dbeaver.model.runtime.RunnableWithResult;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.BaseDialog;

public class SecondaryStepAuthDialogProvider implements SecondaryStepAuthProvider {

    @Override
    public String obtainSecondaryPassword(String reason) {
        return UIUtils.syncExec(new RunnableWithResult<String>() {
            @Override
            public String runWithResult()  {
                InputPasswordDialog  dialog = new InputPasswordDialog(UIUtils.getActiveWorkbenchShell(), reason);
                if (dialog.open() == IDialogConstants.OK_ID) {
                    return dialog.getPasswordString();
                } else {
                    return null;
                }
            }
        });
    }

    private class InputPasswordDialog extends BaseDialog {
        private String passwordText;
        private Text passwordTextControl;
        private String reason;
        
        public InputPasswordDialog(Shell parentShell, String reason) {
            super(parentShell, MySQLUIMessages.two_factor_auth_secondary_password_dialog_title, null);
            this.reason = reason;
        }
        
        @Override
        protected boolean isResizable() {
            return false;
        }

        public String getPasswordString() {
            return passwordText;
        }

        @Override
        protected Composite createDialogArea(Composite parent) {
            Composite composite = super.createDialogArea(parent);
            
            GridData gd = new GridData(GridData.FILL_BOTH);
            gd.widthHint = 300;
            gd.minimumWidth = 100;

            passwordTextControl = UIUtils.createLabelText(composite, NLS.bind(MySQLUIMessages.two_factor_auth_secondary_password_dialog_message, reason), "", SWT.BORDER | SWT.PASSWORD, gd);

            return composite;
        }

        @Override
        protected void createButtonsForButtonBar(Composite parent) {
            createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true);
            createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
        }
        
        @Override
        protected void okPressed() {
            passwordText = passwordTextControl.getText();
            super.okPressed();
        }
    }

}
