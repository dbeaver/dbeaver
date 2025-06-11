/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2025 DBeaver Corp and others
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

package org.jfree.swt;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.DirectColorModel;
import java.awt.image.VolatileImage;

/**
 * A graphics configuration for the {@link SWTGraphics2D} class.
 */
public class SWTGraphicsConfiguration extends GraphicsConfiguration {

    private GraphicsDevice device;

    private final int width, height;

    /**
     * Creates a new instance.
     *
     * @param width  the width of the bounds.
     * @param height  the height of the bounds.
     */
    public SWTGraphicsConfiguration(int width, int height) {
        super();
        this.width = width;
        this.height = height;
    }

    /**
     * Returns the graphics device that this configuration is associated with.
     *
     * @return The graphics device (never {@code null}).
     */
    @Override
    public GraphicsDevice getDevice() {
        if (this.device == null) {
            this.device = new SWTGraphicsDevice("SWTGraphicsDevice", this);
        }
        return this.device;
    }

    /**
     * Returns the color model for this configuration.
     *
     * @return The color model.
     */
    @Override
    public ColorModel getColorModel() {
        return getColorModel(Transparency.TRANSLUCENT);
    }

    /**
     * Returns the color model for the specified transparency type, or
     * {@code null}.
     *
     * @param transparency  the transparency type.
     *
     * @return A color model (possibly {@code null}).
     */
    @Override
    public ColorModel getColorModel(int transparency) {
        if (transparency == Transparency.TRANSLUCENT) {
            return ColorModel.getRGBdefault();
        } else if (transparency == Transparency.OPAQUE) {
            return new DirectColorModel(32, 0x00ff0000, 0x0000ff00, 0x000000ff);
        } else {
            return null;
        }
    }

    /**
     * Returns the default transform.
     *
     * @return The default transform.
     */
    @Override
    public AffineTransform getDefaultTransform() {
        return new AffineTransform();
    }

    /**
     * Returns the normalizing transform.
     *
     * @return The normalizing transform.
     */
    @Override
    public AffineTransform getNormalizingTransform() {
        return new AffineTransform();
    }

    /**
     * Returns the bounds for this configuration.
     *
     * @return The bounds.
     */
    @Override
    public Rectangle getBounds() {
        return new Rectangle(this.width, this.height);
    }

    private BufferedImage img;
    private GraphicsConfiguration gc;

    /**
     * Returns a volatile image.  This method is a workaround for a
     * ClassCastException that occurs on MacOSX when exporting a Swing UI
     * that uses the Nimbus Look and Feel.
     *
     * @param width  the image width.
     * @param height  the image height.
     * @param caps  the image capabilities.
     * @param transparency  the transparency.
     *
     * @return The volatile image.
     *
     * @throws AWTException if there is a problem creating the image.
     */
    @Override
    public VolatileImage createCompatibleVolatileImage(int width, int height,
                                                       ImageCapabilities caps, int transparency) throws AWTException {
        if (img == null) {
            img = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
            gc = img.createGraphics().getDeviceConfiguration();
        }
        return gc.createCompatibleVolatileImage(width, height, caps,
                transparency);
    }

}
