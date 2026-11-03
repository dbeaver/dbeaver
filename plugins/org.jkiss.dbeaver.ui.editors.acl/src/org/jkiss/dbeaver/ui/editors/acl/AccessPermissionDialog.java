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

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.DBIcon;
import org.jkiss.dbeaver.model.access.DBAAccessState;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.BaseDialog;
import org.jkiss.dbeaver.ui.editors.acl.internal.AccessMessages;

import java.util.List;
import java.util.Locale;

class AccessPermissionDialog extends BaseDialog {
    private final List<DBAAccessState.Target> targets;
    private final List<DBAAccessState.Permission> existingPermissions;
    private final DBAAccessState.Permission original;
    private final boolean revoke;
    private Button cascade;
    private List<DBAAccessState.Target> filtered;
    private Combo target;
    private Combo permission;
    private Combo state;
    private DBAAccessState.Permission result;

    AccessPermissionDialog(
        @NotNull Shell shell,
        @NotNull List<DBAAccessState.Target> targets,
        @NotNull List<DBAAccessState.Permission> existingPermissions
    ) {
        this(shell, targets, existingPermissions, null, false);
    }

    AccessPermissionDialog(
        @NotNull Shell shell,
        @NotNull List<DBAAccessState.Target> targets,
        @Nullable DBAAccessState.Permission original,
        boolean revoke
    ) {
        this(shell, targets, List.of(), original, revoke);
    }

    private AccessPermissionDialog(
        @NotNull Shell shell,
        @NotNull List<DBAAccessState.Target> targets,
        @NotNull List<DBAAccessState.Permission> existingPermissions,
        @Nullable DBAAccessState.Permission original,
        boolean revoke
    ) {
        super(shell,
            revoke ? AccessMessages.access_revoke_permission : original == null ? AccessMessages.access_add : AccessMessages.access_edit,
            DBIcon.TREE_PERMISSIONS);
        this.original = original;
        this.revoke = revoke;
        this.existingPermissions = existingPermissions;
        this.targets = original == null
            ? targets.stream().filter(value -> value.editable() && !value.permissions().isEmpty()).toList()
            : List.of(original.target());
    }

    @NotNull
    @Override
    protected Composite createDialogArea(@NotNull Composite parent) {
        Composite area = super.createDialogArea(parent);
        Composite fields = UIUtils.createComposite(area, 2);
        fields.setLayoutData(new GridData(GridData.FILL_BOTH));
        if (original == null) {
            final Text filter = UIUtils.createLabelText(fields, AccessMessages.access_filter, "");
            target = UIUtils.createLabelCombo(fields, AccessMessages.access_securable, SWT.DROP_DOWN | SWT.READ_ONLY);
            ((GridData) target.getLayoutData()).widthHint = 500;
            permission = UIUtils.createLabelCombo(fields, AccessMessages.access_permission, SWT.DROP_DOWN | SWT.READ_ONLY);
            filter.addModifyListener(event -> filterTargets(filter.getText()));
            target.addSelectionListener(SelectionListener.widgetSelectedAdapter(event -> fillPermissions()));
            permission.addSelectionListener(SelectionListener.widgetSelectedAdapter(event -> updateSelection()));
        } else {
            Text objectName = UIUtils.createLabelText(fields, AccessMessages.access_securable, original.target().label(),
                SWT.BORDER | SWT.READ_ONLY);
            ((GridData) objectName.getLayoutData()).widthHint = 500;
            UIUtils.createLabelText(fields, AccessMessages.access_permission, original.name(), SWT.BORDER | SWT.READ_ONLY);
        }
        state = UIUtils.createLabelCombo(fields, AccessMessages.access_state, SWT.DROP_DOWN | SWT.READ_ONLY);
        String[] states = PrincipalAccessControl.permissionStateLabels();
        state.setItems(original == null ? java.util.Arrays.copyOf(states, 3) : states);
        state.select(revoke ? DBAAccessState.PermissionState.REVOKE.ordinal() : original == null ? 0 : original.state().ordinal());
        state.setEnabled(!revoke);
        cascade = UIUtils.createCheckbox(fields, AccessMessages.access_cascade, original != null && original.cascade());
        cascade.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
        cascade.setToolTipText(AccessMessages.access_cascade_tip);
        state.addSelectionListener(SelectionListener.widgetSelectedAdapter(event -> updateCascade()));
        cascade.addSelectionListener(SelectionListener.widgetSelectedAdapter(event -> updateButtons()));
        if (original == null) {
            filterTargets("");
        }
        updateCascade();
        return area;
    }

