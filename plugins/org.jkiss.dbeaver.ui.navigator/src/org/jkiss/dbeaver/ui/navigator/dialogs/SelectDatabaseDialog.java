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
package org.jkiss.dbeaver.ui.navigator.dialogs;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.exec.DBCExecutionContextDefaults;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseNode;
import org.jkiss.dbeaver.model.navigator.DBNUtils;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRRunnableWithResult;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectContainer;
import org.jkiss.dbeaver.model.struct.rdb.DBSCatalog;
import org.jkiss.dbeaver.ui.internal.UINavigatorMessages;
import org.jkiss.dbeaver.ui.navigator.itemlist.DatabaseObjectListControl;
import org.jkiss.utils.CommonUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * SelectDatabaseDialog
 */
public class SelectDatabaseDialog extends SelectObjectDialog<DBNDatabaseNode>
{
    private static final Log log = Log.getLog(SelectDatabaseDialog.class);

    private final DBPDataSourceContainer dataSourceContainer;
    private String currentInstanceName;

    private DatabaseObjectListControl<DBNDatabaseNode> instanceList;
    private List<DBNDatabaseNode> selectedInstances = new ArrayList<>();

    public SelectDatabaseDialog(
        Shell parentShell,
        DBPDataSourceContainer dataSourceContainer,
        String currentInstanceName,
        Collection<DBNDatabaseNode> objects,
        Collection<DBNDatabaseNode> selected)
    {
        super(parentShell,
            UINavigatorMessages.label_choose_catalog,
            true,
            "SchemaSelector", //$NON-NLS-1$
            objects,
            selected);
        this.dataSourceContainer = dataSourceContainer;
        this.currentInstanceName = currentInstanceName;
    }

    @Override
    protected void createUpperControls(Composite dialogArea) {

        DBPDataSource dataSource = dataSourceContainer.getDataSource();
        if (currentInstanceName != null && dataSource != null) {

            DBSObjectContainer instanceContainer = DBUtils.getAdapter(DBSObjectContainer.class, dataSource);
            DBCExecutionContextDefaults contextDefaults = null;
            DBCExecutionContext defaultContext = DBUtils.getDefaultContext(instanceContainer, true);
            if (defaultContext != null) {
                contextDefaults = defaultContext.getContextDefaults();
            }
            if (instanceContainer != null && contextDefaults != null && contextDefaults.supportsCatalogChange() && contextDefaults.supportsSchemaChange()) {
                createInstanceSelector(dialogArea, instanceContainer);
            }
        }
    }

    private void createInstanceSelector(Composite group, DBSObjectContainer instanceContainer) {
        ((GridLayout)group.getLayout()).numColumns++;
        instanceList = createObjectSelector(group, true, "DatabaseInstanceSelector", selectedInstances, new DBRRunnableWithResult<List<DBNDatabaseNode>>() {
            @Override
            public void run(DBRProgressMonitor monitor) throws InvocationTargetException {
                try {
                    if (!CommonUtils.isEmpty(currentInstanceName) && selectedInstances.isEmpty()) {
                        DBSObject activeInstance = instanceContainer.getChild(monitor, currentInstanceName);
                        if (activeInstance != null) {
                            DBNDatabaseNode activeInstanceNode = DBNUtils.getNodeByObject(monitor, activeInstance, false);
                            if (activeInstanceNode != null) {
                                selectedInstances.add(activeInstanceNode);
                            }
                        }
                    }
                    Collection<? extends DBSObject> instances = instanceContainer.getChildren(new VoidProgressMonitor());
                    List<DBNDatabaseNode> instanceNodes = new ArrayList<>();
                    if (!CommonUtils.isEmpty(instances)) {
                        for (DBSObject instance : instances) {
                            DBNDatabaseNode instanceNode = DBNUtils.getNodeByObject(monitor, instance, false);
                            if (instanceNode != null) {
                                instanceNodes.add(instanceNode);
                            }
                        }
                    }
                    result = instanceNodes;
                    objectList.loadData();
                } catch (DBException e) {
                    throw new InvocationTargetException(e);
                }
            }
        });
        instanceList.createProgressPanel();
        GridData gd = new GridData(GridData.FILL_BOTH);
        gd.heightHint = 300;
        gd.minimumWidth = 300;
        instanceList.setLayoutData(gd);
        instanceList.getSelectionProvider().addSelectionChangedListener(event -> {
            IStructuredSelection selection = (IStructuredSelection) event.getSelection();
            selectedInstances.clear();
            selectedInstances.addAll(selection.toList());
            DBNDatabaseNode instance = selectedInstances.isEmpty() ? null : selectedInstances.get(0);
            if (instance != null && !CommonUtils.equalObjects(instance.getNodeName(), currentInstanceName)) {
                currentInstanceName = instance.getNodeName();
                objectList.loadData();
            }
        });

        instanceList.loadData();
        closeOnFocusLost(instanceList);
    }

    protected List<DBNDatabaseNode> getObjects(DBRProgressMonitor monitor) throws DBException {
        DBSObject rootObject;
        if (selectedInstances != null && currentInstanceName != null) {
            DBNDatabaseNode instanceNode = DBUtils.findObject(selectedInstances, currentInstanceName);
            rootObject = instanceNode == null ? null : instanceNode.getObject();
        } else {
            rootObject = dataSourceContainer.getDataSource();
        }
        if (rootObject instanceof DBSObjectContainer) {
            try {
                Collection<? extends DBSObject> objectList = ((DBSObjectContainer) rootObject).getChildren(monitor);
                if (objectList == null) {
                    return Collections.emptyList();
                }
                List<DBNDatabaseNode> nodeList = new ArrayList<>(objectList.size());
                for (DBSObject object : objectList) {
                    if (object instanceof DBSObjectContainer) {
                        DBNDatabaseNode databaseNode = DBNUtils.getNodeByObject(monitor, object, false);
                        if (databaseNode != null) {
                            nodeList.add(databaseNode);
                        }
                    }
                }
                return nodeList;
            } catch (DBException e) {
                // Do not show error (it will close the dialog)
                log.error(e);
                return Collections.emptyList();
            }
        }
        return objects;
    }

    public String getCurrentInstanceName() {
        DBNDatabaseNode selectedObject = getSelectedObject();
        if (selectedObject.getObject() instanceof DBSCatalog) {
            return selectedObject.getObject().getName();
        }
        return currentInstanceName;
    }
}
