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

import org.eclipse.swt.graphics.Image;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.ext.ui.IObjectImageProvider;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.DBCAttributeMetaData;
import org.jkiss.dbeaver.model.exec.DBCEntityMetaData;
import org.jkiss.dbeaver.model.struct.DBSEntity;
import org.jkiss.dbeaver.model.struct.DBSEntityAttribute;

/**
 * Type attribute value binding info
 */
public class DBDAttributeBindingType extends DBDAttributeBinding implements DBCAttributeMetaData, IObjectImageProvider {
    @NotNull
    private DBSEntityAttribute entityAttribute;

    public DBDAttributeBindingType(
        @NotNull DBPDataSource dataSource,
        @NotNull DBDAttributeBinding parent,
        @NotNull DBSEntityAttribute entityAttribute)
    {
        super(dataSource, parent, DBUtils.findValueHandler(dataSource, entityAttribute));
        this.entityAttribute = entityAttribute;
    }

    /**
     * Attribute index in result set
     * @return attribute index (zero based)
     */
    @Override
    public int getOrdinalPosition()
    {
        return entityAttribute.getOrdinalPosition();
    }

    @NotNull
    @Override
    public DBSEntity getSource() {
        return entityAttribute.getParentObject();
    }

    /**
     * Attribute label
     */
    @NotNull
    public String getLabel()
    {
        return entityAttribute.getName();
    }

    @Nullable
    @Override
    public String getEntityName() {
        return getSource().getName();
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
        return entityAttribute.getName();
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
        return entityAttribute;
    }

    /**
     * Row identifier (may be null)
     */
    @Nullable
    public DBDRowIdentifier getRowIdentifier() {
        return parent.getRowIdentifier();
    }

    @Nullable
    @Override
    public Image getObjectImage() {
        if (entityAttribute instanceof IObjectImageProvider) {
            return ((IObjectImageProvider) entityAttribute).getObjectImage();
        }
        return DBUtils.getDataIcon(this).getImage();
    }

}
