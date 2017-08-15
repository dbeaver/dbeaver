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

package org.jkiss.dbeaver.ui;

import org.eclipse.core.runtime.Assert;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.core.DBeaverUI;

/**
 * Image-related utils
 *
 * <p/>
 * Web image transformation originally get from org.eclipse.gmf.runtime.diagram.ui.render.util.CopyToImageUtil
 *
 * Web image transformation by Anthony Hunter, cmahoney
 */
public class ImageUtils {

    private static Image imageCheckboxEnabledOn, imageCheckboxEnabledOff, imageCheckboxDisabledOn, imageCheckboxDisabledOff;

    public static Image getImageCheckboxEnabledOn()
    {
        if (imageCheckboxEnabledOn == null) {
            initImages();
        }
        return imageCheckboxEnabledOn;
    }

    public static Image getImageCheckboxEnabledOff()
    {
        if (imageCheckboxEnabledOff == null) {
            initImages();
        }
        return imageCheckboxEnabledOff;
    }

    public static Image getImageCheckboxDisabledOn()
    {
        if (imageCheckboxDisabledOn == null) {
            initImages();
        }
        return imageCheckboxDisabledOn;
    }

    public static Image getImageCheckboxDisabledOff()
    {
        if (imageCheckboxDisabledOff == null) {
            initImages();
        }
        return imageCheckboxDisabledOff;
    }

    private static synchronized void initImages()
    {
        // Capture checkbox image - only for windows
        // There could be hard-to-understand problems in Linux
        /*if (!DBeaverCore.getInstance().getLocalSystem().isWindows())*/ {
            imageCheckboxEnabledOff = DBeaverIcons.getImage(UIIcon.CHECK_OFF);
            imageCheckboxEnabledOn = DBeaverIcons.getImage(UIIcon.CHECK_ON);
            imageCheckboxDisabledOn = makeDisableImage(DBeaverIcons.getImage(UIIcon.CHECK_ON));
            imageCheckboxDisabledOff = makeDisableImage(DBeaverIcons.getImage(UIIcon.CHECK_OFF));
            return;
        }
/*
        final Shell shell = DBeaverUI.getActiveWorkbenchShell();
        Button checkBox = new Button(shell, SWT.CHECK);
        checkBox.setVisible(true);
        final Color borderColor = shell.getDisplay().getSystemColor(SWT.COLOR_LIST_BACKGROUND);
        checkBox.setBackground(borderColor);
        Point checkboxSize = checkBox.computeSize(SWT.DEFAULT, SWT.DEFAULT);
        checkBox.setBounds(0, 0, checkboxSize.x, checkboxSize.y);
        try {
            checkBox.setSelection(false);
            imageCheckboxEnabledOff = captureWidget(checkBox, borderColor, DBeaverIcons.getImage(UIIcon.CHECK_OFF));
            checkBox.setSelection(true);
            imageCheckboxEnabledOn = captureWidget(checkBox, borderColor, DBeaverIcons.getImage(UIIcon.CHECK_ON));
            checkBox.setEnabled(false);
            imageCheckboxDisabledOn = captureWidget(checkBox, borderColor, DBeaverIcons.getImage(UIIcon.CHECK_ON));
            checkBox.setSelection(false);
            imageCheckboxDisabledOff = captureWidget(checkBox, borderColor, DBeaverIcons.getImage(UIIcon.CHECK_OFF));
        } finally {
            UIUtils.dispose(checkBox);
        }
*/
    }

    public static Image captureWidget(Control widget, Color borderColor, Image defaultImage)
    {
        Point size = widget.computeSize(SWT.DEFAULT, SWT.DEFAULT);
        Image image = new Image(widget.getDisplay(), size.x, size.y);
        //image.
        GC gc = new GC(image);
        try {
            //gc.copyArea(image, 0, 0);
            widget.print(gc);
        } finally {
            UIUtils.dispose(gc);
        }
        Image result = removeImageBorder(image, borderColor);
        if (result == null) {
            return defaultImage;
        } else {
            return result;
        }
/*
        final ImageData imageData = image.getImageData();
        imageData.transparentPixel = imageData.getPixel(0, 0);
        Image fixedImage = new Image(widget.getDisplay(), imageData);
        image.dispose();
        return fixedImage;
*/
    }

