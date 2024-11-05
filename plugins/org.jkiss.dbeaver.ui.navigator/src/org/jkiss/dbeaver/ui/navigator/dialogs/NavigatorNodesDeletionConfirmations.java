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
package org.jkiss.dbeaver.ui.navigator.dialogs;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.DBIcon;
import org.jkiss.dbeaver.model.navigator.DBNLocalFolder;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.MessageBoxBuilder;
import org.jkiss.dbeaver.ui.dialogs.Reply;
import org.jkiss.dbeaver.ui.internal.UINavigatorMessages;
import org.jkiss.dbeaver.ui.navigator.actions.NavigatorObjectsDeleter;
import org.jkiss.utils.CommonUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public final class NavigatorNodesDeletionConfirmations {
    public static final Reply SHOW_SCRIPT = new Reply(UINavigatorMessages.actions_navigator_view_script_button);

    /**
     * Asks the user if they want to delete navigator objects.
     *
     * @param shell confirmation's parent shell
     * @param selectedObjects objects to delete
     * @param deleter deleter
     * @return user's reply
     */
    public static Reply confirm(
        @NotNull Shell shell,
        @NotNull Collection<?> selectedObjects,
        @Nullable NavigatorObjectsDeleter deleter
    ) {
        if (selectedObjects.size() > 1) {
            return confirm(
                shell,
                UINavigatorMessages.confirm_deleting_multiple_objects_title,
                NLS.bind(UINavigatorMessages.confirm_deleting_multiple_objects_message, selectedObjects.size()),
                selectedObjects,
                deleter
            );
        }
        DBNNode node = (DBNNode) selectedObjects.iterator().next();
        String title = NLS.bind(node instanceof DBNLocalFolder ? UINavigatorMessages.confirm_local_folder_delete_title : UINavigatorMessages.confirm_entity_delete_title,
            node.getNodeTypeLabel(),
            node.getNodeDisplayName());
        String message = NLS.bind(node instanceof DBNLocalFolder ? UINavigatorMessages.confirm_local_folder_delete_message : UINavigatorMessages.confirm_entity_delete_message,
            node.getNodeTypeLabel(),
            node.getNodeDisplayName());
        return confirm(shell, title, message, selectedObjects, deleter);
    }

    /**
     * Asks the user if they want to delete navigator objects.
     *
     * @param shell confirmation's parent shell
     * @param title confirmation's title
     * @param message confirmation's message
     * @param selectedObjects objects to delete
     * @param deleter deleter
     * @return user's reply
     */
    public static Reply confirm(
        @NotNull Shell shell,
        @NotNull String title,
        @NotNull String message,
        @NotNull Collection<?> selectedObjects,
        @Nullable NavigatorObjectsDeleter deleter
    ) {
        List<Reply> replies = new ArrayList<>(3);
        replies.add(Reply.YES);
        if (deleter != null && deleter.supportsShowViewScript()) {
            replies.add(SHOW_SCRIPT);
        }
        replies.add(Reply.CANCEL);

        final Reply[] reply = {null};
        MessageBoxBuilder messageBoxBuilder = MessageBoxBuilder.builder(shell)
            .setTitle(title)
            .setMessage(message)
            .setPrimaryImage(DBIcon.STATUS_WARNING)
            .setReplies(replies)
            .setDefaultReply(Reply.CANCEL)
            .setCustomArea(parent -> createCustomArea(parent, selectedObjects, deleter));
        UIUtils.syncExec(() -> reply[0] = messageBoxBuilder.showMessageBox());
        return reply[0];
    }

    private static void createCustomArea(
        @NotNull Composite parent,
        @NotNull Collection<?> selectedObjects,
        @Nullable NavigatorObjectsDeleter deleter
    ) {
        if (selectedObjects.size() > 1) {
            createObjectsTable(parent, selectedObjects);
        }
        if (deleter != null) {
            createDeleteContents(parent, deleter);
            for (NavigatorObjectsDeleter.Option option : deleter.getSupportedOptions()) {
                createCheckbox(parent, option, deleter);
            }
        }
    }

    private static void createObjectsTable(@NotNull Composite parent, @NotNull Collection<?> selectedObjects) {
        Composite placeholder = UIUtils.createComposite(parent, 1);
        placeholder.setLayoutData(new GridData(GridData.FILL_BOTH));
        Group tableGroup = UIUtils.createControlGroup(
            placeholder,
            UINavigatorMessages.confirm_deleting_multiple_objects_table_group_name,
            1,
            GridData.FILL_BOTH,
            0
        );
        tableGroup.setLayoutData(new GridData(GridData.FILL_BOTH));
        Table objectsTable = new Table(tableGroup, SWT.BORDER | SWT.FULL_SELECTION);
        objectsTable.setHeaderVisible(true);
        objectsTable.setLinesVisible(true);
        GridData gd = new GridData(GridData.FILL_BOTH);
        int fontHeight = UIUtils.getFontHeight(objectsTable);
        int rowCount = selectedObjects.size();
        gd.widthHint = fontHeight * 7;
        //gd.heightHint = rowCount < 6 ? fontHeight * 2 * rowCount : fontHeight * 10;
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
            if (node.getAdapter(IResource.class) != null) {
                IResource resource = node.getAdapter(IResource.class);
                item.setText(0, node.getName());
                IPath resLocation = resource.getLocation();
                item.setText(1, "File");
                item.setText(2, resLocation == null ? "" : resLocation.toString());
            } else {
                item.setText(0, node.getNodeFullName());
                item.setText(1, node.getNodeTypeLabel());
                item.setText(2, CommonUtils.toString(node.getNodeDescription()));
            }
        }
        UIUtils.asyncExec(() -> UIUtils.packColumns(objectsTable, true));
    }

    private static void createDeleteContents(@NotNull Composite parent, @NotNull NavigatorObjectsDeleter deleter) {
        if (!deleter.supportsDeleteContents()) {
            return;
        }
        IProject project = deleter.getProjectToDelete();
        if (project == null || DBWorkbench.isDistributed()) {
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

    private static void createCheckbox(
        @NotNull Composite checkboxesComposite,
        @NotNull NavigatorObjectsDeleter.Option option,
        @NotNull NavigatorObjectsDeleter deleter
    ) {
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

    private NavigatorNodesDeletionConfirmations() {
        // This is a utility class, no instances for you!
    }
}
