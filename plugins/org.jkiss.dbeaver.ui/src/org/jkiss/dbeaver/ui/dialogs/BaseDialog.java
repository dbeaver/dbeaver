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

    @Override
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
