/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
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

    protected JDBCAttribute(String name, String typeName, int valueType, int ordinalPosition, long maxLength, Integer scale,
                            Integer precision, boolean required, boolean sequence)
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
