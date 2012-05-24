/*
 * Copyright (c) 2012, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.impl.jdbc.struct;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.Image;
import org.jkiss.dbeaver.ext.ui.IObjectImageProvider;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.struct.AbstractColumn;
import org.jkiss.dbeaver.ui.DBIcon;
import org.jkiss.dbeaver.ui.OverlayImageDescriptor;

import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;

/**
 * JDBC abstract column
 */
public abstract class JDBCColumn extends AbstractColumn implements IObjectImageProvider {
    protected JDBCColumn()
    {
    }

    protected JDBCColumn(String name, String typeName, int valueType, int ordinalPosition, long maxLength, int scale,
                         int precision, boolean nullable)
    {
        super(name, typeName, valueType, ordinalPosition, maxLength, scale, precision, nullable);
    }

    @Override
    public Image getObjectImage()
    {
        Image columnImage = JDBCUtils.getDataIcon(this).getImage();
        JDBCColumnKeyType keyType = getKeyType();
        if (keyType != null) {
            columnImage = getOverlayImage(columnImage, keyType);
        }
        return columnImage;
    }

    protected JDBCColumnKeyType getKeyType()
    {
        return null;
    }

    private static final Map<Image, Map<JDBCColumnKeyType, Image>> overlayCache = new IdentityHashMap<Image, Map<JDBCColumnKeyType, Image>>();

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
