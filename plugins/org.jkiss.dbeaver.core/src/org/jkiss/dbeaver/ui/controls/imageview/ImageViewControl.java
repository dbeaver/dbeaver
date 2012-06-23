/*
 * Copyright (C) 2010-2012 Serge Rieder
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
import org.eclipse.swt.dnd.*;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.ui.DBIcon;
import org.jkiss.dbeaver.ui.UIUtils;

import java.io.InputStream;

/**
 * A scrollable image canvas that extends org.eclipse.swt.graphics.Canvas.
 * <p/>
 * It requires Eclipse (version >= 2.1) on Win32/win32; Linux/gtk; MacOSX/carbon.
 * <p/>
 * This implementation using the pure SWT, no UI AWT package is used. For 
 * convenience, I put everything into one class. However, the best way to
 * implement this is to use inheritance to create multiple hierarchies.
 * 
 * @author Chengdong Li: cli4@uky.edu
 */
public class ImageViewControl extends Composite {

    private Color redColor = Display.getCurrent().getSystemColor(SWT.COLOR_RED);
    private Color blackColor = Display.getCurrent().getSystemColor(SWT.COLOR_BLACK);

    private ImageViewCanvas canvas;
    private SWTException lastError;
    private Label messageLabel;

    private ToolItem itemZoomIn;
    private ToolItem itemZoomOut;
    private ToolItem itemRotate;
    private ToolItem itemFit;
    private ToolItem itemOriginal;

    public ImageViewControl(Composite parent, int style)
    {
        super(parent, style);

        setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_LIST_BACKGROUND));
        GridLayout layout = new GridLayout(1, false);
        layout.verticalSpacing = 3;
        layout.horizontalSpacing = 3;
        setLayout(layout);

        canvas = new ImageViewCanvas(this, SWT.NONE);
        GridData gd = new GridData(GridData.FILL_BOTH);
        canvas.setLayoutData(gd);

        {
            // Status & toolbar
            Composite statusGroup = new Composite(this, SWT.BORDER);
            gd = new GridData(GridData.FILL_HORIZONTAL);
            statusGroup.setLayoutData(gd);

            layout = new GridLayout(2, false);
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

        // Add DND support
        Transfer[] types = new Transfer[] {ImageTransfer.getInstance()};
        int operations = DND.DROP_COPY;

        final DragSource source = new DragSource(canvas, operations);
        source.setTransfer(types);
        source.addDragListener (new DragSourceListener() {
            @Override
            public void dragStart(DragSourceEvent event) {
            }
            @Override
            public void dragSetData (DragSourceEvent event) {
                if (canvas.getImageData() != null) {
                    event.data = canvas.getImageData();
                } else {
                    event.data = null;
                }
            }
            @Override
            public void dragFinished(DragSourceEvent event) {
            }
        });

    }

    @Override
    public void dispose() {
        if (canvas != null) {
            canvas.dispose();
            canvas = null;
        }
        super.dispose();
    }

    private void updateActions()
    {
        boolean hasImage = canvas.getSourceImage() != null;
        itemZoomIn.setEnabled(hasImage);
        itemZoomOut.setEnabled(hasImage);
        itemRotate.setEnabled(hasImage);
        itemFit.setEnabled(hasImage);
        itemOriginal.setEnabled(hasImage);
    }

    ImageViewCanvas getCanvas()
    {
        return canvas;
    }

    public boolean loadImage(InputStream inputStream)
    {
        canvas.loadImage(inputStream);
        try {
            lastError = canvas.getError();
            if (lastError != null) {
                messageLabel.setText(lastError.getMessage());
                messageLabel.setForeground(redColor);
                return false;
            } else {
                ImageData imageData = canvas.getImageData();

                messageLabel.setText(
                    getImageType(imageData.type) + " " + imageData.width + "x" + imageData.height + "x" + imageData.depth + //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                    "  "/* + imageData.data.length + " bytes"*/); //$NON-NLS-1$
                messageLabel.setForeground(blackColor);
                return true;
            }
        }
        finally {
            updateActions();
        }
    }

    public SWTException getLastError()
    {
        return lastError;
    }

    public static String getImageType(int type)
    {
        switch (type) {
        case SWT.IMAGE_BMP: return "BMP"; //$NON-NLS-1$
        case SWT.IMAGE_BMP_RLE: return "BMP RLE"; //$NON-NLS-1$
        case SWT.IMAGE_GIF: return "GIF"; //$NON-NLS-1$
        case SWT.IMAGE_ICO: return "ICO"; //$NON-NLS-1$
        case SWT.IMAGE_JPEG: return "JPEG"; //$NON-NLS-1$
        case SWT.IMAGE_PNG: return "PNG"; //$NON-NLS-1$
        case SWT.IMAGE_TIFF: return "TIFF"; //$NON-NLS-1$
        case SWT.IMAGE_OS2_BMP: return "OS2 BMP"; //$NON-NLS-1$
        default: return "UNKNOWN"; //$NON-NLS-1$
        }
    }
}
