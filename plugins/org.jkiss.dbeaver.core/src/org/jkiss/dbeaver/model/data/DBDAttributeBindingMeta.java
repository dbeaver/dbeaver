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
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.DBCAttributeMetaData;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.struct.DBSEntityAttribute;
import org.jkiss.dbeaver.model.struct.DBSEntityReferrer;

import java.util.List;

/**
 * Attribute value binding info
 */
public class DBDAttributeBindingMeta extends DBDAttributeBinding {
    @NotNull
    private final DBCAttributeMetaData metaAttribute;
    @Nullable
    private DBSEntityAttribute entityAttribute;
    @Nullable
    private DBDRowIdentifier rowIdentifier;
    @Nullable
    private List<DBSEntityReferrer> referrers;

    public DBDAttributeBindingMeta(
        @NotNull DBPDataSource dataSource,
        @Nullable DBDAttributeBindingMeta parent,
        @NotNull DBCAttributeMetaData metaAttribute)
    {
        super(dataSource, parent, DBUtils.findValueHandler(dataSource, metaAttribute));
        this.metaAttribute = metaAttribute;
    }

    /**
     * Attribute index in result set
     * @return attribute index (zero based)
     */
    @Override
    public int getOrdinalPosition()
    {
        return metaAttribute.getOrdinalPosition();
    }

    /**
     * Attribute label
     */
    @NotNull
    public String getLabel()
    {
        return metaAttribute.getLabel();
    }

    /**
     * Attribute name
     */
    @NotNull
    public String getName()
    {
        return metaAttribute.getName();
    }

    /**
     * Meta attribute (obtained from result set)
     */
    @NotNull
    public DBCAttributeMetaData getMetaAttribute() {
        return metaAttribute;
    }

    /**
     * Entity attribute (may be null)
     */
    @Nullable
    public DBSEntityAttribute getEntityAttribute()
    {
        return entityAttribute;
    }

    /**
     * Row identifier (may be null)
     */
    @Nullable
    public DBDRowIdentifier getRowIdentifier() {
        return rowIdentifier;
    }

    @Nullable
    @Override
    public List<DBSEntityReferrer> getReferrers() {
        return referrers;
    }

    public void setEntityAttribute(@Nullable DBSEntityAttribute entityAttribute) {
        this.entityAttribute = entityAttribute;
    }

    public void setRowIdentifier(@Nullable DBDRowIdentifier rowIdentifier) {
        this.rowIdentifier = rowIdentifier;
    }

    @Override
    public void lateBinding(@NotNull DBCSession session, List<Object[]> rows) throws DBException {
        DBSEntityAttribute entityAttribute = getEntityAttribute();
        if (entityAttribute != null) {
            referrers = DBUtils.getAttributeReferrers(session.getProgressMonitor(), entityAttribute);
        }
        super.lateBinding(session, rows);
    }
}
