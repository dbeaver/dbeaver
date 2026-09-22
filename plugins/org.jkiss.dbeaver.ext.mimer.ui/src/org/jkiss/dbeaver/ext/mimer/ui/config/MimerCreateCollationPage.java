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
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.mimer.model.MimerCollation;
import org.jkiss.dbeaver.ext.mimer.model.MimerDataSource;
import org.jkiss.dbeaver.ext.mimer.ui.internal.MimerUIMessages;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.editors.object.struct.BaseObjectEditPage;
import org.jkiss.utils.CommonUtils;

/**
 * "Create Collation" dialog: Name, Source Collation (every collation in the datasource, not just
 * this schema's own - a new collation can be based on any existing one, matching {@code
 * CREATE_COLLATION.htm}'s own grammar), and an optional {@code USING} delta-string. All attributes
 * are collected before the object is created - see {@link MimerCollationConfigurator}.
 *
 * @author Mimer Information Technology
 */
public class MimerCreateCollationPage extends BaseObjectEditPage {

    private static final Log log = Log.getLog(MimerCreateCollationPage.class);

    private final MimerCollation collation;

    private String name = "";
    private String sourceCollation = "";
    private String usingClause = "";

    public MimerCreateCollationPage(@NotNull MimerCollation collation) {
        super("Create collation");
        this.collation = collation;
    }

    @Override
    public DBSObject getObject() {
        return collation;
    }

    @NotNull
    @Override
    protected Control createPageContents(@NotNull Composite parent) {
        Composite group = new Composite(parent, SWT.NONE);
        group.setLayout(new GridLayout(2, false));
        group.setLayoutData(new GridData(GridData.FILL_BOTH));

        UIUtils.createLabelText(group, "Schema",
            DBUtils.getObjectFullName(collation.getParentObject(), DBPEvaluationContext.UI)).setEditable(false);

        Text nameText = UIUtils.createLabelText(group, "Name", "");
        nameText.addModifyListener(e -> {
            name = nameText.getText();
            updatePageState();
        });
        nameText.setFocus();

        Combo sourceCombo = UIUtils.createLabelCombo(group, "Source collation", SWT.DROP_DOWN);
        sourceCombo.setToolTipText(MimerUIMessages.page_create_collation_source_tooltip);
        String[] names = loadCollationNames();
        for (String c : names) {
            sourceCombo.add(c);
        }
        if (names.length > 0) {
            sourceCombo.select(0);
            sourceCollation = names[0];
        }
        sourceCombo.addModifyListener(e -> {
            sourceCollation = sourceCombo.getText();
            updatePageState();
        });

        Text usingText = UIUtils.createLabelText(group, "Using clause", "");
        usingText.setMessage(MimerUIMessages.page_create_collation_using_placeholder);
        usingText.addModifyListener(e -> usingClause = usingText.getText());

        return group;
    }

    @NotNull
    private String[] loadCollationNames() {
        try {
            if (collation.getDataSource() instanceof MimerDataSource ds) {
                return ds.getCollations(new VoidProgressMonitor()).stream()
                    .map(c -> "\"" + c.getSchemaName() + "\".\"" + c.getName() + "\"")
                    .toArray(String[]::new);
            }
        } catch (Exception e) {
            log.debug("Can't load collation list for the create-collation dialog", e);
        }
        return new String[0];
    }

    @Override
    public boolean isPageComplete() {
        return !CommonUtils.isEmptyTrimmed(name) && !CommonUtils.isEmptyTrimmed(sourceCollation);
    }

    /**
     * Applies the collected values to the collation. Call after {@link #edit()} returns {@code true}.
     */
    public void applyChanges() {
        collation.setName(name.trim());
        collation.setSourceCollation(sourceCollation.trim());
        collation.setUsingClause(CommonUtils.isEmptyTrimmed(usingClause) ? null : usingClause.trim());
    }
}
