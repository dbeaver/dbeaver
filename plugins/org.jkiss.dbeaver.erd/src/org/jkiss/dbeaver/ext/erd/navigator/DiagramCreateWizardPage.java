/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.erd.navigator;

import org.jkiss.dbeaver.ext.erd.ERDMessages;
import org.jkiss.utils.CommonUtils;
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
import org.jkiss.dbeaver.ext.erd.model.EntityDiagram;
import org.jkiss.dbeaver.model.navigator.DBNDataSource;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseNode;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.model.navigator.DBNProject;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSTable;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.views.navigator.database.DatabaseNavigatorTree;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;


class DiagramCreateWizardPage extends WizardPage {

    private EntityDiagram diagram;
    private DatabaseNavigatorTree contentTree;

    protected DiagramCreateWizardPage(EntityDiagram diagram)
    {
        super(ERDMessages.create_new_diagram);
        this.diagram = diagram;

        setTitle(ERDMessages.create_new_diagram);
        setDescription(ERDMessages.manage_diagram_content);
    }

    @Override
    public boolean isPageComplete()
    {
        return !CommonUtils.isEmpty(diagram.getName());
    }

    public void createControl(Composite parent)
    {
        Composite placeholder = UIUtils.createPlaceholder(parent, 1);
        Composite configGroup = UIUtils.createControlGroup(placeholder, ERDMessages.settings, 2, GridData.FILL_BOTH, 0);

        final Text projectNameText = UIUtils.createLabelText(configGroup, ERDMessages.name, "");
        projectNameText.addModifyListener(new ModifyListener() {
            public void modifyText(ModifyEvent e)
            {
                diagram.setName(projectNameText.getText());
                updateState();
            }
        });

        Label contentLabel = UIUtils.createControlLabel(configGroup, ERDMessages.initial_content_optional);
        GridData gd = new GridData(GridData.BEGINNING);
        gd.horizontalSpan = 2;
        contentLabel.setLayoutData(gd);

        final DBNProject rootNode = DBeaverCore.getInstance().getNavigatorModel().getRoot().getProject(DBeaverCore.getInstance().getProjectRegistry().getActiveProject());
        contentTree = new DatabaseNavigatorTree(configGroup, rootNode.getDatabases(), SWT.SINGLE | SWT.CHECK);
        gd = new GridData(GridData.FILL_BOTH);
        gd.horizontalSpan = 2;
        contentTree.setLayoutData(gd);

        CheckboxTreeViewer viewer = (CheckboxTreeViewer) contentTree.getViewer();
        viewer.setCheckStateProvider(new ICheckStateProvider() {
            public boolean isChecked(Object element)
            {
                return false;
            }

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
        List<DBNNode> nodes = new ArrayList<DBNNode>();
        CheckboxTreeViewer viewer = (CheckboxTreeViewer) contentTree.getViewer();
        for (Object obj : viewer.getCheckedElements()) {
            DBNNode node = (DBNNode)obj;
            nodes.add(node);
        }
        return nodes;
    }
}