    private static Image makeDisableImage(Image image) {
        return new Image(image.getDevice(), image, SWT.IMAGE_DISABLE);
    }

    public static Image removeImageBorder(Image srcImage, Color borderColor)
    {
        final ImageData imageData = srcImage.getImageData();
        if (imageData.height == 0 || imageData.width == 0) {
            return srcImage;
        }
        int borderPixel = imageData.getPixel(0, 0);
        if (!imageData.palette.getRGB(borderPixel).equals(borderColor.getRGB())) {
            // First pixel isn't a border
            return srcImage;
        }

        int emptyTopRows = 0, emptyBottomRows = 0, emptyLeftColumns = 0, emptyRightColumns = 0;
        // Check top rows
        int row;
        for (row = 0; row < imageData.height; row++) {
            boolean emptyRow = true;
            for (int col = 0; col < imageData.width; col++) {
                if (borderPixel != imageData.getPixel(col, row)) {
                    emptyRow = false;
                    break;
                }
            }
            if (!emptyRow) {
                emptyTopRows = row;
                break;
            }
        }
        if (row == imageData.height) {
            // All rows are empty
            return null;
        }
        // Check bottom rows
        for (row = imageData.height - 1; row >= 0; row--) {
            boolean emptyRow = true;
            for (int col = 0; col < imageData.width; col++) {
                if (borderPixel != imageData.getPixel(col, row)) {
                    emptyRow = false;
                    break;
                }
            }
            if (!emptyRow) {
                emptyBottomRows = imageData.height - row - 1;
                break;
            }
        }
        if (emptyTopRows > 0 || emptyBottomRows > 0 || emptyLeftColumns > 0 || emptyRightColumns > 0) {
            return cropImage(
                srcImage,
                emptyLeftColumns,
                emptyTopRows,
                imageData.width - emptyLeftColumns - emptyRightColumns,
                imageData.height - emptyTopRows - emptyBottomRows);
        }
        return srcImage;
    }

	public static Image cropImage(Image srcImage, int x, int y, int w, int h)
	{
		Image cropImage = new Image(srcImage.getDevice(), w, h);

        // Redefine w and h to void them to be too big
		if (x+w > srcImage.getBounds().width) {
			w = srcImage.getBounds().width - x;
		}
		if (y+h > srcImage.getBounds().height) {
			h = srcImage.getBounds().height - y;
		}

		GC cropGC = new GC(cropImage);
		cropGC.drawImage(srcImage,
				x, y,
				w, h,
				0, 0,
				w, h);
		UIUtils.dispose(cropGC);
        UIUtils.dispose(srcImage);

		return cropImage;
	}

    /**
     * Retrieve the image data for the image, using a palette of at most 256
     * colours.
     *
     * @param image the SWT image.
     * @return new image data.
     */
    public static ImageData makeWebImageData(Image image)
    {

        ImageData imageData = image.getImageData();

        /**
         * If the image depth is 8 bits or less, then we can use the existing
         * image data.
         */
        if (imageData.depth <= 8) {
            return imageData;
        }

        /**
         * get an 8 bit imageData for the image
         */
        ImageData newImageData = get8BitPaletteImageData(imageData);

        /**
         * if newImageData is null, it has more than 256 colours. Use the web
         * safe palette to get an 8 bit image data for the image.
         */
        if (newImageData == null) {
            newImageData = getWebSafePaletteImageData(imageData);
        }

        return newImageData;
    }

