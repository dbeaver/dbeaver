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
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.DBCAttributeMetaData;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.struct.DBSAttributeBase;
import org.jkiss.dbeaver.model.struct.DBSDataType;
import org.jkiss.dbeaver.model.struct.DBSEntity;
import org.jkiss.dbeaver.model.struct.DBSEntityAttribute;
import org.jkiss.utils.CommonUtils;

import java.util.Collection;

/**
 * Attribute value binding info
 */
public class DBDAttributeBinding {
    @NotNull
    private final DBCAttributeMetaData metaAttribute;
    @NotNull
    private final DBDValueHandler valueHandler;
    private final int attributeIndex;
    @Nullable
    private DBSEntityAttribute entityAttribute;
    @Nullable
    private DBDRowIdentifier rowIdentifier;
    @Nullable
    private DBDAttributeBinding nestedBindings;

    public DBDAttributeBinding(@NotNull DBCAttributeMetaData metaAttribute, @NotNull DBDValueHandler valueHandler, int attributeIndex) {
        this.metaAttribute = metaAttribute;
        this.valueHandler = valueHandler;
        this.attributeIndex = attributeIndex;
    }

    /**
     * Attribute index in result set
     * @return attribute index (zero based)
     */
    public int getAttributeIndex()
    {
        return attributeIndex;
    }

    /**
     * Attribute name
     */
    @NotNull
    public String getAttributeName()
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
     * Attribute value handler
     */
    @NotNull
    public DBDValueHandler getValueHandler() {
        return valueHandler;
    }

    /**
     * Entity attribute (may be null)
     */
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

    @NotNull
    public DBSAttributeBase getAttribute()
    {
        return entityAttribute == null ? metaAttribute : entityAttribute;
    }

    public void initValueLocator(DBSEntityAttribute entityAttribute, DBDRowIdentifier rowIdentifier) {
        this.entityAttribute = entityAttribute;
        this.rowIdentifier = rowIdentifier;
    }

    public void readNestedBindings(@NotNull DBCSession session) throws DBException {
        DBSAttributeBase attribute = getAttribute();
        switch (attribute.getDataKind()) {
            case STRUCT:
            case OBJECT:
                DBSDataType dataType = DBUtils.resolveDataType(session.getProgressMonitor(), session.getDataSource(), attribute.getTypeName());
                if (dataType instanceof DBSEntity) {
                    Collection<? extends DBSEntityAttribute> nestedAttributes = ((DBSEntity) dataType).getAttributes(session.getProgressMonitor());
                    if (!CommonUtils.isEmpty(nestedAttributes)) {
                        int nestedIndex = 0;
                        for (DBSEntityAttribute nestedAttr : nestedAttributes) {

                        }
                    }
                }
                System.out.println(dataType);
                break;
        }

    }

    @Override
    public String toString() {
        return getAttributeName() + " [" + getAttributeIndex() + "]";
    }

}
