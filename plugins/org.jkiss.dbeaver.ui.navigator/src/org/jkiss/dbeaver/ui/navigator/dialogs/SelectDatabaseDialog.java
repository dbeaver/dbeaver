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
package org.jkiss.dbeaver.ui.navigator.dialogs;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
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
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectContainer;
import org.jkiss.dbeaver.model.struct.rdb.DBSCatalog;
import org.jkiss.dbeaver.model.struct.rdb.DBSSchema;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.ActionUtils;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.internal.UIMessages;
import org.jkiss.dbeaver.ui.navigator.NavigatorPreferences;
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
public class SelectDatabaseDialog extends ObjectListDialog<DBNDatabaseNode>
{
    private static final Log log = Log.getLog(SelectDatabaseDialog.class);
    private static final String CMD_ACTIVE_DATASOURCE = "org.jkiss.dbeaver.ui.tools.select.connection";

    private final DBPDataSourceContainer dataSourceContainer;
    private volatile String currentInstanceName;

    private DatabaseObjectListControl<DBNDatabaseNode> instanceList;
    private final List<DBNDatabaseNode> selectedInstances = new ArrayList<>();

    public SelectDatabaseDialog(
        Shell parentShell,
        DBPDataSourceContainer dataSourceContainer,
        String currentInstanceName,
        Collection<DBNDatabaseNode> objects,
        Collection<DBNDatabaseNode> selected)
    {
        super(parentShell,
            NLS.bind(
                UIMessages.label_choose,
                UIUtils.getCatalogSchemaTerms(dataSourceContainer, true)),
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
        if (currentInstanceName == null || dataSource == null) {
            return;
        }
        DBCExecutionContextDefaults<?,?> contextDefaults = getContextDefaults();
        if (contextDefaults != null && contextDefaults.supportsCatalogChange() && contextDefaults.supportsSchemaChange()) {
            DBSObjectContainer instanceContainer = DBUtils.getAdapter(DBSObjectContainer.class, dataSource);
            if (instanceContainer == null) {
                UIUtils.showMessageBox(getShell(), "No database objects were found", "No database objects were found. Please set active datasource (" +
                    ActionUtils.findCommandDescription(CMD_ACTIVE_DATASOURCE, UIUtils.getActiveWorkbenchWindow(), true) +
                    ") for this editor.", SWT.ICON_ERROR);
                return;
            }
            createInstanceSelector(dialogArea, instanceContainer);
        }
    }

    @Nullable
    private DBCExecutionContextDefaults<?,?> getContextDefaults() {
        DBPDataSource dataSource = dataSourceContainer.getDataSource();
        DBSObjectContainer instanceContainer = DBUtils.getAdapter(DBSObjectContainer.class, dataSource);
        DBCExecutionContext defaultContext = DBUtils.getDefaultContext(instanceContainer, true);
        if (defaultContext == null) {
            return null;
        }
        return defaultContext.getContextDefaults();
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
                    Collection<? extends DBSObject> instances = instanceContainer.getChildren(monitor);
                    result = getNodeList(monitor, instances);
                    objectList.loadData();
                } catch (DBException e) {
                    throw new InvocationTargetException(e);
                }
            }
        });
        instanceList.createProgressPanel();
        GridData gd = new GridData(GridData.FILL_BOTH);
        gd.heightHint = 300;
        gd.minimumWidth = 500;
        instanceList.setLayoutData(gd);
        instanceList.getSelectionProvider().addSelectionChangedListener(event -> {
            IStructuredSelection selection = (IStructuredSelection) event.getSelection();
            selectedInstances.clear();
            selectedInstances.addAll(selection.toList());
            DBNDatabaseNode instance = selectedInstances.isEmpty() ? null : selectedInstances.get(0);
            if (instance != null && !CommonUtils.equalObjects(instance.getNodeDisplayName(), currentInstanceName)) {
                currentInstanceName = instance.getNodeDisplayName();
                objectList.loadData();
            }
        });

        instanceList.loadData();
        closeOnFocusLost(instanceList.getItemsViewer().getControl());
    }

    protected List<DBNDatabaseNode> getObjects(DBRProgressMonitor monitor) throws DBException {
        DBSObject rootObject;
        if (selectedInstances != null && currentInstanceName != null && getContextDefaults() != null
            && getContextDefaults().supportsSchemaChange()) {
            DBNDatabaseNode instanceNode = DBUtils.findObject(selectedInstances, currentInstanceName);
            rootObject = instanceNode == null ? null : instanceNode.getObject();
        } else {
            rootObject = dataSourceContainer.getDataSource();
        }
        if (rootObject instanceof DBSObjectContainer) {
            try {
                DBCExecutionContextDefaults<?,?> contextDefaults = getContextDefaults();
                Collection<? extends DBSObject> objectsCollection;
                if (rootObject instanceof DBSCatalog && contextDefaults != null && !contextDefaults.supportsSchemaChange()) {
                    DBSSchema schema = contextDefaults.getDefaultSchema();
                    objectsCollection = Collections.singletonList(schema);
                } else {
                    objectsCollection = ((DBSObjectContainer) rootObject).getChildren(monitor);
                }
                return getNodeList(monitor, objectsCollection);
            } catch (DBException e) {
                // Do not show error (it will close the dialog)
                log.error(e);
                return Collections.emptyList();
            }
        }
        return objects;
    }

    @NotNull
    private static List<DBNDatabaseNode> getNodeList(@NotNull DBRProgressMonitor monitor, @Nullable Collection<? extends DBSObject> objectList) {
        if (CommonUtils.isEmpty(objectList)) {
            return Collections.emptyList();
        }
        int nodesLimit = DBWorkbench.getPlatform().getPreferenceStore().getInt(NavigatorPreferences.NAVIGATOR_LONG_LIST_FETCH_SIZE);

        List<DBNDatabaseNode> nodeList = new ArrayList<>(objectList.size());
        for (DBSObject object : objectList) {
            if (object instanceof DBSObjectContainer) {
                DBNDatabaseNode databaseNode = DBNUtils.getNodeByObject(monitor, object, false);
                if (databaseNode != null) {
                    nodeList.add(databaseNode);
                    if (nodeList.size() >= nodesLimit) {
                        break;
                    }
                }
            }
        }
        return nodeList;
    }

    public String getCurrentInstanceName() {
        DBNDatabaseNode selectedObject = getSelectedObject();
        if (selectedObject.getObject() instanceof DBSCatalog) {
            return selectedObject.getObject().getName();
        }
        return currentInstanceName;
    }
}
