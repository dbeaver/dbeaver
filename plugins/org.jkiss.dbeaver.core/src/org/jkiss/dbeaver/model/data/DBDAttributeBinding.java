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

import org.jkiss.dbeaver.model.exec.DBCAttributeMetaData;
import org.jkiss.dbeaver.model.struct.DBSAttributeBase;
import org.jkiss.dbeaver.model.struct.DBSEntityAttribute;

/**
 * Attribute value binding info
 */
public class DBDAttributeBinding {
    private final DBCAttributeMetaData metaAttribute;
    private final DBDValueHandler valueHandler;
    private final int attributeIndex;
    private DBSEntityAttribute entityAttribute;
    private DBDRowIdentifier rowIdentifier;

    public DBDAttributeBinding(DBCAttributeMetaData metaAttribute, DBDValueHandler valueHandler, int attributeIndex) {
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
    public String getAttributeName()
    {
        return metaAttribute.getName();
    }

    /**
     * Meta attribute (obtained from result set)
     */
    public DBCAttributeMetaData getMetaAttribute() {
        return metaAttribute;
    }

    /**
     * Attribute value handler
     */
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
    public DBDRowIdentifier getRowIdentifier() {
        return rowIdentifier;
    }

    public DBSAttributeBase getAttribute()
    {
        return entityAttribute == null ? metaAttribute : entityAttribute;
    }

    public void initValueLocator(DBSEntityAttribute entityAttribute, DBDRowIdentifier rowIdentifier) {
        this.entityAttribute = entityAttribute;
        this.rowIdentifier = rowIdentifier;
    }
}
