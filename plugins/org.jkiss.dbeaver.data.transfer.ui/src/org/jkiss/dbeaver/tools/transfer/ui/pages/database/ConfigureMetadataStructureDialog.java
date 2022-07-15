/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2022 DBeaver Corp and others
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
package org.jkiss.dbeaver.tools.transfer.ui.pages.database;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.swt.widgets.Text;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPDataSourcePermission;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.impl.DataSourceContextProvider;
import org.jkiss.dbeaver.model.sql.SQLUtils;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectContainer;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.runtime.ui.UIServiceSQL;
import org.jkiss.dbeaver.tools.transfer.database.DatabaseConsumerSettings;
import org.jkiss.dbeaver.tools.transfer.database.DatabaseMappingContainer;
import org.jkiss.dbeaver.tools.transfer.database.DatabaseMappingType;
import org.jkiss.dbeaver.tools.transfer.database.DatabaseTransferUtils;
import org.jkiss.dbeaver.tools.transfer.internal.DTMessages;
import org.jkiss.dbeaver.tools.transfer.ui.internal.DTUIMessages;
import org.jkiss.dbeaver.tools.transfer.ui.wizard.DataTransferWizard;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.BaseDialog;
import org.jkiss.dbeaver.ui.editors.object.struct.PropertyObjectEditPage;
import org.jkiss.utils.ArrayUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;

/**
 *  Dialog with tabs to change target table properties and table columns mapping
 */
public class ConfigureMetadataStructureDialog extends BaseDialog {

    private DataTransferWizard wizard;
    private DatabaseConsumerSettings settings;
    private DatabaseMappingContainer mapping;
    private final DBSObject[] tableObject = {null};
    private TabFolder configTabs;
    private final DatabaseConsumerPageMapping pageMapping;

    public ConfigureMetadataStructureDialog(@NotNull DataTransferWizard wizard,
                                            @NotNull DatabaseConsumerSettings settings,
                                            @NotNull DatabaseMappingContainer mapping,
                                            @NotNull DatabaseConsumerPageMapping pageMapping) {
        super(wizard.getShell(), "Configure metadata structure", null);
        this.wizard = wizard;
        this.settings = settings;
        this.mapping = mapping;
        this.pageMapping = pageMapping;
    }

