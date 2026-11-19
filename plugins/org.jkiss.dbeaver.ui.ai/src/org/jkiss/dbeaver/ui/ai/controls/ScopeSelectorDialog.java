/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2026 DBeaver Corp and others
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

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.exec.DBCExecutionContextDefaults;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseNode;
import org.jkiss.dbeaver.model.navigator.DBNModel;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.model.runtime.DBRRunnableContext;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectContainer;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.ai.internal.AIUIActivator;
import org.jkiss.dbeaver.ui.ai.internal.AIUIMessages;
import org.jkiss.dbeaver.ui.dialogs.BaseDialog;
import org.jkiss.dbeaver.ui.internal.UIConnectionMessages;
import org.jkiss.dbeaver.ui.navigator.database.DatabaseObjectSelectionTree;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class ScopeSelectorDialog extends BaseDialog {

    private static final String DIALOG_ID = "DBeaver.AIScopeSelectorDialog.v2";//$NON-NLS-1$

    private final DBRRunnableContext runnableContext;
    private final DBPDataSourceContainer dataSourceContainer;
    private final DBCExecutionContext executionContext;
    private DatabaseObjectSelectionTree selectorTree;
    private List<? extends DBNNode> selectedNodes;

    public ScopeSelectorDialog(
        @NotNull Shell parentShell,
        @NotNull DBRRunnableContext runnableContext,
        @NotNull DBPDataSourceContainer container,
        @NotNull DBCExecutionContext executionContext,
        @NotNull List<? extends DBNNode> selectedNodes
    ) {
        super(parentShell, "Customize database objects accessible to AI", null);
        this.runnableContext = runnableContext;
        this.dataSourceContainer = container;
        this.executionContext = executionContext;
        this.selectedNodes = selectedNodes;
    }

    @Override
    protected IDialogSettings getDialogBoundsSettings() {
        return UIUtils.getSettingsSection(AIUIActivator.getInstance().getDialogSettings(), DIALOG_ID);
    }

    @NotNull
    @Override
    protected Point getInitialSize() {
        Point size = super.getInitialSize();
        size.x = getShell().computeSize(SWT.DEFAULT, SWT.DEFAULT).x;
        return size;
    }

    @NotNull
    @Override
    protected Composite createDialogArea(@NotNull Composite parent) {
        Composite dialogArea = super.createDialogArea(parent);

        DBSObjectContainer root = DBUtils.getAdapter(DBSObjectContainer.class, dataSourceContainer.getDataSource());
        DBCExecutionContextDefaults<?,?> contextDefaults = executionContext.getContextDefaults();

        if (root != null) {
            selectorTree = new DatabaseObjectSelectionTree(dialogArea, runnableContext, root);
            GridData treeData = new GridData(GridData.FILL_BOTH);
            treeData.widthHint = convertWidthInCharsToPixels(60);
            treeData.heightHint = 300;
            selectorTree.setLayoutData(treeData);
            selectorTree.setCheckedObjects(selectedNodes.stream()
                .filter(DBNDatabaseNode.class::isInstance)
                .map(DBNDatabaseNode.class::cast)
                .map(DBNDatabaseNode::getObject)
                .filter(Objects::nonNull)
                .toList());
            Button showSystemObjects = UIUtils.createCheckbox(
                dialogArea, UIConnectionMessages.dialog_connection_wizard_final_checkbox_show_system_objects,
                selectorTree.isShowSystemObjects());
            showSystemObjects.addSelectionListener(SelectionListener.widgetSelectedAdapter(
                event -> selectorTree.setShowSystemObjects(showSystemObjects.getSelection())));
            try {
                if (contextDefaults != null) {
                    DBSObject defaultObject = contextDefaults.getDefaultSchema();
                    if (defaultObject == null) {
                        defaultObject = contextDefaults.getDefaultCatalog();
                    }
                    if (defaultObject != null) {
                        selectorTree.expandObject(defaultObject);
                    }
                }
            } catch (InvocationTargetException e) {
                DBWorkbench.getPlatformUI().showError(AIUIMessages.scope_selector_load_error, null, e.getTargetException());
            } catch (InterruptedException e) {
                // Keep the current selection if loading was cancelled.
            }
            selectorTree.addCheckListener(this::updateOkButton);
        }

        UIUtils.createInfoLabel(dialogArea, "Customize database metadata accessible to AI.");

        return dialogArea;
    }

    @Override
    protected void createButtonsForButtonBar(@NotNull Composite parent) {
        super.createButtonsForButtonBar(parent);
        updateOkButton();
    }

    private void updateOkButton() {
        Button okButton = getButton(IDialogConstants.OK_ID);
        if (okButton != null) {
            okButton.setEnabled(selectorTree != null && selectorTree.hasCheckedObjects());
        }
    }

    @Override
    protected void okPressed() {
        Set<DBNNode> resolvedNodes = new LinkedHashSet<>();
        List<DBSObject> checkedObjects = selectorTree.getCheckedObjects();
        DBNModel navigatorModel = dataSourceContainer.getProject().getNavigatorModel();
        try {
            runnableContext.run(true, true, monitor -> {
                monitor.beginTask(AIUIMessages.scope_selector_loading, checkedObjects.size());
                try {
                    for (DBSObject object : checkedObjects) {
                        if (monitor.isCanceled()) {
                            throw new InterruptedException();
                        }
                        DBNDatabaseNode node = navigatorModel.getNodeByObject(monitor, object, true);
                        if (node == null) {
                            throw new InvocationTargetException(new DBException("Cannot resolve selected object " + object.getName()));
                        }
                        resolvedNodes.add(node);
                        monitor.worked(1);
                    }
                    if (monitor.isCanceled()) {
                        throw new InterruptedException();
                    }
                } finally {
                    monitor.done();
                }
            });
        } catch (InvocationTargetException e) {
            DBWorkbench.getPlatformUI().showError(AIUIMessages.scope_selector_load_error, null, e.getTargetException());
            return;
        } catch (InterruptedException e) {
            return;
        }
        if (resolvedNodes.isEmpty()) {
            MessageDialog.openInformation(getShell(), AIUIMessages.scope_selector_customize, AIUIMessages.scope_selector_empty);
            return;
        }
        selectedNodes = new ArrayList<>(resolvedNodes);
        super.okPressed();
    }

    public List<? extends DBNNode> getSelectedNodes() {
        return selectedNodes;
    }
}
