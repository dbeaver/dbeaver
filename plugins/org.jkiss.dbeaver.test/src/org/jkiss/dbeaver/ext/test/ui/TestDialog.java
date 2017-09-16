/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
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
package org.jkiss.dbeaver.ext.test.ui;

import org.eclipse.jface.dialogs.TrayDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.jkiss.dbeaver.model.DBIcon;
import org.jkiss.dbeaver.ui.controls.folders.TabbedFolderComposite;
import org.jkiss.dbeaver.ui.controls.folders.TabbedFolderInfo;
import org.jkiss.dbeaver.ui.controls.folders.TabbedFolderPage;

public class TestDialog extends TrayDialog {

    private TabbedFolderComposite folderComposite;
    private final TabbedFolderInfo[] tabs;

    public TestDialog(Shell shell)
    {
        super(shell);

        tabs = new TabbedFolderInfo[3];
        tabs[0] = new TabbedFolderInfo("tab1", "Tab 1", DBIcon.TREE_TABLE, "Tooltip 1", false, new TabbedFolderPage() {
            @Override
            public void createControl(Composite parent) {
                new Text(parent, SWT.NONE);
            }
        });
        tabs[1] = new TabbedFolderInfo("tab2", "Tab with long-long name", DBIcon.TREE_COLUMNS, "Tooltip 2", false, new TabbedFolderPage() {

            @Override
            public void createControl(Composite parent) {
                new Label(parent, SWT.NONE);
            }
        });
        tabs[2] = new TabbedFolderInfo("tab3", "Example", DBIcon.PROJECT, "123123", false, new TabbedFolderPage() {

            @Override
            public void createControl(Composite parent) {
                new Text(parent, SWT.V_SCROLL | SWT.H_SCROLL);
            }
        });
    }

    @Override
    protected boolean isResizable() {
        return true;
    }

    @Override
    protected Control createDialogArea(Composite parent)
    {
        getShell().setText("Test");
        Composite group = (Composite) super.createDialogArea(parent);
        GridData gd = new GridData(GridData.FILL_BOTH);
        group.setLayoutData(gd);

        folderComposite = new TabbedFolderComposite(group, SWT.LEFT | SWT.MULTI);
        gd = new GridData(GridData.FILL_BOTH);
        gd.widthHint = 300;
        gd.heightHint = 300;
        folderComposite.setLayoutData(gd);
        folderComposite.setFolders("TestDialog", tabs);

        return group;
    }

}