    private void filterTargets(@NotNull String text) {
        String pattern = text.toLowerCase(Locale.ROOT);
        filtered = targets.stream().filter(value -> value.label().toLowerCase(Locale.ROOT).contains(pattern)).toList();
        target.setItems(filtered.stream().map(DBAAccessState.Target::label).toArray(String[]::new));
        if (!filtered.isEmpty()) {
            target.select(0);
        }
        fillPermissions();
    }

    private void fillPermissions() {
        permission.removeAll();
        int index = target.getSelectionIndex();
        if (index >= 0) {
            permission.setItems(filtered.get(index).permissions().toArray(String[]::new));
            if (permission.getItemCount() > 0) {
                permission.select(0);
            }
        }
        updateSelection();
    }

    private void updateSelection() {
        DBAAccessState.Permission existing = selectedOriginal();
        state.select(existing == null || existing.state() == DBAAccessState.PermissionState.REVOKE
            ? DBAAccessState.PermissionState.GRANT.ordinal() : existing.state().ordinal());
        cascade.setSelection(existing != null && existing.cascade());
        updateCascade();
    }

    @Nullable
    private DBAAccessState.Permission selectedOriginal() {
        if (original != null) {
            return original;
        }
        int index = target.getSelectionIndex();
        if (index < 0 || permission.getSelectionIndex() < 0) {
            return null;
        }
        return existingPermissions.stream().filter(value -> value.target().id().equals(filtered.get(index).id())
            && value.name().equals(permission.getText())).findFirst().orElse(null);
    }

    private void updateCascade() {
        boolean visible = selectedOriginal() != null
            && state.getSelectionIndex() != DBAAccessState.PermissionState.GRANT_WITH_OPTION.ordinal();
        cascade.setVisible(visible);
        ((GridData) cascade.getLayoutData()).exclude = !visible;
        cascade.getParent().layout(true, true);
        updateButtons();
    }

    private void updateButtons() {
        DBAAccessState.Permission existing = selectedOriginal();
        boolean requiresCascade = existing != null && existing.state() == DBAAccessState.PermissionState.GRANT_WITH_OPTION
            && state.getSelectionIndex() != DBAAccessState.PermissionState.GRANT_WITH_OPTION.ordinal();
        enableButton(IDialogConstants.OK_ID, (original != null
            || target.getSelectionIndex() >= 0 && permission.getSelectionIndex() >= 0)
            && (existing == null || existing.editable()) && (!requiresCascade || cascade.getSelection()));
    }

    @Override
    protected void createButtonsForButtonBar(@NotNull Composite parent) {
        super.createButtonsForButtonBar(parent);
        updateButtons();
    }

    @Override
    protected void okPressed() {
        DBAAccessState.Permission existing = selectedOriginal();
        result = new DBAAccessState.Permission(original == null ? filtered.get(target.getSelectionIndex()) : original.target(),
            original == null ? permission.getText() : original.name(),
            DBAAccessState.PermissionState.values()[state.getSelectionIndex()], existing == null ? null : existing.grantor(), true,
            cascade.getVisible() && cascade.getSelection());
        super.okPressed();
    }

    @NotNull
    DBAAccessState.Permission getPermission() {
        return result;
    }
}