    /**
     * Retrieve an image data with an 8 bit palette for an image. We assume that
     * the image has less than 256 colours.
     *
     * @param imageData the imageData for the image.
     * @return the new 8 bit imageData or null if the image has more than 256
     *         colours.
     */
    private static ImageData get8BitPaletteImageData(ImageData imageData)
    {
        PaletteData palette = imageData.palette;
        RGB colours[] = new RGB[256];
        PaletteData newPaletteData = new PaletteData(colours);
        ImageData newImageData = new ImageData(imageData.width, imageData.height, 8, newPaletteData);

        int lastPixel = -1;
        int newPixel = -1;
        for (int i = 0; i < imageData.width; ++i) {
            for (int j = 0; j < imageData.height; ++j) {
                int pixel = imageData.getPixel(i, j);

                if (pixel != lastPixel) {
                    lastPixel = pixel;

                    RGB colour = palette.getRGB(pixel);
                    for (newPixel = 0; newPixel < 256; ++newPixel) {
                        if (colours[newPixel] == null) {
                            colours[newPixel] = colour;
                            break;
                        }
                        if (colours[newPixel].equals(colour)) {
                            break;
                        }
                    }

                    if (newPixel >= 256) {
                        /**
                         * Diagram has more than 256 colors, return null
                         */
                        return null;
                    }
                }

                newImageData.setPixel(i, j, newPixel);
            }
        }

        RGB colour = new RGB(0, 0, 0);
        for (int k = 0; k < 256; ++k) {
            if (colours[k] == null) {
                colours[k] = colour;
            }
        }

        return newImageData;
    }

    /**
     * If the image has less than 256 colours, simply create a new 8 bit palette
     * and map the colours to the new palette.
     */
    private static ImageData getWebSafePaletteImageData(ImageData imageData)
    {
        PaletteData palette = imageData.palette;
        RGB[] webSafePalette = getWebSafePalette();
        PaletteData newPaletteData = new PaletteData(webSafePalette);
        ImageData newImageData = new ImageData(imageData.width,
            imageData.height, 8, newPaletteData);

        int lastPixel = -1;
        int newPixel = -1;
        for (int i = 0; i < imageData.width; ++i) {
            for (int j = 0; j < imageData.height; ++j) {
                int pixel = imageData.getPixel(i, j);

                if (pixel != lastPixel) {
                    lastPixel = pixel;

                    RGB colour = palette.getRGB(pixel);
                    RGB webSafeColour = getWebSafeColour(colour);
                    for (newPixel = 0; newPixel < 256; ++newPixel) {
                        if (webSafePalette[newPixel].equals(webSafeColour)) {
                            break;
                        }
                    }

                    Assert.isTrue(newPixel < 216);
                }
                newImageData.setPixel(i, j, newPixel);
            }
        }

        return newImageData;
    }

    /**
     * Retrieves a web safe colour that closely matches the provided colour.
     *
     * @param colour a colour.
     * @return the web safe colour.
     */
    private static RGB getWebSafeColour(RGB colour)
    {
        int red = Math.round((colour.red + 25) / 51) * 51;
        int green = Math.round((colour.green + 25) / 51) * 51;
        int blue = Math.round((colour.blue + 25) / 51) * 51;
        return new RGB(red, green, blue);
    }

    /**
     * Retrieves a web safe palette. Our palette will be 216 web safe colours
     * and the remaining filled with white.
     *
     * @return array of 256 colours.
     */
    private static RGB[] getWebSafePalette()
    {
        RGB[] colours = new RGB[256];
        int i = 0;
        for (int red = 0; red <= 255; red = red + 51) {
            for (int green = 0; green <= 255; green = green + 51) {
                for (int blue = 0; blue <= 255; blue = blue + 51) {
                    RGB colour = new RGB(red, green, blue);
                    colours[i++] = colour;
                }
            }
        }

        RGB colour = new RGB(0, 0, 0);
        for (int k = 0; k < 256; ++k) {
            if (colours[k] == null) {
                colours[k] = colour;
            }
        }

        return colours;
    }

}