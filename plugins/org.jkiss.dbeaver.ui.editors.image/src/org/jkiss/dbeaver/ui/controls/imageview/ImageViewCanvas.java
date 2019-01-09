/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
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
package org.jkiss.dbeaver.ui.controls.imageview;

import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTException;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.ScrollBar;

import java.awt.geom.AffineTransform;
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
class ImageViewCanvas extends Canvas {
    /* zooming rates in x and y direction are equal.*/
    final float ZOOMIN_RATE = 1.1f; /* zoomin rate */
    final float ZOOMOUT_RATE = 0.9f; /* zoomout rate */
    private Image sourceImage; /* original image */
    private ImageData imageData;
    private AffineTransform transform = new AffineTransform();
    private SWTException error;

    public ImageViewCanvas(final Composite parent) {
        this(parent, SWT.NULL);
    }

    /**
     * Constructor for ScrollableCanvas.
     *
     * @param parent the parent of this control.
     * @param style  the style of this control.
     */
    public ImageViewCanvas(final Composite parent, int style) {
        super(parent, style | SWT.V_SCROLL | SWT.H_SCROLL);
        addControlListener(new ControlAdapter() { /* resize listener. */
            @Override
            public void controlResized(ControlEvent event) {
                syncScrollBars();
            }
        });
        addPaintListener(new PaintListener() { /* paint listener. */
            @Override
            public void paintControl(final PaintEvent event) {
                paint(event.gc);
            }
        });
        addDisposeListener(new DisposeListener() {
            @Override
            public void widgetDisposed(DisposeEvent e) {
                if (sourceImage != null && !sourceImage.isDisposed()) {
                    sourceImage.dispose();
                }
            }
        });
        initScrollBars();
    }

    /* Paint function */
    private void paint(GC gc) {
        Rectangle clientRect = getClientArea(); /* Canvas' painting area */
        if (sourceImage != null) {
            Rectangle imageRect =
                    ImageViewUtil.inverseTransformRect(transform, clientRect);
            int gap = 2; /* find a better start point to render */
            imageRect.x -= gap;
            imageRect.y -= gap;
            imageRect.width += 2 * gap;
            imageRect.height += 2 * gap;

            Rectangle imageBound = sourceImage.getBounds();
            imageRect = imageRect.intersection(imageBound);
            Rectangle destRect = ImageViewUtil.transformRect(transform, imageRect);

            gc.setBackground(getParent().getBackground());
            gc.fillRectangle(clientRect);
            gc.drawImage(
                    sourceImage,
                    imageRect.x,
                    imageRect.y,
                    imageRect.width,
                    imageRect.height,
                    destRect.x,
                    destRect.y,
                    destRect.width,
                    destRect.height);
        } else {
            gc.setClipping(clientRect);
            gc.fillRectangle(clientRect);
            initScrollBars();
        }
    }

