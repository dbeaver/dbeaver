package org.jkiss.dbeaver.ext.dm.ui.config;

import java.util.Map;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.jkiss.dbeaver.ext.dm.model.DmDataSource;
import org.jkiss.dbeaver.ext.dm.model.DmSchema;
import org.jkiss.dbeaver.ext.dm.model.DmUser;
import org.jkiss.dbeaver.model.edit.DBEObjectConfigurator;
import org.jkiss.dbeaver.model.impl.DBObjectNameCaseTransformer;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.ui.UITask;
import org.jkiss.dbeaver.ui.UIUtils;

public class DmSchemaConfigurator implements DBEObjectConfigurator<DmSchema>  {

	@Override
	public DmSchema configureObject(DBRProgressMonitor monitor, Object container, DmSchema object,Map<String, Object> options) {
		// TODO Auto-generated method stub
        return new UITask<DmSchema>() {
            @Override
            protected DmSchema runTask() {
                NewUserDialog dialog = new NewUserDialog(UIUtils.getActiveWorkbenchShell(), (DmDataSource) container);
                if (dialog.open() != IDialogConstants.OK_ID) {
                    return null;
                }
                object.setName(dialog.getUser().getName());
                object.setUser(dialog.getUser());

                return object;
            }
        }.execute();
	}
	static class NewUserDialog extends Dialog {

        private DmUser user;
        private Text nameText;
        private Text passwordText;

        NewUserDialog(Shell parentShell, DmDataSource dataSource)
        {
            super(parentShell);
            this.user = new DmUser(dataSource);
        }

        DmUser getUser()
        {
            return user;
        }

        @Override
        protected boolean isResizable()
        {
            return true;
        }

        @Override
        protected Control createDialogArea(Composite parent)
        {
            getShell().setText("设置用户/模式属性");

            Control container = super.createDialogArea(parent);
            Composite composite = UIUtils.createPlaceholder((Composite) container, 2, 5);
            composite.setLayoutData(new GridData(GridData.FILL_BOTH));

            nameText = UIUtils.createLabelText(composite, "模式名/用户名", null);
            nameText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
            passwordText = UIUtils.createLabelText(composite, "密码", null, SWT.BORDER | SWT.PASSWORD);
            passwordText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

            Control label=UIUtils.createInfoLabel(composite, "创建模式同时会创建对应用户。\n请注意:如果您需要使用该模式，请用新建的用户重新创建连接!!!\n请注意:如果您需要使用该模式，请用新建的用户重新创建连接!!!", GridData.FILL_HORIZONTAL, 2);
            label.setForeground(new Color(255,0,0));

            return parent;
        }

        @Override
        protected void okPressed()
        {
            user.setName(DBObjectNameCaseTransformer.transformObjectName(user, nameText.getText()));
            user.setPassword(passwordText.getText());
            super.okPressed();
        }

    }
}
