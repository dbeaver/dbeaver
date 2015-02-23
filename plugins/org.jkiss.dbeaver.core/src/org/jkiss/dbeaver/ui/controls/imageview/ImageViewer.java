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
package org.jkiss.dbeaver.ui.controls.imageview;

import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTException;
import org.eclipse.swt.dnd.*;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.ui.DBIcon;
import org.jkiss.dbeaver.ui.UIUtils;

import java.io.InputStream;

/**
 * Image viewer control
 */
public class ImageViewer extends Composite {

    private ImageViewCanvas canvas;
    private ToolItem itemZoomIn;
    private ToolItem itemZoomOut;
    private ToolItem itemRotate;
    private ToolItem itemFit;
    private ToolItem itemOriginal;

    public ImageViewer(Composite parent, int style)
    {
        super(parent, style);

        GridLayout layout = new GridLayout(1, false);
        layout.marginHeight = 0;
        layout.marginWidth = 0;
        layout.verticalSpacing = 0;
        layout.horizontalSpacing = 0;
        setLayout(layout);

        canvas = new ImageViewCanvas(this, SWT.NONE);
        GridData gd = new GridData(GridData.FILL_BOTH);
        canvas.setLayoutData(gd);

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

    ImageViewCanvas getCanvas()
    {
        return canvas;
    }

    public boolean loadImage(InputStream inputStream)
    {
        canvas.loadImage(inputStream);
        return canvas.getError() == null;
    }

    public SWTException getLastError()
    {
        return canvas.getError();
    }

    public String getImageDescription()
    {
        ImageData imageData = getCanvas().getImageData();

        return getImageType(imageData.type) + " " +
            imageData.width + "x" + imageData.height + "x" + imageData.depth + //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                "  "; //$NON-NLS-1$
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

    public void updateActions()
    {
        boolean hasImage = getCanvas().getSourceImage() != null;
        itemZoomIn.setEnabled(hasImage);
        itemZoomOut.setEnabled(hasImage);
        itemRotate.setEnabled(hasImage);
        itemFit.setEnabled(hasImage);
        itemOriginal.setEnabled(hasImage);
    }

    public void fillToolBar(ToolBar toolBar) {
        itemZoomIn = UIUtils.createToolItem(toolBar, CoreMessages.controls_imageview_zoom_in, DBIcon.ZOOM_IN.getImage(), new ImageActionDelegate(this, ImageActionDelegate.TOOLBAR_ZOOMIN));
        itemZoomOut = UIUtils.createToolItem(toolBar, CoreMessages.controls_imageview_zoom_out, DBIcon.ZOOM_OUT.getImage(), new ImageActionDelegate(this, ImageActionDelegate.TOOLBAR_ZOOMOUT));
        itemRotate = UIUtils.createToolItem(toolBar, CoreMessages.controls_imageview_rotate, DBIcon.ROTATE_LEFT.getImage(), new ImageActionDelegate(this, ImageActionDelegate.TOOLBAR_ROTATE));
        itemFit = UIUtils.createToolItem(toolBar, CoreMessages.controls_imageview_fit_window, DBIcon.FIT_WINDOW.getImage(), new ImageActionDelegate(this, ImageActionDelegate.TOOLBAR_FIT));
        itemOriginal = UIUtils.createToolItem(toolBar, CoreMessages.controls_imageview_original_size, DBIcon.ORIGINAL_SIZE.getImage(), new ImageActionDelegate(this, ImageActionDelegate.TOOLBAR_ORIGINAL));
    }

}
