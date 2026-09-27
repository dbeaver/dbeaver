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
package org.jkiss.dbeaver.ui.controls;

import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.DBPDataSourceFolder;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.app.DBPDataSourceRegistry;
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.internal.UIMessages;

import java.util.IdentityHashMap;
import java.util.Map;

/**
 * General connection page (common for all connection types)
 */
public class ConnectionFolderSelector {

    private final CSmartCombo<DBPDataSourceFolder> connectionFolderCombo;
    private final Map<DBPDataSourceFolder, Integer> folderLevels = new IdentityHashMap<>();
    private DBPDataSourceFolder dataSourceFolder;

    public ConnectionFolderSelector(@NotNull Composite parent) {
        UIUtils.createControlLabel(parent, UIMessages.control_label_connection_folder);

        connectionFolderCombo = new CSmartCombo<>(
            parent,
            SWT.BORDER | SWT.DROP_DOWN | SWT.READ_ONLY,
            new LabelProvider() {
                @Override
                public @NotNull String getText(@Nullable Object element) {
                    if (!(element instanceof DBPDataSourceFolder folder)) {
                        return "";
                    }
                    StringBuilder label = new StringBuilder(folder.getName());
                    for (int i = 0; i < folderLevels.getOrDefault(folder, 0); i++) {
                        label.insert(0, "   ");
                    }
                    return label.toString();
                }
            }
        );
        GridData gd = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
        gd.widthHint = UIUtils.getFontHeight(connectionFolderCombo) * 20;
        connectionFolderCombo.setLayoutData(gd);
        connectionFolderCombo.addSelectionListener(SelectionListener.widgetSelectedAdapter(e ->
            dataSourceFolder = connectionFolderCombo.getSelectedItem()));
    }

    public @Nullable DBPDataSourceFolder getFolder() {
        return dataSourceFolder;
    }

    public void setFolder(@Nullable DBPDataSourceFolder folder) {
        dataSourceFolder = folder;
        connectionFolderCombo.select(dataSourceFolder);
    }

    public boolean isEmpty() {
        return connectionFolderCombo.getItemCount() == 0;
    }

    public void loadConnectionFolders(@Nullable DBPProject project) {
        connectionFolderCombo.removeAll();
        connectionFolderCombo.addItem(null);
        folderLevels.clear();
        DBPDataSourceRegistry registry = project == null ? null : project.getDataSourceRegistry();
        if (registry != null) {
            for (DBPDataSourceFolder folder : DBUtils.makeOrderedObjectList(registry.getRootFolders())) {
                loadConnectionFolder(folder, 0);
            }
        }
    }

    private void loadConnectionFolder(@NotNull DBPDataSourceFolder folder, int level) {
        folderLevels.put(folder, level);
        connectionFolderCombo.addItem(folder);
        for (DBPDataSourceFolder child : DBUtils.makeOrderedObjectList(folder.getChildren())) {
            loadConnectionFolder(child, level + 1);
        }
    }

}
