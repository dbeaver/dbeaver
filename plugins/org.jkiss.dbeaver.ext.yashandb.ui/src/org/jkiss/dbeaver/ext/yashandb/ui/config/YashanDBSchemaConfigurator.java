package org.jkiss.dbeaver.ext.yashandb.ui.config;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.SWT;
import org.jkiss.dbeaver.ext.yashandb.model.YashanDBDataSource;
import org.jkiss.dbeaver.ext.yashandb.model.YashanDBSchema;
import org.jkiss.dbeaver.ext.yashandb.model.YashanDBUser;
import org.jkiss.dbeaver.ext.yashandb.ui.internal.YashanDBUIMessages;
import org.jkiss.dbeaver.model.edit.DBEObjectConfigurator;
import org.jkiss.dbeaver.model.impl.DBObjectNameCaseTransformer;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.ui.UITask;
import org.jkiss.dbeaver.ui.UIUtils;
import org.eclipse.jface.dialogs.Dialog;

import java.util.Map;

/**
 * @Author: donghy
 * @Date: 2022/08
 * @Description:
 */
public class YashanDBSchemaConfigurator implements DBEObjectConfigurator<YashanDBSchema> {
    @Override
    public YashanDBSchema configureObject(DBRProgressMonitor monitor, Object container, YashanDBSchema newSchema, Map<String, Object> options) {
        return new UITask<YashanDBSchema>() {
            @Override
            protected YashanDBSchema runTask() {
                NewUserDialog dialog = new NewUserDialog(UIUtils.getActiveWorkbenchShell(), (YashanDBDataSource) container);
                if (dialog.open() != IDialogConstants.OK_ID) {
                    return null;
                }
                newSchema.setName(dialog.getUser().getName());
                newSchema.setUser(dialog.getUser());

                return newSchema;
            }
        }.execute();
    }

    static class NewUserDialog extends Dialog {

        private YashanDBUser user;
        private Text nameText;
        private Text passwordText;

        NewUserDialog(Shell parentShell, YashanDBDataSource dataSource) {
            super(parentShell);
            this.user = new YashanDBUser(dataSource);
        }


        YashanDBUser getUser() {
            return user;
        }

        @Override
        protected boolean isResizable() {
            return true;
        }

        @Override
        protected Control createDialogArea(Composite parent) {
            getShell().setText(YashanDBUIMessages.dialog_schema_edit_title);

            Control container = super.createDialogArea(parent);
            Composite composite = UIUtils.createPlaceholder((Composite) container, 2, 5);
            composite.setLayoutData(new GridData(GridData.FILL_BOTH));

            nameText = UIUtils.createLabelText(composite, YashanDBUIMessages.dialog_schema_edit_user_name, null);
            nameText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
            passwordText = UIUtils.createLabelText(composite, YashanDBUIMessages.dialog_schema_edit_user_password, null, SWT.BORDER | SWT.PASSWORD);
            passwordText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

            UIUtils.createInfoLabel(composite, YashanDBUIMessages.dialog_schema_edit_label, GridData.FILL_HORIZONTAL, 2);

            return parent;
        }

        @Override
        protected void okPressed() {
            user.setName(DBObjectNameCaseTransformer.transformObjectName(user, nameText.getText().trim()));
            user.setPassword(passwordText.getText());
            super.okPressed();
        }

    }
}
