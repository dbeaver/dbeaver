/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2025 DBeaver Corp and others
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
package org.jkiss.dbeaver.ui.ai.controls;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseFolder;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseNode;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.model.runtime.DBRRunnableContext;
import org.jkiss.dbeaver.model.struct.DBSEntity;
import org.jkiss.dbeaver.model.struct.DBSEntityContainer;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.ai.internal.AIUIActivator;
import org.jkiss.dbeaver.ui.dialogs.BaseDialog;
import org.jkiss.dbeaver.ui.navigator.database.DatabaseObjectsSelectorPanel;

import java.util.List;

public class ScopeSelectorDialog extends BaseDialog {

    private static final String DIALOG_ID = "DBeaver.AIScopeSelectorDialog";//$NON-NLS-1$

    private final DBRRunnableContext runnableContext;
    private final DBPDataSourceContainer dataSourceContainer;
    private DatabaseObjectsSelectorPanel selectorPanel;
    private List<? extends DBNNode> selectedNodes;

    public ScopeSelectorDialog(
        @NotNull Shell parentShell,
        @NotNull DBRRunnableContext runnableContext,
        @NotNull DBPDataSourceContainer container,
        @NotNull List<? extends DBNNode> selectedNodes
    ) {
        super(parentShell, "Select objects to include in completion scope", null);
        this.runnableContext = runnableContext;
        this.dataSourceContainer = container;
        this.selectedNodes = selectedNodes;
    }

    @Override
    protected IDialogSettings getDialogBoundsSettings() {
        return UIUtils.getSettingsSection(AIUIActivator.getDefault().getDialogSettings(), DIALOG_ID);
    }

    @Override
    protected Composite createDialogArea(Composite parent) {
        Composite dialogArea = super.createDialogArea(parent);

        selectorPanel = new DatabaseObjectsSelectorPanel(
            dialogArea,
            true,
            this.runnableContext
        ) {
            @Override
            protected DBPProject getSelectedProject() {
                return dataSourceContainer.getProject();
            }

            @Override
            protected DBNNode getRootNode() {
                return getProject().getNavigatorModel().getNodeByObject(dataSourceContainer);
            }

            @Override
            protected boolean isDatabaseFolderVisible(DBNDatabaseFolder folder) {
                Class<? extends DBSObject> childrenClass = folder.getChildrenClass();
                if (childrenClass == null) {
                    return false;
                }
                return DBSEntity.class.isAssignableFrom(childrenClass) ||
                    DBSEntityContainer.class.isAssignableFrom(childrenClass);
            }

            @Override
            protected boolean isDatabaseObjectVisible(DBSObject obj) {
                return DBSEntity.class.isInstance(obj);
            }

        };
        selectorPanel.getNavigatorTree().getViewer().expandToLevel(2);
        selectorPanel.checkNodes(selectedNodes, true);
        selectorPanel.setSelection(selectedNodes);

        return dialogArea;
    }

    @Override
    protected void okPressed() {
        selectedNodes = selectorPanel.getCheckedNodes();
        selectedNodes.removeIf(n -> {
                if (n instanceof DBNDatabaseNode dbn) {
                    if (dbn.getObject() instanceof DBSEntity || dbn.getObject() instanceof DBSEntityContainer) {
                        return false;
                    }
                }
                return true;
            });
        selectedNodes.removeIf(n -> selectedNodes.contains(n.getParentNode()));

        super.okPressed();
    }

    public List<? extends DBNNode> getSelectedNodes() {
        return selectedNodes;
    }
}
