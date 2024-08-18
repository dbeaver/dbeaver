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
package org.jkiss.dbeaver.tools.transfer.ui.pages.database;

import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPDataSourcePermission;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.impl.DataSourceContextProvider;
import org.jkiss.dbeaver.model.preferences.DBPPropertyDescriptor;
import org.jkiss.dbeaver.model.sql.SQLUtils;
import org.jkiss.dbeaver.model.struct.DBSEntity;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectContainer;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.runtime.properties.ObjectPropertyDescriptor;
import org.jkiss.dbeaver.runtime.properties.PropertySourceEditable;
import org.jkiss.dbeaver.runtime.ui.UIServiceSQL;
import org.jkiss.dbeaver.tools.transfer.database.DatabaseConsumerSettings;
import org.jkiss.dbeaver.tools.transfer.database.DatabaseMappingContainer;
import org.jkiss.dbeaver.tools.transfer.database.DatabaseTransferUtils;
import org.jkiss.dbeaver.tools.transfer.internal.DTMessages;
import org.jkiss.dbeaver.tools.transfer.ui.internal.DTUIMessages;
import org.jkiss.dbeaver.tools.transfer.ui.wizard.DataTransferWizard;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.BaseDialog;
import org.jkiss.dbeaver.ui.properties.PropertyTreeViewer;
import org.jkiss.utils.ArrayUtils;
import org.jkiss.utils.CommonUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Map;

/**
 *  Dialog with tabs to change target table properties and table columns mapping
 */
public class ConfigureMetadataStructureDialog extends BaseDialog {

    private DataTransferWizard wizard;
    private DatabaseConsumerSettings settings;
    private DatabaseMappingContainer mapping;
    private DBSObject tableObject;
    private TabFolder configTabs;
    private final DatabaseConsumerPageMapping pageMapping;
    private UIServiceSQL serviceSQL;
    private Object sqlPanel;
    private DBEPersistAction[] persistActions;
    private boolean ddlTabNeedRefresh = true;

    private PropertySourceEditable propertySource;

