/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2019 Serge Rider (serge@jkiss.org)
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
package org.jkiss.dbeaver.ui.controls;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.DBIcon;
import org.jkiss.dbeaver.model.DBPImage;
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseNode;
import org.jkiss.dbeaver.model.navigator.DBNModel;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.model.navigator.DBNProject;
import org.jkiss.dbeaver.model.struct.DBSObjectContainer;
import org.jkiss.dbeaver.model.struct.rdb.DBSSchema;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UIUtils;

/**
 * ObjectContainerSelectorPanel
 */
public abstract class ObjectContainerSelectorPanel extends Composite
{

    private final Label containerIcon;
    private final Text containerName;

    protected ObjectContainerSelectorPanel(Composite parent, String containerTitle) {
        super(parent, SWT.NONE);

        GridLayout layout = new GridLayout(4, false);
        layout.marginHeight = 0;
        layout.marginWidth = 0;
        this.setLayout(layout);
        this.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        UIUtils.createControlLabel(this, containerTitle);

        containerIcon = new Label(this, SWT.NONE);
        containerIcon.setImage(DBeaverIcons.getImage(DBIcon.TYPE_UNKNOWN));

        containerName = new Text(this, SWT.BORDER | SWT.READ_ONLY);
        containerName.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        containerName.setText("");

        Button browseButton = new Button(this, SWT.PUSH);
        browseButton.setImage(DBeaverIcons.getImage(DBIcon.TREE_FOLDER));
        browseButton.setText("...");
        browseButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                DBPProject activeProject = DBWorkbench.getPlatform().getWorkspace().getActiveProject();
                if (activeProject != null) {
                    final DBNModel navigatorModel = DBWorkbench.getPlatform().getNavigatorModel();
                    final DBNProject rootNode = navigatorModel.getRoot().getProjectNode(activeProject);
                    DBNNode selectedNode = getSelectedNode();
                    DBNNode node = DBWorkbench.getPlatformUI().selectObject(
                        getShell(),
                        containerTitle,
                        rootNode.getDatabases(),
                        selectedNode,
                        new Class[] {DBSObjectContainer.class},
                        null, new Class[] { DBSSchema.class });
                    if (node instanceof DBNDatabaseNode) {
                        setSelectedNode((DBNDatabaseNode) node);
                    }
                }
            }
        });
    }

    public void setContainerInfo(DBPImage image, String name) {
        containerIcon.setImage(DBeaverIcons.getImage(image));
        containerName.setText(name);
    }

    protected abstract void setSelectedNode(DBNDatabaseNode node);

    @Nullable
    protected abstract DBNNode getSelectedNode();
}