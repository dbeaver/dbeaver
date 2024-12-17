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

package org.jkiss.dbeaver.tools.transfer.ui.wizard;

import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.PlatformUI;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.*;
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.exec.DBCExecutionContextDefaults;
import org.jkiss.dbeaver.model.exec.DBExecUtils;
import org.jkiss.dbeaver.model.impl.DataSourceContextProvider;
import org.jkiss.dbeaver.model.navigator.*;
import org.jkiss.dbeaver.model.runtime.DBRRunnableContext;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLQuery;
import org.jkiss.dbeaver.model.sql.SQLScriptContext;
import org.jkiss.dbeaver.model.sql.data.SQLQueryDataContainer;
import org.jkiss.dbeaver.model.struct.*;
import org.jkiss.dbeaver.model.struct.rdb.DBSCatalog;
import org.jkiss.dbeaver.model.struct.rdb.DBSSchema;
import org.jkiss.dbeaver.model.task.DBTTask;
import org.jkiss.dbeaver.model.task.DBTTaskType;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.runtime.ui.UIServiceSQL;
import org.jkiss.dbeaver.tasks.ui.DBTTaskConfigPanel;
import org.jkiss.dbeaver.tasks.ui.DBTTaskConfigPanelProvider;
import org.jkiss.dbeaver.tasks.ui.DBTTaskConfigurator;
import org.jkiss.dbeaver.tasks.ui.wizard.TaskConfigurationWizard;
import org.jkiss.dbeaver.tools.transfer.*;
import org.jkiss.dbeaver.tools.transfer.database.DatabaseTransferConsumer;
import org.jkiss.dbeaver.tools.transfer.database.DatabaseTransferProducer;
import org.jkiss.dbeaver.tools.transfer.internal.DTMessages;
import org.jkiss.dbeaver.tools.transfer.ui.internal.DTUIMessages;
import org.jkiss.dbeaver.ui.BaseEditorColors;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.UIWidgets;
import org.jkiss.dbeaver.ui.navigator.NavigatorUtils;
import org.jkiss.dbeaver.ui.navigator.dialogs.ObjectBrowserDialog;
import org.jkiss.utils.CommonUtils;

import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

/**
 * Data transfer task configurator
 */
public class DataTransferTaskConfigurator implements DBTTaskConfigurator, DBTTaskConfigPanelProvider {

    private static final Log log = Log.getLog(DataTransferTaskConfigurator.class);

    @Override
    public ConfigPanel createInputConfigurator(DBRRunnableContext runnableContext, @NotNull DBTTaskType taskType) {
        return new ConfigPanel(runnableContext, taskType);
    }

    @Override
    public TaskConfigurationWizard<?> createTaskConfigWizard(@NotNull DBTTask taskConfiguration) {
        return DataTransferWizard.openWizard(taskConfiguration);
    }

    private static class ConfigPanel implements DBTTaskConfigPanel {

        private DBRRunnableContext runnableContext;
        private DBTTaskType taskType;
        private Table objectsTable;
        private DBPProject currentProject;
        private DataTransferWizard dtWizard;

        ConfigPanel(DBRRunnableContext runnableContext, DBTTaskType taskType) {
            this.runnableContext = runnableContext;
            this.taskType = taskType;
            this.currentProject = NavigatorUtils.getSelectedProject();
        }

        public DBPDataSource getLastDataSource() {
            int itemCount = objectsTable.getItemCount();
            if (itemCount <= 0) return null;
            DataTransferPipe pipe = (DataTransferPipe) objectsTable.getItem(itemCount - 1).getData();
            DBSObject databaseObject = getTableNode(pipe).getDatabaseObject();
            return databaseObject == null ? null : databaseObject.getDataSource();
        }

