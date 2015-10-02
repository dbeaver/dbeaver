/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2015 Serge Rieder (serge@jkiss.org)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (version 2)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package org.jkiss.dbeaver.ui;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.Image;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBIconComposite;
import org.jkiss.dbeaver.model.DBPImage;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 * DBeaverIcons
 */
public class DBeaverIcons
{
    static final Log log = Log.getLog(DBeaverIcons.class);

    private static class IconDescriptor {
        String id;
        Image image;
        ImageDescriptor imageDescriptor;

        public IconDescriptor(String id, ImageDescriptor imageDescriptor) {
            this.id = id;
            this.image = imageDescriptor.createImage(false);
            this.imageDescriptor = imageDescriptor;
        }
        public IconDescriptor(String id, Image image) {
            this.id = id;
            this.image = image;
            this.imageDescriptor = ImageDescriptor.createFromImage(image);
        }
    }

    private static Map<String, IconDescriptor> imageMap = new HashMap<>();
    private static Map<String, IconDescriptor> compositeMap = new HashMap<>();

    public static Image getImage(DBPImage image)
    {
        if (image == null) {
            return null;
        } else if (image instanceof DBIconBinary) {
            return ((DBIconBinary) image).getImage();
        } else {
            IconDescriptor icon = getIconByLocation(image.getLocation());
            if (icon == null) {
                return null;
            } else if (image instanceof DBIconComposite) {
                return getCompositeIcon(icon, (DBIconComposite) image).image;
            } else {
                return icon.image;
            }
        }
    }

    public static ImageDescriptor getImageDescriptor(DBPImage image)
    {
        if (image == null) {
            return null;
        } else if (image instanceof DBIconBinary) {
            return ((DBIconBinary) image).getImageDescriptor();
        } else {
            IconDescriptor icon = getIconByLocation(image.getLocation());
            if (icon == null) {
                return null;
            } else if (image instanceof DBIconComposite) {
                return getCompositeIcon(icon, (DBIconComposite) image).imageDescriptor;
            } else {
                return icon.imageDescriptor;
            }
        }
    }

    private static IconDescriptor getCompositeIcon(IconDescriptor mainIcon, DBIconComposite image) {
        if (!image.hasOverlays()) {
            return mainIcon;
        }
        String compositeId = mainIcon.id + "^" +
            (image.getTopLeft() == null ? "" : image.getTopLeft().getLocation()) + "^" +
            (image.getTopRight() == null ? "" : image.getTopRight().getLocation()) + "^" +
            (image.getBottomLeft() == null ? "" : image.getBottomLeft().getLocation()) + "^" +
            (image.getBottomRight() == null ? "" : image.getBottomRight().getLocation());
        IconDescriptor icon = compositeMap.get(compositeId);
        if (icon == null) {
            OverlayImageDescriptor ovrImage = new OverlayImageDescriptor(mainIcon.image.getImageData());
            if (image.getTopLeft() != null) ovrImage.setTopLeft(new ImageDescriptor[] { getImageDescriptor(image.getTopLeft())} );
            if (image.getTopRight() != null) ovrImage.setTopRight(new ImageDescriptor[]{ getImageDescriptor(image.getTopRight()) });
            if (image.getBottomLeft() != null) ovrImage.setBottomLeft(new ImageDescriptor[]{getImageDescriptor(image.getBottomLeft())});
            if (image.getBottomRight() != null) ovrImage.setBottomRight(new ImageDescriptor[]{getImageDescriptor(image.getBottomRight())});
            Image resultImage = ovrImage.createImage();
            icon = new IconDescriptor(compositeId, resultImage);
            compositeMap.put(compositeId, icon);
        }
        return icon;
    }

    private static IconDescriptor getIconByLocation(String location) {
        IconDescriptor icon = imageMap.get(location);
        if (icon == null) {
            try {
                ImageDescriptor imageDescriptor = ImageDescriptor.createFromURL(new URL(location));
                icon = new IconDescriptor(location, imageDescriptor);
                if (icon.image == null) {
                    log.warn("Bad image: " + location);
                    return null;
                } else {
                    imageMap.put(location, icon);
                }
            } catch (Exception e) {
                log.error(e);
                return null;
            }
        }
        return icon;
    }

}
