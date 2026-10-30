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

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.ui.PlatformUI;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.DBIcon;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class SVGUtils {
    static final String SVG_BLUE = "#3387C5";
    static final String SVG_BLUE_LIGHT = "org.jkiss.dbeaver.ui.icons.blue.light";
    static final String SVG_BLUE_DARK = "org.jkiss.dbeaver.ui.icons.blue.dark";

    private static final String SVG_BLUE_TYPE = "#338ECC";
    private static final String SVG_BLUE_UNKNOWN = "#338BC9";
    private static final String SVG_BLUE_DARK_DEFAULT = "#C62828";
    private static final Pattern BLUE_PAINT = Pattern.compile(
        "(?i)(\\b(?:fill|stroke)\\s*=\\s*[\"'])(?:" + SVG_BLUE + "|" + SVG_BLUE_TYPE + "|" + SVG_BLUE_UNKNOWN + ")(?=[\"'])");
    private static final Pattern SVG_TAG = Pattern.compile("<svg\\b[^>]*>", Pattern.DOTALL);
    private static final Pattern SVG_SIZE = Pattern.compile("(\\b(?:width|height)\\s*=\\s*[\"'])([0-9]+(?:\\.[0-9]+)?)([\"'])");

    private SVGUtils() {
    }

    static boolean isThemeableSvg(@Nullable String location) {
        return location != null && location.endsWith(".svg") && DBIcon.isRegisteredIconLocation(location);
    }

    @NotNull
    static String getThemeColor() {
        boolean dark = UIStyles.isDarkTheme();
        String colorId = dark ? SVG_BLUE_DARK : SVG_BLUE_LIGHT;
        RGB color = PlatformUI.getWorkbench().getThemeManager().getCurrentTheme().getColorRegistry().getRGB(colorId);
        return color == null ? (dark ? SVG_BLUE_DARK_DEFAULT : SVG_BLUE) :
            String.format("#%02X%02X%02X", color.red, color.green, color.blue);
    }

    @NotNull
    static ImageDescriptor createRecoloredSvgDescriptor(@NotNull String location, @NotNull String color) throws IOException {
        try (var stream = URI.create(location).toURL().openStream()) {
            String svg = recolorSvg(new String(stream.readAllBytes(), StandardCharsets.UTF_8), color);
            return ImageDescriptor.createFromImageDataProvider(zoom -> {
                byte[] bytes = scaleSvg(svg, zoom).getBytes(StandardCharsets.UTF_8);
                return new ImageData(new ByteArrayInputStream(bytes));
            });
        }
    }

    @NotNull
    static String recolorSvg(@NotNull String svg, @NotNull String color) {
        return BLUE_PAINT.matcher(svg).replaceAll("$1" + color);
    }

    @NotNull
    private static String scaleSvg(@NotNull String svg, int zoom) {
        if (zoom == 100) {
            return svg;
        }
        Matcher tag = SVG_TAG.matcher(svg);
        if (!tag.find()) {
            return svg;
        }
        Matcher size = SVG_SIZE.matcher(tag.group());
        StringBuilder scaledTag = new StringBuilder();
        // Rasterize at the requested SWT zoom rather than scaling an already-rendered bitmap.
        while (size.find()) {
            double dimension = Double.parseDouble(size.group(2)) * zoom / 100;
            size.appendReplacement(scaledTag, Matcher.quoteReplacement(
                size.group(1) + dimension + size.group(3)));
        }
        size.appendTail(scaledTag);
        return svg.substring(0, tag.start()) + scaledTag + svg.substring(tag.end());
    }

    static boolean matchesOriginalIcon(@NotNull Image image, @NotNull String location) {
        // Only replace the default view icon; preserve independently supplied title images.
        try {
            ImageData current = image.getImageData(100);
            ImageData original = ImageDescriptor.createFromURL(URI.create(location).toURL()).getImageData(100);
            if (original == null || current.width != original.width || current.height != original.height) {
                return false;
            }
            for (int y = 0; y < current.height; y++) {
                for (int x = 0; x < current.width; x++) {
                    int alpha = current.getAlpha(x, y);
                    if (alpha != original.getAlpha(x, y) ||
                        alpha != 0 && !current.palette.getRGB(current.getPixel(x, y)).equals(
                            original.palette.getRGB(original.getPixel(x, y)))) {
                        return false;
                    }
                }
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
