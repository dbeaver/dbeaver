/*
 * Copyright (C) 2010-2015 Serge Rieder
 * serge@jkiss.org
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package org.jkiss.dbeaver.ext.test.ui;

import org.eclipse.jface.dialogs.TrayDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.jkiss.dbeaver.ui.DBIcon;
import org.jkiss.dbeaver.ui.controls.folders.FolderComposite;
import org.jkiss.dbeaver.ui.controls.folders.FolderInfo;
import org.jkiss.dbeaver.ui.controls.folders.FolderPage;

public class TestDialog extends TrayDialog {

    private FolderComposite folderComposite;
    private final FolderInfo[] tabs;

    public TestDialog(Shell shell)
    {
        super(shell);

        tabs = new FolderInfo[3];
        tabs[0] = new FolderInfo("tab1", "Tab 1", DBIcon.TREE_TABLE.getImage(), "Tooltip 1", false, new FolderPage() {
            @Override
            public void createControl(Composite parent) {
                new Text(parent, SWT.NONE);
            }
        });
        tabs[1] = new FolderInfo("tab2", "Tab with long-long name", DBIcon.TREE_COLUMNS.getImage(), "Tooltip 2", false, new FolderPage() {

            @Override
            public void createControl(Composite parent) {
                new Label(parent, SWT.NONE);
            }
        });
        tabs[2] = new FolderInfo("tab3", "Example", DBIcon.PROJECT.getImage(), "123123", false, new FolderPage() {

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

        folderComposite = new FolderComposite(group, SWT.LEFT | SWT.MULTI);
        gd = new GridData(GridData.FILL_BOTH);
        gd.widthHint = 300;
        gd.heightHint = 300;
        folderComposite.setLayoutData(gd);
        folderComposite.setFolders(tabs);

        return group;
    }

}
