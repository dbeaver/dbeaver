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
import org.jkiss.dbeaver.model.DBPQualifiedObject;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.DBCAttributeMetaData;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.struct.*;
import org.jkiss.utils.CommonUtils;

import java.util.*;

/**
 * Base attribute binding
 */
public abstract class DBDAttributeBinding implements DBSObject, DBSAttributeBase, DBPQualifiedObject {
    @NotNull
    protected final DBPDataSource dataSource;
    @Nullable
    protected final DBDAttributeBinding parent;
    @NotNull
    protected final DBDValueHandler valueHandler;
    @Nullable
    private List<DBDAttributeBinding> nestedBindings;

    protected DBDAttributeBinding(
        @NotNull DBPDataSource dataSource,
        @Nullable DBDAttributeBinding parent,
        @NotNull DBDValueHandler valueHandler)
    {
        this.dataSource = dataSource;
        this.parent = parent;
        this.valueHandler = valueHandler;
    }

    @NotNull
    public DBPDataSource getDataSource() {
        return dataSource;
    }

    /**
     * Attribute index in result set
     * @return attribute index (zero based)
     */
    @Override
    public abstract int getOrdinalPosition();

    /**
     * Attribute label
     */
    @NotNull
    public abstract String getLabel();

    /**
     * Attribute name
     */
    @NotNull
    public abstract String getName();

    /**
     * Meta attribute (obtained from result set)
     */
    @NotNull
    public abstract DBCAttributeMetaData getMetaAttribute();

    /**
     * Entity attribute (may be null)
     */
    @Nullable
    public abstract DBSEntityAttribute getEntityAttribute();

    /**
     * Row identifier (may be null)
     */
    @Nullable
    public abstract DBDRowIdentifier getRowIdentifier();

    @Nullable
    public abstract List<DBSEntityReferrer> getReferrers();

    @Nullable
    public abstract Object extractNestedValue(@NotNull Object ownerValue)
        throws DBCException;

    /**
     * Attribute value handler
     */
    @NotNull
    public DBDValueHandler getValueHandler() {
        return valueHandler;
    }

    @NotNull
    public DBSAttributeBase getAttribute()
    {
        DBSEntityAttribute attr = getEntityAttribute();
        return attr == null ? getMetaAttribute() : attr;
    }

    public boolean matches(DBSAttributeBase attr) {
        return attr != null && (this == attr || getMetaAttribute() == attr || getEntityAttribute() == attr);
    }

    @Nullable
    public List<DBDAttributeBinding> getNestedBindings() {
        return nestedBindings;
    }

    public boolean hasNestedBindings() {
        return nestedBindings != null;
    }

    @Nullable
    @Override
    public String getDescription() {
        return null;
    }

    @Nullable
    public DBDAttributeBinding getParentObject() {
        return parent;
    }

    @Override
    public String getFullQualifiedName() {
        if (parent == null) {
            return DBUtils.getQuotedIdentifier(dataSource, getName());
        }
        StringBuilder query = new StringBuilder();
        boolean hasPrevIdentifier = false;
        for (DBDAttributeBinding attribute = this; attribute != null; attribute = attribute.parent) {
            if (hasPrevIdentifier) {
                query.insert(0, '.');
            }
            query.insert(0, DBUtils.getQuotedIdentifier(dataSource, attribute.getName()));
            hasPrevIdentifier = true;
        }

        return query.toString();
    }

    @Override
    public boolean isPersisted() {
        return false;
    }

