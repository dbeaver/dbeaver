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
package org.jkiss.dbeaver.ui.dialogs;

import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.DBPImage;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UIUtils;

/**
 * Base dialog with title and image
 */
public class BaseTitleDialog extends TitleAreaDialog {
    private DBPImage icon;

    public BaseTitleDialog(Shell parentShell, @Nullable DBPImage icon) {
        super(parentShell);
        this.icon = icon;
    }

    public DBPImage getImage() {
        return icon;
    }

    public void setImage(DBPImage image) {
        this.icon = image;
    }

    @Override
    protected boolean isResizable() {
        return true;
    }

    @Override
    protected Composite createDialogArea(Composite parent) {
        Composite composite = new Composite(parent, SWT.NONE);
        GridLayout layout = new GridLayout();
        layout.verticalSpacing = 0;
        layout.horizontalSpacing = 0;
        composite.setLayout(layout);
        composite.setLayoutData(new GridData(GridData.FILL_BOTH));
        composite.setFont(parent.getFont());
        if (needsTopSeparator()) {
            // Build the separator line
            UIUtils.createLabelSeparator(composite, SWT.HORIZONTAL);
        }
        return composite;
    }

    private boolean needsTopSeparator() {
        return false;
    }

    @Override
    public void create() {
        super.create();
        if (icon != null) {
            getShell().setImage(DBeaverIcons.getImage(icon));
        }

    }
}
