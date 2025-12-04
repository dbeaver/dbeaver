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
package org.jkiss.dbeaver.ui.dialogs.connection;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Text;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.connection.DBPDriver;
import org.jkiss.dbeaver.registry.DataSourceDescriptor;
import org.jkiss.dbeaver.registry.DataSourcePreferenceStore;
import org.jkiss.dbeaver.ui.ShellUtils;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.utils.HelpUtils;

public class ConnectionPageInternalParameters extends ConnectionWizardPage {
    private final DataSourceDescriptor dataSourceDescriptor;

    ConnectionPageInternalParameters(@NotNull DataSourceDescriptor dataSourceDescriptor) {
        super(ConnectionPageInternalParameters.class.getSimpleName());
        this.dataSourceDescriptor = dataSourceDescriptor;

        setTitle(CoreMessages.dialog_connection_internal_parameters_title);
        setDescription(CoreMessages.dialog_connection_internal_parameters_description);
    }

    @Override
    public void createControl(Composite parent) {
        initializeDialogUnits(parent);

        Composite host = new Composite(parent, SWT.NONE);
        host.setLayout(GridLayoutFactory.fillDefaults().create());

        createParametersGroup(host);
        createCustomPropertiesGroup(host);

        UIUtils.createInfoLink(
            parent,
            CoreMessages.dialog_connection_internal_parameters_tip,
            () -> ShellUtils.launchProgram(HelpUtils.getHelpExternalReference("Admin-Manage-Preferences")),
            SWT.NONE,
            1,
            300
        );

        setControl(host);
    }

    private void createParametersGroup(@NotNull Composite parent) {
        Group group = UIUtils.createControlGroup(
            parent,
            CoreMessages.dialog_connection_internal_parameters_parameters,
            4,
            GridData.FILL_HORIZONTAL,
            0
        );

        Text projectIdText = UIUtils.createLabelText(
            group,
            CoreMessages.dialog_connection_internal_parameters_parameters_project_id,
            dataSourceDescriptor.getProject().getId()
        );
        projectIdText.setEditable(false);
        ((GridData) projectIdText.getLayoutData()).horizontalSpan = 3;

        Text connectionIdText = UIUtils.createLabelText(
            group,
            CoreMessages.dialog_connection_internal_parameters_parameters_connection_id,
            dataSourceDescriptor.getId()
        );
        connectionIdText.setEditable(false);
        ((GridData) connectionIdText.getLayoutData()).horizontalSpan = 3;

        DBPDriver driver = dataSourceDescriptor.getDriver();

        Text driverIdText = UIUtils.createLabelText(
            group,
            CoreMessages.dialog_connection_internal_parameters_parameters_driver_id,
            driver.getId()
        );
        driverIdText.setEditable(false);

        Text driverProviderIdText = UIUtils.createLabelText(
            group,
            CoreMessages.dialog_connection_internal_parameters_parameters_driver_provider_id,
            driver.getProviderId()
        );
        driverProviderIdText.setEditable(false);
    }

    private void createCustomPropertiesGroup(@NotNull Composite parent) {
        Group group = UIUtils.createControlGroup(
            parent,
            CoreMessages.dialog_connection_internal_parameters_custom,
            1,
            GridData.FILL_BOTH,
            0
        );

        TableViewer viewer = new TableViewer(group, SWT.SINGLE | SWT.FULL_SELECTION | SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL);
        viewer.getTable().setHeaderVisible(true);

        GridDataFactory.fillDefaults()
            .grab(true, true)
            .hint(500, 200)
            .applyTo(viewer.getControl());

        TableViewerColumn keyColumn = new TableViewerColumn(viewer, SWT.NONE);
        keyColumn.getColumn().setText(CoreMessages.dialog_connection_internal_parameters_custom_key);
        keyColumn.setLabelProvider(new ColumnLabelProvider() {
            @Override
            public String getText(Object element) {
                return (String) element;
            }
        });

        DataSourcePreferenceStore preferences = dataSourceDescriptor.getPreferenceStore();

        TableViewerColumn valueColumn = new TableViewerColumn(viewer, SWT.NONE);
        valueColumn.getColumn().setText(CoreMessages.dialog_connection_internal_parameters_custom_value);
        valueColumn.setLabelProvider(new ColumnLabelProvider() {
            @Override
            public String getText(Object element) {
                return preferences.getString((String) element);
            }
        });

        viewer.setContentProvider(new ArrayContentProvider());
        viewer.setInput(preferences.preferenceNames());

        UIUtils.asyncExec(() -> UIUtils.packColumns(viewer.getTable()));
    }

    @Override
    public boolean isPageComplete() {
        return true;
    }

    @Override
    public void saveSettings(@NotNull DBPDataSourceContainer dataSource) {
        // Nothing to save
    }
}
