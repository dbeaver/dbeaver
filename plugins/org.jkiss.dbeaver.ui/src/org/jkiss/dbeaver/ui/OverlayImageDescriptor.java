/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2019 Serge Rider (serge@jkiss.org)
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
import org.eclipse.swt.graphics.Point;

/**
 * An OverlayIcon consists of a main icon and several adornments.
 */
public class OverlayImageDescriptor extends CompositeImageDescriptor {

	private Point imageSize = null;

    private CachedImageDataProvider baseImageData;
	private ImageDescriptor[] topLeft, topRight, bottomLeft, bottomRight;

    public OverlayImageDescriptor(ImageDescriptor baseImage) {
        this.baseImageData = createCachedImageDataProvider(baseImage);
        this.imageSize = new Point(baseImageData.getWidth(), baseImageData.getHeight());
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
        CachedImageDataProvider base = baseImageData;
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

    private void drawTopLeft(ImageDescriptor[] overlays) {
        if (overlays == null)
            return;
        int length = overlays.length;
        int x = 0;
        for (int i = 0; i < 3; i++) {
            if (i < length && overlays[i] != null) {
                CachedImageDataProvider idp = createCachedImageDataProvider(overlays[i]);
                drawImage(idp, x, 0);
                x += idp.getWidth();
            }
        }
    }
    private void drawTopRight(ImageDescriptor[] overlays) {
        if (overlays == null)
            return;
        int length = overlays.length;
        int x = getSize().x;
        for (int i = 2; i >= 0; i--) {
            if (i < length && overlays[i] != null) {
                CachedImageDataProvider idp = createCachedImageDataProvider(overlays[i]);
                x -= idp.getWidth();
                drawImage(idp, x, 0);
            }
        }
    }

	private void drawBottomLeft(ImageDescriptor[] overlays) {
		if (overlays == null)
			return;
		int length = overlays.length;
		int x = 0;
		for (int i = 0; i < 3; i++) {
			if (i < length && overlays[i] != null) {
                CachedImageDataProvider idp = createCachedImageDataProvider(overlays[i]);
				drawImage(idp, x, getSize().y - idp.getHeight());
				x += idp.getWidth();
			}
		}
	}
	private void drawBottomRight(ImageDescriptor[] overlays) {
		if (overlays == null)
			return;
		int length = overlays.length;
		int x = getSize().x;
		for (int i = 2; i >= 0; i--) {
			if (i < length && overlays[i] != null) {
                CachedImageDataProvider idp = createCachedImageDataProvider(overlays[i]);
				x -= idp.getWidth();
				drawImage(idp, x, getSize().y - idp.getHeight());
			}
		}
	}

}