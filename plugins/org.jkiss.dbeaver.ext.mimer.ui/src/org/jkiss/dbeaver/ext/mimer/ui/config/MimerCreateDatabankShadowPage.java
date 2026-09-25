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
import org.jkiss.dbeaver.ext.mimer.model.MimerDatabankShadow;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.editors.object.struct.BaseObjectEditPage;
import org.jkiss.utils.CommonUtils;

/**
 * "Create Shadow" dialog for a Mimer SQL databank - the databank is fixed (shown read-only),
 * Name and File are both required (unlike a databank's own optional File, {@code CREATE SHADOW}
 * always requires a file name).
 *
 * @author Mimer Information Technology
 */
public class MimerCreateDatabankShadowPage extends BaseObjectEditPage {

    private final MimerDatabankShadow shadow;

    private String name = "";
    private String file = "";

    public MimerCreateDatabankShadowPage(@NotNull MimerDatabankShadow shadow) {
        super("Create shadow");
        this.shadow = shadow;
    }

    @Override
    public DBSObject getObject() {
        return shadow;
    }

    @NotNull
    @Override
    protected Control createPageContents(@NotNull Composite parent) {
        Composite group = new Composite(parent, SWT.NONE);
        group.setLayout(new GridLayout(2, false));
        group.setLayoutData(new GridData(GridData.FILL_BOTH));

        UIUtils.createLabelText(group, "Databank", shadow.getDatabank().getName()).setEditable(false);

        Text nameText = UIUtils.createLabelText(group, "Name", "");
        nameText.addModifyListener(e -> {
            name = nameText.getText();
            updatePageState();
        });
        nameText.setFocus();

        Text fileText = UIUtils.createLabelText(group, "File", "");
        fileText.addModifyListener(e -> {
            file = fileText.getText();
            updatePageState();
        });

        return group;
    }

    @Override
    public boolean isPageComplete() {
        return !CommonUtils.isEmptyTrimmed(name) && !CommonUtils.isEmptyTrimmed(file);
    }

    /**
     * Applies the collected values to the shadow. Call after {@link #edit()} returns {@code true}.
     */
    public void applyChanges() {
        shadow.setName(name.trim());
        shadow.setFileName(file.trim());
    }
}
