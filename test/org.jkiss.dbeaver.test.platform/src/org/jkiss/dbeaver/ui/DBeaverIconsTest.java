/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2026 DBeaver Corp and others
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

import org.eclipse.jface.resource.DeviceResourceManager;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.LazyResourceManager;
import org.eclipse.jface.resource.ResourceManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTException;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.ImageLoader;
import org.eclipse.swt.graphics.PaletteData;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Display;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.DBIcon;
import org.jkiss.dbeaver.model.DBIconComposite;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.*;

class DBeaverIconsTest {
    @TempDir
    Path directory;

    @Test
    void descriptorsDoNotReadImagesOrRequireDisplay() throws Exception {
        DBIcon base = new DBIcon(directory.resolve("base.png").toUri().toString());
        DBIcon overlay = new DBIcon(directory.resolve("overlay.png").toUri().toString());
        DBIconComposite composite = new DBIconComposite(base, false, overlay, null, null, null);
        ImageDescriptor descriptor = CompletableFuture.supplyAsync(() -> DBeaverIcons.getImageDescriptor(composite)).get();
        assertSame(descriptor, DBeaverIcons.getImageDescriptor(composite));
        assertSame(DBeaverIcons.getImageDescriptor(base), DBeaverIcons.getImageDescriptor(
            new DBIconComposite(base, false, null, null, null, null)));
        // The files do not exist until after lookup: neither base nor overlay may be rasterized eagerly.
        save("base.png", 16, 0x0000ff);
        save("overlay.png", 4, 0xff0000);
        ImageData data = descriptor.getImageData(100);
        assertEquals(16, data.width);
        assertEquals(new RGB(255, 0, 0), data.palette.getRGB(data.getPixel(0, 0)));
        assertEquals(new RGB(0, 0, 255), data.palette.getRGB(data.getPixel(15, 15)));
    }

    @Test
    void firstNativeImageRequestRequiresDisplayThread() {
        DBIcon icon = new DBIcon(directory.resolve("thread.png").toUri().toString());
        DBeaverIcons.getImageDescriptor(icon);
        ExecutionException exception = assertThrows(ExecutionException.class,
            () -> CompletableFuture.supplyAsync(() -> DBeaverIcons.getImage(icon)).get());
        SWTException swtException = assertInstanceOf(SWTException.class, exception.getCause());
        assertEquals(SWT.ERROR_THREAD_INVALID_ACCESS, swtException.code);
    }

    @Test
    void cachedImagesAndCallerOwnedCompositesHaveSeparateLifetimes() {
        save("base.png", 16, 0x0000ff);
        save("overlay.png", 4, 0xff0000);
        DBIcon base = new DBIcon(directory.resolve("base.png").toUri().toString());
        DBIcon overlay = new DBIcon(directory.resolve("overlay.png").toUri().toString());
        DBIconComposite composite = new DBIconComposite(base, false, null, null, null, overlay);
        Display display = new Display();
        try {
            ImageDescriptor descriptor = DBeaverIcons.getImageDescriptor(base);
            Image image = DBeaverIcons.getImage(base);
            Image cached = DBeaverIcons.getImage(composite);
            Image owned = DBeaverIcons.getImage(composite, false);
            try {
                assertSame(image, DBeaverIcons.getImageByLocation(base.getLocation()));
                assertSame(descriptor, DBeaverIcons.getImageDescriptor(base));
                assertSame(cached, DBeaverIcons.getImage(composite));
                assertNotSame(cached, owned);
                owned.dispose();
                assertFalse(cached.isDisposed());
                assertFalse(image.isDisposed());
                ImageData data = cached.getImageData(100);
                assertEquals(new RGB(255, 0, 0), data.palette.getRGB(data.getPixel(15, 15)));
            } finally {
                owned.dispose();
                cached.dispose();
                image.dispose();
            }
        } finally {
            display.dispose();
        }
    }

