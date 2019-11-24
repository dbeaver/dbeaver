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
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.navigator.DBNDataSource;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseFolder;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseItem;
import org.jkiss.dbeaver.model.navigator.DBNLocalFolder;
import org.jkiss.dbeaver.model.runtime.DBRRunnableContext;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.task.DBTTask;
import org.jkiss.dbeaver.model.task.DBTTaskConfigPanel;
import org.jkiss.dbeaver.model.task.DBTTaskConfigurator;
import org.jkiss.dbeaver.model.task.DBTTaskType;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.connection.ClientHomesSelector;
import org.jkiss.dbeaver.ui.navigator.database.DatabaseObjectsSelectorPanel;

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
        private ClientHomesSelector homesSelector;
        private MySQLCatalog selectedCatalog;
        private DBPDataSource curDataSource;

        public ConfigPanel(DBRRunnableContext runnableContext, DBTTaskType taskType) {
            this.runnableContext = runnableContext;
            this.taskType = taskType;
        }

        @Override
        public void createControl(Object parent, Object wizard, Runnable propertyChangeListener) {
            {
                Group databasesGroup = UIUtils.createControlGroup((Composite) parent, "Select target database", 1, GridData.FILL_BOTH, 0);

                DatabaseObjectsSelectorPanel selectorPanel = new DatabaseObjectsSelectorPanel(
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
