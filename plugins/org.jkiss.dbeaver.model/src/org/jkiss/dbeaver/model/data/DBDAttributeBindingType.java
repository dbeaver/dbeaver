/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2022 DBeaver Corp and others
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jkiss.dbeaver.model.data;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.*;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.struct.*;
import org.jkiss.utils.CommonUtils;

import java.util.List;

/**
 * Type attribute value binding info
 */
public class DBDAttributeBindingType extends DBDAttributeBindingNested implements DBPImageProvider {

    private static final Log log = Log.getLog(DBDAttributeBindingType.class);

    @NotNull
    private final DBSAttributeBase attribute;
    private List<DBSEntityReferrer> referrers;
    private int ordinalPosition;

    public DBDAttributeBindingType(
        @NotNull DBDAttributeBinding parent,
        @NotNull DBSAttributeBase attribute,
        int ordinalPosition)
    {
        super(parent, DBUtils.findValueHandler(parent.getDataSource(), attribute));
        this.attribute = attribute;
        this.ordinalPosition = ordinalPosition;
    }

    /**
     * Attribute index in result set
     * @return attribute index (zero based)
     */
    @Override
    public int getOrdinalPosition() {
        return ordinalPosition < 0 ? attribute.getOrdinalPosition() : ordinalPosition;
    }

    public void setOrdinalPosition(int ordinalPosition) {
        this.ordinalPosition = ordinalPosition;
    }

    @Override
    public boolean isRequired() {
        return attribute.isRequired();
    }

    @Override
    public boolean isAutoGenerated() {
        return attribute.isAutoGenerated();
    }

    @Override
    public boolean isPseudoAttribute() {
        return false;
    }

    @Nullable
    @Override
    public DBSObject getSource() {
        if (attribute instanceof DBSObject) {
            return ((DBSObject)attribute).getParentObject();
        }
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
        DBSObject source = getSource();
        if (source instanceof DBSEntity) {
            return source.getName();
        }
        return null;
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
     * Entity attribute
     */
    @Nullable
    public DBSEntityAttribute getEntityAttribute()
    {
        if (attribute instanceof DBSEntityAttribute) {
            return (DBSEntityAttribute) attribute;
        }
        return null;
    }

    @Nullable
    @Override
    public DBSAttributeBase getAttribute() {
        return attribute;
    }

    @Nullable
    @Override
    public Object extractNestedValue(@NotNull Object ownerValue) throws DBCException {
        assert parent != null;
        if (parent.getDataKind() == DBPDataKind.ARRAY) {
            // If we have a collection then use first element
            if (ownerValue instanceof DBDCollection) {
                DBDCollection collection = (DBDCollection) ownerValue;
                if (collection.getItemCount() > 0) {
                    ownerValue = collection.getItem(0);
                } else {
                    return null;
                }
            }
        }
        if (ownerValue instanceof DBDComposite) {
            return ((DBDComposite) ownerValue).getAttributeValue(attribute);
        }

        DBDAttributeBinding parent = getParent(1);
        log.debug("Can't extract field '" + getName() + "' from type '" + (parent == null ? null : parent.getName()) + "': wrong value (" + ownerValue + ")");

        throw new DBCException(DBValueFormatting.getDefaultValueDisplayString(ownerValue, DBDDisplayFormat.NATIVE));
    }

    @Nullable
    @Override
    public List<DBSEntityReferrer> getReferrers() {
        return referrers;
    }

    @Override
    public void lateBinding(@NotNull DBCSession session, List<Object[]> rows) throws DBException {
        referrers = findVirtualReferrers();

        super.lateBinding(session, rows);
    }


    @Nullable
    @Override
    public DBSDataType getDataType() {
        if (attribute instanceof DBSTypedObjectEx) {
            return ((DBSTypedObjectEx) attribute).getDataType();
        }
        return super.getDataType();
    }

    @Nullable
    @Override
    public DBPImage getObjectImage() {
        return DBValueFormatting.getObjectImage(attribute);
    }

    @Override
    public String getTypeName() {
        return attribute.getTypeName();
    }

    @Override
    public String getFullTypeName() {
        return DBUtils.getFullTypeName(attribute);
    }

    @Override
    public int getTypeID() {
        return attribute.getTypeID();
    }

    @Override
    public DBPDataKind getDataKind() {
        return attribute.getDataKind();
    }

    @Override
    public Integer getScale() {
        return attribute.getScale();
    }

    @Override
    public Integer getPrecision() {
        return attribute.getPrecision();
    }

    @Override
    public long getMaxLength() {
        return attribute.getMaxLength();
    }

    @Override
    public long getTypeModifiers() {
        return attribute.getTypeModifiers();
    }

    @Override
    public String toString() {
        return attribute.toString();
    }

    @Override
    public boolean equals(Object obj) {
        return super.equals(obj) && obj instanceof DBDAttributeBindingType &&
            CommonUtils.equalObjects(attribute, ((DBDAttributeBindingType) obj).attribute);
    }

    @Override
    public int hashCode() {
        return attribute.hashCode();
    }

}
