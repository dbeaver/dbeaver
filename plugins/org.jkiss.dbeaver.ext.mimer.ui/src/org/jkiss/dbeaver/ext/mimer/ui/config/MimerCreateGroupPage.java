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

import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Text;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.ext.mimer.model.MimerGroup;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.editors.object.struct.BaseObjectEditPage;
import org.jkiss.utils.CommonUtils;

/**
 * "Create Group" dialog for Mimer SQL - all attributes are collected before the ident is
 * created.
 *
 * @author Mimer Information Technology
 */
public class MimerCreateGroupPage extends BaseObjectEditPage {

    private final MimerGroup group;

    private String name = "";

    public MimerCreateGroupPage(@NotNull MimerGroup group) {
        super("Create group");
        this.group = group;
    }

    @Override
    public DBSObject getObject() {
        return group;
    }

    @NotNull
    @Override
    protected Control createPageContents(@NotNull Composite parent) {
        Composite composite = UIUtils.createComposite(parent, 2);
        composite.setLayoutData(new GridData(GridData.FILL_BOTH));

        Text nameText = UIUtils.createLabelText(composite, "Name", "");
        nameText.addModifyListener(e -> {
            name = nameText.getText();
            updatePageState();
        });
        nameText.setFocus();

        return composite;
    }

    @Override
    public boolean isPageComplete() {
        return !CommonUtils.isEmptyTrimmed(name);
    }

    /**
     * Applies the collected values to the group. Call after {@link #edit()} returns {@code true}.
     */
    public void applyChanges() {
        group.setName(name.trim());
    }
}
