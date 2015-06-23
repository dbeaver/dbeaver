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

import org.eclipse.jface.action.Action;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.PaletteData;
import org.jkiss.dbeaver.model.DBPImage;
import org.jkiss.dbeaver.ui.DBeaverIcons;

/**
 * Action delegate for all toolbar push-buttons.
 * <p/>
 *
 * @author Chengdong Li: cli4@uky.edu
 */
public class ImageActionDelegate extends Action {

    public static final String TOOLBAR_ZOOMIN = "toolbar.zoomin";
    public static final String TOOLBAR_ZOOMOUT = "toolbar.zoomout";
    public static final String TOOLBAR_FIT = "toolbar.fit";
    public static final String TOOLBAR_ROTATE = "toolbar.rotate";
    public static final String TOOLBAR_ORIGINAL = "toolbar.original";

    /**
     * pointer to image view
     */
    public ImageViewer imageViewControl = null;
    /**
     * Action id of this delegate
     */
    public String id;

    public ImageActionDelegate(ImageViewer viewControl, String id, String name, DBPImage image) {
        super(name, DBeaverIcons.getImageDescriptor(image));
        this.imageViewControl = viewControl;
        this.id = id;
    }

    @Override
    public void run()
    {
        ImageViewCanvas imageViewCanvas = imageViewControl.getCanvas();
        if (imageViewCanvas.getSourceImage() == null) return;
        if (id.equals(TOOLBAR_ZOOMIN)) {
            imageViewCanvas.zoomIn();
        } else if (id.equals(TOOLBAR_ZOOMOUT)) {
            imageViewCanvas.zoomOut();
        } else if (id.equals(TOOLBAR_FIT)) {
            imageViewCanvas.fitCanvas();
        } else if (id.equals(TOOLBAR_ROTATE)) {
            /* rotate image anti-clockwise */
            ImageData src = imageViewCanvas.getSourceImage().getImageData();
            if (src == null) return;
            PaletteData srcPal = src.palette;
            PaletteData destPal;
            ImageData dest;
            /* construct a new ImageData */
            if (srcPal.isDirect) {
                destPal = new PaletteData(srcPal.redMask, srcPal.greenMask, srcPal.blueMask);
            } else {
                destPal = new PaletteData(srcPal.getRGBs());
            }
            dest = new ImageData(src.height, src.width, src.depth, destPal);
            /* rotate by rearranging the pixels */
            for (int i = 0; i < src.width; i++) {
                for (int j = 0; j < src.height; j++) {
                    int pixel = src.getPixel(i, j);
                    dest.setPixel(j, src.width - 1 - i, pixel);
                }
            }
            imageViewCanvas.updateImage(dest);
        } else if (id.equals(TOOLBAR_ORIGINAL)) {
            imageViewCanvas.showOriginal();
        }
    }

}
