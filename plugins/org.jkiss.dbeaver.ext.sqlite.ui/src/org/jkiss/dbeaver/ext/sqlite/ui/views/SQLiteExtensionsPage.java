/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2022 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.sqlite.ui.views;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.jkiss.dbeaver.ext.sqlite.SQLiteConstants;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.ui.UIIcon;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.connection.ConnectionPageAbstract;
import org.jkiss.utils.CommonUtils;

import java.io.File;

public class SQLiteExtensionsPage extends ConnectionPageAbstract {
    private List extensionsList;

    public SQLiteExtensionsPage() {
        setTitle("SQLite Extensions");
        setDescription("SQLite extension management");
    }

    @Override
    public void createControl(Composite parent) {
        final Composite composite = new Composite(parent, SWT.NONE);
        composite.setLayout(new GridLayout(2, false));
        composite.setLayoutData(new GridData(GridData.FILL_BOTH));

        extensionsList = new List(composite, SWT.BORDER | SWT.SINGLE | SWT.H_SCROLL | SWT.V_SCROLL);
        extensionsList.setLayoutData(new GridData(GridData.FILL_BOTH));

        final ToolBar toolbar = new ToolBar(composite, SWT.VERTICAL);
        toolbar.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_BEGINNING));
        UIUtils.createToolItem(toolbar, "Add", UIIcon.ADD, new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                final FileDialog dialog = new FileDialog(getShell(), SWT.OPEN | SWT.MULTI);
                dialog.setText("Choose SQLite extensions");
                dialog.setFilterExtensions(new String[]{"*.dll;*.dylib;*.so"});
                dialog.setFilterNames(new String[]{"SQLite Extension"});

                if (dialog.open() != null) {
                    for (String name : dialog.getFileNames()) {
                        final String path = dialog.getFilterPath() + File.separator + name;

                        if (extensionsList.indexOf(path) < 0) {
                            extensionsList.add(path);
                        }
                    }
                }
            }
        });
        final ToolItem removeItem = UIUtils.createToolItem(toolbar, "Remove", UIIcon.DELETE, new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                final int index = extensionsList.getSelectionIndex();
                extensionsList.remove(index);
                extensionsList.select(CommonUtils.clamp(index, 0, extensionsList.getItemCount() - 1));
                extensionsList.notifyListeners(SWT.Selection, new Event());
            }
        });

        removeItem.setEnabled(false);
        extensionsList.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                removeItem.setEnabled(extensionsList.getItemCount() > 0);
            }
        });

        UIUtils.createInfoLabel(composite, "Extensions must match your OS and architecture.", GridData.FILL_HORIZONTAL, 2);

        loadSettings();

        setControl(composite);
    }

    @Override
    public void loadSettings() {
        super.loadSettings();

        final DBPConnectionConfiguration configuration = site.getActiveDataSource().getConnectionConfiguration();
        final String extensions = configuration.getProviderProperty(SQLiteConstants.PROP_EXTENSIONS);

        if (CommonUtils.isNotEmpty(extensions)) {
            for (String extension : extensions.split(File.pathSeparator)) {
                extensionsList.add(extension);
            }
        }
    }

    @Override
    public void saveSettings(DBPDataSourceContainer dataSource) {
        super.saveSettings(dataSource);

        final DBPConnectionConfiguration configuration = dataSource.getConnectionConfiguration();
        final String extensions = String.join(File.pathSeparator, extensionsList.getItems());

        configuration.setProviderProperty(SQLiteConstants.PROP_EXTENSIONS, extensions);
    }

    @Override
    public boolean isComplete() {
        return true;
    }
}
