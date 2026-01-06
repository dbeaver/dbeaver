/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2025 DBeaver Corp and others
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jkiss.dbeaver.ext.yashandb.ui.config;

import java.util.Map;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.ext.yashandb.model.YashanDBDataSource;
import org.jkiss.dbeaver.ext.yashandb.model.YashanDBSchema;
import org.jkiss.dbeaver.ext.yashandb.model.YashanDBUser;
import org.jkiss.dbeaver.ext.yashandb.ui.internal.YashanDBUIMessages;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.DBEObjectConfigurator;
import org.jkiss.dbeaver.model.impl.DBObjectNameCaseTransformer;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.ui.UITask;
import org.jkiss.dbeaver.ui.UIUtils;

public class YashanDBSchemaConfigurator implements DBEObjectConfigurator<YashanDBSchema> {

	@Override
	public YashanDBSchema configureObject(@Nullable DBRProgressMonitor monitor,
			@Nullable DBECommandContext commandContext, @Nullable Object container, @Nullable YashanDBSchema newSchema,
			@Nullable Map<String, Object> options) {
		return new UITask<YashanDBSchema>() {
			@Override
			protected YashanDBSchema runTask() {
				NewUserDialog dialog = new NewUserDialog(UIUtils.getActiveWorkbenchShell(),
						(YashanDBDataSource) container);
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
			passwordText = UIUtils.createLabelText(composite, YashanDBUIMessages.dialog_schema_edit_user_password, null,
					SWT.BORDER | SWT.PASSWORD);
			passwordText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

			UIUtils.createInfoLabel(composite, YashanDBUIMessages.dialog_schema_edit_label, GridData.FILL_HORIZONTAL,
					2);

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