    @Test
    void binaryImagesRemainCallerOwned() {
        Display display = new Display();
        try {
            Image image = new Image(display, 16, 16);
            DBIconBinary binary = new DBIconBinary("test", image);
            assertSame(image, DBeaverIcons.getImage(binary));
            assertSame(binary.getImageDescriptor(), DBeaverIcons.getImageDescriptor(binary));
            binary.dispose();
            assertTrue(image.isDisposed());
        } finally {
            display.dispose();
        }
    }

    @Test
    void urlDescriptorsKeepJFaceNativeResourceReuse() {
        save("reuse.png", 16, 0x0000ff);
        ImageDescriptor descriptor = DBeaverIcons.getImageDescriptor(
            new DBIcon(directory.resolve("reuse.png").toUri().toString()));
        // A plain wrapper inherits shouldBeCached=false, even when its delegate is a URL descriptor.
        ImageDescriptor wrapper = new ImageDescriptor() {
            @Override
            public ImageData getImageData(int zoom) {
                return descriptor.getImageData(zoom);
            }
        };
        Display display = new Display();
        DeviceResourceManager resources = new DeviceResourceManager(display);
        try {
            ResourceManager cache = new LazyResourceManager(16, resources);
            assertEquals(1, countNativeImages(cache, descriptor));
            assertEquals(10, countNativeImages(cache, wrapper));
        } finally {
            resources.dispose();
            display.dispose();
        }
    }

    private static int countNativeImages(@NotNull ResourceManager resources, @NotNull ImageDescriptor descriptor) {
        Set<Image> images = Collections.newSetFromMap(new IdentityHashMap<>());
        for (int i = 0; i < 10; i++) {
            images.add(resources.create(descriptor));
            resources.destroy(descriptor);
        }
        return images.size();
    }

    @Test
    void overlaysKeepAllFourCornersAtDifferentZooms() {
        OverlayImageDescriptor descriptor = new OverlayImageDescriptor(solidDescriptor(16, 0));
        descriptor.setTopLeft(new ImageDescriptor[]{solidDescriptor(4, 0xff0000)});
        descriptor.setTopRight(new ImageDescriptor[]{solidDescriptor(4, 0x00ff00)});
        descriptor.setBottomLeft(new ImageDescriptor[]{solidDescriptor(4, 0x0000ff)});
        descriptor.setBottomRight(new ImageDescriptor[]{solidDescriptor(4, 0xffffff)});
        for (int zoom : new int[]{100, 200, 100}) {
            ImageData data = descriptor.getImageData(zoom);
            int edge = 16 * zoom / 100 - 1;
            assertEquals(edge + 1, data.width);
            assertEquals(new RGB(255, 0, 0), data.palette.getRGB(data.getPixel(0, 0)));
            assertEquals(new RGB(0, 255, 0), data.palette.getRGB(data.getPixel(edge, 0)));
            assertEquals(new RGB(0, 0, 255), data.palette.getRGB(data.getPixel(0, edge)));
            assertEquals(new RGB(255, 255, 255), data.palette.getRGB(data.getPixel(edge, edge)));
            assertEquals(new RGB(0, 0, 0), data.palette.getRGB(data.getPixel(edge / 2, edge / 2)));
        }
    }

    @NotNull
    private static ImageDescriptor solidDescriptor(int size, int color) {
        return ImageDescriptor.createFromImageDataProvider(zoom -> {
            int pixels = size * zoom / 100;
            ImageData data = new ImageData(pixels, pixels, 24, new PaletteData(0xff0000, 0xff00, 0xff));
            for (int y = 0; y < pixels; y++) {
                for (int x = 0; x < pixels; x++) {
                    data.setPixel(x, y, color);
                }
            }
            return data;
        });
    }

    private void save(@NotNull String name, int size, int rgb) {
        ImageData data = new ImageData(size, size, 24, new PaletteData(0xff0000, 0xff00, 0xff));
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                data.setPixel(x, y, rgb);
            }
        }
        ImageLoader loader = new ImageLoader();
        loader.data = new ImageData[]{data};
        loader.save(directory.resolve(name).toString(), SWT.IMAGE_PNG);
    }
}
