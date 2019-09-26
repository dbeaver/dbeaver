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

package org.jkiss.dbeaver.tools.transfer.ui.wizard;

import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.DBValueFormatting;
import org.jkiss.dbeaver.model.runtime.DBRRunnableContext;
import org.jkiss.dbeaver.model.struct.DBSDataContainer;
import org.jkiss.dbeaver.model.struct.DBSDataManipulator;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectContainer;
import org.jkiss.dbeaver.model.task.DBTTask;
import org.jkiss.dbeaver.model.task.DBTTaskConfigPanel;
import org.jkiss.dbeaver.model.task.DBTTaskConfigurator;
import org.jkiss.dbeaver.model.task.DBTTaskType;
import org.jkiss.dbeaver.tools.transfer.DTConstants;
import org.jkiss.dbeaver.tools.transfer.DataTransferSettings;
import org.jkiss.dbeaver.tools.transfer.IDataTransferNode;
import org.jkiss.dbeaver.tools.transfer.database.DatabaseTransferConsumer;
import org.jkiss.dbeaver.tools.transfer.database.DatabaseTransferProducer;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.utils.ArrayUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
    public IWizard createTaskConfigWizard(@NotNull DBTTask taskConfiguration) {
        return new DataTransferWizard(UIUtils.getDefaultRunnableContext(), taskConfiguration);
    }

    private static class ConfigPanel implements DBTTaskConfigPanel {

        private DBRRunnableContext runnableContext;
        private DBTTaskType taskType;
        private Table objectsTable;
        //private DatabaseObjectsSelectorPanel selectorPanel;

        ConfigPanel(DBRRunnableContext runnableContext, DBTTaskType taskType) {
            this.runnableContext = runnableContext;
            this.taskType = taskType;
        }

        @Override
        public void createControl(Object parent, IPropertyChangeListener propertyChangeListener) {
            Group group = UIUtils.createControlGroup(
                (Composite) parent,
                (DTConstants.TASK_EXPORT.equals(taskType.getId()) ? "Export tables" : "Import into"),
                1,
                GridData.FILL_BOTH,
                0);
            objectsTable = new Table(group, SWT.BORDER | SWT.SINGLE);
            objectsTable.setLayoutData(new GridData(GridData.FILL_BOTH));
            //objectEditor.setHeaderVisible(true);
            UIUtils.createTableColumn(objectsTable, SWT.NONE, "Object");
            UIUtils.createTableColumn(objectsTable, SWT.NONE, "Data Source");

            Composite buttonsPanel = UIUtils.createComposite(group, 2);
            UIUtils.createDialogButton(buttonsPanel, "Add ...", new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    super.widgetSelected(e);
                }
            });
            Button removeButton = UIUtils.createDialogButton(buttonsPanel, "Remove", new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    super.widgetSelected(e);
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

        @Override
        public void loadSettings(DBRRunnableContext runnableContext, DBTTask task) {
            DataTransferSettings settings = new DataTransferSettings(runnableContext, task);
            List<DBSObject> selectedObjects = new ArrayList<>();
            for (IDataTransferNode node : ArrayUtils.safeArray(settings.getInitConsumers())) {
                if (node instanceof DatabaseTransferConsumer) {
                    selectedObjects.add(((DatabaseTransferConsumer) node).getTargetObject());
                }
            }
            for (IDataTransferNode node : ArrayUtils.safeArray(settings.getInitProducers())) {
                if (node instanceof DatabaseTransferProducer) {
                    selectedObjects.add(((DatabaseTransferProducer) node).getDatabaseObject());
                }
            }

            if (!selectedObjects.isEmpty()) {
                for (DBSObject object : selectedObjects) {
                    addObjectToTable(object);
                }
            }
            UIUtils.asyncExec(() -> UIUtils.packColumns(objectsTable, true));
        }

        private void addObjectToTable(DBSObject object) {
            TableItem item = new TableItem(objectsTable, SWT.NONE);
            item.setData(object);
            item.setImage(0, DBeaverIcons.getImage(DBValueFormatting.getObjectImage(object)));
            item.setText(0, DBUtils.getObjectFullName(object, DBPEvaluationContext.UI));
            if (object.getDataSource() != null) {
                item.setText(1, object.getDataSource().getContainer().getName());
            }
        }

        @Override
        public void saveSettings(DBRRunnableContext runnableContext, DBTTask task) {
            TableItem[] items = objectsTable.getItems();
            List<IDataTransferNode> nodes = new ArrayList<>();
            boolean isExport = taskType.getId().equals(DTConstants.TASK_EXPORT);
            String nodeType = isExport ? "producers" : "consumers";
            for (TableItem item : items) {
                DBSObject object = (DBSObject) item.getData();
                if (object instanceof DBSObjectContainer) {
/*
                    runnableContext.run(true, true, monitor -> {
                        ((DBSObjectContainer) object).getChildren(monitor)
                    });
*/
                }
                addNode(nodes, object, isExport);
            }
            Map<String, Object> taskConfig = new LinkedHashMap<>();
            DataTransferSettings.saveNodesLocation(taskConfig, nodes, nodeType);
            task.setProperties(taskConfig);
        }

        private void addNode(List<IDataTransferNode> nodes, DBSObject object, boolean isExport) {
            if (isExport) {
                if (object instanceof DBSDataContainer) {
                    nodes.add(new DatabaseTransferProducer((DBSDataContainer) object));
                }
            } else {
                if (object instanceof DBSDataManipulator) {
                    nodes.add(new DatabaseTransferConsumer((DBSDataManipulator) object));
                }
            }
        }

        @Override
        public boolean isComplete() {
            return objectsTable.getItemCount() > 0;
        }
    }

}
