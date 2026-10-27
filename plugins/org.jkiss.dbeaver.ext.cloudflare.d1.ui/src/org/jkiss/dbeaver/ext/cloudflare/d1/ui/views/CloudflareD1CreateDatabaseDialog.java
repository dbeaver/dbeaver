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
package org.jkiss.dbeaver.ext.cloudflare.d1.ui.views;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.ext.cloudflare.d1.model.CloudflareD1Catalog;
import org.jkiss.dbeaver.ext.cloudflare.d1.ui.internal.CloudflareD1UIMessages;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.BaseDialog;
import org.jkiss.utils.CommonUtils;

public class CloudflareD1CreateDatabaseDialog extends BaseDialog {
    private static final String[] JURISDICTIONS = {"", "eu", "fedramp"};
    private static final String[] LOCATIONS = {"", "wnam", "enam", "weur", "eeur", "apac", "oc"};
    private final CloudflareD1Catalog catalog;
    private Text nameText;
    private Combo jurisdiction;
    private Combo location;
    private Button appendToScope;

    public CloudflareD1CreateDatabaseDialog(@NotNull Shell parent, @NotNull CloudflareD1Catalog catalog) {
        super(parent, CloudflareD1UIMessages.create_title, null);
        this.catalog = catalog;
    }

    @Override
    @NotNull
    protected Composite createDialogArea(@NotNull Composite parent) {
        Composite area = super.createDialogArea(parent);
        Composite group = UIUtils.createComposite(area, 2);
        GridData layout = new GridData(GridData.FILL_HORIZONTAL);
        layout.widthHint = convertHorizontalDLUsToPixels(250);
        group.setLayoutData(layout);
        nameText = UIUtils.createLabelText(group, CloudflareD1UIMessages.create_name, CommonUtils.notEmpty(catalog.getName()));
        nameText.addModifyListener(event -> updateCreateButton());
        jurisdiction = UIUtils.createLabelCombo(group, CloudflareD1UIMessages.create_jurisdiction, SWT.DROP_DOWN | SWT.READ_ONLY);
        jurisdiction.setItems(CloudflareD1UIMessages.create_none, CloudflareD1UIMessages.create_eu, CloudflareD1UIMessages.create_fedramp);
        jurisdiction.select(0);
        createHelp(group, CloudflareD1UIMessages.create_jurisdiction_help);
        location = UIUtils.createLabelCombo(group, CloudflareD1UIMessages.create_location, SWT.DROP_DOWN | SWT.READ_ONLY);
        location.setItems(CloudflareD1UIMessages.create_automatic, CloudflareD1UIMessages.create_wnam, CloudflareD1UIMessages.create_enam,
            CloudflareD1UIMessages.create_weur, CloudflareD1UIMessages.create_eeur, CloudflareD1UIMessages.create_apac,
            CloudflareD1UIMessages.create_oc);
        location.select(0);
        createHelp(group, CloudflareD1UIMessages.create_location_help);
        jurisdiction.addListener(SWT.Selection, event -> location.setEnabled(jurisdiction.getSelectionIndex() == 0));
        if (!CommonUtils.isEmpty(catalog.getDataSource().getContainer().getConnectionConfiguration().getProperty("databaseIds"))) {
            appendToScope = new Button(group, SWT.CHECK);
            appendToScope.setText(CloudflareD1UIMessages.create_append_scope);
            appendToScope.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
            appendToScope.setSelection(true);
        }
        return area;
    }

    @Override
    protected void createButtonsForButtonBar(@NotNull Composite parent) {
        createButton(parent, IDialogConstants.OK_ID, CloudflareD1UIMessages.create_action, true);
        createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
        updateCreateButton();
    }

    private void createHelp(@NotNull Composite parent, @NotNull String text) {
        new Label(parent, SWT.NONE);
        Label help = new Label(parent, SWT.WRAP);
        help.setText(text);
        help.setForeground(parent.getDisplay().getSystemColor(SWT.COLOR_WIDGET_DARK_SHADOW));
        GridData layout = new GridData(SWT.FILL, SWT.TOP, true, false);
        layout.widthHint = convertHorizontalDLUsToPixels(165);
        layout.verticalIndent = convertVerticalDLUsToPixels(2);
        help.setLayoutData(layout);
    }

    private void updateCreateButton() {
        var button = getButton(IDialogConstants.OK_ID);
        if (button != null) {
            button.setEnabled(!nameText.getText().isBlank());
        }
    }

    @Override
    protected void okPressed() {
        String name = nameText.getText().trim();
        if (name.isEmpty()) {
            return;
        }
        catalog.setName(name);
        String selectedJurisdiction = JURISDICTIONS[jurisdiction.getSelectionIndex()];
        catalog.setJurisdiction(selectedJurisdiction.isEmpty() ? null : selectedJurisdiction);
        String selectedLocation = LOCATIONS[location.getSelectionIndex()];
        catalog.setPrimaryLocationHint(selectedJurisdiction.isEmpty() && !selectedLocation.isEmpty() ? selectedLocation : null);
        catalog.setAppendToScope(appendToScope != null && appendToScope.getSelection());
        super.okPressed();
    }
}
