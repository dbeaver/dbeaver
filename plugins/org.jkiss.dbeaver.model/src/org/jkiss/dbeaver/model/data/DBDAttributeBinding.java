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
package org.jkiss.dbeaver.model.data;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPQualifiedObject;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.DBCAttributeMetaData;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.struct.DBSAttributeBase;
import org.jkiss.dbeaver.model.struct.DBSEntityAttribute;
import org.jkiss.dbeaver.model.struct.DBSEntityReferrer;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.virtual.DBVUtils;

import java.util.List;

/**
 * Base attribute binding
 */
public abstract class DBDAttributeBinding implements DBSObject, DBSAttributeBase, DBPQualifiedObject {
    @NotNull
    protected DBDValueHandler valueHandler;
    @NotNull
    protected DBDValueRenderer valueRenderer;
    @Nullable
    private List<DBDAttributeBinding> nestedBindings;

    protected DBDAttributeBinding(@NotNull DBDValueHandler valueHandler)
    {
        this.valueHandler = valueHandler;
        this.valueRenderer = valueHandler;
    }

    @Nullable
    @Override
    public abstract DBDAttributeBinding getParentObject();

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
    public DBDValueRenderer getValueRenderer() {
        return valueRenderer;
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
                DBDAttributeBinding cmpAttr = (DBDAttributeBinding) attr;
                if (getLevel() != cmpAttr.getLevel() || getOrdinalPosition() != cmpAttr.getOrdinalPosition()) {
                    return false;
                }
                // Match all hierarchy names
                for (DBDAttributeBinding a1 = cmpAttr, a2 = this; a1 != null && a2 != null; a1 = a1.getParentObject(), a2 = a2.getParentObject()) {
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

    public void setNestedBindings(@NotNull List<DBDAttributeBinding> nestedBindings) {
        this.nestedBindings = nestedBindings;
    }

    @Nullable
    @Override
    public String getDescription() {
        DBSEntityAttribute attr = getEntityAttribute();
        return attr == null ? null : attr.getDescription();
    }

    @NotNull
    @Override
    public String getFullQualifiedName() {
        if (getParentObject() == null) {
            return DBUtils.getQuotedIdentifier(getDataSource(), getName());
        }
        StringBuilder query = new StringBuilder();
        boolean hasPrevIdentifier = false;
        for (DBDAttributeBinding attribute = this; attribute != null; attribute = attribute.getParentObject()) {
            if (attribute.isPseudoAttribute()) {
                // Skip pseudo attributes (e.g. Mongo root document)
                continue;
            }
            if (hasPrevIdentifier) {
                query.insert(0, '.');
            }
            query.insert(0, DBUtils.getQuotedIdentifier(getDataSource(), attribute.getName()));
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
            p = p.getParentObject();
        }
        return p;
    }

    @NotNull
    public DBDAttributeBinding getTopParent() {
        for (DBDAttributeBinding binding = this; ; binding = binding.getParentObject()) {
            if (binding.getParentObject() == null) {
                return binding;
            }
        }
    }

    /**
     * Attribute level. Zero based
     * @return attribute level (depth)
     */
    public int getLevel() {
        if (getParentObject() == null) {
            return 0;
        }
        int level = 0;
        for (DBDAttributeBinding binding = getParentObject(); binding != null; binding = binding.getParentObject()) {
            level++;
        }
        return level;
    }

    public void lateBinding(@NotNull DBCSession session, List<Object[]> rows) throws DBException {
        DBSAttributeBase attribute = getAttribute();
        final DBDAttributeTransformer[] transformers = DBVUtils.findAttributeTransformers(this, false);
        if (transformers != null) {
            session.getProgressMonitor().subTask("Transform attribute '" + attribute.getName() + "'");
            for (DBDAttributeTransformer transformer : transformers) {
                transformer.transformAttribute(session, this, rows);
            }
        }
    }

}
