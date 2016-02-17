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
package org.jkiss.dbeaver.ui.dialogs;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.DBPImage;
import org.jkiss.dbeaver.ui.DBeaverIcons;

/**
 * Base dialog with title and image
 */
public class BaseDialog extends Dialog
{

    private String title;
    private DBPImage icon;

    public BaseDialog(Shell parentShell, String title, @Nullable DBPImage icon)
    {
        super(parentShell);
        this.title = title;
        this.icon = icon;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public DBPImage getImage() {
        return icon;
    }

    public void setImage(DBPImage image)
    {
        this.icon = image;
    }

    protected boolean isResizable() {
        return true;
    }

    @Override
    protected Composite createDialogArea(Composite parent) {
        return (Composite)super.createDialogArea(parent);
    }

    @Override
    public void create()
    {
        super.create();
        getShell().setText(title);
        if (icon != null) {
            getShell().setImage(DBeaverIcons.getImage(icon));
        }

    }
}
