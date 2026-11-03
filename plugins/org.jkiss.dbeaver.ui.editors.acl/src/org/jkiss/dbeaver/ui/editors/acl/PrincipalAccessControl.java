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
package org.jkiss.dbeaver.ui.editors.acl;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IContributionManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.DBPImage;
import org.jkiss.dbeaver.model.access.DBAAccessState;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UIIcon;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.editors.acl.internal.AccessMessages;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

public class PrincipalAccessControl extends Composite {
    private Composite mappingPanel;
    private Button mapped;
    private Text userName;
    private Text defaultSchema;
    private Table roles;
    private Table permissions;
    private Action addPermission;
    private Action editPermission;
    private Action revokePermission;
    private final Consumer<DBAAccessState> listener;
    private DBAAccessState current;
    private boolean updating;
    private boolean editable;

    public PrincipalAccessControl(
        @NotNull Composite parent,
        boolean rolesPage,
        @NotNull Consumer<DBAAccessState> listener
    ) {
        super(parent, SWT.NONE);
        this.listener = listener;
        setLayout(new GridLayout(1, false));
        mappingPanel = UIUtils.createComposite(this, 2);
        mappingPanel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        mapped = UIUtils.createCheckbox(mappingPanel, AccessMessages.access_mapping, false);
        mapped.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
        userName = UIUtils.createLabelText(mappingPanel, AccessMessages.access_user_name, "");
        defaultSchema = UIUtils.createLabelText(mappingPanel, AccessMessages.access_default_schema, "");
        mapped.addSelectionListener(SelectionListener.widgetSelectedAdapter(event -> changeMapping()));
        userName.addModifyListener(event -> changeMapping());
        defaultSchema.addModifyListener(event -> changeMapping());
        if (rolesPage) {
            createRoles();
        } else {
            createPermissions();
        }
    }

    private void createRoles() {
        roles = new Table(this, SWT.CHECK | SWT.BORDER | SWT.FULL_SELECTION | SWT.V_SCROLL);
        roles.setLayoutData(new GridData(GridData.FILL_BOTH));
        roles.setHeaderVisible(true);
        UIUtils.createTableColumn(roles, SWT.LEFT, AccessMessages.access_role);
        roles.addSelectionListener(SelectionListener.widgetSelectedAdapter(event -> {
            if (event.detail == SWT.CHECK && event.item instanceof TableItem item && current != null) {
                DBAAccessState.Role role = (DBAAccessState.Role) item.getData();
                if (!canEdit() || !role.editable()) {
                    item.setChecked(role.granted());
                    return;
                }
                List<DBAAccessState.Role> changed = new ArrayList<>(current.roles());
                changed.set(changed.indexOf(role), new DBAAccessState.Role(role.name(), item.getChecked(), role.editable()));
                apply(new DBAAccessState(current.scope(), current.mapping(), changed, current.targets(), current.permissions()));
            }
        }));
    }

    private void createPermissions() {
        addPermission = createAction(AccessMessages.access_add, UIIcon.ADD, this::addPermission);
        editPermission = createAction(AccessMessages.access_edit, UIIcon.EDIT, () -> editPermission(false));
        revokePermission = createAction(AccessMessages.access_revoke_permission, UIIcon.DELETE, () -> editPermission(true));
        permissions = new Table(this, SWT.BORDER | SWT.FULL_SELECTION | SWT.V_SCROLL | SWT.H_SCROLL);
        permissions.setLayoutData(new GridData(GridData.FILL_BOTH));
        permissions.setHeaderVisible(true);
        permissions.setLinesVisible(true);
        UIUtils.createTableColumn(permissions, SWT.LEFT, AccessMessages.access_securable);
        UIUtils.createTableColumn(permissions, SWT.LEFT, AccessMessages.access_permission);
        UIUtils.createTableColumn(permissions, SWT.LEFT, AccessMessages.access_state);
        UIUtils.createTableColumn(permissions, SWT.LEFT, AccessMessages.access_grantor);
        permissions.addSelectionListener(SelectionListener.widgetSelectedAdapter(event -> updatePermissionControls()));
        permissions.addListener(SWT.DefaultSelection, event -> editPermission(false));
        MenuManager menu = new MenuManager();
        menu.add(addPermission);
        menu.add(editPermission);
        menu.add(revokePermission);
        permissions.setMenu(menu.createContextMenu(permissions));
        addDisposeListener(event -> menu.dispose());
    }

    public void contributeActions(@NotNull IContributionManager manager) {
        if (permissions != null) {
            manager.add(new Separator());
            manager.add(addPermission);
            manager.add(editPermission);
            manager.add(revokePermission);
        }
    }

    @Override
    public boolean setFocus() {
        return roles == null ? permissions.setFocus() : roles.setFocus();
    }

    @NotNull
    private Action createAction(@NotNull String label, @NotNull DBPImage image, @NotNull Runnable runnable) {
        return new Action(label, DBeaverIcons.getImageDescriptor(image)) {
            @Override
            public void run() {
                runnable.run();
            }
        };
    }

    public void setEditable(boolean editable) {
        this.editable = editable;
    }

    public void setState(@Nullable DBAAccessState state) {
        current = state;
        showState();
    }

    private void changeMapping() {
        if (updating || current == null || current.mapping() == null || !canEditMapping()) {
            return;
        }
        DBAAccessState.Mapping mapping = current.mapping();
        apply(new DBAAccessState(current.scope(), new DBAAccessState.Mapping(mapped.getSelection(), userName.getText().trim(),
            defaultSchema.getText().trim(), mapping.editable(), mapping.schemaEditable(), mapping.schemaNullable()),
            current.roles(), current.targets(), current.permissions()));
    }

