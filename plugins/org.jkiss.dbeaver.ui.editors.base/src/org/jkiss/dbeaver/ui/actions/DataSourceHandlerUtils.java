/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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
package org.jkiss.dbeaver.ui.actions;

import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.runtime.DBRProgressListener;
import org.jkiss.dbeaver.model.runtime.RunnableWithResult;
import org.jkiss.dbeaver.model.secret.DBSSecretValue;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.BaseDialog;
import org.jkiss.dbeaver.utils.GeneralUtils;

import java.util.List;

public class DataSourceHandlerUtils {

    public static boolean resolveSharedCredentials(DBPDataSourceContainer dataSource, @Nullable DBRProgressListener onFinish) {
        if (dataSource.isSharedCredentials() && !dataSource.isSharedCredentialsSelected()) {
            try {
                List<DBSSecretValue> sharedCredentials = dataSource.listSharedCredentials();
                if (sharedCredentials.size() == 1) {
                    dataSource.setSelectedSharedCredentials(sharedCredentials.get(0));
                } else if (!sharedCredentials.isEmpty()) {
                    // Show credentials selector
                    DBSSecretValue selectedCredentials = selectSharedCredentials(dataSource, sharedCredentials);
                    if (selectedCredentials != null) {
                        dataSource.setSelectedSharedCredentials(selectedCredentials);
                    } else {
                        if (onFinish != null) {
                            onFinish.onTaskFinished(Status.CANCEL_STATUS);
                        }
                        return false;
                    }
                }
            } catch (DBException e) {
                dataSource.resetAllSecrets();
                if (onFinish != null) {
                    onFinish.onTaskFinished(GeneralUtils.makeExceptionStatus(e));
                }
                DBWorkbench.getPlatformUI().showError(dataSource.getName(), e.getMessage(), e);
                return false;
            }
        }
        return true;
    }

    private static DBSSecretValue selectSharedCredentials(DBPDataSourceContainer dataSource, List<DBSSecretValue> credentials) {
        return UIUtils.syncExec(new RunnableWithResult<>() {
            @Override
            public DBSSecretValue runWithResult() {
                CredentialsSelectorDialog dialog = new CredentialsSelectorDialog(dataSource, credentials);
                if (dialog.open() == IDialogConstants.OK_ID) {
                    return dialog.selected;
                }
                return null;
            }
        });
    }

    static class CredentialsSelectorDialog extends BaseDialog {

        private final DBPDataSourceContainer dataSource;
        private final List<DBSSecretValue> credentials;
        private DBSSecretValue selected;
        public CredentialsSelectorDialog(DBPDataSourceContainer dataSource, List<DBSSecretValue> credentials) {
            super(UIUtils.getActiveShell(),
                "'" + dataSource.getName() + "' credentials",
                dataSource.getDriver().getIcon());
            this.dataSource = dataSource;
            this.credentials = credentials;
        }

        @Override
        protected Composite createDialogArea(Composite parent) {
            final Composite composite = super.createDialogArea(parent);

            UIUtils.createInfoLabel(composite, "There are multiple credentials available for authentication.\nPlease choose credentials you want to use:");
            final Table credsTable = new Table(composite, SWT.BORDER | SWT.SINGLE | SWT.FULL_SELECTION);
            GridData gd = new GridData(GridData.FILL_BOTH);
            //gd.widthHint = 50 * UIUtils.getFontHeight(credsTable);
            credsTable.setLayoutData(gd);

            for (DBSSecretValue sv : credentials) {
                TableItem item = new TableItem(credsTable, SWT.NONE);
                item.setText(sv.getDisplayName());
                item.setData(sv);
            }

            credsTable.setSelection(0);
            selected = credentials.get(0);

            credsTable.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    selected = (DBSSecretValue) e.item.getData();
                }

                @Override
                public void widgetDefaultSelected(SelectionEvent e) {
                    if (selected != null) {
                        okPressed();
                    }
                }
            });

            return composite;
        }
    }
}
