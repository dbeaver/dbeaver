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
package org.jkiss.dbeaver.ui.navigator.dialogs;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Combo;
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
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectContainer;
import org.jkiss.dbeaver.model.struct.rdb.DBSCatalog;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.internal.UINavigatorMessages;
import org.jkiss.utils.CommonUtils;

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
    private Collection<? extends DBSObject> instanceObjects;

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

                // Create instance selector
                Composite instancePanel = UIUtils.createComposite(dialogArea, 3);
                instancePanel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
                UIUtils.createLabel(instancePanel, dataSourceContainer.getDriver().getIcon());
                Combo instanceCombo = UIUtils.createLabelCombo(instancePanel, UINavigatorMessages.label_instance, UINavigatorMessages.label_active_service_instance, SWT.DROP_DOWN | SWT.READ_ONLY);
                instanceCombo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
//                Label databaseTermLabel = UIUtils.createControlLabel(instancePanel, dataSource.getInfo().getSchemaTerm());
//                GridData gd = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
//                gd.horizontalSpan = 3;
//                databaseTermLabel.setLayoutData(gd);

                try {
                    instanceObjects = instanceContainer.getChildren(new VoidProgressMonitor());
                    if (instanceObjects != null) {
                        for (DBSObject object : instanceObjects) {
                            instanceCombo.add(object.getName());
                        }
                        instanceCombo.setText(currentInstanceName);
                    }
                } catch (DBException e) {
                    log.error(UINavigatorMessages.label_error_list, e);
                }

                instanceCombo.addModifyListener(e -> {
                    String instanceName = instanceCombo.getText();
                    if (!CommonUtils.equalObjects(instanceName, currentInstanceName)) {
                        currentInstanceName = instanceName;
                        objectList.loadData();
                    }
                });

                closeOnFocusLost(instanceCombo);
            }
        }
    }

    protected Collection<DBNDatabaseNode> getObjects(DBRProgressMonitor monitor) throws DBException {
        DBSObject rootObject;
        if (instanceObjects != null && currentInstanceName != null) {
            rootObject = DBUtils.findObject(instanceObjects, currentInstanceName);
        } else {
            rootObject = dataSourceContainer.getDataSource();
        }
        if (rootObject instanceof DBSObjectContainer) {
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
