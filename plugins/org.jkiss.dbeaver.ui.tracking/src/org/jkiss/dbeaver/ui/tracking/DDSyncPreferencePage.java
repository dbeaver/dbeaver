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
package org.jkiss.dbeaver.ui.tracking;

import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.secret.DBSSecretController;
import org.jkiss.dbeaver.model.tracking.DDAccessKey;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.preferences.AbstractPrefPage;
import org.jkiss.utils.CommonUtils;

public class DDSyncPreferencePage extends AbstractPrefPage implements IWorkbenchPreferencePage {

    private static final Log log = Log.getLog(DDSyncPreferencePage.class);

    public static final String SECRET_ACCESS_KEY = "datadam.access-key";

    private Text accountText;
    private Text damText;
    private Button deleteButton;

    @Override
    public void init(@NotNull IWorkbench workbench) {
        //empty
    }

    @NotNull
    @Override
    protected Control createPreferenceContent(@NotNull Composite parent) {
        Composite composite = UIUtils.createPlaceholder(parent, 1);
        Composite group = UIUtils.createTitledComposite(
            composite,
            "Access key",
            2,
            GridData.FILL_HORIZONTAL,
            SWT.DEFAULT);
        accountText = UIUtils.createLabelText(group, "Account", "", SWT.READ_ONLY);
        damText = UIUtils.createLabelText(group, "DAM", "", SWT.READ_ONLY);

        Composite buttons = UIUtils.createComposite(group, 2);
        GridData gd = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
        gd.horizontalSpan = 2;
        buttons.setLayoutData(gd);
        UIUtils.createPushButton(buttons, "Import Key...", null, SelectionListener.widgetSelectedAdapter(e -> {
            DDImportKeyDialog dialog = new DDImportKeyDialog(getShell());
            if (dialog.open() == Window.OK) {
                saveKey(dialog.getKey());
                refresh();
            }
        }));
        deleteButton = UIUtils.createPushButton(buttons, "Delete", null, SelectionListener.widgetSelectedAdapter(e -> {
            if (UIUtils.confirmAction(getShell(), "Delete access key", "Delete the stored access key?")) {
                saveKey(null);
                refresh();
            }
        }));

        refresh();
        return composite;
    }

    private void saveKey(@Nullable String key) {
        try {
            DBSSecretController.getGlobalSecretController().setPrivateSecretValue(SECRET_ACCESS_KEY, key);
        } catch (DBException e) {
            log.error("Error saving access key", e);
        }
    }

    private void refresh() {
        String key = null;
        try {
            key = DBSSecretController.getGlobalSecretController().getPrivateSecretValue(SECRET_ACCESS_KEY);
        } catch (DBException e) {
            log.error("Error reading access key", e);
        }
        boolean present = !CommonUtils.isEmpty(key);
        DDAccessKey accessKey = present ? DDAccessKey.parseOrNull(key) : null;
        accountText.setText(accessKey == null ? "" : accessKey.accountId().toString());
        damText.setText(accessKey == null ? "" : accessKey.damId().toString());
        deleteButton.setEnabled(present);
    }
}
