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
package org.jkiss.dbeaver.ui.svg;

//import com.github.weisj.jsvg.SVGDocument;
//import com.github.weisj.jsvg.parser.SVGLoader;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import javax.imageio.ImageIO;

public class SVGTest {
    @Test
    @Disabled("See #26434")
    public void rasterizeSvgAndCompareToPngTest() throws IOException {
        compareImages("logo.svg", "logo.png", 1.0f);
        compareImages("logo.svg", "logo@1.5x.png", 1.5f);
        compareImages("logo.svg", "logo@2x.png", 2.0f);
    }

    @Test
    @Disabled("See #26434")
    public void rasterizeSvgStressTest() throws IOException {
        final long start = System.currentTimeMillis();
        final int count = 1000;

        for (int i = 0; i < count; i++) {
            compareImages("logo.svg", "logo.png", 1.0f);
        }

        final long end = System.currentTimeMillis();
        System.out.println("Took " + (end - start) + "ms to rasterize " + count + " SVG images (average " + ((end - start) / count) + "ms per image)");
    }

    private static void compareImages(@NotNull String svgImagePath, @NotNull String pngImagePath, float factor) throws IOException {
        final BufferedImage expected;
        final BufferedImage actual;

        try (InputStream is = SVGTest.class.getResourceAsStream(svgImagePath)) {
            Assertions.assertNotNull(is, "Should be able to locate source SVG image at " + svgImagePath);
            actual = getImage(is, factor);
            Assertions.assertNotNull(actual, "Should be able to load source SVG image at " + svgImagePath);
        }

        try (InputStream is = SVGTest.class.getResourceAsStream(pngImagePath)) {
            Assertions.assertNotNull(is, "Should be able to locate target PNG image at " + pngImagePath);
            expected = ImageIO.read(is);
            Assertions.assertNotNull(expected, "Should be able to load target PNG image at " + pngImagePath);
        }

        compareImages(actual, expected);
    }

    private static void compareImages(@NotNull BufferedImage a, @NotNull BufferedImage b) {
        Assertions.assertEquals(a.getWidth(), b.getWidth());
        Assertions.assertEquals(a.getHeight(), b.getHeight());

        for (int x = 0; x < a.getWidth(); x++) {
            for (int y = 0; y < a.getHeight(); y++) {
                Assertions.assertEquals(a.getRGB(x, y), b.getRGB(x, y), "Pixel at %d-%d should be the same".formatted(x, y));
            }
        }
    }

    @Nullable
    private static BufferedImage getImage(@NotNull InputStream is, float factor) {
        return null;
//        final SVGLoader loader = new SVGLoader();
//        final SVGDocument document = loader.load(is);
//
//        if (document == null) {
//            return null;
//        }
//
//        final int width = (int) (document.size().width * factor);
//        final int height = (int) (document.size().height * factor);
//
//        final BufferedImage target = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
//        final Graphics2D graphics = target.createGraphics();
//
//        try {
//            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_DEFAULT);
//            graphics.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_DEFAULT);
//            graphics.scale(factor, factor);
//
//            document.render(null, graphics);
//        } finally {
//            graphics.dispose();
//        }
//
//        return target;
    }
}