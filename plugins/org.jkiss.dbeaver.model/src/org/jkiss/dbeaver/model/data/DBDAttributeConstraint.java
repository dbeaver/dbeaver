/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
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
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.struct.DBSAttributeBase;
import org.jkiss.utils.CommonUtils;

import java.util.Arrays;

/**
 * Attribute constraint
 */
public class DBDAttributeConstraint extends DBDAttributeConstraintBase {

//    public static final String FEATURE_HIDDEN = "hidden";

    @Nullable
    private DBSAttributeBase attribute;
    private String attributeName;
    private int originalVisualPosition;

    public DBDAttributeConstraint(@NotNull DBDAttributeBinding attribute) {
        setAttribute(attribute);
        setVisualPosition(attribute.getOrdinalPosition());
    }

    public DBDAttributeConstraint(@NotNull DBSAttributeBase attribute, int visualPosition) {
        setAttribute(attribute);
        setVisualPosition(visualPosition);
    }

    public DBDAttributeConstraint(@NotNull String attributeName, int originalVisualPosition) {
        this.attribute = null;
        this.attributeName = attributeName;
        this.originalVisualPosition = originalVisualPosition;
    }

    public DBDAttributeConstraint(@NotNull DBDAttributeConstraint source) {
        super(source);
        this.attribute = source.attribute;
        this.attributeName = source.attributeName;
        this.originalVisualPosition = source.originalVisualPosition;
    }

    public static boolean isVisibleByDefault(DBDAttributeBinding binding) {
        return !binding.isPseudoAttribute();
    }

    @Nullable
    public DBSAttributeBase getAttribute() {
        return attribute;
    }

    void setAttribute(@NotNull DBSAttributeBase binding) {
        this.attribute = binding;
        this.attributeName = this.attribute.getName();
        this.originalVisualPosition = attribute.getOrdinalPosition();
    }

    @NotNull
    public String getAttributeName() {
        return attributeName;
    }

    @NotNull
    public String getFullAttributeName() {
        return attribute == null ? attributeName : DBUtils.getObjectFullName(attribute, DBPEvaluationContext.DML);
    }

    public int getOriginalVisualPosition() {
        return originalVisualPosition;
    }

    @Override
    public boolean hasFilter() {
        return super.hasFilter() || // compare visual position only if it explicitly set
            (getVisualPosition() != NULL_VISUAL_POSITION && originalVisualPosition != getVisualPosition());
    }

    public void reset() {
        super.reset();
        setVisualPosition(originalVisualPosition);
    }

    public boolean equalFilters(DBDAttributeConstraintBase obj, boolean compareOrders) {
        return
            obj instanceof DBDAttributeConstraint &&
                CommonUtils.equalObjects(this.attribute, ((DBDAttributeConstraint) obj).attribute) &&
                super.equalFilters(obj, compareOrders);
    }

    @Override
    public int hashCode() {
        return this.attributeName.hashCode() + getVisualPosition();
    }

    @Override
    public boolean equals(Object obj) {
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
        return attributeName + " " + clause;
    }

    public boolean matches(DBSAttributeBase attr, boolean matchByName) {
        return attribute == attr ||
            (attribute instanceof DBDAttributeBinding && ((DBDAttributeBinding) attribute).matches(attr, matchByName));
    }

    public boolean equalVisibility(DBDAttributeConstraint constraint) {
        return isVisible() == constraint.isVisible() && getVisualPosition() == constraint.getVisualPosition() &&
            Arrays.equals(getOptions(), constraint.getOptions());
    }
}
