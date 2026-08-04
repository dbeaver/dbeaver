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
package org.jkiss.dbeaver.tools.transfer.ui.pages.database;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.events.SelectionListener;
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
import org.jkiss.dbeaver.model.sql.SQLConstants;
import org.jkiss.dbeaver.model.sql.SQLUtils;
import org.jkiss.dbeaver.model.struct.DBSDataContainer;
import org.jkiss.dbeaver.model.struct.DBSEntity;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectContainer;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.runtime.properties.ObjectPropertyDescriptor;
import org.jkiss.dbeaver.runtime.properties.PropertySourceEditable;
import org.jkiss.dbeaver.runtime.ui.UIServiceSQL;
import org.jkiss.dbeaver.tools.transfer.DataTransferPipe;
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
import java.util.*;
import java.util.List;

/**
 * Dialog with tabs to change target table properties and table columns mapping
 */
public class ConfigureMetadataStructureDialog extends BaseDialog {

    private final DataTransferWizard wizard;
    private final DatabaseConsumerSettings settings;
    private final DatabaseMappingContainer mapping;
    private DBSObject tableObject;
    private CTabFolder configTabs;
    private final DatabaseConsumerPageMapping pageMapping;
    private UIServiceSQL serviceSQL;
    private Object sqlPanel;
    private Object fullSqlPanel;
    private DBEPersistAction[] persistActions;
    private DBEPersistAction[] fullDdlActions;
    private String targetDdlText = "";
    private String fullDdlText = "";
    private CTabItem showDDLTab;
    private CTabItem fullDDLTab;
    private boolean ddlTabNeedRefresh = true;

    private PropertySourceEditable propertySource;

    public ConfigureMetadataStructureDialog(
        @NotNull DataTransferWizard wizard,
        @NotNull DatabaseConsumerSettings settings,
        @NotNull DatabaseMappingContainer mapping,
        @NotNull DatabaseConsumerPageMapping pageMapping
    ) {
        super(wizard.getShell(), DTUIMessages.page_configure_metadata_title, null);
        this.wizard = wizard;
        this.settings = settings;
        this.mapping = mapping;
        this.pageMapping = pageMapping;
    }

    @NotNull
    @Override
    protected Composite createDialogArea(@NotNull Composite parent) {
        Composite composite = super.createDialogArea(parent);
        GridData gd = new GridData(GridData.FILL_BOTH);
        composite.setLayoutData(gd);

        configTabs = new CTabFolder(composite, SWT.TOP | SWT.FLAT);
        configTabs.setLayoutData(new GridData(GridData.FILL_BOTH));

        CTabItem columnsMappingTab = new CTabItem(configTabs, SWT.NONE);
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
            CTabItem tablePropertiesTab = new CTabItem(configTabs, SWT.NONE);
            tablePropertiesTab.setText(DTUIMessages.page_configure_table_properties_tab_title);
            DBPDataSource dataSource = container.getDataSource();
            DBCExecutionContext executionContext = DBUtils.getDefaultContext(dataSource, true);
            if (executionContext != null) {
                detectTargetObjects(executionContext, container, tablePropertiesTab);
            }
        }

        showDDLTab = new CTabItem(configTabs, SWT.NONE);
        showDDLTab.setText(DTMessages.data_transfer_wizard_page_ddl_name);
        showDDL(showDDLTab);

        fullDDLTab = new CTabItem(configTabs, SWT.NONE);
        fullDDLTab.setText(DTUIMessages.page_configure_table_DDL_full_tab_title);
        createFullDDLTab(fullDDLTab);

