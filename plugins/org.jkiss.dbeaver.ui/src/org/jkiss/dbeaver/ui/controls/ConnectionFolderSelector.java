/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2021 DBeaver Corp and others
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

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.jkiss.dbeaver.model.DBPDataSourceFolder;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.app.DBPDataSourceRegistry;
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.internal.UIMessages;

import java.util.ArrayList;
import java.util.List;

/**
 * General connection page (common for all connection types)
 */
public class ConnectionFolderSelector {

    private final Combo connectionFolderCombo;
    private DBPDataSourceFolder dataSourceFolder;
    private final List<DBPDataSourceFolder> connectionFolders = new ArrayList<>();

    public ConnectionFolderSelector(Composite parent) {
        UIUtils.createControlLabel(parent, UIMessages.control_label_connection_folder);

        connectionFolderCombo = new Combo(parent, SWT.BORDER | SWT.DROP_DOWN | SWT.READ_ONLY);
        GridData gd = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
        gd.widthHint = UIUtils.getFontHeight(connectionFolderCombo) * 20;
        connectionFolderCombo.setLayoutData(gd);
        connectionFolderCombo.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                dataSourceFolder = connectionFolders.get(connectionFolderCombo.getSelectionIndex());
            }
        });
    }

    public DBPDataSourceFolder getFolder() {
        return dataSourceFolder;
    }

    public void setFolder(DBPDataSourceFolder folder) {
        dataSourceFolder = folder;
        if (dataSourceFolder != null) {
            connectionFolderCombo.select(connectionFolders.indexOf(dataSourceFolder));
        } else {
            connectionFolderCombo.select(0);
        }
    }

    public boolean isEmpty() {
        return connectionFolders.isEmpty();
    }

    public void loadConnectionFolders(DBPProject project)
    {
        connectionFolderCombo.removeAll();
        connectionFolderCombo.add("");
        connectionFolders.clear();
        connectionFolders.add(null);
        DBPDataSourceRegistry registry = project == null ? null : project.getDataSourceRegistry();
        if (registry != null) {
            for (DBPDataSourceFolder folder : DBUtils.makeOrderedObjectList(registry.getRootFolders())) {
                loadConnectionFolder(0, folder);
            }
        }
    }

    private void loadConnectionFolder(int level, DBPDataSourceFolder folder) {
        StringBuilder prefix = new StringBuilder();
        for (int i = 0; i < level; i++) {
            prefix.append("   ");
        }

        connectionFolders.add(folder);
        connectionFolderCombo.add(prefix + folder.getName());
        for (DBPDataSourceFolder child : DBUtils.makeOrderedObjectList(folder.getChildren())) {
            loadConnectionFolder(level + 1, child);
        }
    }

}
