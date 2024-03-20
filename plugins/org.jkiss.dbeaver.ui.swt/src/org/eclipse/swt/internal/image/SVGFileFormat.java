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
package org.eclipse.swt.internal.image;

import com.github.weisj.jsvg.SVGDocument;
import com.github.weisj.jsvg.parser.SVGLoader;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.ImageLoader;
import org.eclipse.swt.internal.DPIUtil;
import org.jkiss.dbeaver.ui.swt.ImageConverter;
import org.jkiss.dbeaver.utils.RuntimeUtils;

import java.awt.*;
import java.awt.image.BufferedImage;

@SuppressWarnings("unused")
public class SVGFileFormat extends FileFormat {
    @Override
    boolean isFileFormat(LEDataInputStream stream) {
        try {
            final byte[] sig = new byte[4];
            if (stream.read(sig) != 4) {
                return false;
            }
            stream.unread(sig);
            return sig[0] == '<' && sig[1] == '?' && sig[2] == 'x' && sig[3] == 'm'
                || sig[0] == '<' && sig[1] == 's' && sig[2] == 'v' && sig[3] == 'g';
        } catch (Exception ignored) {
            return false;
        }
    }

    @Override
    ImageData[] loadFromByteStream() {
        final SVGLoader loader = new SVGLoader();
        final SVGDocument document = loader.load(inputStream);

        if (document == null) {
            SWT.error(SWT.ERROR_INVALID_IMAGE);
            return null;
        }

        final float factor = getScalingFactor();
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

        return new ImageData[]{ImageConverter.convertToImageData(target)};
    }

    @Override
    void unloadIntoByteStream(ImageLoader loader) {
        SWT.error(SWT.ERROR_NOT_IMPLEMENTED);
    }

    private static float getScalingFactor() {
        if (RuntimeUtils.isMacOS() || DPIUtil.useCairoAutoScale()) {
            return 1.0f;
        } else {
            return DPIUtil.getDeviceZoom() / 100.0f;
        }
    }
}
