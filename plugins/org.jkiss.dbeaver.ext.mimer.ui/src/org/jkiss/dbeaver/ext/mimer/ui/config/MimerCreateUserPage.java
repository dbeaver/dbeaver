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
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.Text;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.mimer.MimerConstants;
import org.jkiss.dbeaver.ext.mimer.model.MimerDataSource;
import org.jkiss.dbeaver.ext.mimer.model.MimerGroup;
import org.jkiss.dbeaver.ext.mimer.ui.internal.MimerUIMessages;
import org.jkiss.dbeaver.ext.mimer.model.MimerUser;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.editors.object.struct.BaseObjectEditPage;
import org.jkiss.utils.CommonUtils;

import java.util.Collections;

/**
 * "Create User" dialog for Mimer SQL - all attributes are collected before the ident is
 * created, including which existing groups (if any) the user should be granted MEMBER of
 * right away - see {@link MimerUser#buildGroupGrantDDL}.
 *
 * @author Mimer Information Technology
 */
public class MimerCreateUserPage extends BaseObjectEditPage {

    private static final Log log = Log.getLog(MimerCreateUserPage.class);

    private final MimerUser user;

    private String name = "";
    private String password = "";
    private boolean withoutSchema;
    private java.util.List<String> selectedGroups = Collections.emptyList();

    public MimerCreateUserPage(@NotNull MimerUser user) {
        super("Create user");
        this.user = user;
    }

    @Override
    public DBSObject getObject() {
        return user;
    }

    @NotNull
    @Override
    protected Control createPageContents(@NotNull Composite parent) {
        Composite group = UIUtils.createComposite(parent, 2);
        group.setLayoutData(new GridData(GridData.FILL_BOTH));

        Text nameText = UIUtils.createLabelText(group, "Name", "");
        nameText.addModifyListener(e -> {
            name = nameText.getText();
            updatePageState();
        });
        nameText.setFocus();

        Text passwordText = UIUtils.createLabelText(group, "Password", "", SWT.BORDER | SWT.PASSWORD);
        passwordText.setMessage(MimerUIMessages.page_create_user_password_placeholder);
        passwordText.addModifyListener(e -> password = passwordText.getText());

        Button withoutSchemaCheck = UIUtils.createCheckbox(group, "Without Schema", null, false, 2);
        withoutSchemaCheck.setToolTipText(
            "By default (WITH SCHEMA), a schema named after the user is created automatically. " +
            "Check this to skip that (WITHOUT SCHEMA).");
        withoutSchemaCheck.addSelectionListener(new org.eclipse.swt.events.SelectionAdapter() {
            @Override
            public void widgetSelected(org.eclipse.swt.events.SelectionEvent e) {
                withoutSchema = withoutSchemaCheck.getSelection();
            }
        });

        UIUtils.createControlLabel(group, "Groups", 2);
        List groupList = new List(group, SWT.MULTI | SWT.BORDER | SWT.V_SCROLL);
        GridData groupListData = new GridData(GridData.FILL_HORIZONTAL);
        groupListData.horizontalSpan = 2;
        groupListData.heightHint = 80;
        groupList.setLayoutData(groupListData);
        groupList.setItems(loadGroupNames());
        groupList.addSelectionListener(new org.eclipse.swt.events.SelectionAdapter() {
            @Override
            public void widgetSelected(org.eclipse.swt.events.SelectionEvent e) {
                selectedGroups = java.util.Arrays.asList(groupList.getSelection());
            }
        });

        return group;
    }

    @NotNull
    private String[] loadGroupNames() {
        try {
            if (user.getDataSource() instanceof MimerDataSource ds) {
                return ds.getGroups(new VoidProgressMonitor()).stream()
                    .map(MimerGroup::getName)
                    // Every ident is already implicitly a member of PUBLIC - Mimer SQL rejects
                    // an explicit GRANT MEMBER ON GROUP PUBLIC, so don't offer it here.
                    .filter(name -> !MimerConstants.GROUP_PUBLIC.equalsIgnoreCase(name))
                    .sorted()
                    .toArray(String[]::new);
            }
        } catch (Exception e) {
            log.debug("Can't load group list for the create-user dialog", e);
        }
        return new String[0];
    }

    @Override
    public boolean isPageComplete() {
        return !CommonUtils.isEmptyTrimmed(name);
    }

    /**
     * Applies the collected values to the user. Call after {@link #edit()} returns {@code true}.
     */
    public void applyChanges() {
        user.setName(name.trim());
        user.setPassword(password.isEmpty() ? null : password);
        user.setWithoutSchema(withoutSchema);
        user.setInitialGroups(selectedGroups);
    }
}