        configTabs.setSelection(0);
        configTabs.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> {
            if (ddlTabNeedRefresh) {
                final CTabItem selection = configTabs.getSelection();
                final DBSObjectContainer objContainer = settings.getContainer();
                if (objContainer != null) {
                    DBPDataSource dataSource = objContainer.getDataSource();
                    if (selection == showDDLTab) {
                        setNewTextToDDLTab(objContainer, dataSource);
                    } else if (selection == fullDDLTab) {
                        setNewTextToFullDDLTab(objContainer, dataSource);
                    }
                }
            }
        }));

        return composite;
    }

    private void detectTargetObjects(
        @NotNull DBCExecutionContext executionContext,
        @NotNull DBSObjectContainer container,
        @NotNull CTabItem tablePropertiesTab
    ) {
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
                if (prop instanceof ObjectPropertyDescriptor obj) {
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
                propertiesComposite.setLayoutData(new GridData(GridData.FILL_BOTH));

                UIUtils.createLabel(propertiesComposite, DTUIMessages.page_configure_table_properties_text);

                PropertyTreeViewer propertyViewer = new PropertyTreeViewer(propertiesComposite, SWT.BORDER);
                propertyViewer.getControl().setLayoutData(new GridData(GridData.FILL_BOTH));
                propertyViewer.loadProperties(propertySource);

                propertyViewer.changeColumnsWidth();

                tablePropertiesTab.setControl(propertiesComposite);
            } else {
                createCompositeWithMessage(
                    new GridData(GridData.FILL_BOTH),
                    tablePropertiesTab,
                    DTUIMessages.page_configure_table_properties_no_properties
                );
            }
        } else {
            createCompositeWithMessage(
                new GridData(GridData.FILL_BOTH),
                tablePropertiesTab,
                DTUIMessages.page_configure_table_properties_info_text
            );
        }
    }

    private void createCompositeWithMessage(@NotNull GridData gd, @NotNull CTabItem tablePropertiesTab, @NotNull String message) {
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

    private void showDDL(@NotNull CTabItem showDDLTab) {
        final DBSObjectContainer container = settings.getContainer();
        if (container == null) {
            return;
        }
        serviceSQL = DBWorkbench.getService(UIServiceSQL.class);
        if (serviceSQL == null) {
            return;
        }
        DBPDataSource dataSource = container.getDataSource();
        persistActions = generateTableActions(container, dataSource, mapping);
        DBEPersistAction[] foreignKeyActions = generateForeignKeysDDL(dataSource, mapping);

        boolean dynamicMetadata = dataSource != null && dataSource.getInfo().isDynamicMetadata();
        if (dynamicMetadata) {
            targetDdlText = DTUIMessages.database_consumer_page_mapping_sqlviewer_nonsql_tables_message;
            ddlTabNeedRefresh = false;
        } else {
            targetDdlText = composeDdlText(dataSource, persistActions, foreignKeyActions);
        }
        boolean showSaveButton = !dynamicMetadata
            && !ArrayUtils.isEmpty(persistActions)
            && dataSource != null
            && dataSource.getContainer().hasModifyPermission(DBPDataSourcePermission.PERMISSION_EDIT_METADATA);
        sqlPanel = createDdlTab(
            showDDLTab,
            container,
            targetDdlText,
            showSaveButton,
            () -> executeDdlAndClose(container, persistActions, List.of(mapping)));
    }

    private void createFullDDLTab(@NotNull CTabItem fullDDLTab) {
        final DBSObjectContainer container = settings.getContainer();
        if (container == null || serviceSQL == null) {
            return;
        }
        DBPDataSource dataSource = container.getDataSource();
        boolean dynamicMetadata = dataSource != null && dataSource.getInfo().isDynamicMetadata();
        if (dynamicMetadata) {
            fullDdlText = DTUIMessages.database_consumer_page_mapping_sqlviewer_nonsql_tables_message;
        }
        boolean showExecuteButton = !dynamicMetadata
            && dataSource != null
            && dataSource.getContainer().hasModifyPermission(DBPDataSourcePermission.PERMISSION_EDIT_METADATA);
        fullSqlPanel = createDdlTab(
            fullDDLTab,
            container,
            fullDdlText,
            showExecuteButton,
            () -> executeDdlAndClose(container, fullDdlActions, settings.getDataMappings().values()));
    }

    @Nullable
    private Object createDdlTab(
        @NotNull CTabItem tabItem,
        @NotNull DBSObjectContainer container,
        @NotNull String initialText,
        boolean showExecuteButton,
        @NotNull Runnable executeAction
    ) {
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

        final Object panelObject = createSqlPanel(editorPH, container, initialText);

        Composite buttonsBar = UIUtils.createComposite(viewerComposite, 2);
        buttonsBar.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_END));

        GridData gridData = new GridData(GridData.FILL_BOTH);
        gridData.minimumHeight = 25;
        gridData.minimumWidth = 100;
        if (showExecuteButton) {
            final Button persistButton = UIUtils.createPushButton(
                buttonsBar,
                DTUIMessages.page_configure_table_DDL_button_execute,
                null);
            persistButton.setLayoutData(gridData);
            persistButton.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> executeAction.run()));
        }
        final Button copyButton = UIUtils.createPushButton(buttonsBar, DTUIMessages.page_configure_table_DDL_button_copy, null);
        copyButton.setLayoutData(gridData);
        copyButton.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> UIUtils.setClipboardContents(
            Display.getCurrent(),
            TextTransfer.getInstance(),
            panelObject != null ? serviceSQL.getSQLPanelText(panelObject) : ""
        )));
        tabItem.setControl(viewerComposite);
        return panelObject;
    }

    @Nullable
    private Object createSqlPanel(@NotNull Composite parent, @NotNull DBSObjectContainer container, @NotNull String text) {
        try {
            Object panelObject = serviceSQL.createSQLPanel(
                UIUtils.getActiveWorkbenchWindow().getActivePage().getActivePart().getSite(),
                parent,
                new DataSourceContextProvider(container),
                DTUIMessages.database_consumer_page_mapping_sqlviewer_title,
                false,
                text
            );
            serviceSQL.setSQLPanelText(panelObject, text);
            return panelObject;
        } catch (DBException e) {
            DBWorkbench.getPlatformUI().showError(
                "Can't create SQL panel",
                "Error creating SQL panel",
                e);
            return null;
        }
    }

    private void executeDdlAndClose(
        @NotNull DBSObjectContainer container,
        @Nullable DBEPersistAction[] actions,
        @NotNull Collection<DatabaseMappingContainer> mappingsToRefresh
    ) {
        if (ArrayUtils.isEmpty(actions)) {
            return;
        }
        if (!UIUtils.confirmAction(
            getShell(),
            DTUIMessages.database_consumer_page_mapping_create_target_object_confirmation_title,
            DTUIMessages.database_consumer_page_mapping_create_target_object_confirmation_question
        )) {
            return;
        }
        if (applySchemaChanges(container, actions, mappingsToRefresh)) {
            pageMapping.autoAssignMappings();
        }
        close();
    }

    private boolean applySchemaChanges(
        @NotNull DBSObjectContainer targetContainer,
        @NotNull DBEPersistAction[] actions,
        @NotNull Collection<DatabaseMappingContainer> mappingsToRefresh
    ) {
        try {
            wizard.getRunnableContext().run(true, true, monitor -> {
                monitor.beginTask("Save schema changes in the database", 1);

                try (DBCSession session = DBUtils.openUtilSession(
                    monitor,
                    targetContainer,
                    "Apply schema changes")) {
                    DatabaseTransferUtils.executeDDL(session, actions);

                    for (DatabaseMappingContainer containerMapping : mappingsToRefresh) {
                        DatabaseTransferUtils.refreshDatabaseModel(monitor, settings, containerMapping);
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

    private void setNewTextToDDLTab(@NotNull DBSObjectContainer container, @Nullable DBPDataSource dataSource) {
        persistActions = generateTableActions(container, dataSource, mapping);
        targetDdlText = composeDdlText(dataSource, persistActions, generateForeignKeysDDL(dataSource, mapping));
        if (serviceSQL != null) {
            serviceSQL.setSQLPanelText(sqlPanel, targetDdlText);
        }
    }

    private void setNewTextToFullDDLTab(@NotNull DBSObjectContainer container, @Nullable DBPDataSource dataSource) {
        DBEPersistAction[] tableActions = generateTableActions(container, dataSource, null);
        DBEPersistAction[] foreignKeyActions = generateForeignKeysDDL(dataSource, null);
        fullDdlActions = ArrayUtils.concatArrays(tableActions, foreignKeyActions);
        fullDdlText = composeDdlText(dataSource, tableActions, foreignKeyActions);
        if (serviceSQL != null && fullSqlPanel != null) {
            serviceSQL.setSQLPanelText(fullSqlPanel, fullDdlText);
        }
    }

    @NotNull
    private static String composeDdlText(
        @Nullable DBPDataSource dataSource,
        @Nullable DBEPersistAction[] tableActions,
        @Nullable DBEPersistAction[] foreignKeyActions
    ) {
        if (ArrayUtils.isEmpty(tableActions) && ArrayUtils.isEmpty(foreignKeyActions)) {
            return DTUIMessages.database_consumer_page_mapping_error_no_schema_changes_info;
        }
        StringBuilder text = new StringBuilder();
        if (!ArrayUtils.isEmpty(tableActions)) {
            text.append(SQLUtils.generateScript(dataSource, tableActions, false));
        }
        if (!ArrayUtils.isEmpty(foreignKeyActions)) {
            if (!text.isEmpty()) {
                text.append("\n");
            }
            String commentPrefix = dataSource == null || ArrayUtils.isEmpty(dataSource.getSQLDialect().getSingleLineComments())
                ? SQLConstants.SL_COMMENT
                : dataSource.getSQLDialect().getSingleLineComments()[0];
            text.append(commentPrefix).append(" ")
                .append(DTUIMessages.page_configure_table_DDL_foreign_keys_comment).append("\n");
            text.append(SQLUtils.generateScript(dataSource, foreignKeyActions, false));
        }
        return text.toString();
    }

    @NotNull
    private DBEPersistAction[] generateTableActions(
        @NotNull DBSObjectContainer container,
        @Nullable DBPDataSource dataSource,
        @Nullable DatabaseMappingContainer onlyMapping
    ) {
        final List<DBEPersistAction> actions = new ArrayList<>();
        try {
            wizard.getRunnableContext().run(true, true, monitor -> {
                try {
                    DBCExecutionContext executionContext = DBUtils.getDefaultContext(dataSource, true);
                    if (executionContext == null) {
                        return;
                    }
                    List<DatabaseMappingContainer> mappings = onlyMapping != null ? List.of(onlyMapping) : getPipeMappings();
                    for (DatabaseMappingContainer containerMapping : mappings) {
                        Collections.addAll(actions, DatabaseTransferUtils.generateTargetTableDDL(
                            monitor,
                            executionContext,
                            container,
                            containerMapping,
                            getChangedProperties(containerMapping),
                            settings));
                    }
                } catch (DBException e) {
                    throw new InvocationTargetException(e);
                }
            });
        } catch (InvocationTargetException e) {
            showDdlGenerationError(e);
        } catch (InterruptedException e) {
            // ignore
        }
        return actions.toArray(DBEPersistAction[]::new);
    }

    @NotNull
    private DBEPersistAction[] generateForeignKeysDDL(
        @Nullable DBPDataSource dataSource,
        @Nullable DatabaseMappingContainer onlyMapping
    ) {
        final List<DBEPersistAction> actions = new ArrayList<>();
        try {
            wizard.getRunnableContext().run(true, true, monitor -> {
                try {
                    DBCExecutionContext executionContext = DBUtils.getDefaultContext(dataSource, true);
                    if (executionContext != null) {
                        Collections.addAll(actions, settings.generatePostTransferDDL(monitor, executionContext, onlyMapping));
                    }
                } catch (DBException e) {
                    throw new InvocationTargetException(e);
                }
            });
        } catch (InvocationTargetException e) {
            showDdlGenerationError(e);
        } catch (InterruptedException e) {
            // ignore
        }
        return actions.toArray(DBEPersistAction[]::new);
    }

    private static void showDdlGenerationError(@NotNull InvocationTargetException e) {
        DBWorkbench.getPlatformUI().showError(
            DTUIMessages.database_consumer_page_mapping_title_target_DDL,
            DTUIMessages.database_consumer_page_mapping_message_error_generating_target_DDL,
            e);
    }

    @NotNull
    private List<DatabaseMappingContainer> getPipeMappings() {
        List<DatabaseMappingContainer> mappings = new ArrayList<>();
        for (DataTransferPipe pipe : wizard.getSettings().getDataPipes()) {
            if (pipe.getProducer() != null
                && pipe.getProducer().getDatabaseObject() instanceof DBSDataContainer dataContainer
            ) {
                DatabaseMappingContainer containerMapping = settings.getDataMapping(dataContainer);
                if (containerMapping != null) {
                    mappings.add(containerMapping);
                }
            }
        }
        return mappings;
    }

    @Nullable
    private Map<DBPPropertyDescriptor, Object> getChangedProperties(@NotNull DatabaseMappingContainer containerMapping) {
        return containerMapping == mapping && propertySource != null
            ? propertySource.getChangedPropertiesValues()
            : containerMapping.getChangedPropertiesMap();
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
