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

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
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
        DBIcon id;
        ImageDescriptor imageDescriptor;
        Image image;
    }

    private static Map<String, IconDescriptor> iconMap = new HashMap<String, IconDescriptor>();

    public static void initRegistry()
    {
        for (Field field : DBIcon.class.getDeclaredFields()) {
            if ((field.getModifiers() & Modifier.STATIC) == 0 || field.getType() != DBIcon.class) {
                continue;
            }
            try {
                DBIcon icon = (DBIcon) field.get(null);
                ImageDescriptor imageDescriptor = ImageDescriptor.createFromURL(new URL(icon.getLocation()));
                IconDescriptor iconDescriptor = new IconDescriptor();
                iconDescriptor.id = icon;
                iconDescriptor.imageDescriptor = imageDescriptor;
                iconDescriptor.image = imageDescriptor.createImage(false);
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

    public static Image getImage(DBIcon icon)
    {
        return getImage(icon.getToken());
    }

    public static ImageDescriptor getImageDescriptor(DBIcon icon)
    {
        return getImageDescriptor(icon.getToken());
    }

    public static Image createImage(DBIcon icon)
    {
        return createImage(icon.getToken());
    }

    public static Image getImage(String token)
    {
        IconDescriptor iconDescriptor = iconMap.get(token);
        if (iconDescriptor == null) {
            return null;
        }
        return iconDescriptor.image;
    }

    public static ImageDescriptor getImageDescriptor(String token)
    {
        IconDescriptor iconDescriptor = iconMap.get(token);
        if (iconDescriptor == null) {
            return null;
        }
        return iconDescriptor.imageDescriptor;
    }

    public static Image createImage(String token)
    {
        IconDescriptor iconDescriptor = iconMap.get(token);
        if (iconDescriptor == null) {
            return null;
        }
        return iconDescriptor.imageDescriptor.createImage();
    }
}
