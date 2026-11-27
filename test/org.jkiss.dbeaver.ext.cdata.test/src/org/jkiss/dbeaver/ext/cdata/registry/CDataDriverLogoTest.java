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
package org.jkiss.dbeaver.ext.cdata.registry;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.DBIcon;
import org.jkiss.junit.DBeaverUnitTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import javax.imageio.ImageIO;

public class CDataDriverLogoTest extends DBeaverUnitTest {
    @TempDir
    Path tempDirectory;

    @Test
    public void downloadLogoPreservesAspectRatioAndTransparency() throws IOException {
        HttpURLConnection connection = createConnection(createImage(216, 108));
        Path icon = tempDirectory.resolve("postgresql.png");
        Files.write(icon, createImage(16, 16));

        DBIcon logo = CDataDriverIconLoader.loadLogo("postgresql", tempDirectory, uri -> {
            Assertions.assertEquals("https://www.cdata.com/ui/img/logo-postgresql.png", uri.toString());
            return connection;
        });

        Path cachedLogo = tempDirectory.resolve("postgresql_logo.png");
        Assertions.assertEquals(cachedLogo.toUri().toString(), logo.getLocation());
        BufferedImage image = ImageIO.read(cachedLogo.toFile());
        Assertions.assertEquals(160, image.getWidth());
        Assertions.assertEquals(80, image.getHeight());
        Assertions.assertEquals(0, image.getRGB(0, 0));
        Assertions.assertEquals(16, ImageIO.read(icon.toFile()).getWidth());
        Mockito.verify(connection).disconnect();
    }

    @Test
    public void cachedLogoIsAvailableOffline() throws IOException {
        Path cachedLogo = tempDirectory.resolve("postgresql_logo.png");
        byte[] content = createImage(160, 80);
        Files.write(cachedLogo, content);
        CDataDriverIconLoader.loadLogo("postgresql", tempDirectory, uri -> {
            throw new AssertionError("A cached logo must not trigger a request");
        });
        Assertions.assertArrayEquals(content, Files.readAllBytes(cachedLogo));
    }

    @Test
    public void resizePreviouslyCachedLogoOffline() throws IOException {
        Path cachedLogo = tempDirectory.resolve("postgresql_logo.png");
        Files.write(cachedLogo, createImage(216, 108));
        CDataDriverIconLoader.loadLogo("postgresql", tempDirectory, uri -> {
            throw new AssertionError("Resizing a cached logo must not trigger a request");
        });
        BufferedImage image = ImageIO.read(cachedLogo.toFile());
        Assertions.assertEquals(160, image.getWidth());
        Assertions.assertEquals(80, image.getHeight());
    }

    @Test
    public void wideLogoFitsMaximumWidth() throws IOException {
        assertLogoSize(500, 100, 200, 40);
    }

    @Test
    public void tallLogoFitsMaximumHeight() throws IOException {
        assertLogoSize(100, 500, 16, 80);
    }

    @Test
    public void smallLogoIsNotEnlarged() throws IOException {
        assertLogoSize(50, 20, 50, 20);
    }

    @Test
    public void narrowLogoRetainsNonzeroDimensions() throws IOException {
        assertLogoSize(1, 512, 1, 80);
    }

    @Test
    public void replaceCorruptCachedLogo() throws IOException {
        Path cachedLogo = tempDirectory.resolve("postgresql_logo.png");
        Files.write(cachedLogo, new byte[] {1, 2, 3});
        HttpURLConnection connection = createConnection(createImage(216, 108));
        CDataDriverIconLoader.loadLogo("postgresql", tempDirectory, uri -> connection);
        Assertions.assertEquals(160, ImageIO.read(cachedLogo.toFile()).getWidth());
        Mockito.verify(connection).getInputStream();
    }

    @Test
    public void missingLogoDoesNotCreateCacheEntry() throws IOException {
        HttpURLConnection connection = Mockito.mock(HttpURLConnection.class);
        Mockito.when(connection.getResponseCode()).thenReturn(HttpURLConnection.HTTP_NOT_FOUND);
        Assertions.assertThrows(IOException.class,
            () -> CDataDriverIconLoader.loadLogo("postgresql", tempDirectory, uri -> connection));
        Assertions.assertFalse(Files.exists(tempDirectory.resolve("postgresql_logo.png")));
        Mockito.verify(connection).disconnect();
    }

    @Test
    public void rejectInvalidLogoImage() throws IOException {
        assertRejectedImage(new byte[] {1, 2, 3});
    }

    @Test
    public void rejectOversizedLogoImage() throws IOException {
        assertRejectedImage(new byte[512 * 1024 + 1]);
    }

    @Test
    public void rejectExcessiveLogoDimensions() throws IOException {
        assertRejectedImage(createImage(513, 108));
    }

    @Test
    public void rejectInvalidDataSourceBeforeRequest() {
        for (String dataSource : List.of("../postgresql", "postgresql/other", "", "Postgresql", "postgresql?query")) {
            Assertions.assertThrows(IllegalArgumentException.class,
                () -> CDataDriverIconLoader.loadLogo(dataSource, tempDirectory, uri -> {
                    throw new AssertionError("An invalid data source must not trigger a request");
                }));
        }
    }

    private void assertLogoSize(int width, int height, int expectedWidth, int expectedHeight) throws IOException {
        HttpURLConnection connection = createConnection(createImage(width, height));
        CDataDriverIconLoader.loadLogo("postgresql", tempDirectory, uri -> connection);
        BufferedImage image = ImageIO.read(tempDirectory.resolve("postgresql_logo.png").toFile());
        Assertions.assertEquals(expectedWidth, image.getWidth());
        Assertions.assertEquals(expectedHeight, image.getHeight());
    }

    private void assertRejectedImage(@NotNull byte[] content) throws IOException {
        HttpURLConnection connection = createConnection(content);
        Assertions.assertThrows(IOException.class,
            () -> CDataDriverIconLoader.loadLogo("postgresql", tempDirectory, uri -> connection));
        Assertions.assertFalse(Files.exists(tempDirectory.resolve("postgresql_logo.png")));
        Mockito.verify(connection).disconnect();
    }

    @NotNull
    private HttpURLConnection createConnection(@NotNull byte[] content) throws IOException {
        HttpURLConnection connection = Mockito.mock(HttpURLConnection.class);
        Mockito.when(connection.getResponseCode()).thenReturn(HttpURLConnection.HTTP_OK);
        Mockito.when(connection.getInputStream()).thenAnswer(invocation -> new ByteArrayInputStream(content));
        return connection;
    }

    @NotNull
    private byte[] createImage(int width, int height) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB), "png", output);
        return output.toByteArray();
    }
}
