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
import org.jkiss.dbeaver.ext.mimer.model.MimerLibrary;
import org.jkiss.dbeaver.ext.mimer.ui.internal.MimerUIMessages;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.editors.object.struct.BaseObjectEditPage;
import org.jkiss.utils.CommonUtils;

/**
 * "Create Library" dialog: Name, File (the absolute path to the library file on the server -
 * must already exist there, {@code CREATE LIBRARY} doesn't create it), Language (a combo
 * defaulting to {@code CLR}, but left free-typeable since the server treats it as a plain
 * keyword - other languages may follow in a future server version).
 *
 * @author Mimer Information Technology
 */
public class MimerCreateLibraryPage extends BaseObjectEditPage {

    private final MimerLibrary library;

    private String name = "";
    private String fileName = "";
    private String language = "CLR";

    public MimerCreateLibraryPage(@NotNull MimerLibrary library) {
        super("Create library");
        this.library = library;
    }

    @Override
    public DBSObject getObject() {
        return library;
    }

    @NotNull
    @Override
    protected Control createPageContents(@NotNull Composite parent) {
        Composite group = new Composite(parent, SWT.NONE);
        group.setLayout(new GridLayout(2, false));
        group.setLayoutData(new GridData(GridData.FILL_BOTH));

        Text nameText = UIUtils.createLabelText(group, "Name", "");
        nameText.addModifyListener(e -> {
            name = nameText.getText();
            updatePageState();
        });
        nameText.setFocus();

        Text fileText = UIUtils.createLabelText(group, "File", "");
        fileText.setMessage(MimerUIMessages.page_create_library_file_placeholder);
        fileText.addModifyListener(e -> {
            fileName = fileText.getText();
            updatePageState();
        });

        Combo languageCombo = UIUtils.createLabelCombo(group, "Language", SWT.DROP_DOWN);
        languageCombo.add("CLR");
        languageCombo.setText(language);
        languageCombo.addModifyListener(e -> {
            language = languageCombo.getText();
            updatePageState();
        });

        return group;
    }

    @Override
    public boolean isPageComplete() {
        return !CommonUtils.isEmptyTrimmed(name) && !CommonUtils.isEmptyTrimmed(fileName) && !CommonUtils.isEmptyTrimmed(language);
    }

    /**
     * Applies the collected values to the library. Call after {@link #edit()} returns {@code true}.
     */
    public void applyChanges() {
        library.setName(name.trim());
        library.setFileName(fileName.trim());
        library.setLanguage(language.trim());
    }
}
