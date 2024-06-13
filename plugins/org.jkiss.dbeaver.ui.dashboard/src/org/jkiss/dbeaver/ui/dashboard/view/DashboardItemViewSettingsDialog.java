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
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.jkiss.dbeaver.model.DBIcon;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.dashboard.registry.DashboardItemConfiguration;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.runtime.ui.UIServiceSQL;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.IObjectPropertyConfigurator;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dashboard.internal.UIDashboardMessages;
import org.jkiss.dbeaver.ui.dashboard.model.DashboardConfiguration;
import org.jkiss.dbeaver.ui.dashboard.model.DashboardItemContainer;
import org.jkiss.dbeaver.ui.dashboard.model.DashboardItemViewSettings;
import org.jkiss.dbeaver.ui.dashboard.registry.DashboardRendererDescriptor;
import org.jkiss.dbeaver.ui.dashboard.registry.DashboardUIRegistry;
import org.jkiss.dbeaver.ui.dialogs.BaseDialog;
import org.jkiss.utils.CommonUtils;

public class DashboardItemViewSettingsDialog extends BaseDialog {

    private static final boolean SHOW_QUERIES_BUTTON = false;

    private final DashboardItemViewSettings itemViewSettings;
    private final DashboardConfiguration viewConfiguration;
    private final DashboardItemContainer dashboardContainer;
    private IObjectPropertyConfigurator<DashboardItemViewSettings, DashboardItemViewSettings> itemViewSettingsEditor;

    public DashboardItemViewSettingsDialog(Shell shell, DashboardItemContainer dashboardContainer, DashboardConfiguration viewConfiguration) {
        super(shell, NLS.bind(UIDashboardMessages.dialog_dashboard_item_config_title, dashboardContainer.getItemDescriptor().getName()), null);

        this.viewConfiguration = viewConfiguration;
        this.dashboardContainer = dashboardContainer;
        this.itemViewSettings = new DashboardItemViewSettings(dashboardContainer.getItemConfiguration());
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

        {
            Group infoGroup = UIUtils.createControlGroup(composite, UIDashboardMessages.dialog_dashboard_item_config_dashboardinfo, 4, GridData.FILL_HORIZONTAL, 0);

            UIUtils.createLabelText(infoGroup, UIDashboardMessages.dialog_dashboard_item_config_dashboardinfo_labels_name,
                    itemViewSettings.getItemConfiguration().getName(), SWT.BORDER | SWT.READ_ONLY)
                .setLayoutData(new GridData(GridData.FILL, GridData.BEGINNING, true, false, 3, 1));

            UIUtils.createControlLabel(infoGroup, UIDashboardMessages.dialog_dashboard_item_config_dashboardinfo_labels_description).setLayoutData(new GridData(GridData.VERTICAL_ALIGN_BEGINNING));
            Text descriptionText = new Text(infoGroup, SWT.BORDER | SWT.MULTI | SWT.WRAP | SWT.V_SCROLL);
            descriptionText.setText(CommonUtils.notEmpty(itemViewSettings.getDescription()));
            descriptionText.addModifyListener(e -> itemViewSettings.setDescription(descriptionText.getText()));
            GridData gd = new GridData(GridData.FILL_HORIZONTAL);
            gd.widthHint = 200;
            gd.heightHint = 50;
            descriptionText.setLayoutData(gd);

            if (SHOW_QUERIES_BUTTON) {
                Composite btnGroup = UIUtils.createComposite(infoGroup, 1);
                btnGroup.setLayoutData(new GridData(GridData.FILL, GridData.BEGINNING, true, false, 4, 1));
                Button queriesButton = new Button(btnGroup, SWT.PUSH);
                queriesButton.setText(UIDashboardMessages.dialog_dashboard_item_config_buttons_sqlqueries);
                queriesButton.setImage(DBeaverIcons.getImage(DBIcon.TREE_SCRIPT));
                queriesButton.setLayoutData(new GridData(GridData.END, GridData.BEGINNING, true, false));
                queriesButton.addSelectionListener(new SelectionAdapter() {
                    @Override
                    public void widgetSelected(SelectionEvent e) {
                        StringBuilder sql = new StringBuilder();
                        for (DashboardItemConfiguration.QueryMapping query : itemViewSettings.getItemConfiguration().getQueries()) {
                            sql.append(query.getQueryText()).append(";\n");
                        }
                        UIServiceSQL serviceSQL = DBWorkbench.getService(UIServiceSQL.class);
                        if (serviceSQL != null) {
                            serviceSQL.openSQLViewer(
                                DBUtils.getDefaultContext(dashboardContainer.getDataSourceContainer().getDataSource(), true),
                                UIDashboardMessages.dialog_dashboard_item_config_buttons_sqlqueries_dash,
                                DBIcon.TREE_SCRIPT,
                                sql.toString(),
                                false, false);
                        }
                    }
                });
                queriesButton.setEnabled(dashboardContainer.getDataSourceContainer().isConnected());
            }
        }

        DashboardRendererDescriptor renderer = DashboardUIRegistry.getInstance().getViewType(dashboardContainer.getItemDescriptor().getDashboardRenderer());

        if (renderer != null) {
            try {
                itemViewSettingsEditor = renderer.createItemViewSettingsEditor();
            } catch (Exception e) {
                DBWorkbench.getPlatformUI().showError("Error creating configuration editor", null, e);
            }
        }
        if (itemViewSettingsEditor != null) {
            Composite configComposite = UIUtils.createComposite(composite, 1);
            configComposite.setLayoutData(new GridData(GridData.FILL_BOTH));
            itemViewSettingsEditor.createControl(configComposite, itemViewSettings, () -> {});

            itemViewSettingsEditor.loadSettings(itemViewSettings);
        }

        return parent;
    }

    @Override
    protected Control createContents(Composite parent) {
        Control contents = super.createContents(parent);

        return contents;
    }

    @Override
    protected void createButtonsForButtonBar(Composite parent) {
        createButton(parent, IDialogConstants.CANCEL_ID, UIDashboardMessages.dialog_dashboard_item_config_buttons_configuration, false).addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                DashboardItemConfigurationDialog editDialog = new DashboardItemConfigurationDialog(getShell(), itemViewSettings.getItemConfiguration(), false);
                editDialog.open();
            }
        });
        super.createButtonsForButtonBar(parent);
    }

    @Override
    protected void okPressed() {
        super.okPressed();
        if (itemViewSettingsEditor != null) {
            itemViewSettingsEditor.saveSettings(itemViewSettings);
        }
        viewConfiguration.updateItemConfig(this.itemViewSettings);
        dashboardContainer.getGroup().getView().saveChanges();
        dashboardContainer.updateDashboardView();
    }
}