/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2016 Serge Rieder (serge@jkiss.org)
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
package org.jkiss.dbeaver.model.impl.jdbc.struct;

import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.*;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.struct.AbstractAttribute;
import org.jkiss.dbeaver.model.struct.DBSAttributeBase;
import org.jkiss.dbeaver.model.struct.DBSObject;

/**
 * JDBC abstract column
 */
public abstract class JDBCAttribute extends AbstractAttribute implements DBSObject, DBPImageProvider {

    protected JDBCAttribute()
    {
    }

    protected JDBCAttribute(String name, String typeName, int valueType, int ordinalPosition, long maxLength, int scale,
                            int precision, boolean required, boolean sequence)
    {
        super(name, typeName, valueType, ordinalPosition, maxLength, scale, precision, required, sequence);
    }

    // Copy constructor
    protected JDBCAttribute(DBSAttributeBase source)
    {
        super(source);
    }

    @Nullable
    @Override
    public DBPImage getObjectImage()
    {
        DBPImage columnImage = DBValueFormatting.getTypeImage(this);
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

    protected static DBPImage getOverlayImage(DBPImage columnImage, JDBCColumnKeyType keyType)
    {
        if (keyType == null || !(keyType.isInUniqueKey() || keyType.isInReferenceKey())) {
            return columnImage;
        }
        DBPImage overImage = null;
        if (keyType.isInUniqueKey()) {
            overImage = DBIcon.OVER_KEY;
        } else if (keyType.isInReferenceKey()) {
            overImage = DBIcon.OVER_REFERENCE;
        }
        if (overImage == null) {
            return columnImage;
        }
        return new DBIconComposite(columnImage, false, null, null, null, overImage);
    }

}
