/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2019 Serge Rider (serge@jkiss.org)
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

package org.jkiss.dbeaver.ext.mysql.tools;

import org.eclipse.jface.wizard.IWizard;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.mysql.model.MySQLCatalog;
import org.jkiss.dbeaver.ext.mysql.tasks.MySQLTasks;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.navigator.*;
import org.jkiss.dbeaver.model.runtime.DBRRunnableContext;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.task.DBTTask;
import org.jkiss.dbeaver.model.task.DBTTaskConfigPanel;
import org.jkiss.dbeaver.model.task.DBTTaskConfigurator;
import org.jkiss.dbeaver.model.task.DBTTaskType;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.tasks.ui.nativetool.AbstractImportExportWizard;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.connection.ClientHomesSelector;
import org.jkiss.dbeaver.ui.navigator.database.DatabaseObjectsSelectorPanel;
import org.jkiss.utils.CommonUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * MySQL task configurator
 */
public class MySQLTaskConfigurator implements DBTTaskConfigurator {

    private static final Log log = Log.getLog(MySQLTaskConfigurator.class);

    @Override
    public ConfigPanel createInputConfigurator(DBRRunnableContext runnableContext, @NotNull DBTTaskType taskType) {
        return new ConfigPanel(runnableContext, taskType);
    }

    @Override
    public IWizard createTaskConfigWizard(@NotNull DBTTask taskConfiguration) {
        switch (taskConfiguration.getType().getId()) {
            case MySQLTasks.TASK_DATABASE_BACKUP:
                return new MySQLExportWizard(taskConfiguration);
            case MySQLTasks.TASK_DATABASE_RESTORE:
                return new MySQLScriptExecuteWizard(taskConfiguration, true);
            case MySQLTasks.TASK_SCRIPT_EXECUTE:
                return new MySQLScriptExecuteWizard(taskConfiguration, false);
        }
        return null;
    }

    private static class ConfigPanel implements DBTTaskConfigPanel {

        private final DBRRunnableContext runnableContext;
        private final DBTTaskType taskType;
        private AbstractImportExportWizard<?, ?> ieWizard;
        private ClientHomesSelector homesSelector;
        private MySQLCatalog selectedCatalog;
        private DBPDataSource curDataSource;
        private DatabaseObjectsSelectorPanel selectorPanel;

        public ConfigPanel(DBRRunnableContext runnableContext, DBTTaskType taskType) {
            this.runnableContext = runnableContext;
            this.taskType = taskType;
        }

        @Override
        public void createControl(Object parent, Object wizard, Runnable propertyChangeListener) {
            ieWizard = (AbstractImportExportWizard) wizard;
            {
                Group databasesGroup = UIUtils.createControlGroup((Composite) parent, "Select target database", 1, GridData.FILL_BOTH, 0);

                selectorPanel = new DatabaseObjectsSelectorPanel(
                    databasesGroup,
                    false,
                    this.runnableContext) {
                    @Override
                    protected boolean isDatabaseFolderVisible(DBNDatabaseFolder folder) {
                        return folder.getChildrenClass() == MySQLCatalog.class;
                    }

                    @Override
                    protected boolean isDatabaseObjectVisible(DBSObject obj) {
                        return obj instanceof MySQLCatalog;
                    }

                    @Override
                    protected void onSelectionChange(Object element) {
                        selectedCatalog = element instanceof DBNDatabaseItem && ((DBNDatabaseItem) element).getObject() instanceof MySQLCatalog ?
                            (MySQLCatalog) ((DBNDatabaseItem) element).getObject() : null;
                        ieWizard.getDatabaseObjects().clear();
                        if (selectedCatalog != null) {
                            ieWizard.getDatabaseObjects().add(selectedCatalog);
                        }
                        updateHomeSelector();
                        propertyChangeListener.run();
                    }

                    @Override
                    protected boolean isFolderVisible(DBNLocalFolder folder) {
                        for (DBNDataSource ds : folder.getNestedDataSources()) {
                            if (isDataSourceVisible(ds)) {
                                return true;
                            }
                        }
                        return false;
                    }

                    @Override
                    protected boolean isDataSourceVisible(DBNDataSource dataSource) {
                        String driverId = dataSource.getDataSourceContainer().getDriver().getName().toLowerCase(Locale.ENGLISH);
                        return driverId.contains("mysql") || driverId.contains("mariadb");
                    }


                };
            }

            {
                Composite clientGroup = UIUtils.createControlGroup((Composite) parent, "Client files", 1, GridData.FILL_HORIZONTAL, 0);
                homesSelector = new ClientHomesSelector(clientGroup, "Native client");
                homesSelector.getPanel().setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
            }
        }

        private void updateHomeSelector() {
            DBPDataSource newDataSource = selectedCatalog != null ? selectedCatalog.getDataSource() : null;
            if (newDataSource != null && curDataSource != newDataSource) {
                homesSelector.populateHomes(newDataSource.getContainer().getDriver(), newDataSource.getContainer().getConnectionConfiguration().getClientHomeId(), true);
            }
            curDataSource = newDataSource;
        }

        @Override
        public void loadSettings() {
            List<DBSObject> databaseObjects = ieWizard.getSettings().getDatabaseObjects();
            if (!CommonUtils.isEmpty(databaseObjects)) {
                for (DBSObject obj : databaseObjects) {
                    if (obj instanceof MySQLCatalog) {
                        selectedCatalog = (MySQLCatalog) obj;
                    }
                }
            }

            if (selectorPanel != null && selectedCatalog != null) {
                try {
                    DBNDatabaseNode[] catalogNode = new DBNDatabaseNode[1];
                    ieWizard.getRunnableContext().run(true, true, monitor -> {
                        catalogNode[0] = DBNUtils.getNodeByObject(monitor, selectedCatalog, false);
                    });
                    if (catalogNode[0] != null) {
                        List<DBNNode> selCatalogs = Collections.singletonList(catalogNode[0]);
                        //selectorPanel.checkNodes(selCatalogs, true);
                        selectorPanel.setSelection(selCatalogs);
                    }
                } catch (InvocationTargetException e) {
                    DBWorkbench.getPlatformUI().showError("Catalogs", " Error loading catalog list", e.getTargetException());
                } catch (InterruptedException e) {
                    // ignore
                }
            }
        }

        @Override
        public void saveSettings() {

        }

        @Override
        public boolean isComplete() {
            return homesSelector.getSelectedHome() != null && selectedCatalog != null;
        }
    }

}
