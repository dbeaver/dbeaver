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

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.jkiss.dbeaver.model.DBPImage;
import org.jkiss.utils.CommonUtils;

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
        this.imageDescriptor = ImageDescriptor.createFromImageData(data);
    }

    public DBIconBinary(final String location, final Image image) {
        this.location = "image:" + location;
        this.image = image;
        this.imageDescriptor = ImageDescriptor.createFromImage(image);
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

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof DBIconBinary) {
            return CommonUtils.equalObjects(location, ((DBIconBinary) obj).location);
        }
        return false;
    }

    @Override
    public String toString() {
        return location;
    }

}
