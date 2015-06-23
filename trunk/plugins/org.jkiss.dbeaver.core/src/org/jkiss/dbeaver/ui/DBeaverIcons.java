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
import org.jkiss.dbeaver.core.Log;
import org.jkiss.dbeaver.model.DBIcon;
import org.jkiss.dbeaver.model.DBPImage;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
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
        ImageDescriptor imageDescriptor;
        Image image;

        public IconDescriptor(String id, ImageDescriptor imageDescriptor) {
            this.id = id;
            this.imageDescriptor = imageDescriptor;
            this.image = imageDescriptor.createImage(false);
        }
    }

    private static Map<String, IconDescriptor> iconMap = new HashMap<String, IconDescriptor>();

    static  {
        for (Field field : DBIcon.class.getDeclaredFields()) {
            if ((field.getModifiers() & Modifier.STATIC) == 0 || field.getType() != DBIcon.class) {
                continue;
            }
            try {
                DBIcon icon = (DBIcon) field.get(null);
                ImageDescriptor imageDescriptor = ImageDescriptor.createFromURL(new URL(icon.getLocation()));
                IconDescriptor iconDescriptor = new IconDescriptor(icon.getLocation(), imageDescriptor);
                if (iconDescriptor.image == null) {
                    log.warn("Bad image '" + icon.getToken() + "' location: " + icon.getLocation());
                    continue;
                }
                iconMap.put(icon.getToken(), iconDescriptor);
            } catch (Exception e) {
                log.error(e);
            }
        }
    }

    public static Collection<String> getPredefinedImages() {
        return new ArrayList<String>(iconMap.keySet());
    }

    public static Image getImage(DBPImage image)
    {
        IconDescriptor icon = getIconByLocation(image.getLocation());
        return icon == null ? null : icon.image;
    }

    public static ImageDescriptor getImageDescriptor(DBPImage image)
    {
        IconDescriptor icon = getIconByLocation(image.getLocation());
        return icon == null ? null : icon.imageDescriptor;
    }

    public static Image getImageById(String token)
    {
        IconDescriptor iconDescriptor = iconMap.get(token);
        if (iconDescriptor == null) {
            return null;
        }
        return iconDescriptor.image;
    }

    private static IconDescriptor getIconByLocation(String location) {
        IconDescriptor icon = iconMap.get(location);
        if (icon == null) {
            try {
                ImageDescriptor imageDescriptor = ImageDescriptor.createFromURL(new URL(location));
                icon = new IconDescriptor(location, imageDescriptor);
                if (icon.image == null) {
                    log.warn("Bad image: " + location);
                    return null;
                } else {
                    iconMap.put(location, icon);
                }
            } catch (Exception e) {
                log.error(e);
                return null;
            }
        }
        return icon;
    }

}
