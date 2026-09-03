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
package com.dbeaver.db.cdata.registry;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBIcon;
import org.jkiss.dbeaver.utils.RuntimeUtils;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

final class CDataDriverIconLoader {
    private static final Log log = Log.getLog(CDataDriverIconLoader.class);
    private static final String ICON_URL_PREFIX = "https://www.cdata.com/ui/img/drivers/icon-";
    private static final int MAX_ICON_BYTES = 512 * 1024;
    private static final int CONNECT_TIMEOUT_MS = 3_000;
    private static final int READ_TIMEOUT_MS = 5_000;
    private static final ExecutorService EXECUTOR;

    static {
        ThreadPoolExecutor executor = new ThreadPoolExecutor(
            4,
            4,
            30,
            TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(),
            runnable -> {
                Thread thread = new Thread(runnable, "CData driver icon loader");
                thread.setDaemon(true);
                thread.setContextClassLoader(ClassLoader.getPlatformClassLoader());
                return thread;
            }
        );
        executor.allowCoreThreadTimeOut(true);
        EXECUTOR = executor;
    }

    private CDataDriverIconLoader() {
    }

    static void load(@NotNull String dataSource, @NotNull IconConsumer consumer) {
        EXECUTOR.execute(() -> {
            try {
                getIconUri(dataSource);
                Path iconDirectory = RuntimeUtils.getUserHomePath().resolve(".CData/icons");
                Path icon = iconDirectory.resolve(dataSource + ".png");
                Path icon2x = iconDirectory.resolve(dataSource + "@2x.png");
                Path iconBig = iconDirectory.resolve(dataSource + "_big.png");
                Path iconBig2x = iconDirectory.resolve(dataSource + "_big@2x.png");
                if (!isValidCachedIcon(icon, 16) || !isValidCachedIcon(icon2x, 32) ||
                    !isValidCachedIcon(iconBig, 64) || !isValidCachedIcon(iconBig2x, 128)) {
                    downloadIcons(dataSource, iconDirectory, icon, icon2x, iconBig, iconBig2x);
                }
                consumer.accept(new DBIcon(icon.toUri().toString()), new DBIcon(iconBig.toUri().toString()));
            } catch (Exception e) {
                log.debug("Unable to load the CData driver icon for '" + dataSource + "'", e);
                consumer.accept(null, null);
            }
        });
    }

    @NotNull
    static URI getIconUri(@NotNull String dataSource) {
        if (!dataSource.matches("[a-z0-9]+")) {
            throw new IllegalArgumentException("Invalid CData data source name");
        }
        return URI.create(ICON_URL_PREFIX + dataSource + ".png");
    }

    private static void downloadIcons(
        @NotNull String dataSource,
        @NotNull Path iconDirectory,
        @NotNull Path icon,
        @NotNull Path icon2x,
        @NotNull Path iconBig,
        @NotNull Path iconBig2x
    ) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) getIconUri(dataSource).toURL().openConnection();
        connection.setConnectTimeout(CONNECT_TIMEOUT_MS);
        connection.setReadTimeout(READ_TIMEOUT_MS);
        connection.setRequestProperty("User-Agent", "DBeaver");
        connection.setInstanceFollowRedirects(false);
        try {
            int responseCode = connection.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                throw new IOException("CData icon server returned HTTP " + responseCode);
            }
            byte[] content;
            try (InputStream input = connection.getInputStream()) {
                content = input.readNBytes(MAX_ICON_BYTES + 1);
            }
            if (content.length > MAX_ICON_BYTES) {
                throw new IOException("CData driver icon is too large");
            }
            BufferedImage source = readImage(content, 512, 512);
            Files.createDirectories(iconDirectory);
            writeIcon(source, icon, 16);
            writeIcon(source, icon2x, 32);
            writeIcon(source, iconBig, 64);
            writeIcon(source, iconBig2x, 128);
        } finally {
            connection.disconnect();
        }
    }

    private static boolean isValidCachedIcon(@NotNull Path icon, int expectedSize) {
        try {
            if (!Files.isRegularFile(icon) || Files.isSymbolicLink(icon) || Files.size(icon) > MAX_ICON_BYTES) {
                return false;
            }
            try (InputStream input = Files.newInputStream(icon)) {
                BufferedImage image = readImage(input.readNBytes(MAX_ICON_BYTES + 1), expectedSize, expectedSize);
                return image.getWidth() == expectedSize && image.getHeight() == expectedSize;
            }
        } catch (IOException e) {
            return false;
        }
    }

    @NotNull
    private static BufferedImage readImage(@NotNull byte[] content, int maxWidth, int maxHeight) throws IOException {
        if (content.length > MAX_ICON_BYTES) {
            throw new IOException("CData driver icon is too large");
        }
        try (ImageInputStream input = ImageIO.createImageInputStream(new ByteArrayInputStream(content))) {
            if (input == null) {
                throw new IOException("CData driver icon is not a valid image");
            }
            var readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) {
                throw new IOException("CData driver icon is not a valid image");
            }
            ImageReader reader = readers.next();
            try {
                reader.setInput(input, true, true);
                int width = reader.getWidth(0);
                int height = reader.getHeight(0);
                if (width <= 0 || height <= 0 || width > maxWidth || height > maxHeight) {
                    throw new IOException("CData driver icon has invalid dimensions");
                }
                BufferedImage image = reader.read(0);
                if (image == null) {
                    throw new IOException("CData driver icon is not a valid image");
                }
                return image;
            } finally {
                reader.dispose();
            }
        }
    }

    private static void writeIcon(@NotNull BufferedImage source, @NotNull Path target, int size) throws IOException {
        BufferedImage scaled = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = scaled.createGraphics();
        try {
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            graphics.drawImage(source, 0, 0, size, size, null);
        } finally {
            graphics.dispose();
        }

        Path temporary = Files.createTempFile(target.getParent(), target.getFileName().toString(), ".tmp");
        try {
            if (!ImageIO.write(scaled, "png", temporary.toFile())) {
                throw new IOException("PNG image writer is unavailable");
            }
            try {
                Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    @FunctionalInterface
    interface IconConsumer {
        void accept(DBIcon icon, DBIcon iconBig);
    }
}
