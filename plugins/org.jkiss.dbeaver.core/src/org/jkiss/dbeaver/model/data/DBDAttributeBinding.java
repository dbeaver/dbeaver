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
import org.jkiss.dbeaver.model.DBPNamedObject;
import org.jkiss.dbeaver.model.DBPQualifiedObject;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.DBCAttributeMetaData;
import org.jkiss.dbeaver.model.exec.DBCNestedAttributeMetaData;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.struct.DBSAttributeBase;
import org.jkiss.dbeaver.model.struct.DBSDataType;
import org.jkiss.dbeaver.model.struct.DBSEntity;
import org.jkiss.dbeaver.model.struct.DBSEntityAttribute;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Attribute value binding info
 */
public class DBDAttributeBinding implements DBPNamedObject, DBPQualifiedObject {
    @NotNull
    private final DBPDataSource dataSource;
    @Nullable
    private final DBDAttributeBinding parent;
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
    private List<DBDAttributeBinding> nestedBindings;
    private int level;

    public DBDAttributeBinding(@NotNull DBPDataSource dataSource, @NotNull DBCAttributeMetaData metaAttribute, @NotNull DBDValueHandler valueHandler, int attributeIndex) {
        this(dataSource, null, metaAttribute, valueHandler, attributeIndex);
    }

    public DBDAttributeBinding(@NotNull DBPDataSource dataSource, @Nullable DBDAttributeBinding parent, @NotNull DBCAttributeMetaData metaAttribute, @NotNull DBDValueHandler valueHandler, int attributeIndex) {
        this.dataSource = dataSource;
        this.parent = parent;
        this.metaAttribute = metaAttribute;
        this.valueHandler = valueHandler;
        this.attributeIndex = attributeIndex;
        this.level = (parent == null ? 0 : parent.level + 1);
    }

    public DBPDataSource getDataSource() {
        return dataSource;
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
     * Attribute value handler
     */
    @NotNull
    public DBDValueHandler getValueHandler() {
        return valueHandler;
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

    @NotNull
    public DBSAttributeBase getAttribute()
    {
        return entityAttribute == null ? metaAttribute : entityAttribute;
    }

    @Nullable
    public List<DBDAttributeBinding> getNestedBindings() {
        return nestedBindings;
    }

    public boolean hasNestedBindings() {
        return nestedBindings != null;
    }

    @Nullable
    public DBDAttributeBinding getParent() {
        return parent;
    }

    /**
     * Get parent by level.
     * @param grand 0 - self, 1 - direct parent, 2 - grand parent, etc
     * @return parent or null
     */
    @Nullable
    public DBDAttributeBinding getParent(int grand) {
        if (grand == 0) {
            return this;
        }
        DBDAttributeBinding p = this;
        for (int i = 0; i < grand; i++) {
            p = p.parent;
        }
        return p;
    }

    @NotNull
    public DBDAttributeBinding getTopParent() {
        if (parent == null) {
            return this;
        }
        for (DBDAttributeBinding binding = parent; ;binding = binding.parent) {
            if (binding.parent == null) {
                return binding;
            }
        }
    }

    /**
     * Attribute level. Zero based
     * @return attribute level (depth)
     */
    public int getLevel() {
        return level;
    }

    public void initValueLocator(@Nullable DBSEntityAttribute entityAttribute, @Nullable DBDRowIdentifier rowIdentifier) {
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
                    if (nestedAttributes != null && !nestedAttributes.isEmpty()) {
                        createNestedBindings(session, nestedAttributes);
                    }
                }
                break;
        }

    }

    private void createNestedBindings(DBCSession session, Collection<? extends DBSEntityAttribute> nestedAttributes) throws DBException {
        nestedBindings = new ArrayList<DBDAttributeBinding>();
        int nestedIndex = 0;
        for (DBSEntityAttribute nestedAttr : nestedAttributes) {
            DBCAttributeMetaData nestedMeta = new DBCNestedAttributeMetaData(nestedAttr, nestedIndex, metaAttribute);
            DBDValueHandler nestedHandler = DBUtils.findValueHandler(session, nestedAttr);
            DBDAttributeBinding nestedBinding = new DBDAttributeBinding(dataSource, this, nestedMeta, nestedHandler, nestedIndex);
            nestedBinding.initValueLocator(nestedAttr, rowIdentifier);
            nestedBinding.readNestedBindings(session);
            nestedBindings.add(nestedBinding);
            nestedIndex++;
        }
    }

    @Override
    public String toString() {
        return getName() + " [" + getAttributeIndex() + "]";
    }

    @Override
    public String getFullQualifiedName() {
        if (parent == null) {
            return DBUtils.getQuotedIdentifier(dataSource, getName());
        }
        StringBuilder query = new StringBuilder();
        boolean hasPrevIdentifier = false;
        for (DBDAttributeBinding attribute = this; attribute != null; attribute = attribute.getParent()) {
            if (hasPrevIdentifier) {
                query.insert(0, '.');
            }
            query.insert(0, DBUtils.getQuotedIdentifier(dataSource, attribute.getName()));
            hasPrevIdentifier = true;
        }

        return query.toString();
    }

}
