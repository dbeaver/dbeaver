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
import org.eclipse.swt.internal.DPIUtil;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.services.IDisposable;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.DBPImage;
import org.jkiss.dbeaver.ui.DBeaverIcons;

import java.lang.invoke.MethodHandles;
import java.nio.ByteOrder;
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
        var data = generateAtlasData(display, images, size, step, mips);

        var handle = MethodHandles.byteArrayViewVarHandle(int[].class, ByteOrder.LITTLE_ENDIAN);
        var pixels = data.data;
        var filler = color.red << 16 | color.green << 8 | color.blue;
        for (int i = 0; i < data.width * data.height; i++) {
            handle.set(pixels, i * 4, filler);
        }

        return new SnowflakeAtlas(
            new Image(display, data),
            images.size(),
            DPIUtil.autoScaleUp(size),
            DPIUtil.autoScaleUp(step),
            mips
        );
    }

    @NotNull
    private static ImageData generateAtlasData(
        @NotNull Display display,
        @NotNull List<? extends DBPImage> images,
        int size,
        int step,
        int mips
    ) {
        int width = size * images.size();
        int height = size * mips - sum(mips - 1) * step;
        var data = new ImageData(width, height, 24, new PaletteData(0xFF0000, 0xFF00, 0xFF));
        data.alphaData = new byte[width * height];

        var image = new Image(display, data);
        var transform = new Transform(display);
        var gc = new GC(image);

        var random = new Random();

        try {
            gc.setAntialias(SWT.ON);
            gc.setInterpolation(SWT.HIGH);

            for (int i = 0; i < images.size(); i++) {
                for (int j = 0; j < mips; j++) {
                    var sprite = DBeaverIcons.getImage(images.get(i));
                    var bounds = sprite.getBounds();
                    var rotation = random.nextInt(360);
                    var scale = size - j * step;
                    var scale2 = (int) (scale * 0.5f);

                    int x = i * scale;
                    int y = size * j - sum(j - 1) * step;

                    transform.translate(x + scale2, y + scale2);
                    transform.rotate(rotation);

                    gc.setTransform(transform);
                    gc.drawImage(sprite, 0, 0, bounds.width, bounds.height, -scale2, -scale2, scale, scale);

                    transform.rotate(-rotation);
                    transform.translate(-(x + scale2), -(y + scale2));
                }
            }

            // Can't use the old data because it gets copied when an image is constructed
            return image.getImageData();
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
