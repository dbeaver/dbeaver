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
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Text;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.ext.mimer.model.MimerModule;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.editors.object.struct.BaseObjectEditPage;
import org.jkiss.utils.CommonUtils;

/**
 * "Create Module" dialog for Mimer SQL - name only, matching how {@code
 * OraclePackageConfigurator} keeps its own equivalent dialog minimal (a package/module's real
 * content is written afterward in the source editor that opens once this closes, {@code
 * FEATURE_EDITOR_ON_CREATE}). Seeds that editor with a one-routine template: {@code CREATE MODULE
 * ... DECLARE PROCEDURE ... LANGUAGE SQL ... BEGIN ... END; END MODULE} - a module can declare
 * more than one routine, but the
 * template only stubs the first; the user adds any more directly in the source editor.
 *
 * @author Mimer Information Technology
 */
public class MimerCreateModulePage extends BaseObjectEditPage {

    private final MimerModule module;

    private String name = "";

    public MimerCreateModulePage(@NotNull MimerModule module) {
        super("Create module");
        this.module = module;
    }

    @Override
    public DBSObject getObject() {
        return module;
    }

    @NotNull
    @Override
    protected Control createPageContents(@NotNull Composite parent) {
        Composite group = new Composite(parent, SWT.NONE);
        group.setLayout(new GridLayout(2, false));
        group.setLayoutData(new GridData(GridData.FILL_BOTH));

        UIUtils.createLabelText(group, "Schema",
            DBUtils.getObjectFullName(module.getParentObject(), DBPEvaluationContext.UI)).setEditable(false);

        Text nameText = UIUtils.createLabelText(group, "Name", "");
        nameText.addModifyListener(e -> {
            name = nameText.getText();
            updatePageState();
        });
        nameText.setFocus();

        return group;
    }

    @Override
    public boolean isPageComplete() {
        return !CommonUtils.isEmptyTrimmed(name);
    }

    /**
     * Applies the collected name and seeds the initial template. Call after {@link #edit()}
     * returns {@code true}.
     */
    public void applyChanges() {
        String moduleName = name.trim();
        module.setName(moduleName);

        String schemaName = module.getSchema().getName();
        String procedureName = moduleName + "_proc";
        StringBuilder sb = new StringBuilder();
        sb.append("CREATE MODULE \"").append(schemaName).append("\".\"").append(moduleName).append("\"\n");
        sb.append("DECLARE PROCEDURE \"").append(schemaName).append("\".\"").append(procedureName).append("\" ()\n");
        sb.append("LANGUAGE SQL\n");
        sb.append("NOT DETERMINISTIC\n");
        sb.append("READS SQL DATA\n");
        sb.append("BEGIN\n");
        sb.append("    -- TODO: procedure body\n");
        sb.append("END;\n");
        sb.append("END MODULE");

        module.setObjectDefinitionText(sb.toString());
    }
}
