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
package org.jkiss.dbeaver.ext.postgresql.tools.fdw;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.navigator.*;
import org.jkiss.dbeaver.model.struct.DBSEntity;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.ActiveWizardPage;
import org.jkiss.dbeaver.ui.navigator.NavigatorUtils;
import org.jkiss.dbeaver.ui.navigator.database.DatabaseObjectsSelectorPanel;
import org.jkiss.dbeaver.ui.navigator.dialogs.SelectDataSourceDialog;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;


class PostgreFDWConfigWizardPageInput extends ActiveWizardPage<PostgreFDWConfigWizard> {
    private DatabaseObjectsSelectorPanel selectorPanel;
    private boolean activated;

    protected PostgreFDWConfigWizardPageInput(PostgreFDWConfigWizard wizard)
    {
        super("Settings");
        setTitle("Configure foreign data wrappers");
        setDescription("Choose which databases/tables you need to configure");
        setWizard(wizard);
    }

    @Override
    public boolean isPageComplete() {
        return !getWizard().getSelectedEntities().isEmpty();
    }

    @Override
    public void createControl(Composite parent)
    {
        Composite composite = UIUtils.createComposite(parent, 1);

        {
            Group databasesGroup = UIUtils.createControlGroup(composite, "Foreign databases", 1, GridData.FILL_BOTH, 0);

            selectorPanel = new DatabaseObjectsSelectorPanel(
                databasesGroup,
                true,
                getWizard().getRunnableContext()) {

                @Override
                protected boolean isDatabaseObjectVisible(DBSObject obj) {
                    return super.isDatabaseObjectVisible(obj);
                }

                @Override
                protected void onSelectionChange(Object element) {
                    updateState();
                }

                @Override
                protected boolean isFolderVisible(DBNLocalFolder folder) {
                    List<DBPDataSourceContainer> dataSources = getWizard().getAvailableDataSources();
                    for (DBNDataSource ds : folder.getNestedDataSources()) {
                        if (dataSources.contains(ds.getDataSourceContainer())) {
                            return true;
                        }
                    }
                    return false;
                }

                @Override
                protected boolean isDataSourceVisible(DBNDataSource dataSource) {
                    return getWizard().getAvailableDataSources().contains(dataSource.getDataSourceContainer());
                }
            };

            Composite buttonsPanel = UIUtils.createComposite(databasesGroup, 2);
            UIUtils.createDialogButton(buttonsPanel, "Add database", new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    SelectDataSourceDialog dialog = new SelectDataSourceDialog(getShell(), selectorPanel.getProject(), null);
                    if (dialog.open() == IDialogConstants.OK_ID) {
                        DBPDataSourceContainer dataSource = dialog.getDataSource();
                        if (dataSource != null) {
                            getWizard().addAvailableDataSource(dataSource);
                            refreshDataSources();
                        }
                    }
                }
            });

            Button delButton = UIUtils.createDialogButton(buttonsPanel, "Remove database", new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    DBNNode selectedNode = NavigatorUtils.getSelectedNode(selectorPanel.getSelection());
                    if (selectedNode instanceof DBNDatabaseNode) {
                        getWizard().removeAvailableDataSource(((DBNDatabaseNode) selectedNode).getDataSourceContainer());
                        refreshDataSources();
                    }
                }
            });
            delButton.setEnabled(false);
            selectorPanel.addSelectionListener(event -> {
                DBNNode selectedNode = NavigatorUtils.getSelectedNode(event.getSelection());
                delButton.setEnabled(selectedNode instanceof DBNDatabaseNode);
            });
        }

        setControl(composite);
    }

    @Override
    public void activatePage() {
        if (!activated) {
            UIUtils.asyncExec(() -> {
                refreshDataSources();
            });
            activated = true;
        }
        super.activatePage();
    }

    private void refreshDataSources() {
        List<DBNNode> selection = new ArrayList<>();
        try {
            getWizard().getRunnableContext().run(true, true, monitor -> {
                getWizard().collectAvailableDataSources(monitor);
                List<DBSEntity> proposedEntities = getWizard().getProposedEntities();
                for (DBSEntity entity : proposedEntities) {
                    DBNDatabaseNode node = DBNUtils.getNodeByObject(monitor, entity, false);
                    if (node != null) {
                        selection.add(node);
                    }
                }
            });
        } catch (InvocationTargetException e) {
            DBWorkbench.getPlatformUI().showError("Collect data sources", "Error collecting available data sources", e);
        } catch (InterruptedException e) {
            // ignore
        }

        selectorPanel.refreshNodes();
        selectorPanel.checkNodes(selection, true);
        selectorPanel.setSelection(selection);
        updateState();
    }

    protected void updateState()
    {
        List<DBNDatabaseNode> selectedEntities = new ArrayList<>();
        DBPDataSourceContainer dsContainer = null;
        for (DBNNode node : selectorPanel.getCheckedNodes()) {
            if (node instanceof DBNDatabaseNode && ((DBNDatabaseNode) node).getObject() instanceof DBSEntity) {
                DBPDataSourceContainer nodeContainer = ((DBNDatabaseNode) node).getDataSourceContainer();
                if (dsContainer != null && dsContainer != nodeContainer) {
                    selectedEntities.clear();
                    setErrorMessage("You can't select tables from different data sources");
                    break;
                }
                dsContainer = nodeContainer;
                selectedEntities.add((DBNDatabaseNode) node);
            }
        }
        if (!selectedEntities.isEmpty()) {
            setErrorMessage(null);
        }
        getWizard().setSelectedEntities(selectedEntities);
        getContainer().updateButtons();
    }

}
