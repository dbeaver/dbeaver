/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2021 DBeaver Corp and others
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

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.navigator.DBNLocalFolder;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.model.navigator.DBNResource;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UIIcon;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.internal.UINavigatorMessages;
import org.jkiss.dbeaver.ui.navigator.actions.NavigatorObjectsDeleter;
import org.jkiss.utils.CommonUtils;

import java.util.List;

public class ConfirmNavigatorNodesDeleteDialog extends MessageDialog {
    private final List<?> selectedObjects;

    private final NavigatorObjectsDeleter deleter;

    private ConfirmNavigatorNodesDeleteDialog(Shell shell, String title, String message, List<?> selectedObjects, @Nullable NavigatorObjectsDeleter deleter) {
        super(shell, title, DBeaverIcons.getImage(UIIcon.REJECT), message, MessageDialog.ERROR, null, 0);
        this.selectedObjects = selectedObjects;
        this.deleter = deleter;
    }

    public static ConfirmNavigatorNodesDeleteDialog of(Shell shell, String title, String message, List<?> selectedObjects, NavigatorObjectsDeleter deleter) {
        return new ConfirmNavigatorNodesDeleteDialog(shell, title, message, selectedObjects, deleter);
    }

    public static ConfirmNavigatorNodesDeleteDialog of(Shell shell, List<?> selectedObjects, NavigatorObjectsDeleter deleter) {
        if (selectedObjects.size() > 1) {
            return new ConfirmNavigatorNodesDeleteDialog(
                shell,
                UINavigatorMessages.confirm_deleting_multiple_objects_title,
                NLS.bind(UINavigatorMessages.confirm_deleting_multiple_objects_message, selectedObjects.size()),
                selectedObjects,
                deleter
            );
        }
        DBNNode node = (DBNNode) selectedObjects.get(0);
        String title = NLS.bind(node instanceof DBNLocalFolder ? UINavigatorMessages.confirm_local_folder_delete_title : UINavigatorMessages.confirm_entity_delete_title, node.getNodeType(), node.getNodeName());
        String message = NLS.bind(node instanceof DBNLocalFolder ? UINavigatorMessages.confirm_local_folder_delete_message : UINavigatorMessages.confirm_entity_delete_message, node.getNodeType(), node.getNodeName());
        return new ConfirmNavigatorNodesDeleteDialog(shell, title, message, selectedObjects, deleter);
    }

    @Override
    protected Control createCustomArea(Composite parent) {
        if (selectedObjects.size() > 1) {
            createObjectsTable(parent);
        }
        if (deleter != null) {
            createDeleteContents(parent);

            for (NavigatorObjectsDeleter.Option option : deleter.getSupportedOptions()) {
                createCheckbox(parent, option);
            }
        }
        return super.createCustomArea(parent);
    }

    private void createObjectsTable(Composite parent) {
        Composite placeholder = UIUtils.createComposite(parent, 1);
        placeholder.setLayoutData(new GridData(GridData.FILL_BOTH));
        Group tableGroup = UIUtils.createControlGroup(placeholder, UINavigatorMessages.confirm_deleting_multiple_objects_table_group_name, 1, GridData.FILL_BOTH, 0);
        tableGroup.setLayoutData(new GridData(GridData.FILL_BOTH));
        Table objectsTable = new Table(tableGroup, SWT.BORDER | SWT.FULL_SELECTION);
        objectsTable.setHeaderVisible(false);
        objectsTable.setLinesVisible(true);
        GridData gd = new GridData(GridData.FILL_BOTH);
        int fontHeight = UIUtils.getFontHeight(objectsTable);
        int rowCount = selectedObjects.size();
        gd.widthHint = fontHeight * 7;
        gd.heightHint = rowCount < 6 ? fontHeight * 2 * rowCount : fontHeight * 10;
        objectsTable.setLayoutData(gd);
        UIUtils.createTableColumn(objectsTable, SWT.LEFT, UINavigatorMessages.confirm_deleting_multiple_objects_column_name);
        UIUtils.createTableColumn(objectsTable, SWT.LEFT, "Type");
        UIUtils.createTableColumn(objectsTable, SWT.LEFT, UINavigatorMessages.confirm_deleting_multiple_objects_column_description);
        for (Object obj: selectedObjects) {
            if (!(obj instanceof DBNNode)) {
                continue;
            }
            DBNNode node = (DBNNode) obj;
            TableItem item = new TableItem(objectsTable, SWT.NONE);
            item.setImage(DBeaverIcons.getImage(node.getNodeIcon()));
            if (node instanceof DBNResource && ((DBNResource) node).getResource() != null) {
                item.setText(0, node.getName());
                IResource resource = ((DBNResource) node).getResource();
                IPath resLocation = resource == null ? null : resource.getLocation();
                item.setText(1, "File");
                item.setText(2, resLocation == null ? "" : resLocation.toFile().getAbsolutePath());
            } else {
                item.setText(0, node.getNodeFullName());
                item.setText(1, node.getNodeType());
                item.setText(2, CommonUtils.toString(node.getNodeDescription()));
            }
        }
        UIUtils.asyncExec(() -> UIUtils.packColumns(objectsTable, true));
    }

    private void createDeleteContents(Composite parent) {
        if (!deleter.supportsDeleteContents()) {
            return;
        }
        IProject project = deleter.getProjectToDelete();
        if (project == null) {
            return;
        }
        Composite ph = UIUtils.createPlaceholder(parent, 2, 5);
        Button deleteContentsCheck = UIUtils.createCheckbox(
            ph,
            UINavigatorMessages.confirm_deleting_delete_contents_checkbox,
            UINavigatorMessages.confirm_deleting_delete_contents_checkbox_tooltip,
            false,
            2
        );
        deleteContentsCheck.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                deleter.setDeleteContents(deleteContentsCheck.getSelection());
            }
        });
        UIUtils.createLabelText(ph,
            UINavigatorMessages.confirm_deleting_project_location_label,
            project.getLocation().toFile().getAbsolutePath(),
            SWT.READ_ONLY
        );
    }

    private void createCheckbox(Composite checkboxesComposite, NavigatorObjectsDeleter.Option option) {
        Composite placeholder = UIUtils.createPlaceholder(checkboxesComposite, 1, 5);
        Button checkbox = UIUtils.createCheckbox(placeholder, option.getLabel(), option.getTip(), false, 0);
        checkbox.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                if (checkbox.getSelection()) {
                    deleter.enableOption(option);
                } else {
                    deleter.disableOption(option);
                }
            }
        });
    }

    @Override
    protected void createButtonsForButtonBar(final Composite parent) {
        createButton(parent, IDialogConstants.YES_ID, IDialogConstants.YES_LABEL, false);
        createButton(parent, IDialogConstants.NO_ID, IDialogConstants.NO_LABEL, true);
        if (deleter != null && deleter.supportsShowViewScript()) {
            createButton(parent, IDialogConstants.DETAILS_ID, UINavigatorMessages.actions_navigator_view_script_button, false);
        }
    }

    @Override
    protected boolean isResizable() {
        return true;
    }
}
