/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2026 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.firebird.ui;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Text;
import org.jkiss.dbeaver.ext.firebird.ui.internal.FireBirdUIMessages;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.connection.ConnectionPageAbstract;
import org.jkiss.utils.CommonUtils;

public class FireBirdEmbeddedConnectionPage extends ConnectionPageAbstract {

    private static final String PROP_NATIVE_LIBRARY_PATH = "nativeLibraryPath";

    private Text nativeLibPathText;

    public FireBirdEmbeddedConnectionPage() {
        setTitle(FireBirdUIMessages.page_embedded_title);
    }

    @Override
    public void createControl(Composite parent) {
        Composite container = new Composite(parent, SWT.NONE);
        container.setLayout(new GridLayout(1, false));
        container.setLayoutData(new GridData(GridData.FILL_BOTH));

        Composite group = UIUtils.createTitledComposite(container, FireBirdUIMessages.page_embedded_group_native_library, 3, GridData.FILL_HORIZONTAL);

        UIUtils.createControlLabel(group, FireBirdUIMessages.page_embedded_label_native_library_path);

        nativeLibPathText = new Text(group, SWT.BORDER | SWT.SINGLE);
        nativeLibPathText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        nativeLibPathText.setToolTipText(FireBirdUIMessages.page_embedded_label_native_library_path_tip);

        UIUtils.createDialogButton(group, FireBirdUIMessages.page_embedded_button_browse, null, FireBirdUIMessages.page_embedded_label_native_library_path_tip, new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                DirectoryDialog dialog = new DirectoryDialog(getShell(), SWT.NONE);
                dialog.setText(FireBirdUIMessages.page_embedded_dialog_title);
                dialog.setMessage(FireBirdUIMessages.page_embedded_dialog_message);
                String current = nativeLibPathText.getText().trim();
                if (!current.isEmpty()) {
                    dialog.setFilterPath(current);
                }
                String path = dialog.open();
                if (path != null) {
                    nativeLibPathText.setText(path);
                }
            }
        });

        setControl(container);
        loadSettings();
    }

    @Override
    public void loadSettings() {
        if (nativeLibPathText == null) {
            return;
        }
        DBPConnectionConfiguration config = getSite().getActiveDataSource().getConnectionConfiguration();
        nativeLibPathText.setText(CommonUtils.notEmpty(config.getProviderProperty(PROP_NATIVE_LIBRARY_PATH)));
    }

    @Override
    public void saveSettings(DBPDataSourceContainer dataSource) {
        String path = nativeLibPathText.getText().trim();
        dataSource.getConnectionConfiguration().setProviderProperty(
            PROP_NATIVE_LIBRARY_PATH,
            path.isEmpty() ? null : path);
    }

    @Override
    public boolean isComplete() {
        return true;
    }
}
