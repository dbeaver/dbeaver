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
package org.jkiss.dbeaver.ui.controls;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.FileDialog;
import org.jkiss.dbeaver.ui.dialogs.DialogUtils;

/**
 * TextWithOpen
 */
public class TextWithOpenFile extends TextWithOpen
{
    private String title;
    private String[] filterExt;
    public TextWithOpenFile(Composite parent, String title, String[] filterExt) {
        super(parent);
        this.title = title;
        this.filterExt = filterExt;
    }

    protected void openBrowser() {
        FileDialog fd = new FileDialog(getShell(), SWT.OPEN | SWT.SINGLE);
        fd.setText(title);
        fd.setFilterExtensions(filterExt);
        String selected = DialogUtils.openFileDialog(fd);
        if (selected != null) {
            setText(selected);
        }
    }

}