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
package org.jkiss.dbeaver.ui.svg;

import com.github.weisj.jsvg.SVGDocument;
import com.github.weisj.jsvg.parser.SVGLoader;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.ImageData;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.Log;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Objects;

public class SVGImageDescriptor extends ImageDescriptor {
    private static final Log log = Log.getLog(SVGImageDescriptor.class);

    private final URL url;

    private SVGImageDescriptor(@NotNull URL url) {
        this.url = url;
    }

    @NotNull
    public static ImageDescriptor createFromURL(@Nullable URL url) {
        if (url != null && url.getPath().endsWith(".svg")) {
            return new SVGImageDescriptor(url);
        } else {
            return ImageDescriptor.createFromURL(url);
        }
    }

    @Override
    public ImageData getImageData(int zoom) {
        try (InputStream is = openStream()) {
            if (is == null) {
                return null;
            }

            final SVGLoader loader = new SVGLoader();
            final SVGDocument document = loader.load(is);

            if (document == null) {
                return null;
            }

            final float factor = zoom / 100.0f;
            final int width = (int) (document.size().width * factor);
            final int height = (int) (document.size().height * factor);

            final BufferedImage target = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            final Graphics2D graphics = target.createGraphics();

            try {
                graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_DEFAULT);
                graphics.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_DEFAULT);
                graphics.scale(factor, factor);

                document.render(null, graphics);
            } finally {
                graphics.dispose();
            }

            return ImageConverter.convertToImageData(target);
        } catch (IOException e) {
            log.error("Can't load SVG image", e);
            return null;
        }
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (object == null || getClass() != object.getClass()) return false;
        SVGImageDescriptor that = (SVGImageDescriptor) object;
        return Objects.equals(url, that.url);
    }

    @Override
    public int hashCode() {
        return Objects.hash(url);
    }

    @Nullable
    private InputStream openStream() throws IOException {
        final URL resolved = FileLocator.find(url);
        if (resolved == null) {
            return null;
        }
        return new BufferedInputStream(resolved.openStream());
    }
}
