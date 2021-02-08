/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2021 DBeaver Corp and others
 * Copyright (C) 2011-2012 Eugene Fradkin (eugene.fradkin@gmail.com)
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
package org.jkiss.dbeaver.erd.ui.navigator;

import org.eclipse.jface.viewers.CheckboxTreeViewer;
import org.eclipse.jface.viewers.ICheckStateProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.jkiss.dbeaver.erd.ui.internal.ERDUIMessages;
import org.jkiss.dbeaver.erd.ui.model.EntityDiagram;
import org.jkiss.dbeaver.model.navigator.DBNDataSource;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseNode;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.model.navigator.DBNProject;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.rdb.DBSTable;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.navigator.database.DatabaseNavigatorTree;
import org.jkiss.utils.CommonUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;


class DiagramCreateWizardPage extends WizardPage {

    private EntityDiagram diagram;
    private DatabaseNavigatorTree contentTree;
    private IStructuredSelection entitySelection;

    protected DiagramCreateWizardPage(EntityDiagram diagram, IStructuredSelection entitySelection)
    {
        super(ERDUIMessages.wizard_page_diagram_create_name);
        this.diagram = diagram;
        this.entitySelection = entitySelection;

        setTitle(ERDUIMessages.wizard_page_diagram_create_title);
        setDescription(ERDUIMessages.wizard_page_diagram_create_description);
    }

    @Override
    public boolean isPageComplete()
    {
        boolean hasName = !CommonUtils.isEmpty(diagram.getName());
        if (!hasName) {
            setErrorMessage("Set diagram name");
        } else {
            setErrorMessage(null);
        }
    	if (getErrorMessage() != null) {
			return false;
		}
        return hasName;
    }

    @Override
    public void createControl(Composite parent)
    {
        Composite placeholder = UIUtils.createPlaceholder(parent, 1);
        Composite configGroup = UIUtils.createControlGroup(placeholder, ERDUIMessages.wizard_page_diagram_create_group_settings, 2, GridData.FILL_BOTH, 0);

        final Text projectNameText = UIUtils.createLabelText(configGroup, "Name", null); //$NON-NLS-1$
        projectNameText.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent e)
            {
                diagram.setName(projectNameText.getText());
                updateState();
            }
        });

        Label contentLabel = UIUtils.createControlLabel(configGroup, ERDUIMessages.wizard_page_diagram_create_label_init_content);
        GridData gd = new GridData(GridData.BEGINNING);
        gd.horizontalSpan = 2;
        contentLabel.setLayoutData(gd);

        final DBNProject rootNode = DBWorkbench.getPlatform().getNavigatorModel().getRoot().getProjectNode(DBWorkbench.getPlatform().getWorkspace().getActiveProject());
        if (rootNode == null) {
            setControl(placeholder);
			return;
		}
        contentTree = new DatabaseNavigatorTree(configGroup, rootNode.getDatabases(), SWT.SINGLE | SWT.CHECK);
        gd = new GridData(GridData.FILL_BOTH);
        gd.horizontalSpan = 2;
        gd.heightHint = 400;
        contentTree.setLayoutData(gd);

        CheckboxTreeViewer viewer = (CheckboxTreeViewer) contentTree.getViewer();
        viewer.setCheckStateProvider(new ICheckStateProvider() {
            @Override
            public boolean isChecked(Object element)
            {
                return false;
            }

            @Override
            public boolean isGrayed(Object element)
            {
                if (element instanceof DBNDatabaseNode && !(element instanceof DBNDataSource)) {
                    DBSObject object = ((DBNDatabaseNode) element).getObject();
                    if (object instanceof DBSTable) {
                        return false;
                    }
                }
                return true;
            }
        });

        if (entitySelection != null) {
            viewer.setSelection(entitySelection, true);
            viewer.setCheckedElements(entitySelection.toArray());
        }

        setControl(placeholder);
    }

    private void updateState()
    {
        getContainer().updateButtons();
    }

    Collection<DBNNode> getInitialContent()
    {
    	if (contentTree == null) {
			return Collections.emptyList();
		}
        List<DBNNode> nodes = new ArrayList<>();
        for (Object obj : contentTree.getCheckboxViewer().getCheckedElements()) {
            DBNNode node = (DBNNode)obj;
            nodes.add(node);
        }
        return nodes;
    }
}
