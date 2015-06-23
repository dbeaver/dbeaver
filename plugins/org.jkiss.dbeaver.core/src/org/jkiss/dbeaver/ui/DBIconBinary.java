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
import org.eclipse.swt.graphics.ImageData;
import org.jkiss.dbeaver.model.DBPImage;

/**
 * Image with binary data
 */
public class DBIconBinary implements DBPImage
{
    private final String location;
    private Image image;
    private ImageDescriptor imageDescriptor;

    public DBIconBinary(final String location, final ImageData data) {
        this.location = "binary:" + location;
        this.image = new Image(null, data);
        imageDescriptor = ImageDescriptor.createFromImageData(data);
    }

    public Image getImage() {
        return image;
    }

    public ImageDescriptor getImageDescriptor() {
        return imageDescriptor;
    }

    @Override
    public String getLocation() {
        return location;
    }
}
