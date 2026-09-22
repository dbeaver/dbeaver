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
import org.jkiss.dbeaver.ext.mimer.model.MimerDatabankFile;
import org.jkiss.dbeaver.ext.mimer.ui.internal.MimerUIMessages;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.editors.object.struct.BaseObjectEditPage;
import org.jkiss.utils.CommonUtils;

/**
 * {@code ALTER DATABANK ... ADD FILE} dialog for Mimer SQL - collects the new file's
 * name and (optional) size attributes.
 *
 * @author Mimer Information Technology
 */
public class MimerCreateDatabankFilePage extends BaseObjectEditPage {

    private final MimerDatabankFile file;

    private String fileName = "";
    private String fileSize = "";
    private String minSize = "";
    private String goalSize = "";
    private String maxSize = "";

    public MimerCreateDatabankFilePage(@NotNull MimerDatabankFile file) {
        super("Add databank file");
        this.file = file;
    }

    @Override
    public DBSObject getObject() {
        return file;
    }

    @NotNull
    @Override
    protected Control createPageContents(@NotNull Composite parent) {
        Composite group = new Composite(parent, SWT.NONE);
        group.setLayout(new GridLayout(2, false));
        group.setLayoutData(new GridData(GridData.FILL_BOTH));

        UIUtils.createLabelText(group, "Databank", file.getDatabank().getName()).setEditable(false);

        Text fileNameText = UIUtils.createLabelText(group, "File name", "");
        fileNameText.addModifyListener(e -> {
            fileName = fileNameText.getText();
            updatePageState();
        });
        fileNameText.setFocus();

        Text fileSizeText = UIUtils.createLabelText(group, "File size", "");
        fileSizeText.setMessage(MimerUIMessages.page_create_databank_file_file_size_placeholder);
        fileSizeText.addModifyListener(e -> fileSize = fileSizeText.getText());

        Text minSizeText = UIUtils.createLabelText(group, "Min size", "");
        minSizeText.setMessage(MimerUIMessages.page_create_databank_file_min_size_placeholder);
        minSizeText.addModifyListener(e -> minSize = minSizeText.getText());

        Text goalSizeText = UIUtils.createLabelText(group, "Goal size", "");
        goalSizeText.setMessage(MimerUIMessages.page_create_databank_file_goal_size_placeholder);
        goalSizeText.addModifyListener(e -> goalSize = goalSizeText.getText());

        Text maxSizeText = UIUtils.createLabelText(group, "Max size", "");
        maxSizeText.setMessage(MimerUIMessages.page_create_databank_file_max_size_placeholder);
        maxSizeText.addModifyListener(e -> maxSize = maxSizeText.getText());

        return group;
    }

    @Override
    public boolean isPageComplete() {
        return !CommonUtils.isEmptyTrimmed(fileName);
    }

    /**
     * Applies the collected values to the file. Call after {@link #edit()} returns {@code true}.
     */
    public void applyChanges() {
        file.setName(fileName.trim());
        file.setFileSize(fileSize.trim());
        file.setMinSize(minSize.trim());
        file.setGoalSize(goalSize.trim());
        file.setMaxSize(maxSize.trim());
    }
}
