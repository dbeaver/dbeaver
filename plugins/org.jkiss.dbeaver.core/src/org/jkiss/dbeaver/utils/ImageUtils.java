/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.utils;

import org.eclipse.core.runtime.Assert;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.widgets.*;
import org.jkiss.dbeaver.core.DBeaverCore;

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
        final Shell shell = DBeaverCore.getActiveWorkbenchShell();
        Button checkBox = new Button(shell, SWT.CHECK);
        checkBox.setBackground(shell.getDisplay().getSystemColor(SWT.COLOR_LIST_BACKGROUND));
        Point checkboxSize = checkBox.computeSize(SWT.DEFAULT, SWT.DEFAULT);
        checkBox.setBounds(0, 0, checkboxSize.x, checkboxSize.y);
        try {
            checkBox.setSelection(false);
            imageCheckboxEnabledOff = captureWidget(checkBox);
            checkBox.setSelection(true);
            imageCheckboxEnabledOn = captureWidget(checkBox);
            checkBox.setEnabled(false);
            imageCheckboxDisabledOn = captureWidget(checkBox);
            checkBox.setSelection(false);
            imageCheckboxDisabledOff = captureWidget(checkBox);
        } finally {
            checkBox.dispose();
        }
    }

    public static Image captureWidget(Control widget)
    {
        Point size = widget.computeSize(SWT.DEFAULT, SWT.DEFAULT);
        Image image = new Image(widget.getDisplay(), size.x, size.y);
        //image.
        GC gc = new GC(image);
        try {
            widget.print(gc);
        } finally {
            gc.dispose();
        }
        return image;
/*
        final ImageData imageData = image.getImageData();
        imageData.transparentPixel = imageData.getPixel(0, 0);
        Image fixedImage = new Image(widget.getDisplay(), imageData);
        image.dispose();
        return fixedImage;
*/
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
         * safe pallette to get an 8 bit image data for the image.
         */
        if (newImageData == null) {
            newImageData = getWebSafePalletteImageData(imageData);
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
     * and map the colours to the new palatte.
     */
    private static ImageData getWebSafePalletteImageData(ImageData imageData)
    {
        PaletteData palette = imageData.palette;
        RGB[] webSafePallette = getWebSafePallette();
        PaletteData newPaletteData = new PaletteData(webSafePallette);
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
                        if (webSafePallette[newPixel].equals(webSafeColour)) {
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
     * Retrieves a web safe pallette. Our palette will be 216 web safe colours
     * and the remaining filled with white.
     *
     * @return array of 256 colours.
     */
    private static RGB[] getWebSafePallette()
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