    public ConfigureMetadataStructureDialog(@NotNull DataTransferWizard wizard,
                                            @NotNull DatabaseConsumerSettings settings,
                                            @NotNull DatabaseMappingContainer mapping,
                                            @NotNull DatabaseConsumerPageMapping pageMapping)
    {
        super(wizard.getShell(), DTUIMessages.page_configure_metadata_title, null);
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
        columnsMappingTab.setText(DTUIMessages.columns_mapping_dialog_shell_text);
        ColumnsMappingDialog columnsMappingDialog = new ColumnsMappingDialog(settings, mapping);
        columnsMappingDialog.createControl(configTabs);
        columnsMappingTab.setData(columnsMappingDialog);
        Control pageControl = columnsMappingDialog.getControl();
        columnsMappingTab.setControl(pageControl);

        if (!mapping.hasNewTargetObject()) {
            tableObject = mapping.getTarget();
        }
        final DBSObjectContainer container = settings.getContainer();
        if (container != null && mapping.hasNewTargetObject()) {
            TabItem tablePropertiesTab = new TabItem(configTabs, SWT.NONE);
            tablePropertiesTab.setText(DTUIMessages.page_configure_table_properties_tab_title);
            DBPDataSource dataSource = container.getDataSource();
            DBCExecutionContext executionContext = DBUtils.getDefaultContext(dataSource, true);
            try {
                wizard.getRunnableContext().run(true, true, monitor -> {
                    monitor.beginTask("Generate new table object", 1);
                    try {
                        tableObject = DatabaseTransferUtils.generateStructTableDDL(
                            monitor,
                            executionContext,
                            container,
                            mapping,
                            new ArrayList<>(),
                            mapping.getChangedPropertiesMap());
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
            if (tableObject != null) {
                propertySource = new PropertySourceEditable(null, tableObject, tableObject);
                propertySource.collectProperties();

                for (DBPPropertyDescriptor prop : propertySource.getProperties()) {
                    if (prop instanceof ObjectPropertyDescriptor) {
                        final ObjectPropertyDescriptor obj = (ObjectPropertyDescriptor) prop;
                        if (!obj.isEditPossible(tableObject) || obj.isNameProperty()) {
                            propertySource.removeProperty(prop);
                        }
                    }
                }

                if (!ArrayUtils.isEmpty(propertySource.getProperties())) {
                    if (!CommonUtils.isEmpty(mapping.getChangedPropertiesMap())) {
                        // First check properties that could already be applied to this object
                        // (this means that this dialogue was already opened by the user, and the changes have been applied to the target)
                        propertySource.setChangedPropertiesMap(mapping.getChangedPropertiesMap());
                    } else if (!CommonUtils.isEmpty(mapping.getRawChangedPropertiesMap())) {
                        // Or maybe we have task with saved properties map
                        // But this map has only the id of ObjectPropertyDescriptor
                        // So we should find the correct properties and bound them
                        Map<String, Object> rawChangedPropertiesMap = mapping.getRawChangedPropertiesMap();
                        for (Map.Entry<String, Object> entry : rawChangedPropertiesMap.entrySet()) {
                            DBPPropertyDescriptor property = propertySource.getProperty(entry.getKey());
                            if (property != null) {
                                propertySource.addChangedProperties(property, entry.getValue());
                            }
                        }
                        // Update table properties
                        DatabaseTransferUtils.applyPropertyChanges(
                            null,
                            propertySource.getChangedPropertiesValues(),
                            null,
                            null,
                            (DBSEntity) tableObject);
                    }

                    final Composite propertiesComposite = new Composite(configTabs, SWT.NONE);
                    propertiesComposite.setLayout(new GridLayout(1, false));
                    propertiesComposite.setLayoutData(gd);

                    UIUtils.createLabel(propertiesComposite, DTUIMessages.page_configure_table_properties_text);

                    PropertyTreeViewer propertyViewer = new PropertyTreeViewer(propertiesComposite, SWT.BORDER);
                    propertyViewer.getControl().setLayoutData(gd);
                    propertyViewer.loadProperties(propertySource);

                    propertyViewer.changeColumnsWidth();

                    tablePropertiesTab.setControl(propertiesComposite);
                } else {
                    createCompositeWithMessage(gd, tablePropertiesTab, DTUIMessages.page_configure_table_properties_no_properties);
                }
            } else {
                createCompositeWithMessage(gd, tablePropertiesTab, DTUIMessages.page_configure_table_properties_info_text);
            }
        }

        TabItem showDDLTab = new TabItem(configTabs, SWT.NONE);
        showDDLTab.setText(DTMessages.data_transfer_wizard_page_ddl_name);
        showDDL(showDDLTab);

        configTabs.setSelection(0);
        configTabs.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                if (ddlTabNeedRefresh) {
                    final int selectionIndex = configTabs.getSelectionIndex();
                    final Control[] tabList = configTabs.getTabList();
                    if (tabList.length > 0 && selectionIndex == tabList.length - 1) {
                        // Refresh DDL tab - it is the last
                        final DBSObjectContainer container = settings.getContainer();
                        if (container != null) {
                            DBPDataSource dataSource = container.getDataSource();
                            setNewTextToDDLTab(container, dataSource);
                        }
                    }
                }
            }
        });

        return composite;
    }

    private void createCompositeWithMessage(GridData gd, TabItem tablePropertiesTab, String message) {
        Composite compositeEmpty = new Composite(configTabs, SWT.NONE);
        compositeEmpty.setLayout(new GridLayout(1, false));
        compositeEmpty.setLayoutData(gd);
        Composite panel = UIUtils.createPlaceholder(compositeEmpty, 1);
        panel.setLayoutData(gd);
        Text messageText = new Text(panel, SWT.READ_ONLY | SWT.BORDER | SWT.WRAP | SWT.V_SCROLL);
        messageText.setLayoutData(gd);
        messageText.setText(message);
        tablePropertiesTab.setControl(compositeEmpty);
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

        persistActions = generateTablePersistActions(container, dataSource);
        serviceSQL = DBWorkbench.getService(UIServiceSQL.class);
        boolean showSaveButton;
        if (serviceSQL != null) {
            String dialogText;
            if (dataSource != null && dataSource.getInfo().isDynamicMetadata()) {
                dialogText = DTUIMessages.database_consumer_page_mapping_sqlviewer_nonsql_tables_message;
                showSaveButton = false;
                ddlTabNeedRefresh = false;
            } else if (ArrayUtils.isEmpty(persistActions)) {
                dialogText = DTUIMessages.database_consumer_page_mapping_error_no_schema_changes_info;
                showSaveButton = false;
            } else {
                dialogText = SQLUtils.generateScript(dataSource, persistActions, false);
                showSaveButton = dataSource != null && dataSource.getContainer().hasModifyPermission(
                    DBPDataSourcePermission.PERMISSION_EDIT_METADATA);
            }
            try {
                sqlPanel = serviceSQL.createSQLPanel(
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
                final Button persistButton = UIUtils.createPushButton(
                    buttonsBar,
                    DTUIMessages.page_configure_table_DDL_button_execute,
                    null);
                persistButton.setLayoutData(gridData);
                persistButton.addSelectionListener(new SelectionAdapter() {
                        @Override
                        public void widgetSelected(SelectionEvent e) {
                            if (UIUtils.confirmAction(
                                getShell(),
                                DTUIMessages.database_consumer_page_mapping_create_target_object_confirmation_title,
                                DTUIMessages.database_consumer_page_mapping_create_target_object_confirmation_question)) {
                                // Create target objects
                                if (applySchemaChanges(container, mapping)) {
                                    pageMapping.autoAssignMappings();
                                }
                                close();
                            }
                        }
                    });
            }
            final Button copyButton = UIUtils.createPushButton(buttonsBar, DTUIMessages.page_configure_table_DDL_button_copy, null);
            copyButton.setLayoutData(gridData);
            copyButton.addSelectionListener(new SelectionAdapter() {
                    @Override
                    public void widgetSelected(SelectionEvent e) {
                        UIUtils.setClipboardContents(Display.getCurrent(), TextTransfer.getInstance(), dialogText);
                    }
                });
            showDDLTab.setControl(viewerComposite);
        }
    }

    @Nullable
    private DBEPersistAction[] generateTablePersistActions(DBSObjectContainer container, DBPDataSource dataSource) {
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
                        propertySource != null ? propertySource.getChangedPropertiesValues() : mapping.getChangedPropertiesMap());
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
            return null;
        } catch (InterruptedException e) {
            return null;
        }
        return ddl[0];
    }

    private boolean applySchemaChanges(@NotNull DBSObjectContainer targetContainer,
                                       @NotNull DatabaseMappingContainer mapping) {
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

    private void setNewTextToDDLTab(DBSObjectContainer container, DBPDataSource dataSource) {
        persistActions = generateTablePersistActions(container, dataSource);
        String dialogText;
        if (ArrayUtils.isEmpty(persistActions)) {
            dialogText = DTUIMessages.database_consumer_page_mapping_error_no_schema_changes_info;
        } else {
            dialogText = SQLUtils.generateScript(dataSource, persistActions, false);
        }
        if (serviceSQL != null) {
            serviceSQL.setSQLPanelText(sqlPanel, dialogText);
        }
    }

    @Override
    protected boolean isResizable() {
        return true;
    }

    @Override
    protected void okPressed() {
        // Save changes from the new created table if we have it
        if (propertySource != null) {
            Map<DBPPropertyDescriptor, Object> changedPropertiesValues = propertySource.getChangedPropertiesValues();
            if (!CommonUtils.isEmpty(changedPropertiesValues)) {
                mapping.setChangedPropertiesMap(changedPropertiesValues);
            }
        }
        super.okPressed();
    }
}
