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

import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.RGB;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.DBIcon;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

public class SVGUtilsTest {
    @Test
    public void replacesAllPaletteColors() {
        Map<String, String> palette = Map.of(
            "#71717A", "#C0C0C4", "#B7B1AD", "#8F8985", "#3387C5", "#6BB6F2",
            "#14B8A6", "#51B5A6", "#ED8000", "#E69250", "#4FA143", "#69C45C",
            "#B778F4", "#B284E3", "#F44D20", "#EF967F", "#FFFFFF", "#707070");
        for (var color : palette.entrySet()) {
            Assertions.assertEquals(
                "<path fill=\"" + color.getValue() + "\" stroke='" + color.getValue() + "'/>",
                SVGUtils.recolorSvg("<path fill=\"" + color.getKey() + "\" stroke='" + color.getKey() + "'/>", palette));
        }
    }

    @Test
    public void supportsStylesCaseAndShortHex() {
        String svg = "<path fill = '#3387c5' style=\"fill:#fff;stroke: #3387c5 !important;fill-opacity:.5\"/>"
            + "<style>.icon { fill: #3387C5; stroke:#fff }</style>";
        Assertions.assertEquals(
            "<path fill = '#6BB6F2' style=\"fill:#707070;stroke: #6BB6F2 !important;fill-opacity:.5\"/>"
                + "<style>.icon { fill: #6BB6F2; stroke:#707070 }</style>",
            SVGUtils.recolorSvg(svg, Map.of("#3387C5", "#6BB6F2", "#FFFFFF", "#707070")));
    }

    @Test
    public void treatsWhitePaintSpellingsIdentically() {
        Map<String, String> palette = Map.of("#FFFFFF", "#707070");
        for (String white : new String[]{"white", "WHITE", "White", "#fff", "#FFF", "#FFFFFF", "#ffffff"}) {
            String svg = "<path fill='" + white + "' stroke=\"" + white
                + "\" style='fill: " + white + ";stroke:" + white + " !important'/>";
            Assertions.assertEquals(
                "<path fill='#707070' stroke=\"#707070\" style='fill: #707070;stroke:#707070 !important'/>",
                SVGUtils.recolorSvg(svg, palette));
            Assertions.assertEquals(svg, SVGUtils.recolorSvg(svg, Map.of()));
        }
        String unrelated = "<path id='white' data-fill='white' fill='ghostwhite' style='color:white;--fill:white'/>";
        Assertions.assertEquals(unrelated, SVGUtils.recolorSvg(unrelated, palette));
    }

    @Test
    public void darkensActualFolderBacking() throws IOException {
        var descriptor = SVGUtils.createThemedDescriptor(
            DBIcon.TREE_FOLDER.getLocation(),
            () -> Map.of("#FFFFFF", "#707070", "#71717A", "#C0C0C4"));
        ImageData folder = descriptor.getImageData(100);
        Assertions.assertNotNull(folder);
        RGB backing = folder.palette.getRGB(folder.getPixel(4, 8));
        // The folder layers Default at 15% opacity over its Background backing.
        Assertions.assertTrue(backing.red >= 112 && backing.red < 140, backing.toString());
        Assertions.assertTrue(backing.green >= 112 && backing.green < 140, backing.toString());
        Assertions.assertTrue(backing.blue >= 112 && backing.blue < 140, backing.toString());
        Assertions.assertEquals(255, folder.getAlpha(4, 8));
    }

    @Test
    public void preservesNonPaintDataAndBrandColors() {
        String svg = "<path id='#3387C5' data-fill='#3387C5' stroke-width='2' fill='url(#3387C5)' "
            + "stroke='#123456' style='color:#3387C5;--fill:#3387C5;fill:none;stroke:currentColor'/>";
        Assertions.assertEquals(svg, SVGUtils.recolorSvg(svg, Map.of("#3387C5", "#6BB6F2")));
    }

    @Test
    public void doesNotCascadeSubstitutionsOrMutateSource() {
        String svg = "<path fill='#71717A' stroke='#FFFFFF'/>";
        Map<String, String> palette = Map.of("#71717A", "#FFFFFF", "#FFFFFF", "#707070");
        Assertions.assertEquals("<path fill='#FFFFFF' stroke='#707070'/>", SVGUtils.recolorSvg(svg, palette));
        Assertions.assertEquals(svg, SVGUtils.recolorSvg(svg, Map.of()));
        Assertions.assertEquals("<path fill='#FFFFFF' stroke='#707070'/>", SVGUtils.recolorSvg(svg, palette));
    }

    @Test
    public void scalesRootDimensionsWithoutChangingGeometry() {
        String svg = "<svg width=\"16\" height='12' viewBox='0 0 16 12'><rect width='4' height='3'/></svg>";
        Assertions.assertEquals(svg, SVGUtils.scaleSvg(svg, 100));
        Assertions.assertEquals(
            "<svg width=\"24.0\" height='18.0' viewBox='0 0 16 12'><rect width='4' height='3'/></svg>",
            SVGUtils.scaleSvg(svg, 150));
        Assertions.assertEquals("<svg viewBox='0 0 16 16'/>", SVGUtils.scaleSvg("<svg viewBox='0 0 16 16'/>", 200));
    }

    @Test
    public void doesNotThemeBrandArtworkOrExternalImages() {
        Assertions.assertFalse(SVGUtils.isThemeableSvg("file:/tmp/icon.svg"));
        Assertions.assertFalse(SVGUtils.isThemeableSvg(null));
        Assertions.assertFalse(SVGUtils.isThemeableSvg("platform:/plugin/org.jkiss.dbeaver.model/icons/idp/google.svg"));
        Assertions.assertFalse(SVGUtils.isThemeableSvg("platform:/plugin/org.jkiss.dbeaver.ext.db2/icons/db2_icon.svg"));
        Assertions.assertTrue(SVGUtils.isThemeableSvg("platform:/plugin/org.jkiss.dbeaver.model/icons/types/sum.svg"));
        Assertions.assertTrue(SVGUtils.isThemeableSvg("platform:/plugin/org.jkiss.dbeaver.model/icons/connection/database_icon.svg"));
    }

    @Test
    public void retainedDescriptorUsesCurrentPalette(@TempDir @NotNull Path directory) throws IOException {
        Path icon = directory.resolve("icon.svg");
        Files.writeString(icon, "<svg xmlns='http://www.w3.org/2000/svg' width='16' height='16'>"
            + "<rect width='16' height='16' fill='#3387C5'/></svg>");
        var palette = new AtomicReference<>(Map.of("#3387C5", "#3387C5"));
        var descriptor = SVGUtils.createThemedDescriptor(icon.toUri().toString(), palette::get);
        ImageData light = descriptor.getImageData(100);
        Assertions.assertNotNull(light);
        Assertions.assertEquals(new RGB(51, 135, 197), light.palette.getRGB(light.getPixel(8, 8)));

        palette.set(Map.of("#3387C5", "#6BB6F2"));
        ImageData dark = descriptor.getImageData(200);
        Assertions.assertNotNull(dark);
        Assertions.assertEquals(32, dark.width);
        Assertions.assertEquals(32, dark.height);
        Assertions.assertEquals(new RGB(107, 182, 242), dark.palette.getRGB(dark.getPixel(16, 16)));

        palette.set(Map.of("#3387C5", "#3387C5"));
        ImageData restored = descriptor.getImageData(100);
        Assertions.assertNotNull(restored);
        Assertions.assertEquals(light.palette.getRGB(light.getPixel(8, 8)), restored.palette.getRGB(restored.getPixel(8, 8)));
    }
}