    private void apply(@NotNull DBAAccessState changed) {
        if (!updating && !changed.equals(current)) {
            listener.accept(changed);
        }
    }

    private void showState() {
        if (isDisposed()) {
            return;
        }
        DBAAccessState.Permission selected = selectedPermission();
        updating = true;
        try {
            DBAAccessState.Mapping mapping = current == null ? null : current.mapping();
            boolean showMapping = roles != null && mapping != null;
            mappingPanel.setVisible(showMapping);
            ((GridData) mappingPanel.getLayoutData()).exclude = !showMapping;
            if (showMapping) {
                mapped.setSelection(mapping.present());
                if (!userName.getText().equals(mapping.name())) {
                    userName.setText(mapping.name());
                }
                if (!defaultSchema.getText().equals(mapping.defaultSchema())) {
                    defaultSchema.setText(mapping.defaultSchema());
                }
                mapped.setEnabled(canEditMapping());
                userName.setEnabled(canEditMapping() && mapping.present());
                defaultSchema.setEnabled(canEditMapping() && mapping.present() && mapping.schemaEditable());
            }
            if (roles != null) {
                roles.removeAll();
                if (current != null) {
                    for (DBAAccessState.Role role : current.roles()) {
                        TableItem item = new TableItem(roles, SWT.NONE);
                        item.setText(role.name());
                        item.setChecked(role.granted());
                        item.setGrayed(!role.editable());
                        item.setData(role);
                    }
                }
                roles.getColumn(0).pack();
                roles.setEnabled(canEdit());
            }
            if (permissions != null) {
                permissions.removeAll();
                if (current != null) {
                    for (DBAAccessState.Permission permission : current.permissions()) {
                        TableItem item = new TableItem(permissions, SWT.NONE);
                        item.setText(new String[] {permission.target().label(), permission.name(),
                            permissionStateLabels()[permission.state().ordinal()],
                            permission.grantor() == null ? "" : permission.grantor()});
                        item.setData(permission);
                        if (selected != null && selected.target().id().equals(permission.target().id())
                            && selected.name().equals(permission.name()) && Objects.equals(selected.grantor(), permission.grantor())) {
                            permissions.setSelection(item);
                        }
                    }
                }
                for (int index = 0; index < permissions.getColumnCount(); index++) {
                    permissions.getColumn(index).pack();
                }
            }
            updatePermissionControls();
            layout(true, true);
        } finally {
            updating = false;
        }
    }

    private boolean canEditMapping() {
        return current != null && current.mapping() != null && current.mapping().editable()
            && editable;
    }

    private boolean canEdit() {
        return current != null && (current.mapping() == null || current.mapping().present() && current.mapping().editable())
            && editable;
    }

    @Nullable
    private DBAAccessState.Permission selectedPermission() {
        return permissions == null || permissions.getSelectionCount() == 0
            ? null : (DBAAccessState.Permission) permissions.getSelection()[0].getData();
    }

    private void updatePermissionControls() {
        if (permissions == null) {
            return;
        }
        DBAAccessState.Permission selected = selectedPermission();
        boolean editable = canEdit() && selected != null && selected.editable();
        addPermission.setEnabled(canEdit() && current.targets().stream().anyMatch(DBAAccessState.Target::editable));
        editPermission.setEnabled(editable);
        revokePermission.setEnabled(editable && selected.state() != DBAAccessState.PermissionState.REVOKE);
    }

    private void editPermission(boolean revoke) {
        DBAAccessState.Permission selected = selectedPermission();
        if (!canEdit() || selected == null || !selected.editable()) {
            return;
        }
        AccessPermissionDialog dialog = new AccessPermissionDialog(getShell(), current.targets(), selected, revoke);
        if (dialog.open() == IDialogConstants.OK_ID) {
            List<DBAAccessState.Permission> changed = new ArrayList<>(current.permissions());
            changed.set(changed.indexOf(selected), dialog.getPermission());
            apply(new DBAAccessState(current.scope(), current.mapping(), current.roles(), current.targets(), changed));
        }
    }

    private void addPermission() {
        if (!canEdit()) {
            return;
        }
        AccessPermissionDialog dialog = new AccessPermissionDialog(getShell(), current.targets(), current.permissions());
        if (dialog.open() != IDialogConstants.OK_ID) {
            return;
        }
        DBAAccessState.Permission permission = dialog.getPermission();
        List<DBAAccessState.Permission> changed = new ArrayList<>(current.permissions());
        for (int index = 0; index < changed.size(); index++) {
            DBAAccessState.Permission existing = changed.get(index);
            if (existing.target().id().equals(permission.target().id()) && existing.name().equals(permission.name())
                && Objects.equals(existing.grantor(), permission.grantor())) {
                if (existing.editable()) {
                    changed.set(index, new DBAAccessState.Permission(existing.target(), existing.name(), permission.state(),
                        existing.grantor(), true, permission.cascade()));
                }
                apply(new DBAAccessState(current.scope(), current.mapping(), current.roles(), current.targets(), changed));
                return;
            }
        }
        changed.add(permission);
        apply(new DBAAccessState(current.scope(), current.mapping(), current.roles(), current.targets(), changed));
    }

    @NotNull
    static String[] permissionStateLabels() {
        return new String[] {
            AccessMessages.access_grant, AccessMessages.access_grant_option, AccessMessages.access_deny, AccessMessages.access_revoke
        };
    }

}
