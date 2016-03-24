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
import org.eclipse.swt.events.*;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.jkiss.dbeaver.model.DBIcon;
import org.jkiss.dbeaver.ui.DBeaverIcons;

/**
 * TextWithOpen
 */
public class TextWithOpen extends Composite
{
    private final Text text;

    public TextWithOpen(Composite parent)
    {
        super(parent, SWT.NONE);
        final GridLayout gl = new GridLayout(2, false);
        gl.marginHeight = 0;
        gl.marginWidth = 0;
        gl.verticalSpacing = 0;
        gl.horizontalSpacing = 5;
        setLayout(gl);

        text = new Text(this, SWT.BORDER);
        text.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        ToolBar toolbar = new ToolBar(this, SWT.FLAT);
        final ToolItem toolItem = new ToolItem(toolbar, SWT.NONE);
        toolItem.setImage(DBeaverIcons.getImage(DBIcon.TREE_FOLDER));
        toolItem.setToolTipText("Browse");
        toolItem.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                openBrowser();
            }
        });

        final GridData gd = new GridData(GridData.VERTICAL_ALIGN_CENTER | GridData.HORIZONTAL_ALIGN_CENTER);
        toolbar.setLayoutData(gd);

//        final Image browseImage = DBeaverIcons.getImage(DBIcon.TREE_FOLDER);
//        final Rectangle iconBounds = browseImage.getBounds();
//        Label button = new Label(this, SWT.NONE);
//        button.setCursor(getDisplay().getSystemCursor(SWT.CURSOR_HAND));
//        button.setImage(browseImage);
//        final GridData gd = new GridData(GridData.VERTICAL_ALIGN_CENTER | GridData.HORIZONTAL_ALIGN_CENTER);
//        gd.widthHint = iconBounds.width;
//        gd.heightHint = iconBounds.height;
//        button.setLayoutData(gd);
    }

    public String getText() {
        return text.getText();
    }

    public void setText(String str) {
        text.setText(str);
    }

    protected void openBrowser() {

    }

    public Text getTextControl() {
        return text;
    }
}