    /* Initalize the scrollbar and register listeners. */
    private void initScrollBars() {
        ScrollBar horizontal = getHorizontalBar();
        horizontal.setEnabled(false);
        horizontal.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {
                scrollHorizontally((ScrollBar) event.widget);
            }
        });
        ScrollBar vertical = getVerticalBar();
        vertical.setEnabled(false);
        vertical.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {
                scrollVertically((ScrollBar) event.widget);
            }
        });
    }

    /* Scroll horizontally */
    private void scrollHorizontally(ScrollBar scrollBar) {
        if (sourceImage == null)
            return;

        AffineTransform af = transform;
        double tx = af.getTranslateX();
        double select = -scrollBar.getSelection();
        af.preConcatenate(AffineTransform.getTranslateInstance(select - tx, 0));
        transform = af;
        syncScrollBars();
    }

    /* Scroll vertically */
    private void scrollVertically(ScrollBar scrollBar) {
        if (sourceImage == null)
            return;

        AffineTransform af = transform;
        double ty = af.getTranslateY();
        double select = -scrollBar.getSelection();
        af.preConcatenate(AffineTransform.getTranslateInstance(0, select - ty));
        transform = af;
        syncScrollBars();
    }

    /**
     * Source image getter.
     *
     * @return sourceImage.
     */
    public Image getSourceImage() {
        return sourceImage;
    }

    /**
     * Synchronize the scrollbar with the image. If the transform is out
     * of range, it will correct it. This function considers only following
     * factors :<b> transform, image size, client area</b>.
     */
    public void syncScrollBars() {
        if (sourceImage == null) {
            redraw();
            return;
        }

        AffineTransform af = transform;
        double sx = af.getScaleX(), sy = af.getScaleY();
        double tx = af.getTranslateX(), ty = af.getTranslateY();
        if (tx > 0) tx = 0;
        if (ty > 0) ty = 0;

        ScrollBar horizontal = getHorizontalBar();
        horizontal.setIncrement(getClientArea().width / 100);
        horizontal.setPageIncrement(getClientArea().width);
        Rectangle imageBound = sourceImage.getBounds();
        int cw = getClientArea().width, ch = getClientArea().height;
        if (imageBound.width * sx > cw) { /* image is wider than client area */
            horizontal.setMaximum((int) (imageBound.width * sx));
            horizontal.setEnabled(true);
            if (((int) -tx) > horizontal.getMaximum() - cw)
                tx = -horizontal.getMaximum() + cw;
        } else { /* image is narrower than client area */
            horizontal.setEnabled(false);
            tx = (cw - imageBound.width * sx) / 2; //center if too small.
        }
        horizontal.setSelection((int) (-tx));
        horizontal.setThumb(getClientArea().width);

        ScrollBar vertical = getVerticalBar();
        vertical.setIncrement(getClientArea().height / 100);
        vertical.setPageIncrement(getClientArea().height);
        if (imageBound.height * sy > ch) { /* image is higher than client area */
            vertical.setMaximum((int) (imageBound.height * sy));
            vertical.setEnabled(true);
            if (((int) -ty) > vertical.getMaximum() - ch)
                ty = -vertical.getMaximum() + ch;
        } else { /* image is less higher than client area */
            vertical.setEnabled(false);
            ty = (ch - imageBound.height * sy) / 2; //center if too small.
        }
        vertical.setSelection((int) (-ty));
        vertical.setThumb(getClientArea().height);

		/* update transform. */
        af = AffineTransform.getScaleInstance(sx, sy);
        af.preConcatenate(AffineTransform.getTranslateInstance(tx, ty));
        transform = af;

        redraw();
    }

    /**
     * Reload image from a file
     *
     * @return swt image created from image file
     */
    public Image loadImage(InputStream inputStream) {
        if (sourceImage != null && !sourceImage.isDisposed()) {
            sourceImage.dispose();
            sourceImage = null;
            imageData = null;
        }
        this.error = null;
        if (inputStream != null) {
            try {
                imageData = new ImageData(inputStream);
                sourceImage = new Image(getDisplay(), imageData);
            } catch (SWTException e) {
                this.error = e;
            }
        }
        showOriginal();
        return sourceImage;
    }

    public SWTException getError() {
        return error;
    }

    /**
     * Get the image data. (for future use only)
     *
     * @return image data of canvas
     */
    public ImageData getImageData() {
        return imageData;
    }

    /**
     * Reset the image data and update the image
     *
     * @param data image data to be set
     */
    public void updateImage(ImageData data) {
        if (sourceImage != null)
            sourceImage.dispose();
        if (data != null)
            sourceImage = new Image(getDisplay(), data);
        syncScrollBars();
    }

    /**
     * Fit the image onto the canvas
     */
    public void fitCanvas() {
        if (sourceImage == null)
            return;
        Rectangle imageBound = sourceImage.getBounds();
        Rectangle destRect = getClientArea();
        double sx = (double) destRect.width / (double) imageBound.width;
        double sy = (double) destRect.height / (double) imageBound.height;
        double s = Math.min(sx, sy);
        double dx = 0.5 * destRect.width;
        double dy = 0.5 * destRect.height;
        centerZoom(dx, dy, s, new AffineTransform());
    }

    /**
     * Show the image with the original size
     */
    public void showOriginal() {
        if (sourceImage != null) {
            transform = new AffineTransform();
        }
        syncScrollBars();
    }

    public void reset() {
        sourceImage = null;
        redraw();
    }

    /**
     * Perform a zooming operation centered on the given point
     * (dx, dy) and using the given scale factor.
     * The given AffineTransform instance is preconcatenated.
     *
     * @param dx    center x
     * @param dy    center y
     * @param scale zoom rate
     * @param af    original affinetransform
     */
    public void centerZoom(
            double dx,
            double dy,
            double scale,
            AffineTransform af) {
        af.preConcatenate(AffineTransform.getTranslateInstance(-dx, -dy));
        af.preConcatenate(AffineTransform.getScaleInstance(scale, scale));
        af.preConcatenate(AffineTransform.getTranslateInstance(dx, dy));
        transform = af;
        syncScrollBars();
    }

    /**
     * Zoom in around the center of client Area.
     */
    public void zoomIn() {
        if (sourceImage == null)
            return;
        Rectangle rect = getClientArea();
        int w = rect.width, h = rect.height;
        double dx = ((double) w) / 2;
        double dy = ((double) h) / 2;
        centerZoom(dx, dy, ZOOMIN_RATE, transform);
    }

    /**
     * Zoom out around the center of client Area.
     */
    public void zoomOut() {
        if (sourceImage == null)
            return;
        Rectangle rect = getClientArea();
        int w = rect.width, h = rect.height;
        double dx = ((double) w) / 2;
        double dy = ((double) h) / 2;
        centerZoom(dx, dy, ZOOMOUT_RATE, transform);
    }
}