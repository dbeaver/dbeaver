/*
 * Copyright (C) 2010-2015 Serge Rieder
 * serge@jkiss.org
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package org.jkiss.dbeaver.model.impl.jdbc.struct;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.Image;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.ext.ui.IObjectImageProvider;
import org.jkiss.dbeaver.model.DBPDataKind;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.struct.AbstractAttribute;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.ui.DBIcon;
import org.jkiss.dbeaver.ui.OverlayImageDescriptor;

import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;

/**
 * JDBC abstract column
 */
public abstract class JDBCColumn extends AbstractAttribute implements DBSObject, IObjectImageProvider {

    private static final Map<Image, Map<JDBCColumnKeyType, Image>> overlayCache = new IdentityHashMap<Image, Map<JDBCColumnKeyType, Image>>();

    protected JDBCColumn()
    {
    }

    protected JDBCColumn(String name, String typeName, int valueType, int ordinalPosition, long maxLength, int scale,
                         int precision, boolean required, boolean sequence)
    {
        super(name, typeName, valueType, ordinalPosition, maxLength, scale, precision, required, sequence);
    }

    @Nullable
    @Override
    public Image getObjectImage()
    {
        Image columnImage = DBUtils.getDataIcon(this).getImage();
        JDBCColumnKeyType keyType = getKeyType();
        if (keyType != null) {
            columnImage = getOverlayImage(columnImage, keyType);
        }
        return columnImage;
    }

    @Nullable
    protected JDBCColumnKeyType getKeyType()
    {
        return null;
    }

    @Override
    public DBPDataKind getDataKind()
    {
        return JDBCUtils.resolveDataKind(getDataSource(), typeName, valueType);
    }

    protected static Image getOverlayImage(Image columnImage, JDBCColumnKeyType keyType)
    {
        if (keyType == null || !(keyType.isInUniqueKey() || keyType.isInReferenceKey())) {
            return columnImage;
        }
        synchronized (overlayCache) {
            Map<JDBCColumnKeyType, Image> keyTypeImageMap = overlayCache.get(columnImage);
            if (keyTypeImageMap == null) {
                keyTypeImageMap = new HashMap<JDBCColumnKeyType, Image>();
                overlayCache.put(columnImage, keyTypeImageMap);
            }
            Image finalImage = keyTypeImageMap.get(keyType);
            if (finalImage == null) {
                OverlayImageDescriptor overlay = new OverlayImageDescriptor(columnImage.getImageData());
                ImageDescriptor overImage = null;
                if (keyType.isInUniqueKey()) {
                    overImage = DBIcon.OVER_KEY.getImageDescriptor();
                } else if (keyType.isInReferenceKey()) {
                    overImage = DBIcon.OVER_REFERENCE.getImageDescriptor();
                }
                if (overImage == null) {
                    return columnImage;
                }
                overlay.setBottomRight(new ImageDescriptor[] {overImage} );
                finalImage = overlay.createImage();
                keyTypeImageMap.put(keyType, finalImage);
            }

            return finalImage;
        }
    }

}
