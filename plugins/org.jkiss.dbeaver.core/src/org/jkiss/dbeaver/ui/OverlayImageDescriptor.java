/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui;

import org.eclipse.jface.resource.CompositeImageDescriptor;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.Point;

/**
 * An OverlayIcon consists of a main icon and several adornments.
 */
public class OverlayImageDescriptor extends CompositeImageDescriptor {

	static final int DEFAULT_WIDTH = 16;
	static final int DEFAULT_HEIGHT = 16;

	private Point imageSize = null;

    private ImageData baseImageData;
	private ImageDescriptor[] topLeft, topRight, bottomLeft, bottomRight;

    public OverlayImageDescriptor(ImageData baseImageData) {
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