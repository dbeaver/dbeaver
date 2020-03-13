/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
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

import org.eclipse.jface.resource.CompositeImageDescriptor;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.Point;

/**
 * Legacy version of OverlayImageDescriptor (for old Eclipse)
 */
@Deprecated
public class OverlayImageDescriptorLegacy extends CompositeImageDescriptor {

    static final int DEFAULT_WIDTH = 16;
    static final int DEFAULT_HEIGHT = 16;

    private Point imageSize = null;

    private ImageData baseImageData;
    private ImageDescriptor[] topLeft, topRight, bottomLeft, bottomRight;

    public OverlayImageDescriptorLegacy(ImageData baseImageData) {
        this.baseImageData = baseImageData;
        this.imageSize = new Point(baseImageData.width, baseImageData.height);
    }

    public void setTopLeft(ImageDescriptor[] topLeft)
    {
        this.topLeft = topLeft;
    }

    public void setTopRight(ImageDescriptor[] topRight)
    {
        this.topRight = topRight;
    }

    public void setBottomLeft(ImageDescriptor[] bottomLeft)
    {
        this.bottomLeft = bottomLeft;
    }

    public void setBottomRight(ImageDescriptor[] bottomRight)
    {
        this.bottomRight = bottomRight;
    }

    @Override
    protected void drawCompositeImage(int width, int height) {
        ImageData base = baseImageData;
        drawImage(base, 0, 0);
        if (topRight != null)
            drawTopRight(topRight);

        if (bottomRight != null)
            drawBottomRight(bottomRight);

        if (bottomLeft != null)
            drawBottomLeft(bottomLeft);

        if (topLeft != null)
            drawTopLeft(topLeft);
    }

    @Override
    protected Point getSize() {
        return imageSize;
    }

    protected void drawTopLeft(ImageDescriptor[] overlays) {
        if (overlays == null)
            return;
        int length = overlays.length;
        int x = 0;
        for (int i = 0; i < 3; i++) {
            if (i < length && overlays[i] != null) {
                ImageData id = overlays[i].getImageData();
                drawImage(id, x, 0);
                x += id.width;
            }
        }
    }
    protected void drawTopRight(ImageDescriptor[] overlays) {
        if (overlays == null)
            return;
        int length = overlays.length;
        int x = getSize().x;
        for (int i = 2; i >= 0; i--) {
            if (i < length && overlays[i] != null) {
                ImageData id = overlays[i].getImageData();
                x -= id.width;
                drawImage(id, x, 0);
            }
        }
    }

    protected void drawBottomLeft(ImageDescriptor[] overlays) {
        if (overlays == null)
            return;
        int length = overlays.length;
        int x = 0;
        for (int i = 0; i < 3; i++) {
            if (i < length && overlays[i] != null) {
                ImageData id = overlays[i].getImageData();
                drawImage(id, x, getSize().y - id.height);
                x += id.width;
            }
        }
    }
    protected void drawBottomRight(ImageDescriptor[] overlays) {
        if (overlays == null)
            return;
        int length = overlays.length;
        int x = getSize().x;
        for (int i = 2; i >= 0; i--) {
            if (i < length && overlays[i] != null) {
                ImageData id = overlays[i].getImageData();
                x -= id.width;
                drawImage(id, x, getSize().y - id.height);
            }
        }
    }

}