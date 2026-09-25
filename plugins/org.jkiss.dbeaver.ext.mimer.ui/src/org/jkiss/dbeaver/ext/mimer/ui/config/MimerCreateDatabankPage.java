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
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Text;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.ext.mimer.model.MimerDatabank;
import org.jkiss.dbeaver.ext.mimer.ui.internal.MimerUIMessages;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.editors.object.struct.BaseObjectEditPage;
import org.jkiss.utils.CommonUtils;

/**
 * "Create Databank" dialog for Mimer SQL - all attributes are collected before
 * the databank object is created (Mimer SQL has no ALTER DATABANK that can change them
 * afterward, other than extending a file or its OPTION).
 *
 * @author Mimer Information Technology
 */
public class MimerCreateDatabankPage extends BaseObjectEditPage {

    private static final String[] OPTIONS = {"TRANSACTION", "LOG", "WORK"};

    private final MimerDatabank databank;

    private String name = "";
    private String file = "";
    private String fileSize = "";
    private String minSize = "";
    private String goalSize = "";
    private String maxSize = "";
    private boolean removable;
    private String option = "TRANSACTION";

    public MimerCreateDatabankPage(@NotNull MimerDatabank databank) {
        super("Create databank");
        this.databank = databank;
    }

    @Override
    public DBSObject getObject() {
        return databank;
    }

    @NotNull
    @Override
    protected Control createPageContents(@NotNull Composite parent) {
        Composite group = new Composite(parent, SWT.NONE);
        group.setLayout(new GridLayout(2, false));
        group.setLayoutData(new GridData(GridData.FILL_BOTH));

        Text nameText = UIUtils.createLabelText(group, "Name", "");
        // File is optional - Mimer SQL defaults it to "<databank name>.dbf" server-side when
        // omitted, so there's no need to pre-fill or suggest one here.
        Text fileText = UIUtils.createLabelText(group, "File", "");
        nameText.addModifyListener(e -> {
            name = nameText.getText();
            updatePageState();
        });
        nameText.setFocus();
        fileText.addModifyListener(e -> file = fileText.getText());

        Text fileSizeText = UIUtils.createLabelText(group, "File size", "");
        fileSizeText.setMessage(MimerUIMessages.page_create_databank_file_size_placeholder);
        fileSizeText.addModifyListener(e -> fileSize = fileSizeText.getText());

        Text minSizeText = UIUtils.createLabelText(group, "Min size", "");
        minSizeText.setMessage(MimerUIMessages.page_create_databank_min_size_placeholder);
        minSizeText.addModifyListener(e -> minSize = minSizeText.getText());

        Text goalSizeText = UIUtils.createLabelText(group, "Goal size", "");
        goalSizeText.setMessage(MimerUIMessages.page_create_databank_goal_size_placeholder);
        goalSizeText.addModifyListener(e -> goalSize = goalSizeText.getText());

        Text maxSizeText = UIUtils.createLabelText(group, "Max size", "");
        maxSizeText.setMessage(MimerUIMessages.page_create_databank_max_size_placeholder);
        maxSizeText.addModifyListener(e -> maxSize = maxSizeText.getText());

        Combo optionCombo = UIUtils.createLabelCombo(group, "Option", SWT.DROP_DOWN | SWT.READ_ONLY);
        for (String o : OPTIONS) {
            optionCombo.add(o);
        }
        optionCombo.setText(option);
        optionCombo.addModifyListener(e -> option = optionCombo.getText());

        Button removableCheck = UIUtils.createCheckbox(group, "Removable", null, false, 2);
        removableCheck.addSelectionListener(new org.eclipse.swt.events.SelectionAdapter() {
            @Override
            public void widgetSelected(org.eclipse.swt.events.SelectionEvent e) {
                removable = removableCheck.getSelection();
            }
        });

        return group;
    }

    @Override
    public boolean isPageComplete() {
        return !CommonUtils.isEmptyTrimmed(name);
    }

    /**
     * Applies the collected values to the databank. Call after {@link #edit()} returns {@code true}.
     */
    public void applyChanges() {
        databank.setName(name.trim());
        databank.setFile(file.trim());
        databank.setFileSize(fileSize.trim());
        databank.setMinSize(minSize.trim());
        databank.setGoalSize(goalSize.trim());
        databank.setMaxSize(maxSize.trim());
        databank.setRemovable(removable);
        databank.setType(option);
    }
}
