/*
 * Copyright (C) 2010-2014 Serge Rieder
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
package org.jkiss.dbeaver.model.data;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.DBCAttributeMetaData;
import org.jkiss.dbeaver.model.exec.DBCEntityMetaData;
import org.jkiss.dbeaver.model.struct.DBSAttributeBase;
import org.jkiss.dbeaver.model.struct.DBSEntity;
import org.jkiss.dbeaver.model.struct.DBSEntityAttribute;

/**
 * Map attribute value binding info
 */
public class DBDAttributeBindingMap extends DBDAttributeBinding implements DBCAttributeMetaData {
    @NotNull
    private DBSAttributeBase attribute;

    public DBDAttributeBindingMap(
        @NotNull DBPDataSource dataSource,
        @NotNull DBDAttributeBinding parent,
        @NotNull DBSAttributeBase attribute)
    {
        super(dataSource, parent, DBUtils.findValueHandler(dataSource, attribute));
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

    @NotNull
    @Override
    public DBSEntity getSource() {
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
        return parent.getMetaAttribute().getEntityMetaData();
    }

    /**
     * Attribute name
     */
    @NotNull
    public String getName()
    {
        return attribute.getName();
    }

    /**
     * Meta attribute (obtained from result set)
     */
    @NotNull
    public DBCAttributeMetaData getMetaAttribute() {
        return this;
    }

    /**
     * Entity attribute
     */
    @NotNull
    public DBSEntityAttribute getEntityAttribute()
    {
        return attribute instanceof DBSEntityAttribute ? (DBSEntityAttribute) attribute : null;
    }

    /**
     * Row identifier (may be null)
     */
    @Nullable
    public DBDRowIdentifier getRowIdentifier() {
        return parent.getRowIdentifier();
    }

}