    @Override
    public String toString() {
        return getName() + " [" + getOrdinalPosition() + "]";
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
            assert p != null;
            p = p.parent;
        }
        return p;
    }

    @NotNull
    public DBDAttributeBinding getTopParent() {
        for (DBDAttributeBinding binding = this; binding != null ;binding = binding.parent) {
            if (binding.parent == null) {
                return binding;
            }
        }
        return this;
    }

    /**
     * Attribute level. Zero based
     * @return attribute level (depth)
     */
    public int getLevel() {
        if (parent == null) {
            return 0;
        }
        int level = 0;
        for (DBDAttributeBinding binding = parent; binding != null; binding = binding.parent) {
            level++;
        }
        return level;
    }

    public void lateBinding(@NotNull DBCSession session, List<Object[]> rows) throws DBException {
        DBSAttributeBase attribute = getAttribute();
        switch (attribute.getDataKind()) {
            case STRUCT:
            case OBJECT:
                DBSDataType dataType = DBUtils.resolveDataType(session.getProgressMonitor(), session.getDataSource(), attribute.getTypeName());
                if (dataType instanceof DBSEntity) {
                    createNestedTypeBindings(session, (DBSEntity) dataType, rows);
                    return;
                }
                // Data type was not resolved - let's threat it as ANY
            case ANY:
                // Nested binding must be resolved for each value
                // Analyse all read values
                resolveMapsFromData(session, rows);
                break;
            case ARRAY:
                //
                DBSDataType collectionType = DBUtils.resolveDataType(session.getProgressMonitor(), session.getDataSource(), attribute.getTypeName());
                if (collectionType != null) {
                    DBSDataType componentType = collectionType.getComponentType(session.getProgressMonitor());
                    if (componentType instanceof DBSEntity) {
                        createNestedTypeBindings(session, (DBSEntity) componentType, rows);
                        return;
                    }
                }
                // No component type found.
                // Array items should be resolved in a lazy mode
                break;
        }

    }

    private void resolveMapsFromData(DBCSession session, List<Object[]> rows) throws DBException {
        Map<DBSAttributeBase, Object> valueAttributes = new LinkedHashMap<DBSAttributeBase, Object>();
        for (int i = 0; i < rows.size(); i++) {
            Object value = rows.get(i)[getOrdinalPosition()];
            if (value instanceof DBDStructure) {
                DBSAttributeBase[] attributes = ((DBDStructure) value).getAttributes();
                if (attributes != null) {
                    for (DBSAttributeBase attr : attributes) {
                        valueAttributes.put(attr, ((DBDStructure) value).getAttributeValue(attr));
                    }
                }
            }
        }
        if (!valueAttributes.isEmpty()) {
            createNestedMapBindings(session, valueAttributes);
        }
    }

    private void createNestedMapBindings(DBCSession session, Map<DBSAttributeBase, Object> nestedAttributes) throws DBException {
        nestedBindings = new ArrayList<DBDAttributeBinding>();
        int maxPosition = 0;
        for (DBSAttributeBase attr : nestedAttributes.keySet()) {
            maxPosition = Math.max(maxPosition, attr.getOrdinalPosition());
        }
        Object[] fakeRow = new Object[maxPosition + 1];

        List<Object[]> fakeRows = Collections.singletonList(fakeRow);
        for (Map.Entry<DBSAttributeBase, Object> nestedAttr : nestedAttributes.entrySet()) {
            DBSAttributeBase attribute = nestedAttr.getKey();
            fakeRow[attribute.getOrdinalPosition()] = nestedAttr.getValue();
            DBDAttributeBindingType nestedBinding = new DBDAttributeBindingType(this, attribute);
            nestedBinding.lateBinding(session, fakeRows);
            nestedBindings.add(nestedBinding);
        }
    }

    private void createNestedTypeBindings(DBCSession session, DBSEntity type, List<Object[]> rows) throws DBException {
        nestedBindings = new ArrayList<DBDAttributeBinding>();
        for (DBSEntityAttribute nestedAttr : CommonUtils.safeCollection(type.getAttributes(session.getProgressMonitor()))) {
            DBDAttributeBindingType nestedBinding = new DBDAttributeBindingType(this, nestedAttr);
            nestedBinding.lateBinding(session, rows);
            nestedBindings.add(nestedBinding);
        }
    }

}