    @Override
    protected Composite createDialogArea(Composite parent) {
        Composite composite = super.createDialogArea(parent);
        GridData gd = new GridData(GridData.FILL_BOTH);
        composite.setLayoutData(gd);

        configTabs = new TabFolder(composite, SWT.TOP | SWT.FLAT);
        configTabs.setLayoutData(new GridData(GridData.FILL_BOTH));

        TabItem columnsMappingTab = new TabItem(configTabs, SWT.NONE);
        columnsMappingTab.setText(DTUIMessages.columns_mapping_dialog_shell_text + mapping.getTargetName());
        ColumnsMappingDialog columnsMappingDialog = new ColumnsMappingDialog(wizard, settings, mapping);
        columnsMappingDialog.createControl(configTabs);
        columnsMappingTab.setData(columnsMappingDialog);
        Control pageControl = columnsMappingDialog.getControl();
        columnsMappingTab.setControl(pageControl);

        final DBSObjectContainer container = settings.getContainer();
        if (container != null) {
            TabItem tablePropertiesTab = new TabItem(configTabs, SWT.NONE);
            tablePropertiesTab.setText("Table properties");
            DBPDataSource dataSource = container.getDataSource();
            DBCExecutionContext executionContext = DBUtils.getDefaultContext(dataSource, true);
            if (mapping.getMappingType() != DatabaseMappingType.create
                && mapping.getMappingType() != DatabaseMappingType.recreate)
            {
                tableObject[0] = mapping.getTarget();
            } else {
                try {
                    wizard.getRunnableContext().run(true, true, monitor -> {
                        monitor.beginTask("Generate new table object", 1);
                        try {
                            tableObject[0] = DatabaseTransferUtils.generateStructTableDDL(
                                monitor,
                                executionContext,
                                container,
                                mapping,
                                new ArrayList<>(),
                                null);
                        } catch (DBException e) {
                            throw new InvocationTargetException(e);
                        }
                        monitor.done();
                    });
                } catch (InvocationTargetException e) {
                    DBWorkbench.getPlatformUI().showError(
                        DTUIMessages.database_consumer_page_mapping_title_target_table,
                        DTUIMessages.database_consumer_page_mapping_message_error_generating_target_table,
                        e);
                } catch (InterruptedException e) {
                   // do nothing
                }
            }
            if (tableObject[0] != null) {
                final PropertyObjectEditPage page = new PropertyObjectEditPage(null, tableObject[0]);
                page.createControl(configTabs);
                tablePropertiesTab.setData(page);
                Control tablePageControl = page.getControl();
                tablePropertiesTab.setControl(tablePageControl);
            } else {
                Composite compositeEmpty = new Composite(configTabs, SWT.NONE);
                compositeEmpty.setLayout(new GridLayout(1, false));
                compositeEmpty.setLayoutData(new GridData(GridData.FILL_BOTH));
                Text messageText = new Text(compositeEmpty, SWT.READ_ONLY | SWT.BORDER | SWT.WRAP | SWT.V_SCROLL);
                messageText.setLayoutData(new GridData(GridData.FILL_BOTH));
                messageText.setText("Can't create target table properties info");
                tablePropertiesTab.setControl(compositeEmpty);
            }
        }

        TabItem showDDLTab = new TabItem(configTabs, SWT.NONE);
        showDDLTab.setText(DTMessages.data_transfer_wizard_page_ddl_name);
        showDDL(showDDLTab);

        configTabs.setSelection(0);
        configTabs.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                // Refresh DDL here
            }
        });

        return composite;
    }

    private void showDDL(@NotNull TabItem showDDLTab) {
        final DBSObjectContainer container = settings.getContainer();
        if (container == null) {
            return;
        }
        Composite viewerComposite = new Composite(configTabs, SWT.BORDER);
        viewerComposite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        viewerComposite.setLayout(new GridLayout(1, false));

        Composite panel = UIUtils.createPlaceholder(viewerComposite, 1);
        panel.setLayoutData(new GridData(GridData.FILL_BOTH));

        Composite editorPH = new Composite(panel, SWT.BORDER);
        GridData gd = new GridData(GridData.FILL_BOTH);
        gd.minimumHeight = 100;
        gd.minimumWidth = 100;
        editorPH.setLayoutData(gd);
        editorPH.setLayout(new FillLayout());

        DBPDataSource dataSource = container.getDataSource();

        final DBEPersistAction[][] ddl = new DBEPersistAction[1][];
        try {
            wizard.getRunnableContext().run(true, true, monitor -> {
                monitor.beginTask(DTUIMessages.database_consumer_page_mapping_monitor_task, 1);
                try {
                    DBCExecutionContext executionContext = DBUtils.getDefaultContext(dataSource, true);
                    ddl[0] = DatabaseTransferUtils.generateTargetTableDDL(
                        monitor,
                        executionContext,
                        container,
                        mapping,
                        tableObject[0]);
                } catch (DBException e) {
                    throw new InvocationTargetException(e);
                }
                monitor.done();
            });
        } catch (InvocationTargetException e) {
            DBWorkbench.getPlatformUI().showError(
                DTUIMessages.database_consumer_page_mapping_title_target_DDL,
                DTUIMessages.database_consumer_page_mapping_message_error_generating_target_DDL,
                e);
            return;
        } catch (InterruptedException e) {
            return;
        }
        DBEPersistAction[] persistActions = ddl[0];
        if (ArrayUtils.isEmpty(persistActions)) {
            UIUtils.showMessageBox(
                getShell(),
                DTUIMessages.database_consumer_page_mapping_error_no_schema_changes_title,
                DTUIMessages.database_consumer_page_mapping_error_no_schema_changes_info,
                SWT.ICON_INFORMATION);
            return;
        }
        UIServiceSQL serviceSQL = DBWorkbench.getService(UIServiceSQL.class);
        boolean showSaveButton;
        if (serviceSQL != null) {
            String dialogText;
            if (dataSource.getInfo().isDynamicMetadata()) {
                dialogText = DTUIMessages.database_consumer_page_mapping_sqlviewer_nonsql_tables_message;
                showSaveButton = false;
            } else {
                dialogText = SQLUtils.generateScript(dataSource, persistActions, false);
                showSaveButton = dataSource.getContainer().hasModifyPermission(
                    DBPDataSourcePermission.PERMISSION_EDIT_METADATA);
            }
            try {
                final Object sqlPanel = serviceSQL.createSQLPanel(
                    UIUtils.getActiveWorkbenchWindow().getActivePage().getActivePart().getSite(),
                    editorPH,
                    new DataSourceContextProvider(container),
                    DTUIMessages.database_consumer_page_mapping_sqlviewer_title,
                    false,
                    dialogText
                );
                serviceSQL.setSQLPanelText(sqlPanel, dialogText);
            } catch (DBException e) {
                DBWorkbench.getPlatformUI().showError(
                    "Can't create SQL panel",
                    "Error creating SQL panel",
                    e);
            }

            Composite buttonsBar = UIUtils.createComposite(viewerComposite, 2);
            buttonsBar.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_END));

            GridData gridData = new GridData(GridData.FILL_BOTH);
            gridData.minimumHeight = 25;
            gridData.minimumWidth = 100;
            if (showSaveButton) {
                final Button persistButton = UIUtils.createPushButton(buttonsBar, "Persist", null);
                persistButton.setLayoutData(gridData);
                persistButton.addSelectionListener(new SelectionAdapter() {
                        @Override
                        public void widgetSelected(SelectionEvent e) {
                            if (UIUtils.confirmAction(
                                getShell(),
                                DTUIMessages.database_consumer_page_mapping_create_target_object_confirmation_title,
                                DTUIMessages.database_consumer_page_mapping_create_target_object_confirmation_question))
                            {
                                // Create target objects
                                if (applySchemaChanges(container, mapping, persistActions)) {
                                    pageMapping.autoAssignMappings();
                                }
                            }
                        }
                    });
            }
            final Button copyButton = UIUtils.createPushButton(buttonsBar, "Copy", null);
            copyButton.setLayoutData(gridData);
            copyButton.addSelectionListener(new SelectionAdapter() {
                    @Override
                    public void widgetSelected(SelectionEvent e) {

                    }
                });
            showDDLTab.setControl(viewerComposite);
        }
    }

    private boolean applySchemaChanges(@NotNull DBSObjectContainer targetContainer,
                                       @NotNull DatabaseMappingContainer mapping,
                                       @NotNull DBEPersistAction[] persistActions) {
        try {
            wizard.getRunnableContext().run(true, true, monitor -> {
                monitor.beginTask("Save schema changes in the database", 1);

                try (DBCSession session = DBUtils.openUtilSession(
                    monitor,
                    targetContainer,
                    "Apply schema changes")) {
                    DatabaseTransferUtils.executeDDL(session, persistActions);

                    if (settings != null) {
                        DatabaseTransferUtils.refreshDatabaseModel(monitor, settings, mapping);
                    }
                } catch (Exception e) {
                    throw new InvocationTargetException(e);
                } finally {
                    monitor.done();
                }
            });
            return true;
        } catch (InvocationTargetException e) {
            DBWorkbench.getPlatformUI().showError(DTUIMessages.database_consumer_page_mapping_error_schema_save_title,
                DTUIMessages.database_consumer_page_mapping_error_schema_save_info, e.getTargetException());
        } catch (InterruptedException e) {
            // ignore
        }
        return false;
    }

    @Override
    protected boolean isResizable() {
        return true;
    }

    @Override
    protected void okPressed() {
        super.okPressed();
    }
}
