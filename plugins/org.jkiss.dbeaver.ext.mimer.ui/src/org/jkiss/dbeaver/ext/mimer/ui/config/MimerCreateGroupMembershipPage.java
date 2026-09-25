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
package org.jkiss.dbeaver.ext.mimer.ui.config;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.mimer.MimerConstants;
import org.jkiss.dbeaver.ext.mimer.model.MimerDataSource;
import org.jkiss.dbeaver.ext.mimer.model.MimerGroup;
import org.jkiss.dbeaver.ext.mimer.model.MimerIdentGroupMembership;
import org.jkiss.dbeaver.ext.mimer.ui.internal.MimerUIMessages;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.editors.object.struct.BaseObjectEditPage;
import org.jkiss.utils.CommonUtils;

/**
 * {@code GRANT MEMBER} dialog shown from an ident's own "Group Memberships" folder - picks which
 * group to join (the ident itself is fixed, shown read-only). Excludes {@link
 * MimerConstants#GROUP_PUBLIC} (every ident is already implicitly a member, Mimer SQL rejects an
 * explicit grant - same rule {@link MimerCreateGroupMemberPage} already enforces from the other
 * side) and, if the owning ident is itself a group, excludes that group's own name (a group can't
 * be a member of itself).
 *
 * @author Mimer Information Technology
 */
public class MimerCreateGroupMembershipPage extends BaseObjectEditPage {

    private static final Log log = Log.getLog(MimerCreateGroupMembershipPage.class);

    private final MimerIdentGroupMembership membership;

    private String groupName = "";
    private boolean grantable;

    public MimerCreateGroupMembershipPage(@NotNull MimerIdentGroupMembership membership) {
        super("Grant membership");
        this.membership = membership;
    }

    @Override
    public DBSObject getObject() {
        return membership;
    }

    @NotNull
    @Override
    protected Control createPageContents(@NotNull Composite parent) {
        Composite composite = UIUtils.createComposite(parent, 2);
        composite.setLayoutData(new GridData(GridData.FILL_BOTH));

        UIUtils.createLabelText(composite, "Ident", membership.getIdent().getName()).setEditable(false);

        Combo groupCombo = UIUtils.createLabelCombo(composite, "Group", SWT.DROP_DOWN);
        groupCombo.setToolTipText(MimerUIMessages.page_create_group_membership_group_tooltip);
        for (String name : loadGroupNames()) {
            groupCombo.add(name);
        }
        groupCombo.addModifyListener(e -> {
            groupName = groupCombo.getText();
            updatePageState();
        });
        groupCombo.setFocus();

        Button grantableCheck = UIUtils.createCheckbox(composite, "With Grant Option", null, false, 2);
        grantableCheck.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                grantable = grantableCheck.getSelection();
            }
        });

        return composite;
    }

    @NotNull
    private String[] loadGroupNames() {
        try {
            if (membership.getDataSource() instanceof MimerDataSource ds) {
                String ownName = membership.getIdent() instanceof MimerGroup ownGroup ? ownGroup.getName() : null;
                return ds.getGroups(new VoidProgressMonitor()).stream()
                    .map(MimerGroup::getName)
                    .filter(name -> !MimerConstants.GROUP_PUBLIC.equalsIgnoreCase(name))
                    .filter(name -> ownName == null || !name.equalsIgnoreCase(ownName))
                    .sorted()
                    .toArray(String[]::new);
            }
        } catch (Exception e) {
            log.debug("Can't load group list for the grant-membership dialog", e);
        }
        return new String[0];
    }

    @Override
    public boolean isPageComplete() {
        return !CommonUtils.isEmptyTrimmed(groupName);
    }

    /**
     * Applies the collected values to the membership. Call after {@link #edit()} returns {@code true}.
     */
    public void applyChanges() {
        membership.setGroupName(groupName.trim());
        membership.setGrantable(grantable);
    }
}
