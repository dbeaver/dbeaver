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
package org.jkiss.dbeaver.tasks.ui.sql.script;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.messages.ModelMessages;
import org.jkiss.dbeaver.model.navigator.DBNLocalFolder;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.model.navigator.DBNProject;
import org.jkiss.dbeaver.model.navigator.DBNResource;
import org.jkiss.dbeaver.tools.transfer.internal.DTMessages;
import org.jkiss.dbeaver.tools.transfer.ui.internal.DTUIMessages;
import org.jkiss.dbeaver.ui.controls.ViewerColumnController;
import org.jkiss.dbeaver.ui.dialogs.BaseDialog;
import org.jkiss.dbeaver.ui.navigator.INavigatorFilter;
import org.jkiss.dbeaver.ui.navigator.database.DatabaseNavigatorTree;
import org.jkiss.dbeaver.ui.navigator.database.DatabaseNavigatorTreeFilter;
import org.jkiss.dbeaver.ui.navigator.database.load.TreeNodeSpecial;
import org.jkiss.utils.CommonUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

class SQLScriptTaskScriptSelectorDialog extends BaseDialog {

    private final DBNProject projectNode;
    private DatabaseNavigatorTree scriptsTree;
    private final List<DBNNode> selectedScripts = new ArrayList<>();

    SQLScriptTaskScriptSelectorDialog(Shell parentShell, DBNProject projectNode) {
        super(parentShell, DTMessages.sql_script_task_page_settings_group_files, null);
        this.projectNode = projectNode;
    }

    @Override
    protected Composite createDialogArea(Composite parent) {
        Composite dialogArea = super.createDialogArea(parent);

        INavigatorFilter scriptFilter = new DatabaseNavigatorTreeFilter() {
            @Override
            public boolean filterFolders() {
                return true;
            }

            @Override
            public boolean isLeafObject(Object object) {
                return object instanceof DBNResource && ((DBNResource) object).getResource() instanceof IFile;
            }

            @Override
            public boolean select(Object element) {
                return element instanceof DBNLocalFolder || element instanceof DBNResource || element instanceof TreeNodeSpecial;
            }

            @Override
            public boolean filterObjectByPattern(Object object) {
                return object instanceof DBNResource && ((DBNResource) object).getResource() instanceof IFile;
            }
        };

        scriptsTree = new DatabaseNavigatorTree(
            dialogArea,
            projectNode,
            SWT.SINGLE | SWT.BORDER | SWT.CHECK,
            false,
            scriptFilter);
        GridData gd = new GridData(GridData.FILL_BOTH);
        gd.heightHint = 300;
        gd.widthHint = 400;
        scriptsTree.setLayoutData(gd);
        scriptsTree.getViewer().addFilter(new ViewerFilter() {
            @Override
            public boolean select(Viewer viewer, Object parentElement, Object element) {
                if (element instanceof TreeNodeSpecial) {
                    return true;
                }
                if (element instanceof DBNResource) {
                    return isResourceApplicable((DBNResource) element);
                }
                return false;
            }
        });
        scriptsTree.getViewer().addSelectionChangedListener(event -> updateSelectedScripts());
        scriptsTree.getViewer().expandToLevel(2);
        scriptsTree.getViewer().getTree().setHeaderVisible(true);
        createScriptColumns(scriptsTree.getViewer());

        return dialogArea;
    }

    private void updateSelectedScripts() {
        selectedScripts.clear();
        for (Object element : scriptsTree.getCheckboxViewer().getCheckedElements()) {
            if (element instanceof DBNResource dbnResource && dbnResource.getResource() instanceof IFile) {
                selectedScripts.add(dbnResource);
            }
        }
        enableButton(IDialogConstants.OK_ID, !selectedScripts.isEmpty());
    }

    public List<DBNNode> getSelectedScripts() {
        return selectedScripts;
    }

    @Override
    protected void createButtonsForButtonBar(Composite parent) {
        super.createButtonsForButtonBar(parent);
        enableButton(IDialogConstants.OK_ID, false);
    }

    static void createScriptColumns(ColumnViewer viewer) {
        final ILabelProvider mainLabelProvider = (ILabelProvider) viewer.getLabelProvider();
        ViewerColumnController<?,?> columnController = new ViewerColumnController<>("sqlTaskScriptViewer", viewer);
        columnController.setForceAutoSize(true);
        columnController.addColumn(ModelMessages.model_navigator_Name, DTUIMessages.sql_script_task_selector_dialog_column_description_script, SWT.LEFT, true, true, new ColumnLabelProvider() {
            @Override
            public String getText(Object element) {
                return mainLabelProvider.getText(element);
            }
            @Override
            public Image getImage(Object element) {
                return mainLabelProvider.getImage(element);
            }
            @Override
            public String getToolTipText(Object element) {
                if (mainLabelProvider instanceof IToolTipProvider) {
                    return ((IToolTipProvider) mainLabelProvider).getToolTipText(element);
                }
                return null;
            }
        });

        columnController.addColumn(ModelMessages.model_navigator_Connection, DTUIMessages.sql_script_task_selector_dialog_column_description_script_data_source, SWT.LEFT, true, true, new ColumnLabelProvider() {
            @Override
            public String getText(Object element) {
                if (element instanceof DBNResource) {
                    Collection<DBPDataSourceContainer> containers = ((DBNResource) element).getAssociatedDataSources();
                    if (!CommonUtils.isEmpty(containers)) {
                        StringBuilder text = new StringBuilder();
                        for (DBPDataSourceContainer container : containers) {
                            if (!text.isEmpty()) {
                                text.append(", ");
                            }
                            text.append(container.getName());
                        }
                        return text.toString();
                    }
                }
                return "";
            }

        });
        columnController.createColumns(true);
    }

    private boolean isResourceApplicable(DBNResource element) {
        IResource resource = element.getResource();
        if (resource instanceof IFolder) {
            // FIXME: this is a hack
            return "script folder".equals(element.getNodeType());
        }
        return resource instanceof IContainer || (resource instanceof IFile && "sql".equals(resource.getFileExtension()));
    }

}
