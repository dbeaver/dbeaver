/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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
package org.jkiss.dbeaver.ui.controls.decorations;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.services.IDisposable;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.DBPImage;
import org.jkiss.dbeaver.ui.DBeaverIcons;

import java.util.List;
import java.util.Random;

record SnowflakeAtlas(
    @NotNull Image image,
    int count,
    int size,
    int step,
    int mips
) implements IDisposable {
    @NotNull
    static SnowflakeAtlas generate(
        @NotNull Display display,
        @NotNull List<? extends DBPImage> images,
        @NotNull RGB color,
        int size,
        int step,
        int mips
    ) {
        var data = generateAtlasData(display, images, color, size, step, mips);
        var image = new Image(display, data);
        var scale = image.getBounds().width / (float) (size * images.size());

        return new SnowflakeAtlas(
            image,
            images.size(),
            Math.round(size * scale),
            Math.round(step * scale),
            mips
        );
    }

    @NotNull
    private static ImageData generateAtlasData(
        @NotNull Display display,
        @NotNull List<? extends DBPImage> images,
        @NotNull RGB color,
        int size,
        int step,
        int mips
    ) {
        var image = new Image(display, size * images.size(), size * mips - sum(mips - 1) * step);
        var transform = new Transform(display);
        var gc = new GC(image);

        var random = new Random();

        try {
            gc.setAntialias(SWT.ON);
            gc.setInterpolation(SWT.HIGH);

            // fill the background with something contrast
            gc.setBackground(display.getSystemColor(SWT.COLOR_MAGENTA));
            gc.fillRectangle(0, 0, image.getBounds().width, image.getBounds().height);

            for (int i = 0; i < images.size(); i++) {
                for (int j = 0; j < mips; j++) {
                    var sprite = DBeaverIcons.getImage(images.get(i));
                    var bounds = sprite.getBounds();

                    int scale = size - j * step;
                    int center = (int) (scale * 0.5f);
                    int angle = random.nextInt(360);

                    int x = i * scale;
                    int y = size * j - sum(j - 1) * step;

                    transform.identity();
                    transform.translate(x + center, y + center);
                    transform.rotate(angle);

                    gc.setTransform(transform);
                    gc.setAlpha(255 / random.nextInt(3, 8));
                    gc.drawImage(sprite, bounds.x, bounds.y, bounds.width, bounds.height, -center, -center, scale, scale);
                }
            }

            var result = image.getImageData();
            var filler = result.palette.getPixel(color);

            for (int y = 0; y < result.height; y++) {
                for (int x = 0; x < result.width; x++) {
                    var pixel = result.palette.getRGB(result.getPixel(x, y));
                    result.setAlpha(x, y, 255 - pixel.red); // use any channel since images are BW
                    result.setPixel(x, y, filler);
                }
            }

            return result;
        } finally {
            transform.dispose();
            gc.dispose();
            image.dispose();
        }
    }

    int getSize(int mip) {
        return size - mip * step;
    }

    @NotNull
    Rectangle getClip(int index, int mip) {
        int mipSize = getSize(mip);
        int mipShift = sum(mip - 1) * step;
        return new Rectangle(
            index * mipSize,
            mip * size - mipShift,
            mipSize,
            mipSize
        );
    }

    @Override
    public void dispose() {
        image.dispose();
    }

    private static int sum(int n) {
        return n * (n + 1) / 2;
    }
}
