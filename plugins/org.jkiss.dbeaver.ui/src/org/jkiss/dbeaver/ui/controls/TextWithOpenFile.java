/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
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
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.FileDialog;
import org.jkiss.dbeaver.ui.dialogs.DialogUtils;

/**
 * TextWithOpen
 */
public class TextWithOpenFile extends TextWithOpen
{
    private String title;
    private String[] filterExt;
    private boolean openFolder = false;

    public TextWithOpenFile(Composite parent, String title, String[] filterExt) {
        super(parent);
        this.title = title;
        this.filterExt = filterExt;
    }

    public void setOpenFolder(boolean openFolder) {
        this.openFolder = openFolder;
    }

    protected void openBrowser() {
        String selected;
        if (openFolder) {
            DirectoryDialog fd = new DirectoryDialog(getShell(), SWT.OPEN | SWT.SINGLE);
            if (title != null) {
                fd.setText(title);
            }
            selected = fd.open();
        } else {
            FileDialog fd = new FileDialog(getShell(), SWT.OPEN | SWT.SINGLE);
            fd.setText(title);
            fd.setFilterExtensions(filterExt);
            selected = DialogUtils.openFileDialog(fd);
        }
        if (selected != null) {
            setText(selected);
        }
    }

}