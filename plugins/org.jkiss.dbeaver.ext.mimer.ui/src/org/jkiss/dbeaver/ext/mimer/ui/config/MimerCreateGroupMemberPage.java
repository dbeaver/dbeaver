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
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.mimer.model.MimerDataSource;
import org.jkiss.dbeaver.ext.mimer.model.MimerGroup;
import org.jkiss.dbeaver.ext.mimer.model.MimerGroupMember;
import org.jkiss.dbeaver.ext.mimer.model.MimerProgram;
import org.jkiss.dbeaver.ext.mimer.model.MimerUser;
import org.jkiss.dbeaver.ext.mimer.ui.internal.MimerUIMessages;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.editors.object.struct.BaseObjectEditPage;
import org.jkiss.utils.CommonUtils;

import java.util.stream.Stream;

/**
 * {@code GRANT MEMBER} dialog for Mimer SQL - collects the ident to add to the group and
 * whether it should be able to grant that membership on to others in turn.
 *
 * @author Mimer Information Technology
 */
public class MimerCreateGroupMemberPage extends BaseObjectEditPage {

    private static final Log log = Log.getLog(MimerCreateGroupMemberPage.class);

    private final MimerGroupMember member;

    private String memberName = "";
    private boolean grantable;

    public MimerCreateGroupMemberPage(@NotNull MimerGroupMember member) {
        super("Grant membership");
        this.member = member;
    }

    @Override
    public DBSObject getObject() {
        return member;
    }

    @NotNull
    @Override
    protected Control createPageContents(@NotNull Composite parent) {
        Composite composite = UIUtils.createComposite(parent, 2);
        composite.setLayoutData(new GridData(GridData.FILL_BOTH));

        UIUtils.createLabelText(composite, "Group", member.getGroup().getName()).setEditable(false);

        // Editable (not READ_ONLY): Users/Groups/Programs cover every ident kind we model, but
        // stays typeable in case some other ident needs naming directly.
        Combo memberCombo = UIUtils.createLabelCombo(composite, "Ident", SWT.DROP_DOWN);
        memberCombo.setToolTipText(MimerUIMessages.page_create_group_member_ident_tooltip);
        for (String name : loadIdentNames()) {
            memberCombo.add(name);
        }
        memberCombo.addModifyListener(e -> {
            memberName = memberCombo.getText();
            updatePageState();
        });
        memberCombo.setFocus();

        Button grantableCheck = UIUtils.createCheckbox(composite, "With Grant Option", null, false, 2);
        grantableCheck.addSelectionListener(new org.eclipse.swt.events.SelectionAdapter() {
            @Override
            public void widgetSelected(org.eclipse.swt.events.SelectionEvent e) {
                grantable = grantableCheck.getSelection();
            }
        });

        return composite;
    }

    @NotNull
    private String[] loadIdentNames() {
        try {
            if (member.getDataSource() instanceof MimerDataSource ds) {
                String ownGroup = member.getGroup().getName();
                return Stream.concat(
                        Stream.concat(
                            ds.getUsers(new VoidProgressMonitor()).stream().map(MimerUser::getName),
                            ds.getPrograms(new VoidProgressMonitor()).stream().map(MimerProgram::getName)),
                        ds.getGroups(new VoidProgressMonitor()).stream().map(MimerGroup::getName)
                            .filter(name -> !name.equals(ownGroup))) // a group can't be its own member
                    .sorted()
                    .toArray(String[]::new);
            }
        } catch (Exception e) {
            log.debug("Can't load ident list for the grant-membership dialog", e);
        }
        return new String[0];
    }

    @Override
    public boolean isPageComplete() {
        return !CommonUtils.isEmptyTrimmed(memberName);
    }

    /**
     * Applies the collected values to the member. Call after {@link #edit()} returns {@code true}.
     */
    public void applyChanges() {
        member.setName(memberName.trim());
        member.setGrantable(grantable);
    }
}
