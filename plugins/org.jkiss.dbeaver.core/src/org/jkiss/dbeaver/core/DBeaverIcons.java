/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
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
