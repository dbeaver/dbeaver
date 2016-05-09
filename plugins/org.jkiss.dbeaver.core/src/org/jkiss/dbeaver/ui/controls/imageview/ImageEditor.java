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
package org.jkiss.dbeaver.ui.controls.imageview;

import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTException;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import java.io.InputStream;

/**
 * Image editor, based on image viewer.
 */
public class ImageEditor extends ImageViewer {

    private Color redColor = Display.getCurrent().getSystemColor(SWT.COLOR_RED);
    private Color blackColor = Display.getCurrent().getSystemColor(SWT.COLOR_BLACK);

    private Text messageLabel;

    public ImageEditor(Composite parent, int style)
    {
        super(parent, style);

        //setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_LIST_BACKGROUND));

        {
            // Status & toolbar
            Composite statusGroup = new Composite(this, SWT.NONE);
            GridData gd = new GridData(GridData.FILL_HORIZONTAL);
            statusGroup.setLayoutData(gd);

            GridLayout layout = new GridLayout(2, false);
            layout.verticalSpacing = 0;
            layout.horizontalSpacing = 0;
            statusGroup.setLayout(layout);

            messageLabel = new Text(statusGroup, SWT.READ_ONLY);
            messageLabel.setText(""); //$NON-NLS-1$
            gd = new GridData(GridData.FILL_HORIZONTAL);
            messageLabel.setLayoutData(gd);

            {
                ToolBarManager toolBar = new ToolBarManager(SWT.NONE);
//                gd = new GridData(GridData.HORIZONTAL_ALIGN_END);
//                toolBar.setLayoutData(gd);

                fillToolBar(toolBar);
                toolBar.createControl(statusGroup);
            }
        }
        updateActions();
    }

    @Override
    public boolean loadImage(InputStream inputStream)
    {
        super.loadImage(inputStream);
        try {
            SWTException lastError = getLastError();
            if (lastError != null) {
                messageLabel.setText(lastError.getMessage());
                messageLabel.setForeground(redColor);
                return false;
            } else {
                messageLabel.setText(getImageDescription());
                messageLabel.setForeground(blackColor);
                return true;
            }
        }
        finally {
            updateActions();
        }
    }

}
