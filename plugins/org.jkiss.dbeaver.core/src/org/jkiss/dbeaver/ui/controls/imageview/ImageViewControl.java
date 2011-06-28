/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
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
            messageLabel.setText("");
            gd = new GridData(GridData.FILL_HORIZONTAL);
            messageLabel.setLayoutData(gd);

            {
                ToolBar toolBar = new ToolBar(statusGroup, SWT.NONE);
                gd = new GridData(GridData.HORIZONTAL_ALIGN_END);
                toolBar.setLayoutData(gd);

                itemZoomIn = UIUtils.createToolItem(toolBar, "Zoom In", DBIcon.ZOOM_IN.getImage(), new ImageActionDelegate(this, ImageActionDelegate.TOOLBAR_ZOOMIN));
                itemZoomOut = UIUtils.createToolItem(toolBar, "Zoom Out", DBIcon.ZOOM_OUT.getImage(), new ImageActionDelegate(this, ImageActionDelegate.TOOLBAR_ZOOMOUT));
                itemRotate = UIUtils.createToolItem(toolBar, "Rotate", DBIcon.ROTATE_LEFT.getImage(), new ImageActionDelegate(this, ImageActionDelegate.TOOLBAR_ROTATE));
                itemFit = UIUtils.createToolItem(toolBar, "Fit Window", DBIcon.FIT_WINDOW.getImage(), new ImageActionDelegate(this, ImageActionDelegate.TOOLBAR_FIT));
                itemOriginal = UIUtils.createToolItem(toolBar, "Original Size", DBIcon.ORIGINAL_SIZE.getImage(), new ImageActionDelegate(this, ImageActionDelegate.TOOLBAR_ORIGINAL));
            }
        }
        updateActions();

        // Add DND support
        Transfer[] types = new Transfer[] {ImageTransfer.getInstance()};
        int operations = DND.DROP_COPY;

        final DragSource source = new DragSource(canvas, operations);
        source.setTransfer(types);
        source.addDragListener (new DragSourceListener() {
            public void dragStart(DragSourceEvent event) {
            }
            public void dragSetData (DragSourceEvent event) {
                if (canvas.getImageData() != null) {
                    event.data = canvas.getImageData();
                } else {
                    event.data = null;
                }
            }
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
                    getImageType(imageData.type) + " " + imageData.width + "x" + imageData.height + "x" + imageData.depth +
                    "  "/* + imageData.data.length + " bytes"*/);
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
        case SWT.IMAGE_BMP: return "BMP";
        case SWT.IMAGE_BMP_RLE: return "BMP RLE";
        case SWT.IMAGE_GIF: return "GIF";
        case SWT.IMAGE_ICO: return "ICO";
        case SWT.IMAGE_JPEG: return "JPEG";
        case SWT.IMAGE_PNG: return "PNG";
        case SWT.IMAGE_TIFF: return "TIFF";
        case SWT.IMAGE_OS2_BMP: return "OS2 BMP";
        default: return "UNKNOWN";
        }
    }
}
