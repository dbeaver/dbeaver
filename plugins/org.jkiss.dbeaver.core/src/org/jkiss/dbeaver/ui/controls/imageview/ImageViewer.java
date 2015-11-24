/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2015 Serge Rieder (serge@jkiss.org)
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

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IContributionManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTException;
import org.eclipse.swt.dnd.*;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.ui.UIIcon;

import java.io.InputStream;

/**
 * Image viewer control
 */
public class ImageViewer extends Composite {

    private ImageViewCanvas canvas;
    private IAction itemZoomIn;
    private IAction itemZoomOut;
    private IAction itemRotate;
    private IAction itemFit;
    private IAction itemOriginal;

    public ImageViewer(Composite parent, int style)
    {
        super(parent, style);

        GridLayout gl = new GridLayout(1, false);
        gl.horizontalSpacing = 0;
        gl.verticalSpacing = 0;
        gl.marginHeight = 0;
        gl.marginWidth = 0;
        setLayout(gl);

        canvas = new ImageViewCanvas(this, SWT.NONE);
        canvas.setLayoutData(new GridData(GridData.FILL_BOTH));

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

    public boolean clearImage()
    {
        canvas.loadImage(null);
        return true;
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

    public void fillToolBar(IContributionManager toolBar) {
        toolBar.add(itemZoomIn = new ImageActionDelegate(this, ImageActionDelegate.TOOLBAR_ZOOMIN, CoreMessages.controls_imageview_zoom_in, UIIcon.ZOOM_IN));
        toolBar.add(itemZoomOut = new ImageActionDelegate(this, ImageActionDelegate.TOOLBAR_ZOOMOUT, CoreMessages.controls_imageview_zoom_out, UIIcon.ZOOM_OUT));
        toolBar.add(itemRotate = new ImageActionDelegate(this, ImageActionDelegate.TOOLBAR_ROTATE, CoreMessages.controls_imageview_rotate, UIIcon.ROTATE_LEFT));
        toolBar.add(itemFit = new ImageActionDelegate(this, ImageActionDelegate.TOOLBAR_FIT, CoreMessages.controls_imageview_fit_window, UIIcon.FIT_WINDOW));
        toolBar.add(itemOriginal = new ImageActionDelegate(this, ImageActionDelegate.TOOLBAR_ORIGINAL, CoreMessages.controls_imageview_original_size, UIIcon.ORIGINAL_SIZE));
    }

}
