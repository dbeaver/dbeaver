/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2016 Serge Rieder (serge@jkiss.org)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (version 2)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
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
        folderComposite.setFolders(tabs);

        return group;
    }

}
