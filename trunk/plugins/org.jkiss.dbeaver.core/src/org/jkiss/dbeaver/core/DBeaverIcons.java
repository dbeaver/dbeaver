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

package org.jkiss.dbeaver.core;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.Image;
import org.jkiss.dbeaver.ui.DBIcon;
import org.osgi.framework.Bundle;

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 * DBeaverIcons
 */
public class DBeaverIcons
{
    private static class IconDescriptor {
        DBIcon id;
        ImageDescriptor imageDescriptor;
        Image image;
    }

    private static Map<String, IconDescriptor> iconMap = new HashMap<String, IconDescriptor>();

    static void initRegistry(Bundle coreBundle)
    {
        for (DBIcon icon : DBIcon.values()) {
            URL iconPath = coreBundle.getEntry(icon.getPath());
            if (iconPath != null) {
                try {
                    iconPath = FileLocator.toFileURL(iconPath);
                }
                catch (IOException ex) {
                    continue;
                }
                ImageDescriptor imageDescriptor = ImageDescriptor.createFromURL(iconPath);
                IconDescriptor iconDescriptor = new IconDescriptor();
                iconDescriptor.id = icon;
                iconDescriptor.imageDescriptor = imageDescriptor;
                iconDescriptor.image = imageDescriptor.createImage();
                iconMap.put(icon.getToken(), iconDescriptor);
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
