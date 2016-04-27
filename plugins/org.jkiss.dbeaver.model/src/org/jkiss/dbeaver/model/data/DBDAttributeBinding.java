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
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPQualifiedObject;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.DBCAttributeMetaData;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.sql.SQLConstants;
import org.jkiss.dbeaver.model.sql.SQLDataSource;
import org.jkiss.dbeaver.model.struct.*;
import org.jkiss.dbeaver.model.virtual.DBVUtils;

import java.util.List;
import java.util.Map;

/**
 * Base attribute binding
 */
public abstract class DBDAttributeBinding implements DBSObject, DBSAttributeBase, DBSTypedObjectEx, DBPQualifiedObject {

    @NotNull
    protected DBDValueHandler valueHandler;
    @Nullable
    protected DBSAttributeBase presentationAttribute;
    @Nullable
    private List<DBDAttributeBinding> nestedBindings;

    protected DBDAttributeBinding(@NotNull DBDValueHandler valueHandler)
    {
        this.valueHandler = valueHandler;
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
     * Entity attribute (may be null).
     * It is always null if {@link #lateBinding(DBCSession, List)} wasn't called
     */
    @Nullable
    public abstract DBSEntityAttribute getEntityAttribute();

    /**
     * Most valuable attribute reference.
     * @return resolved entity attribute or just meta attribute
     */
    @NotNull
    public DBSAttributeBase getAttribute()
    {
        DBSEntityAttribute attr = getEntityAttribute();
        return attr == null ? getMetaAttribute() : attr;
    }

    /**
     * Presentation attribute.
     * Usually the same as {@link #getAttribute()} but may be explicitly set by attribute transformers.
     */
    @NotNull
    public DBSAttributeBase getPresentationAttribute() {
        if (presentationAttribute != null) {
            return presentationAttribute;
        }
        return getAttribute();
    }

    public void setPresentationAttribute(@Nullable DBSAttributeBase presentationAttribute) {
        this.presentationAttribute = presentationAttribute;
    }

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

    public void setValueHandler(@NotNull DBDValueHandler valueHandler) {
        this.valueHandler = valueHandler;
    }

    @NotNull
    public DBDValueRenderer getValueRenderer() {
        return valueHandler;
    }

    public boolean matches(@Nullable DBSAttributeBase attr, boolean searchByName) {
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
        final DBPDataSource dataSource = getDataSource();
        if (getParentObject() == null) {
            return DBUtils.getQuotedIdentifier(dataSource, getName());
        }
        char structSeparator = SQLConstants.STRUCT_SEPARATOR;
        if (dataSource instanceof SQLDataSource) {
            structSeparator = ((SQLDataSource) dataSource).getSQLDialect().getStructSeparator();
        }
        StringBuilder query = new StringBuilder();
        boolean hasPrevIdentifier = false;
        for (DBDAttributeBinding attribute = this; attribute != null; attribute = attribute.getParentObject()) {
            if (attribute.isPseudoAttribute()) {
                // Skip pseudo attributes (e.g. Mongo root document)
                continue;
            }
            if (hasPrevIdentifier) {
                query.insert(0, structSeparator);
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
            if (p == null) {
                throw new IllegalArgumentException("Bad parent depth: " + grand);
            }
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

    @Nullable
    @Override
    public DBSDataType getDataType() {
        DBSEntityAttribute attribute = getEntityAttribute();
        if (attribute instanceof DBSTypedObjectEx) {
            return ((DBSTypedObjectEx) attribute).getDataType();
        }
        return null;
    }

    public void lateBinding(@NotNull DBCSession session, List<Object[]> rows) throws DBException {
        DBSAttributeBase attribute = getAttribute();
        final DBDAttributeTransformer[] transformers = DBVUtils.findAttributeTransformers(this, null);
        if (transformers != null) {
            session.getProgressMonitor().subTask("Transform attribute '" + attribute.getName() + "'");
            final Map<String, String> transformerOptions = DBVUtils.getAttributeTransformersOptions(this);
            for (DBDAttributeTransformer transformer : transformers) {
                transformer.transformAttribute(session, this, rows, transformerOptions);
            }
        }
    }

    @Override
    public String toString() {
        return getName() + " [" + getOrdinalPosition() + "]";
    }

}
