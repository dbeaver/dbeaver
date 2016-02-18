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
package org.jkiss.dbeaver.model.impl.data;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.DBPImage;
import org.jkiss.dbeaver.model.DBPImageProvider;
import org.jkiss.dbeaver.model.DBPDataKind;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.data.DBDPseudoAttribute;
import org.jkiss.dbeaver.model.exec.DBCAttributeMetaData;
import org.jkiss.dbeaver.model.exec.DBCEntityMetaData;
import org.jkiss.dbeaver.model.struct.DBSAttributeBase;
import org.jkiss.dbeaver.model.struct.DBSEntity;
import org.jkiss.dbeaver.model.struct.DBSObject;

/**
 * Type attribute value binding info
 */
public class AttributeMetaDataProxy implements DBCAttributeMetaData, DBPImageProvider {
    @NotNull
    protected final DBSAttributeBase attribute;

    public AttributeMetaDataProxy(
        @NotNull DBSAttributeBase attribute)
    {
        this.attribute = attribute;
    }

    /**
     * Attribute index in result set
     * @return attribute index (zero based)
     */
    @Override
    public int getOrdinalPosition()
    {
        return attribute.getOrdinalPosition();
    }

    @Override
    public boolean isRequired() {
        return attribute.isRequired();
    }

    @Override
    public boolean isAutoGenerated() {
        return attribute.isAutoGenerated();
    }

    @Override
    public boolean isPseudoAttribute() {
        return attribute.isPseudoAttribute();
    }

    @Nullable
    @Override
    public DBSObject getSource() {
        if (attribute instanceof DBSObject) {
            return ((DBSObject)attribute).getParentObject();
        }
        return null;
    }

    /**
     * Attribute label
     */
    @NotNull
    public String getLabel()
    {
        return attribute.getName();
    }

    @Nullable
    @Override
    public String getEntityName() {
        DBSObject source = getSource();
        if (source instanceof DBSEntity) {
            return source.getName();
        }
        return null;
    }

    @Override
    public boolean isReadOnly() {
        return false;
    }

    @Nullable
    @Override
    public DBDPseudoAttribute getPseudoAttribute() {
        return null;
    }

    @Nullable
    @Override
    public DBCEntityMetaData getEntityMetaData() {
        return null;
    }

    /**
     * Attribute name
     */
    @NotNull
    public String getName()
    {
        return attribute.getName();
    }

    @Nullable
    @Override
    public DBPImage getObjectImage() {
        if (attribute instanceof DBPImageProvider) {
            return ((DBPImageProvider) attribute).getObjectImage();
        }
        return DBUtils.getDataIcon(this);
    }

    @Override
    public String getTypeName() {
        return attribute.getTypeName();
    }

    @Override
    public int getTypeID() {
        return attribute.getTypeID();
    }

    @Override
    public DBPDataKind getDataKind() {
        return attribute.getDataKind();
    }

    @Override
    public int getScale() {
        return attribute.getScale();
    }

    @Override
    public int getPrecision() {
        return attribute.getPrecision();
    }

    @Override
    public long getMaxLength() {
        return attribute.getMaxLength();
    }

    @Override
    public String toString() {
        return attribute.toString();
    }

    @Override
    public int hashCode() {
        return attribute.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof AttributeMetaDataProxy &&
            attribute.equals(((AttributeMetaDataProxy) obj).attribute);
    }
}
