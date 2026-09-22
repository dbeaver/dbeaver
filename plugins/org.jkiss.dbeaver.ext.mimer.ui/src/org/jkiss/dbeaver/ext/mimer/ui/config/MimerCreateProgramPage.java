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
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Text;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.ext.mimer.model.MimerProgram;
import org.jkiss.dbeaver.ext.mimer.ui.internal.MimerUIMessages;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.editors.object.struct.BaseObjectEditPage;
import org.jkiss.utils.CommonUtils;

/**
 * "Create Program" dialog for Mimer SQL - unlike {@link MimerCreateUserPage}, the password is
 * mandatory (per the docs, at most 18 characters), and there's no schema or group membership
 * to collect - a program ident doesn't connect to a database directly.
 *
 * @author Mimer Information Technology
 */
public class MimerCreateProgramPage extends BaseObjectEditPage {

    private final MimerProgram program;

    private String name = "";
    private String password = "";

    public MimerCreateProgramPage(@NotNull MimerProgram program) {
        super("Create program");
        this.program = program;
    }

    @Override
    public DBSObject getObject() {
        return program;
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
        passwordText.setMessage(MimerUIMessages.page_create_program_password_placeholder);
        passwordText.addModifyListener(e -> {
            password = passwordText.getText();
            updatePageState();
        });

        return group;
    }

    @Override
    public boolean isPageComplete() {
        return !CommonUtils.isEmptyTrimmed(name) && !CommonUtils.isEmpty(password);
    }

    /**
     * Applies the collected values to the program. Call after {@link #edit()} returns {@code true}.
     */
    public void applyChanges() {
        program.setName(name.trim());
        program.setPassword(password);
    }
}
