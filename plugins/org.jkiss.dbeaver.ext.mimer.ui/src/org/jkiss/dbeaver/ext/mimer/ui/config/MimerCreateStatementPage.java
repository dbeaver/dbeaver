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
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Text;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.ext.mimer.model.MimerStatement;
import org.jkiss.dbeaver.ext.mimer.ui.internal.MimerUIMessages;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.editors.object.struct.BaseObjectEditPage;
import org.jkiss.utils.CommonUtils;

/**
 * "Create Statement" dialog for Mimer SQL - Name and a Cursor mode combo (Default/Scroll/No
 * Scroll, matching the {@code CREATE [SCROLL|NO SCROLL] STATEMENT} grammar), otherwise minimal:
 * a statement's real content is an arbitrary CALL/DELETE/INSERT/SELECT/UPDATE body, written
 * afterward in the Source tab that opens once this closes ({@code FEATURE_EDITOR_ON_CREATE}),
 * same "seed a template, finish in the Source tab" pattern as {@link MimerCreateModulePage}.
 *
 * @author Mimer Information Technology
 */
public class MimerCreateStatementPage extends BaseObjectEditPage {

    private static final String[] CURSOR_MODES = {"Default (both)", "Scroll", "No Scroll"};

    private final MimerStatement statement;

    private String name = "";
    private String cursorMode = CURSOR_MODES[0];

    public MimerCreateStatementPage(@NotNull MimerStatement statement) {
        super("Create statement");
        this.statement = statement;
    }

    @Override
    public DBSObject getObject() {
        return statement;
    }

    @NotNull
    @Override
    protected Control createPageContents(@NotNull Composite parent) {
        Composite group = new Composite(parent, SWT.NONE);
        group.setLayout(new GridLayout(2, false));
        group.setLayoutData(new GridData(GridData.FILL_BOTH));

        UIUtils.createLabelText(group, "Schema",
            DBUtils.getObjectFullName(statement.getParentObject(), DBPEvaluationContext.UI)).setEditable(false);

        Text nameText = UIUtils.createLabelText(group, "Name", "");
        nameText.addModifyListener(e -> {
            name = nameText.getText();
            updatePageState();
        });
        nameText.setFocus();

        Combo cursorCombo = UIUtils.createLabelCombo(group, "Cursor", SWT.DROP_DOWN | SWT.READ_ONLY);
        cursorCombo.setToolTipText(MimerUIMessages.page_create_statement_cursor_tooltip);
        for (String mode : CURSOR_MODES) {
            cursorCombo.add(mode);
        }
        cursorCombo.select(0);
        cursorCombo.addModifyListener(e -> cursorMode = cursorCombo.getText());

        return group;
    }

    @Override
    public boolean isPageComplete() {
        return !CommonUtils.isEmptyTrimmed(name);
    }

    /**
     * Applies the collected name and seeds the initial header + placeholder body. Call after
     * {@link #edit()} returns {@code true}.
     */
    public void applyChanges() {
        String statementName = name.trim();
        statement.setName(statementName);

        String schemaName = statement.getSchema().getName();
        String clause = CURSOR_MODES[1].equals(cursorMode) ? " SCROLL"
            : CURSOR_MODES[2].equals(cursorMode) ? " NO SCROLL" : "";

        StringBuilder sb = new StringBuilder();
        sb.append("CREATE").append(clause).append(" STATEMENT \"").append(schemaName).append("\".\"").append(statementName).append("\"\n");
        sb.append("    -- TODO: statement body, e.g. SELECT/INSERT/UPDATE/DELETE/CALL");

        statement.setObjectDefinitionText(sb.toString());
    }
}
