/*
 * Copyright (C) 2010-2012 Serge Rieder
 * serge@jkiss.org
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
package org.jkiss.dbeaver.tools.transfer.database;

import org.eclipse.core.resources.IProject;
import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.model.navigator.DBNProject;
import org.jkiss.dbeaver.model.struct.DBSObjectContainer;
import org.jkiss.dbeaver.model.struct.DBSWrapper;
import org.jkiss.dbeaver.tools.transfer.wizard.DataTransferWizard;
import org.jkiss.dbeaver.ui.DBIcon;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.ActiveWizardPage;
import org.jkiss.dbeaver.ui.dialogs.BrowseObjectDialog;

public class DatabaseConsumerPageMapping extends ActiveWizardPage<DataTransferWizard> {

    private TableViewer mappingViewer;
    private DBNNode containerNode;

    public DatabaseConsumerPageMapping() {
        super("Entities mapping");
        setTitle("Entities mapping");
        setDescription("Map entities transfer");
        setPageComplete(false);
    }

    @Override
    public void createControl(Composite parent) {
        initializeDialogUnits(parent);

        Composite composite = new Composite(parent, SWT.NULL);
        GridLayout gl = new GridLayout();
        gl.marginHeight = 0;
        gl.marginWidth = 0;
        composite.setLayout(gl);
        composite.setLayoutData(new GridData(GridData.FILL_BOTH));

        {
            // Target container
            Composite containerPanel = new Composite(composite, SWT.NONE);
            containerPanel.setLayout(new GridLayout(4, false));
            containerPanel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
            UIUtils.createControlLabel(containerPanel, "Target container");

            final Label containerIcon = new Label(containerPanel, SWT.NONE);
            containerIcon.setImage(DBIcon.TYPE_UNKNOWN.getImage());

            final Text containerName = new Text(containerPanel, SWT.BORDER | SWT.READ_ONLY);
            containerName.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

            Button browseButton = new Button(containerPanel, SWT.PUSH);
            browseButton.setImage(DBIcon.TREE_FOLDER.getImage());
            browseButton.setText("...");
            browseButton.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e)
                {
                    IProject activeProject = DBeaverCore.getInstance().getProjectRegistry().getActiveProject();
                    if (activeProject != null) {
                        final DBNProject rootNode = DBeaverCore.getInstance().getNavigatorModel().getRoot().getProject(
                            activeProject);
                        DBNNode node = BrowseObjectDialog.selectObject(
                            getShell(),
                            "Choose container",
                            rootNode.getDatabases(),
                            containerNode,
                            DBSObjectContainer.class);
                        if (node != null) {
                            containerNode = node;
                            containerIcon.setImage(containerNode.getNodeIconDefault());
                            containerName.setText(containerNode.getNodeFullName());
                        }
                    }
                }
            });
        }

        {
            // Mapping table
            mappingViewer = new TableViewer(composite, SWT.BORDER | SWT.MULTI);
            mappingViewer.getTable().setLayoutData(new GridData(GridData.FILL_BOTH));
            mappingViewer.getTable().setLinesVisible(true);
            mappingViewer.getTable().setHeaderVisible(true);

            TableViewerColumn columnSource = new TableViewerColumn(mappingViewer, SWT.LEFT);
            columnSource.setLabelProvider(new CellLabelProvider() {
                @Override
                public void update(ViewerCell cell)
                {

                }
            });
            columnSource.getColumn().setText("Source");

            TableViewerColumn columnTarget = new TableViewerColumn(mappingViewer, SWT.LEFT);
            columnTarget.setLabelProvider(new CellLabelProvider() {
                @Override
                public void update(ViewerCell cell)
                {

                }
            });
            columnTarget.getColumn().setText("Target");

            TableViewerColumn columnType = new TableViewerColumn(mappingViewer, SWT.LEFT);
            columnType.setLabelProvider(new CellLabelProvider() {
                @Override
                public void update(ViewerCell cell)
                {

                }
            });
            columnType.getColumn().setText("Type");
        }

        {
            // Control buttons
            Composite buttonsPanel = new Composite(composite, SWT.NONE);
            buttonsPanel.setLayout(new GridLayout(3, false));
            buttonsPanel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

            Button mapTableButton = new Button(buttonsPanel, SWT.PUSH);
            mapTableButton.setImage(DBIcon.TREE_TABLE.getImage());
            mapTableButton.setText("Existing table ...");

            Button createNewButton = new Button(buttonsPanel, SWT.PUSH);
            createNewButton.setImage(DBIcon.TREE_VIEW.getImage());
            createNewButton.setText("Create new ...");

            Button columnsButton = new Button(buttonsPanel, SWT.PUSH);
            columnsButton.setImage(DBIcon.TREE_COLUMNS.getImage());
            columnsButton.setText("Columns' mappings ...");
        }

        final DatabaseConsumerSettings settings = getWizard().getPageSettings(this, DatabaseConsumerSettings.class);

        setControl(composite);

    }

    @Override
    public void activatePage()
    {
        final DatabaseConsumerSettings settings = getWizard().getPageSettings(this, DatabaseConsumerSettings.class);

        UIUtils.packColumns(mappingViewer.getTable(), true);

        updatePageCompletion();
    }

    @Override
    protected boolean determinePageCompletion()
    {
        return true;
    }

}