        @Override
        public void createControl(Composite parent, TaskConfigurationWizard wizard, Runnable propertyChangeListener) {
            dtWizard = (DataTransferWizard) wizard;
            boolean isExport = isExport();

            Group group = UIUtils.createControlGroup(
                parent,
                (DTConstants.TASK_EXPORT.equals(taskType.getId()) ? DTUIMessages.data_transfer_task_configurator_group_label_export_tables : DTUIMessages.data_transfer_task_configurator_group_label_import_into),
                1,
                GridData.FILL_BOTH,
                0);
            objectsTable = new Table(group, SWT.BORDER | SWT.SINGLE | SWT.FULL_SELECTION);
            objectsTable.setLayoutData(new GridData(GridData.FILL_BOTH));
            objectsTable.setHeaderVisible(true);
            UIUtils.createTableColumn(objectsTable, SWT.NONE, DTUIMessages.data_transfer_task_configurator_table_column_text_object);
            UIUtils.createTableColumn(objectsTable, SWT.NONE, DTUIMessages.data_transfer_task_configurator_table_column_text_data_source);
            UIWidgets.createTableContextMenu(objectsTable, null);

            Composite buttonsPanel = UIUtils.createComposite(group, isExport ? 4 : 3);
            UIUtils.createDialogButton(buttonsPanel, DTUIMessages.data_transfer_task_configurator_dialog_button_label_add_table, new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    Class<?> tableClass = isExport ? DBSDataContainer.class : DBSDataManipulator.class;
                    DBNProjectDatabases rootNode = currentProject.getNavigatorModel().getRoot().getProjectNode(currentProject).getDatabases();
                    DBNNode selNode = null;
                    if (objectsTable.getItemCount() > 0) {
                        DBPDataSource lastDataSource = getLastDataSource();
                        if (lastDataSource != null) {
                            selNode = rootNode.getDataSource(lastDataSource.getContainer().getId());
                        }
                    }
                    List<DBNNode> tables = ObjectBrowserDialog.selectObjects(
                        group.getShell(),
                        isExport ? DTUIMessages.data_transfer_task_configurator_tables_title_choose_source : DTUIMessages.data_transfer_task_configurator_tables_title_choose_target,
                        rootNode,
                        CommonUtils.singletonOrEmpty(selNode),
                        new Class[]{DBSInstance.class, DBSObjectContainer.class, tableClass},
                        new Class[]{tableClass},
                        null);
                    if (tables != null) {
                        for (DBNNode node : tables) {
                            if (node instanceof DBNDatabaseNode) {
                                DBSObject object = ((DBNDatabaseNode) node).getObject();
                                DataTransferPipe pipe = new DataTransferPipe(
                                    isExport ? new DatabaseTransferProducer((DBSDataContainer) object) : null,
                                    isExport ? null : new DatabaseTransferConsumer((DBSDataManipulator) object));
                                addPipeToTable(pipe);
                            }
                        }
                        updateSettings(propertyChangeListener);
                    }
                }
            });
            if (isExport) {
                UIUtils.createDialogButton(buttonsPanel, DTUIMessages.data_transfer_task_configurator_dialog_button_label_add_query, new SelectionAdapter() {
                    @Override
                    public void widgetSelected(SelectionEvent e) {
                        DBSObject dataSourceObject = null;
                        DBPDataSource dataSource = null;

                        DBNProjectDatabases rootNode = currentProject.getNavigatorModel().getRoot().getProjectNode(currentProject).getDatabases();
                        DBNNode selNode = null;
                        if (objectsTable.getItemCount() > 0) {
                            DBPDataSource lastDataSource = getLastDataSource();
                            if (lastDataSource != null) {
                                selNode = rootNode.getDataSource(lastDataSource.getContainer().getId());
                            }
                        }
                        DBNNode node = ObjectBrowserDialog.selectObject(
                            group.getShell(),
                            DTUIMessages.data_transfer_task_configurator_tables_title_choose_source,
                            rootNode,
                            selNode,
                            new Class[]{DBSInstance.class, DBSObjectContainer.class},
                            new Class[]{DBPDataSource.class, DBSCatalog.class, DBSSchema.class},
                            null);

                        if (node != null) {
                            if (node instanceof DBNDataSource) {
                                dataSourceObject = ((DBNDataSource) node).getDataSource();
                                dataSource = ((DBNDataSource) node).getDataSource();
                            } else if (node instanceof DBNDatabaseItem) {
                                dataSourceObject = ((DBNDatabaseItem) node).getObject();
                                dataSource = dataSourceObject.getDataSource();
                            } else {
                                log.debug("Unhandled node type: " + node);
                                return;
                            }
                        }

                        if (dataSource != null) {
                            DBPDataSourceContainer dataSourceContainer = DBUtils.getContainer(dataSource);

                            if (dataSourceContainer != null && !dataSourceContainer.isConnected()) {
                                try {
                                    runnableContext.run(true, true, monitor -> {
                                        try {
                                            dataSourceContainer.connect(monitor, true, true);
                                        } catch (DBException ex) {
                                            throw new InvocationTargetException(ex);
                                        }
                                    });
                                } catch (InvocationTargetException ex) {
                                    DBWorkbench.getPlatformUI().showError(DTUIMessages.data_transfer_task_configurator_title_error_opening_data_source,
                                            DTUIMessages.data_transfer_task_configurator_message_error_while_opening_data_source, ex);
                                    return;
                                } catch (InterruptedException ex) {
                                    return;
                                }
                            }

                            String newInstanceName;
                            String newObjectName;

                            if (dataSourceObject instanceof DBSCatalog) {
                                newInstanceName = dataSourceObject.getName();
                                newObjectName = null;
                            } else if (dataSourceObject instanceof DBSSchema) {
                                DBSObject parentObject = dataSourceObject.getParentObject();
                                newInstanceName = parentObject instanceof DBSCatalog ? parentObject.getName() : null;
                                newObjectName = dataSourceObject.getName();
                            } else {
                                // Use default database and schema
                                newInstanceName = null;
                                newObjectName = null;
                            }

                            DataSourceContextProvider contextProvider = new DataSourceContextProvider(dataSourceObject);
                            DBCExecutionContext executionContext = contextProvider.getExecutionContext();

                            String oldInstanceName = null;
                            String oldObjectName = null;

                            if (executionContext instanceof DBCExecutionContextDefaults) {
                                DBCExecutionContextDefaults<?, ?> contextDefaults = ((DBCExecutionContextDefaults<?, ?>) executionContext);
                                DBSCatalog defaultCatalog = contextDefaults.getDefaultCatalog();
                                if (defaultCatalog != null) {
                                    oldInstanceName = defaultCatalog.getName();
                                }
                                DBSSchema defaultSchema = contextDefaults.getDefaultSchema();
                                if (defaultSchema != null) {
                                    oldObjectName = defaultSchema.getName();
                                }
                            }

                            try {
                                DBExecUtils.setExecutionContextDefaults(new VoidProgressMonitor(), dataSource, executionContext, newInstanceName, null, newObjectName);
                            } catch (DBException ex) {
                                log.error("Error setting context defaults", ex);
                                return;
                            }

                            UIServiceSQL serviceSQL = DBWorkbench.getService(UIServiceSQL.class);
                            if (serviceSQL != null) {
                                String query = serviceSQL.openSQLEditor(contextProvider, DTUIMessages.data_transfer_task_configurator_sql_query_title, DBIcon.TREE_SCRIPT, "");
                                if (query != null) {
                                    SQLScriptContext scriptContext = new SQLScriptContext(null, contextProvider, null, new PrintWriter(System.err, true), null);
                                    SQLQueryDataContainer container = new SQLQueryDataContainer(contextProvider, new SQLQuery(dataSource, query), scriptContext, log);
                                    DatabaseTransferProducer producer = new DatabaseTransferProducer(container);
                                    producer.setDefaultCatalog(newInstanceName);
                                    producer.setDefaultSchema(newObjectName);
                                    DataTransferPipe pipe = new DataTransferPipe(producer, null);
                                    addPipeToTable(pipe);
                                    updateSettings(propertyChangeListener);
                                }
                            }

                            try {
                                DBExecUtils.setExecutionContextDefaults(new VoidProgressMonitor(), dataSource, executionContext, oldInstanceName, null, oldObjectName);
                            } catch (DBException ex) {
                                log.error("Error setting context defaults", ex);
                            }
                        }
                    }
                });
            }
            Button editButton = UIUtils.createDialogButton(buttonsPanel, DTMessages.data_transfer_wizard_settings_button_edit, new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    TableItem item = objectsTable.getItem(objectsTable.getSelectionIndex());
                    DataTransferPipe pipe = (DataTransferPipe) item.getData();
                    IDataTransferProducer<?> producer = pipe.getProducer();
                    if (producer instanceof DatabaseTransferProducer && producer.getDatabaseObject() instanceof SQLQueryDataContainer) {
                        DBPDataSource dataSource = producer.getDatabaseObject().getDataSource();
                        DataSourceContextProvider contextProvider = new DataSourceContextProvider(producer.getDatabaseObject());

                        UIServiceSQL serviceSQL = DBWorkbench.getService(UIServiceSQL.class);
                        if (serviceSQL != null) {

                            String query = serviceSQL.openSQLEditor(
                                contextProvider,
                                DTUIMessages.data_transfer_task_configurator_sql_query_title,
                                DBIcon.TREE_SCRIPT,
                                ((SQLQueryDataContainer) producer.getDatabaseObject()).getQuery().getText());
                            if (query != null) {
                                SQLQueryDataContainer container = (SQLQueryDataContainer)producer.getDatabaseObject();
                                container.setQuery(new SQLQuery(dataSource, query));
                                IDataTransferNode node = getTableNode(pipe);
                                item.setText(node.getObjectName());
                            }
                        }
                    }
                }
            });
            Button removeButton = UIUtils.createDialogButton(buttonsPanel, DTUIMessages.data_transfer_task_configurator_dialog_button_label_remove, new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    DataTransferPipe object = (DataTransferPipe) objectsTable.getItem(objectsTable.getSelectionIndex()).getData();
                    if (UIUtils.confirmAction(
                        DTUIMessages.data_transfer_task_configurator_confirm_action_title,
                        NLS.bind(DTUIMessages.data_transfer_task_configurator_confirm_action_question,
                        CommonUtils.truncateString(getTableNode(object).getObjectName(), 255))))
                    {
                        objectsTable.remove(objectsTable.getSelectionIndex());
                        updateSettings(propertyChangeListener);
                    }
                }
            });
            editButton.setEnabled(false);
            removeButton.setEnabled(false);

            objectsTable.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    int selectionIndex = objectsTable.getSelectionIndex();
                    DataTransferPipe pipe = (selectionIndex >= 0) ?
                        (DataTransferPipe) objectsTable.getItem(selectionIndex).getData() : null;
                    editButton.setEnabled(
                        pipe != null &&
                        pipe.getProducer() instanceof DatabaseTransferProducer &&
                        pipe.getProducer().getDatabaseObject() instanceof SQLQueryDataContainer);

                    removeButton.setEnabled(pipe != null);
                }
            });
        }

        private void updateSettings(Runnable propertyChangeListener) {
            saveSettings();
            propertyChangeListener.run();
            UIUtils.asyncExec(() -> UIUtils.packColumns(objectsTable, true));
        }

        private boolean isExport() {
            return taskType.getId().equals(DTConstants.TASK_EXPORT);
        }

        @Override
        public void loadSettings() {
            dtWizard.loadNodeSettings();

            DataTransferSettings settings = dtWizard.getSettings();

            for (DataTransferPipe pipe : settings.getDataPipes()) {
                addPipeToTable(pipe);
            }
            UIUtils.asyncExec(() -> UIUtils.packColumns(objectsTable, true));
        }

        private void addPipeToTable(DataTransferPipe pipe) {
            IDataTransferNode node = getTableNode(pipe);
            TableItem item = new TableItem(objectsTable, SWT.NONE);
            item.setData(pipe);
            item.setImage(0, DBeaverIcons.getImage(node.getObjectIcon()));
            item.setText(0, CommonUtils.getSingleLineString(CommonUtils.toString(node.getObjectName(), "?")));

            if (node.getDataSourceContainer() != null) {
                item.setText(1, node.getDataSourceContainer().getName());
                DBPImage dsIcon = node.getObjectContainerIcon();
                if (dsIcon != null) {
                    item.setImage(1, DBeaverIcons.getImage(dsIcon));
                }
            }
            if (node.getDatabaseObject() == null) {
                item.setBackground(
                    PlatformUI.getWorkbench().getThemeManager().getCurrentTheme().getColorRegistry().get(BaseEditorColors.COLOR_ERROR)
                );
            }
        }

        private IDataTransferNode getTableNode(DataTransferPipe pipe) {
            return isExport() ? pipe.getProducer() : pipe.getConsumer();
        }

        @Override
        public void saveSettings() {
            if (objectsTable == null) {
                return;
            }

            DataTransferSettings settings = dtWizard.getSettings();

            // Save from config table
            List<DataTransferPipe> dataPipes = new ArrayList<>();
            for (TableItem item : objectsTable.getItems()) {
                dataPipes.add((DataTransferPipe) item.getData());
            }
            settings.setDataPipes(dataPipes, isExport());
            dtWizard.loadSettings();
        }

        @Override
        public boolean isComplete() {
            if (objectsTable.getItemCount() == 0) {
                return false;
            }
            for (DataTransferPipe pipe : dtWizard.getSettings().getDataPipes()) {
                if (!dtWizard.getSettings().isProducerOptional()) {
                    if (pipe.getProducer() == null || !pipe.getProducer().isConfigurationComplete()) {
                        return false;
                    }
                }
                if (!dtWizard.getSettings().isConsumerOptional()) {
                    if (pipe.getConsumer() == null || !pipe.getConsumer().isConfigurationComplete()) {
                        return false;
                    }
                }
            }

            return objectsTable.getItemCount() > 0;
        }

        @Override
        public String getErrorMessage() {
            if (objectsTable.getItemCount() == 0) {
                return DTUIMessages.data_transfer_error_no_objects_selected;
            }
            for (DataTransferPipe pipe : dtWizard.getSettings().getDataPipes()) {
                if (!dtWizard.getSettings().isProducerOptional()) {
                    if (pipe.getProducer() == null || !pipe.getProducer().isConfigurationComplete()) {
                        return NLS.bind(DTUIMessages.data_transfer_error_source_not_specified,
                            (pipe.getConsumer() == null ? "?" : pipe.getConsumer().getObjectName()));
                    }
                }
                if (!dtWizard.getSettings().isConsumerOptional()) {
                    if (pipe.getConsumer() == null || !pipe.getConsumer().isConfigurationComplete()) {
                        return NLS.bind(DTUIMessages.data_transfer_error_target_not_specified,
                            (pipe.getProducer() == null ? "?" : pipe.getProducer().getObjectName()));
                    }
                }
            }
            return null;
        }
    }

}
