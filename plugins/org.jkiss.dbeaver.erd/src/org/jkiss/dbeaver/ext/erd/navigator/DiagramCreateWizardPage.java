/*
 * Copyright (C) 2010-2015 Serge Rieder serge@jkiss.org
 * Copyright (C) 2011-2012 Eugene Fradkin eugene.fradkin@gmail.com
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package org.jkiss.dbeaver.ext.erd.navigator;

import org.eclipse.jface.viewers.CheckboxTreeViewer;
import org.eclipse.jface.viewers.ICheckStateProvider;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.ext.erd.ERDMessages;
import org.jkiss.dbeaver.ext.erd.model.EntityDiagram;
import org.jkiss.dbeaver.model.navigator.DBNDataSource;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseNode;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.model.navigator.DBNProject;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.rdb.DBSTable;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.views.navigator.database.DatabaseNavigatorTree;
import org.jkiss.utils.CommonUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;


class DiagramCreateWizardPage extends WizardPage {

    private EntityDiagram diagram;
    private DatabaseNavigatorTree contentTree;

    protected DiagramCreateWizardPage(EntityDiagram diagram)
    {
        super(ERDMessages.wizard_page_diagram_create_name);
        this.diagram = diagram;

        setTitle(ERDMessages.wizard_page_diagram_create_title);
        setDescription(ERDMessages.wizard_page_diagram_create_description);
    }

    @Override
    public boolean isPageComplete()
    {
    	if (getErrorMessage() != null) {
			return false;
		}
        return !CommonUtils.isEmpty(diagram.getName());
    }

    @Override
    public void createControl(Composite parent)
    {
        Composite placeholder = UIUtils.createPlaceholder(parent, 1);
        Composite configGroup = UIUtils.createControlGroup(placeholder, ERDMessages.wizard_page_diagram_create_group_settings, 2, GridData.FILL_BOTH, 0);

        final Text projectNameText = UIUtils.createLabelText(configGroup, "Name", null); //$NON-NLS-1$
        projectNameText.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent e)
            {
                diagram.setName(projectNameText.getText());
                updateState();
            }
        });

        Label contentLabel = UIUtils.createControlLabel(configGroup, ERDMessages.wizard_page_diagram_create_label_init_content);
        GridData gd = new GridData(GridData.BEGINNING);
        gd.horizontalSpan = 2;
        contentLabel.setLayoutData(gd);

        final DBNProject rootNode = DBeaverCore.getInstance().getNavigatorModel().getRoot().getProject(DBeaverCore.getInstance().getProjectRegistry().getActiveProject());
        if (rootNode == null) {
            setControl(placeholder);
			return;
		}
        contentTree = new DatabaseNavigatorTree(configGroup, rootNode.getDatabases(), SWT.SINGLE | SWT.CHECK);
        gd = new GridData(GridData.FILL_BOTH);
        gd.horizontalSpan = 2;
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
        List<DBNNode> nodes = new ArrayList<DBNNode>();
        CheckboxTreeViewer viewer = (CheckboxTreeViewer) contentTree.getViewer();
        for (Object obj : viewer.getCheckedElements()) {
            DBNNode node = (DBNNode)obj;
            nodes.add(node);
        }
        return nodes;
    }
}
