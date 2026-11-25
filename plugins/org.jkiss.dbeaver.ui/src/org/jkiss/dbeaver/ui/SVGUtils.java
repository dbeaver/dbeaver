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
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Map;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * SVG paint substitution before rasterization. Source artwork always uses the light palette.
 */
public final class SVGUtils {
    private static final String PLUGIN_ICON_PREFIX = "platform:/plugin/";
    private static final Pattern PAINT = Pattern.compile(
        "(?i)((?<![\\w:-])(?:fill|stroke)\\s*=\\s*[\"']\\s*|(?<![\\w-])(?:fill|stroke)\\s*:\\s*)"
            + "(#[0-9a-f]{6}|#[0-9a-f]{3}|white)(?=\\s*(?:[\"';}]|!important|$))");
    private static final Pattern SVG_TAG = Pattern.compile("<svg\\b[^>]*>", Pattern.DOTALL);
    private static final Pattern SVG_SIZE = Pattern.compile("(\\b(?:width|height)\\s*=\\s*[\"'])([0-9]+(?:\\.[0-9]+)?)([\"'])");
    private static final Pattern SVG_VIEW_BOX = Pattern.compile("\\bviewBox\\s*=");

    private SVGUtils() {
    }

    static boolean isThemeableSvg(@Nullable String location) {
        if (location == null || !location.startsWith(PLUGIN_ICON_PREFIX) || !location.endsWith(".svg")) {
            return false;
        }
        int bundleEnd = location.indexOf('/', PLUGIN_ICON_PREFIX.length());
        if (bundleEnd < 0 || !location.startsWith("/icons/", bundleEnd)) {
            return false;
        }
        String bundleName = location.substring(PLUGIN_ICON_PREFIX.length(), bundleEnd);
        if (!bundleName.startsWith("org.jkiss.dbeaver.") && !bundleName.startsWith("com.dbeaver.")) {
            return false;
        }
        // Brand artwork is not part of the semantic icon palette.
        String name = location.substring(location.lastIndexOf('/') + 1);
        return !name.contains("_icon") && !name.contains("logo");
    }

    @NotNull
    static String toHex(@NotNull RGB color) {
        return String.format(Locale.ROOT, "#%02X%02X%02X", color.red, color.green, color.blue);
    }

    @NotNull
    static ImageDescriptor createThemedDescriptor(
        @NotNull String location,
        @NotNull Supplier<Map<String, String>> palette
    ) throws IOException {
        String svg;
        try (var stream = URI.create(location).toURL().openStream()) {
            svg = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
        // Resolve the palette at rendering time: actions may retain descriptors across theme changes.
        return ImageDescriptor.createFromImageDataProvider(zoom -> {
            String themed = recolorSvg(svg, palette.get());
            return new ImageData(new ByteArrayInputStream(scaleSvg(themed, zoom).getBytes(StandardCharsets.UTF_8)));
        });
    }

    /**
     * Replace only complete fill/stroke paints, not IDs, URLs, opacity, or unrelated attributes.
     * One pass is important when a target color is also a source color in the palette.
     */
    @NotNull
    static String recolorSvg(@NotNull String svg, @NotNull Map<String, String> palette) {
        Matcher paint = PAINT.matcher(svg);
        StringBuilder result = new StringBuilder();
        while (paint.find()) {
            String source = paint.group(2).toUpperCase(Locale.ROOT);
            if ("WHITE".equals(source)) {
                source = "#FFFFFF";
            } else if (source.length() == 4) {
                source = "#" + source.charAt(1) + source.charAt(1) + source.charAt(2) + source.charAt(2)
                    + source.charAt(3) + source.charAt(3);
            }
            String replacement = palette.get(source);
            paint.appendReplacement(result, Matcher.quoteReplacement(
                paint.group(1) + (replacement == null ? paint.group(2) : replacement)));
        }
        paint.appendTail(result);
        return result.toString();
    }

    @NotNull
    static String scaleSvg(@NotNull String svg, int zoom) {
        if (zoom == 100) {
            return svg;
        }
        Matcher tag = SVG_TAG.matcher(svg);
        if (!tag.find()) {
            return svg;
        }
        Matcher size = SVG_SIZE.matcher(tag.group());
        StringBuilder scaledTag = new StringBuilder();
        String width = null;
        String height = null;
        // Rasterize at the requested SWT zoom rather than scaling a rendered bitmap.
        while (size.find()) {
            if (size.group(1).startsWith("width")) {
                width = size.group(2);
            } else {
                height = size.group(2);
            }
            double dimension = Double.parseDouble(size.group(2)) * zoom / 100;
            size.appendReplacement(scaledTag, Matcher.quoteReplacement(size.group(1) + dimension + size.group(3)));
        }
        size.appendTail(scaledTag);
        if (width != null && height != null && !SVG_VIEW_BOX.matcher(tag.group()).find()) {
            // Without a viewBox, changing the viewport alone does not scale the artwork.
            scaledTag.insert(4, " viewBox=\"0 0 " + width + " " + height + "\"");
        }
        return svg.substring(0, tag.start()) + scaledTag + svg.substring(tag.end());
    }

    static boolean matchesOriginalIcon(@NotNull Image image, @NotNull String location) {
        // Preserve independently supplied title images, including decorated editor icons.
        try {
            ImageData current = image.getImageData(100);
            ImageData original = ImageDescriptor.createFromURL(URI.create(location).toURL()).getImageData(100);
            if (original == null || current.width != original.width || current.height != original.height) {
                return false;
            }
            for (int y = 0; y < current.height; y++) {
                for (int x = 0; x < current.width; x++) {
                    int alpha = current.getAlpha(x, y);
                    if (alpha != original.getAlpha(x, y)
                        || alpha != 0 && !current.palette.getRGB(current.getPixel(x, y)).equals(
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
