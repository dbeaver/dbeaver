/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2015 Serge Rieder (serge@jkiss.org)
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
import org.jkiss.utils.Pair;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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

    public boolean matches(DBSAttributeBase attr, boolean searchByName) {
        if (attr != null && (this == attr || getMetaAttribute() == attr || getEntityAttribute() == attr)) {
            return true;
        }
        if (searchByName) {
            if (attr instanceof DBDAttributeBinding) {
                if (getLevel() != ((DBDAttributeBinding) attr).getLevel()) {
                    return false;
                }
                // Match all hierarchy names
                for (DBDAttributeBinding a1 = (DBDAttributeBinding) attr, a2 = this; a1 != null && a2 != null; a1 = a1.getParentObject(), a2 = a2.getParentObject()) {
                    if (!attr.getName().equals(this.getName())) {
                        return false;
                    }
                }
                return true;
            } else if (attr != null) {
                return attr.getName().equals(this.getName());
            }
        }
        return false;
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
            if (attribute.isPseudoAttribute()) {
                // Skip pseudo attributes (e.g. Mongo root document)
                continue;
            }
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
            case DOCUMENT:
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
                resolveMapsFromData(session, rows);
                break;
        }

    }

    private void resolveMapsFromData(DBCSession session, List<Object[]> rows) throws DBException {
        // Analyse rows and extract meta information from values
        List<Pair<DBSAttributeBase, Object[]>> valueAttributes = new ArrayList<Pair<DBSAttributeBase, Object[]>>();
        for (int i = 0; i < rows.size(); i++) {
            Object value = rows.get(i)[getOrdinalPosition()];
            if (value instanceof DBDCollection) {
                // Use first element to discover structure
                DBDCollection collection = (DBDCollection) value;
                if (collection.getItemCount() > 0) {
                    value = collection.getItem(0);
                }
            }

            if (value instanceof DBDStructure) {
                DBSAttributeBase[] attributes = ((DBDStructure) value).getAttributes();
                for (DBSAttributeBase attr : attributes) {
                    Pair<DBSAttributeBase, Object[]> attrValue = null;
                    for (Pair<DBSAttributeBase, Object[]> pair : valueAttributes) {
                        if (pair.getFirst().getName().equals(attr.getName())) {
                            attrValue = pair;
                            break;
                        }
                    }
                    if (attrValue != null) {
                        // Update attr value
                        attrValue.getSecond()[i] = ((DBDStructure) value).getAttributeValue(attr);
                    } else {
                        Object[] valueList = new Object[rows.size()];
                        valueList[i] = ((DBDStructure) value).getAttributeValue(attr);
                        valueAttributes.add(
                            new Pair<DBSAttributeBase, Object[]>(
                                attr,
                                valueList));
                    }
                }
            }
        }
        if (!valueAttributes.isEmpty()) {
            createNestedMapBindings(session, valueAttributes);
        }
    }

    private void createNestedMapBindings(DBCSession session, List<Pair<DBSAttributeBase, Object[]>> nestedAttributes) throws DBException {
        int maxPosition = 0;
        for (Pair<DBSAttributeBase, Object[]> attr : nestedAttributes) {
            maxPosition = Math.max(maxPosition, attr.getFirst().getOrdinalPosition());
        }
        if (nestedBindings == null) {
            nestedBindings = new ArrayList<DBDAttributeBinding>();
        } else {
            for (DBDAttributeBinding binding : nestedBindings) {
                maxPosition = Math.max(maxPosition, binding.getOrdinalPosition());
            }
        }
        Object[] fakeRow = new Object[maxPosition + 1];

        List<Object[]> fakeRows = Collections.singletonList(fakeRow);
        for (Pair<DBSAttributeBase, Object[]> nestedAttr : nestedAttributes) {
            DBSAttributeBase attribute = nestedAttr.getFirst();
            Object[] values = nestedAttr.getSecond();
            DBDAttributeBinding nestedBinding = null;
            for (DBDAttributeBinding binding : nestedBindings) {
                if (binding.getName().equals(attribute.getName())) {
                    nestedBinding = binding;
                    break;
                }
            }
            if (nestedBinding == null) {
                nestedBinding = new DBDAttributeBindingType(this, attribute);
                nestedBindings.add(nestedBinding);
            }
            if (attribute.getDataKind().isComplex()) {
                // Make late binding for each row value
                for (int i = 0; i < values.length; i++) {
                    if (DBUtils.isNullValue(values[i])) {
                        continue;
                    }
                    fakeRow[nestedBinding.getOrdinalPosition()] = values[i];
                    nestedBinding.lateBinding(session, fakeRows);
                }
            }
        }
    }

    private void createNestedTypeBindings(DBCSession session, DBSEntity type, List<Object[]> rows) throws DBException {
        if (nestedBindings == null) {
            nestedBindings = new ArrayList<DBDAttributeBinding>();
        }
        for (DBSEntityAttribute nestedAttr : CommonUtils.safeCollection(type.getAttributes(session.getProgressMonitor()))) {
            DBDAttributeBindingType nestedBinding = new DBDAttributeBindingType(this, nestedAttr);
            nestedBinding.lateBinding(session, rows);
            nestedBindings.add(nestedBinding);
        }
    }

}
