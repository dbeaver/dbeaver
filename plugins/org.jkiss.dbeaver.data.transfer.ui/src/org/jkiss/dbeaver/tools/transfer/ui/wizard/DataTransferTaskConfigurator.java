/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
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

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.model.impl.DataSourceContextProvider;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseNode;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.model.navigator.DBNProjectDatabases;
import org.jkiss.dbeaver.model.runtime.DBRRunnableContext;
import org.jkiss.dbeaver.model.sql.SQLQuery;
import org.jkiss.dbeaver.model.sql.data.SQLQueryDataContainer;
import org.jkiss.dbeaver.model.struct.DBSDataContainer;
import org.jkiss.dbeaver.model.struct.DBSDataManipulator;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectContainer;
import org.jkiss.dbeaver.model.task.DBTTask;
import org.jkiss.dbeaver.model.task.DBTTaskType;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.runtime.ui.UIServiceSQL;
import org.jkiss.dbeaver.tasks.ui.DBTTaskConfigPanel;
import org.jkiss.dbeaver.tasks.ui.DBTTaskConfigurator;
import org.jkiss.dbeaver.tasks.ui.wizard.TaskConfigurationWizard;
import org.jkiss.dbeaver.tools.transfer.DTConstants;
import org.jkiss.dbeaver.tools.transfer.DataTransferPipe;
import org.jkiss.dbeaver.tools.transfer.DataTransferSettings;
import org.jkiss.dbeaver.tools.transfer.IDataTransferNode;
import org.jkiss.dbeaver.tools.transfer.database.DatabaseTransferConsumer;
import org.jkiss.dbeaver.tools.transfer.database.DatabaseTransferProducer;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UIIcon;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.navigator.NavigatorUtils;
import org.jkiss.dbeaver.ui.navigator.dialogs.BrowseObjectDialog;
import org.jkiss.dbeaver.ui.navigator.dialogs.SelectDataSourceDialog;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

/**
 * Data transfer task configurator
 */
public class DataTransferTaskConfigurator implements DBTTaskConfigurator {

    private static final Log log = Log.getLog(DataTransferTaskConfigurator.class);

    @Override
    public ConfigPanel createInputConfigurator(DBRRunnableContext runnableContext, @NotNull DBTTaskType taskType) {
        return new ConfigPanel(runnableContext, taskType);
    }

    @Override
    public TaskConfigurationWizard createTaskConfigWizard(@NotNull DBTTask taskConfiguration) {
        return new DataTransferWizard(UIUtils.getDefaultRunnableContext(), taskConfiguration);
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
                (DTConstants.TASK_EXPORT.equals(taskType.getId()) ? "Export tables" : "Import into"),
                1,
                GridData.FILL_BOTH,
                0);
            objectsTable = new Table(group, SWT.BORDER | SWT.SINGLE);
            objectsTable.setLayoutData(new GridData(GridData.FILL_BOTH));
            objectsTable.setHeaderVisible(true);
            UIUtils.createTableColumn(objectsTable, SWT.NONE, "Object");
            UIUtils.createTableColumn(objectsTable, SWT.NONE, "Data Source");
            UIUtils.createTableContextMenu(objectsTable, null);

            Composite buttonsPanel = UIUtils.createComposite(group, isExport ? 3 : 2);
            UIUtils.createDialogButton(buttonsPanel, "Add Table ...", new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    Class<?> tableClass = isExport ? DBSDataContainer.class : DBSDataManipulator.class;
                    DBNProjectDatabases rootNode = DBWorkbench.getPlatform().getNavigatorModel().getRoot().getProjectNode(currentProject).getDatabases();
                    DBNNode selNode = null;
                    if (objectsTable.getItemCount() > 0) {
                        DBPDataSource lastDataSource = getLastDataSource();
                        if (lastDataSource != null) {
                            selNode = rootNode.getDataSource(lastDataSource.getContainer().getId());
                        }
                    }
                    List<DBNNode> tables = BrowseObjectDialog.selectObjects(
                        group.getShell(),
                        isExport ? "Choose source table(s)" : "Choose target table(s)",
                        rootNode,
                        selNode,
                        new Class[]{DBSObjectContainer.class, tableClass},
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
                UIUtils.createDialogButton(buttonsPanel, "Add Query ...", new SelectionAdapter() {
                    @Override
                    public void widgetSelected(SelectionEvent e) {
                        DBPDataSource lastDataSource = getLastDataSource();
                        if (lastDataSource == null) {
                            SelectDataSourceDialog dsDialog = new SelectDataSourceDialog(group.getShell(), currentProject, null);
                            if (dsDialog.open() == IDialogConstants.OK_ID) {
                                DBPDataSourceContainer dataSource = dsDialog.getDataSource();
                                if (!dataSource.isConnected()) {
                                    try {
                                        runnableContext.run(true, true, monitor -> {
                                            try {
                                                dataSource.connect(monitor, true, true);
                                            } catch (DBException ex) {
                                                throw new InvocationTargetException(ex);
                                            }
                                        });
                                    } catch (InvocationTargetException ex) {
                                        DBWorkbench.getPlatformUI().showError("Error opening datasource", "Error while opening datasource", ex);
                                        return;
                                    } catch (InterruptedException ex) {
                                        return;
                                    }
                                }
                                lastDataSource = dataSource.getDataSource();
                            }
                        }

                        if (lastDataSource != null) {
                            UIServiceSQL serviceSQL = DBWorkbench.getService(UIServiceSQL.class);
                            if (serviceSQL != null) {
                                DataSourceContextProvider contextProvider = new DataSourceContextProvider(lastDataSource);
                                String query = serviceSQL.openSQLEditor(contextProvider, "SQL Query", UIIcon.SQL_SCRIPT, "");
                                if (query != null) {

                                    DataTransferPipe pipe = new DataTransferPipe(
                                        new DatabaseTransferProducer(
                                            new SQLQueryDataContainer(contextProvider, new SQLQuery(lastDataSource, query), null, log)),
                                        null);

                                    addPipeToTable(pipe);
                                    updateSettings(propertyChangeListener);
                                }
                            }
                        }
                    }
                });
            }
            Button removeButton = UIUtils.createDialogButton(buttonsPanel, "Remove", new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    DataTransferPipe object = (DataTransferPipe) objectsTable.getItem(objectsTable.getSelectionIndex()).getData();
                    if (UIUtils.confirmAction("Remove object", "Remove object " + getTableNode(object).getObjectName() + "?")) {
                        objectsTable.remove(objectsTable.getSelectionIndex());
                        updateSettings(propertyChangeListener);
                    }
                }
            });
            removeButton.setEnabled(false);
            objectsTable.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    removeButton.setEnabled(objectsTable.getSelectionIndex() >= 0);
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
            item.setText(0, node.getObjectName());

            DBSObject object = node.getDatabaseObject();
            if (object != null && object.getDataSource() != null) {
                item.setText(1, object.getDataSource().getContainer().getName());
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
            return objectsTable.getItemCount() > 0;
        }

        @Override
        public String getErrorMessage() {
            if (objectsTable.getItemCount() == 0) {
                return "No objects selected";
            }
            return null;
        }
    }

}
