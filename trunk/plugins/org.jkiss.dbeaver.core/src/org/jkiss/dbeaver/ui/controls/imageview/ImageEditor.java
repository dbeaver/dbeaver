/*
 * Copyright (C) 2010-2013 Serge Rieder
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
package org.jkiss.dbeaver.ui.controls.imageview;

import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTException;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.ui.DBIcon;
import org.jkiss.dbeaver.ui.UIUtils;

import java.io.InputStream;

/**
 * Image editor, based on image viewer.
 */
public class ImageEditor extends ImageViewer {

    private Color redColor = Display.getCurrent().getSystemColor(SWT.COLOR_RED);
    private Color blackColor = Display.getCurrent().getSystemColor(SWT.COLOR_BLACK);

    private Label messageLabel;

    private ToolItem itemZoomIn;
    private ToolItem itemZoomOut;
    private ToolItem itemRotate;
    private ToolItem itemFit;
    private ToolItem itemOriginal;

    public ImageEditor(Composite parent, int style)
    {
        super(parent, style);

        setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_LIST_BACKGROUND));

        {
            // Status & toolbar
            Composite statusGroup = new Composite(this, SWT.NONE);
            GridData gd = new GridData(GridData.FILL_HORIZONTAL);
            statusGroup.setLayoutData(gd);

            GridLayout layout = new GridLayout(2, false);
            layout.verticalSpacing = 0;
            layout.horizontalSpacing = 0;
            statusGroup.setLayout(layout);

            messageLabel = new Label(statusGroup, SWT.NONE);
            messageLabel.setText(""); //$NON-NLS-1$
            gd = new GridData(GridData.FILL_HORIZONTAL);
            messageLabel.setLayoutData(gd);

            {
                ToolBar toolBar = new ToolBar(statusGroup, SWT.NONE);
                gd = new GridData(GridData.HORIZONTAL_ALIGN_END);
                toolBar.setLayoutData(gd);

                itemZoomIn = UIUtils.createToolItem(toolBar, CoreMessages.controls_imageview_zoom_in, DBIcon.ZOOM_IN.getImage(), new ImageActionDelegate(this, ImageActionDelegate.TOOLBAR_ZOOMIN));
                itemZoomOut = UIUtils.createToolItem(toolBar, CoreMessages.controls_imageview_zoom_out, DBIcon.ZOOM_OUT.getImage(), new ImageActionDelegate(this, ImageActionDelegate.TOOLBAR_ZOOMOUT));
                itemRotate = UIUtils.createToolItem(toolBar, CoreMessages.controls_imageview_rotate, DBIcon.ROTATE_LEFT.getImage(), new ImageActionDelegate(this, ImageActionDelegate.TOOLBAR_ROTATE));
                itemFit = UIUtils.createToolItem(toolBar, CoreMessages.controls_imageview_fit_window, DBIcon.FIT_WINDOW.getImage(), new ImageActionDelegate(this, ImageActionDelegate.TOOLBAR_FIT));
                itemOriginal = UIUtils.createToolItem(toolBar, CoreMessages.controls_imageview_original_size, DBIcon.ORIGINAL_SIZE.getImage(), new ImageActionDelegate(this, ImageActionDelegate.TOOLBAR_ORIGINAL));
            }
        }
        updateActions();
    }

    private void updateActions()
    {
        boolean hasImage = getCanvas().getSourceImage() != null;
        itemZoomIn.setEnabled(hasImage);
        itemZoomOut.setEnabled(hasImage);
        itemRotate.setEnabled(hasImage);
        itemFit.setEnabled(hasImage);
        itemOriginal.setEnabled(hasImage);
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
