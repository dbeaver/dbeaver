/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
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

import org.jkiss.dbeaver.model.struct.DBSAttributeBase;
import org.jkiss.utils.CommonUtils;

/**
 * Attribute constraint
 */
public class DBDAttributeConstraint extends DBDAttributeConstraintBase {

    private final DBSAttributeBase attribute;
    private final int originalVisualPosition;

    public DBDAttributeConstraint(DBDAttributeBinding attribute)
    {
        this.attribute = attribute;
        this.originalVisualPosition = attribute.getOrdinalPosition();
        setVisualPosition(this.originalVisualPosition);
    }

    public DBDAttributeConstraint(DBSAttributeBase attribute, int visualPosition)
    {
        this.attribute = attribute;
        this.originalVisualPosition = visualPosition;
        setVisualPosition(this.originalVisualPosition);
    }

    public DBDAttributeConstraint(DBDAttributeConstraint source)
    {
        super(source);
        this.attribute = source.attribute;
        this.originalVisualPosition = source.originalVisualPosition;
    }

    public DBSAttributeBase getAttribute()
    {
        return attribute;
    }

    public int getOriginalVisualPosition() {
        return originalVisualPosition;
    }

    public void reset()
    {
        super.reset();
        setVisualPosition(originalVisualPosition);
    }

    public boolean equalFilters(DBDAttributeConstraintBase obj, boolean compareOrders)
    {
        return
            obj instanceof DBDAttributeConstraint &&
            CommonUtils.equalObjects(this.attribute, ((DBDAttributeConstraint)obj).attribute) &&
            super.equalFilters(obj, compareOrders);
    }

    @Override
    public int hashCode()
    {
        return this.attribute.hashCode();
    }

    @Override
    public boolean equals(Object obj)
    {
        if (obj instanceof DBDAttributeConstraint) {
            DBDAttributeConstraint source = (DBDAttributeConstraint) obj;
            return
                CommonUtils.equalObjects(this.attribute, source.attribute) &&
                super.equals(obj);
        } else {
            return false;
        }
    }

    @Override
    public String toString() {
        String clause = getOperator() == null ?
            (getCriteria() == null ? "" : getCriteria()) :
            (isReverseOperator() ? "NOT " : "") + getOperator().getStringValue() + " " + getValue();
        return attribute.getName() + " " + clause;
    }

    public boolean matches(DBSAttributeBase attr, boolean matchByName) {
        return attribute == attr ||
            (attribute instanceof DBDAttributeBinding && ((DBDAttributeBinding) attribute).matches(attr, matchByName));
    }
}
