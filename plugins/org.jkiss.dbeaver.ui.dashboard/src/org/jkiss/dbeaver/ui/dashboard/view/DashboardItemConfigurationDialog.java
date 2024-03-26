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
package org.jkiss.dbeaver.ui.dashboard.view;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.jkiss.dbeaver.model.dashboard.registry.DashboardItemConfiguration;
import org.jkiss.dbeaver.model.dashboard.registry.DashboardRegistry;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.IObjectPropertyConfigurator;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dashboard.internal.UIDashboardMessages;
import org.jkiss.dbeaver.ui.dashboard.registry.DashboardRendererDescriptor;
import org.jkiss.dbeaver.ui.dashboard.registry.DashboardUIRegistry;
import org.jkiss.dbeaver.ui.dialogs.BaseDialog;
import org.jkiss.utils.CommonUtils;

public class DashboardItemConfigurationDialog extends BaseDialog {

    private final DashboardItemConfiguration itemDescriptor;
    private final boolean newItem;
    private Text idText;
    private Text nameText;
    private Text displayNameText;
    private Text descriptionText;
    private IObjectPropertyConfigurator<DashboardItemConfiguration, DashboardItemConfiguration> itemConfigurationEditor;

    public DashboardItemConfigurationDialog(Shell shell, DashboardItemConfiguration itemDescriptor, boolean isNewItem) {
        super(shell, NLS.bind(UIDashboardMessages.dialog_edit_dashboard_title, itemDescriptor.getName()), null);

        this.itemDescriptor = itemDescriptor;
        this.newItem = isNewItem;
    }

    @Override
    protected IDialogSettings getDialogBoundsSettings()
    {
        return null;//UIUtils.getDialogSettings(DIALOG_ID);
    }

    @Override
    protected Composite createDialogArea(Composite parent)
    {
        Composite composite = super.createDialogArea(parent);

        boolean readOnly = !itemDescriptor.isCustom();
        int baseStyle = !readOnly ? SWT.NONE : SWT.READ_ONLY;

        if (readOnly) {
            UIUtils.createInfoLabel(composite, UIDashboardMessages.dialog_edit_dashboard_infolabels_predifined_dashboard);
        }

        DashboardRendererDescriptor renderer = DashboardUIRegistry.getInstance().getViewType(itemDescriptor.getDashboardRenderer());

        {
            Group infoGroup = UIUtils.createControlGroup(composite, UIDashboardMessages.dialog_edit_dashboard_maininfo, 4, GridData.FILL_HORIZONTAL, 0);

            idText = UIUtils.createLabelText(infoGroup, UIDashboardMessages.dialog_edit_dashboard_maininfo_labels_id, itemDescriptor.getId(), SWT.BORDER | baseStyle);
            idText.setLayoutData(new GridData(GridData.FILL, GridData.BEGINNING, true, false, 3, 1));
            if (newItem) {
                idText.addModifyListener(e -> updateButtons());
            } else {
                idText.setEditable(false);
            }
            nameText = UIUtils.createLabelText(infoGroup, UIDashboardMessages.dialog_edit_dashboard_maininfo_labels_name, itemDescriptor.getName(), SWT.BORDER | baseStyle);
            nameText.setLayoutData(new GridData(GridData.FILL, GridData.BEGINNING, true, false, 3, 1));
            nameText.addModifyListener(e -> updateButtons());
            displayNameText = UIUtils.createLabelText(infoGroup, UIDashboardMessages.dialog_edit_dashboard_maininfo_labels_display_name, itemDescriptor.getDisplayName(), SWT.BORDER | baseStyle);
            displayNameText.setLayoutData(new GridData(GridData.FILL, GridData.BEGINNING, true, false, 3, 1));

            descriptionText = UIUtils.createLabelText(
                infoGroup,
                UIDashboardMessages.dialog_edit_dashboard_maininfo_labels_description,
                CommonUtils.notEmpty(itemDescriptor.getDescription()),
                SWT.BORDER | SWT.MULTI | SWT.WRAP | SWT.V_SCROLL | baseStyle);
            descriptionText.addModifyListener(e -> updateButtons());
            ((GridData) descriptionText.getLayoutData()).heightHint = 30;
            ((GridData) descriptionText.getLayoutData()).widthHint = 300;
        }

        try {
            itemConfigurationEditor = renderer.createItemConfigurationEditor();
        } catch (Exception e) {
            DBWorkbench.getPlatformUI().showError("Error creating configuration editor", null, e);
        }

        if (itemConfigurationEditor != null) {
            Composite configComposite = UIUtils.createControlGroup(composite, UIDashboardMessages.dialog_dashboard_item_config_dashboardinfo, 1, GridData.FILL_HORIZONTAL, 0);
            itemConfigurationEditor.createControl(configComposite, itemDescriptor, this::updateButtons);

            itemConfigurationEditor.loadSettings(itemDescriptor);
        }

        return parent;
    }

    @Override
    protected Control createContents(Composite parent) {
        Control contents = super.createContents(parent);

        updateButtons();

        return contents;
    }

    private void updateButtons() {
        Button okButton = getButton(IDialogConstants.OK_ID);
        if (okButton != null) {
            okButton.setEnabled(
                itemDescriptor.isCustom() &&
                    !idText.getText().isEmpty() &&
                    !nameText.getText().isEmpty() &&
                    (itemConfigurationEditor == null || itemConfigurationEditor.isComplete())
            );
        }
    }


    private void saveSettings() {
        itemDescriptor.setId(idText.getText());
        itemDescriptor.setName(nameText.getText());
        itemDescriptor.setDisplayName(displayNameText.getText());
        itemDescriptor.setDescription(descriptionText.getText());
        if (itemConfigurationEditor != null) {
            itemConfigurationEditor.saveSettings(itemDescriptor);
        }
    }

    @Override
    protected void okPressed() {
        saveSettings();
        DashboardRegistry.getInstance().saveSettings();
        super.okPressed();
    }

}