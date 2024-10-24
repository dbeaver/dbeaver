/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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

import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.struct.DBSAttributeBase;
import org.jkiss.utils.CommonUtils;

import java.util.*;

/**
 * Data filter
 */
public class DBDDataFilter {

    private final List<DBDAttributeConstraint> constraints;
    private boolean anyConstraint; // means OR condition
    private boolean useDisjunctiveNormalForm; // see setUseDisjunctiveNormalForm
    private String order;
    private String where;

    public DBDDataFilter() {
        this.constraints = new ArrayList<>();
    }

    public DBDDataFilter(List<DBDAttributeConstraint> constraints) {
        this.constraints = constraints;
    }

    public DBDDataFilter(DBDDataFilter source) {
        constraints = new ArrayList<>(source.constraints.size());
        for (DBDAttributeConstraint column : source.constraints) {
            constraints.add(new DBDAttributeConstraint(column));
        }
        this.order = source.order;
        this.where = source.where;
        this.anyConstraint = source.anyConstraint;
        this.useDisjunctiveNormalForm = source.useDisjunctiveNormalForm;
    }

    public List<DBDAttributeConstraint> getConstraints() {
        return constraints;
    }

    public boolean hasHiddenAttributes() {
        for (DBDAttributeConstraint ac : getConstraints()) {
            DBSAttributeBase attribute = ac.getAttribute();
            if (!ac.isVisible() && attribute instanceof DBDAttributeBinding && DBDAttributeConstraint.isVisibleByDefault((DBDAttributeBinding) attribute)) {
                return true;
            }
        }
        return false;
    }

    public boolean hasPinnedAttributes() {
        for (DBDAttributeConstraint ac : getConstraints()) {
            if (ac.hasOption(DBDAttributeConstraintBase.ATTR_OPTION_PINNED)) {
                return true;
            }
        }
        return false;
    }

    @Nullable
    public DBDAttributeConstraint getConstraint(DBDAttributeBinding binding) {
        for (DBDAttributeConstraint co : constraints) {
            if (binding.equals(co.getAttribute())) {
                return co;
            }
        }
        return null;
    }

    @Nullable
    public DBDAttributeConstraint getConstraint(DBSAttributeBase attribute, boolean metaChanged) {
        for (DBDAttributeConstraint co : constraints) {
            if (co.matches(attribute, metaChanged)) {
                return co;
            }
        }
        return null;
    }

    @Nullable
    public DBDAttributeConstraint getConstraint(String name) {
        for (DBDAttributeConstraint co : constraints) {
            if (CommonUtils.equalObjects(co.getAttributeName(), name)) {
                return co;
            }
        }
        return null;
    }

    public void addConstraints(List<DBDAttributeConstraint> constraints) {
        this.constraints.addAll(constraints);
    }

    public List<DBSAttributeBase> getOrderedVisibleAttributes() {
        List<DBDAttributeConstraint> visibleConstraints = new ArrayList<>();
        for (DBDAttributeConstraint constraint : constraints) {
            if (constraint.isVisible()) {
                visibleConstraints.add(constraint);
            }
        }
        visibleConstraints.sort(Comparator.comparingInt(DBDAttributeConstraintBase::getVisualPosition));
        List<DBSAttributeBase> attributes = new ArrayList<>(visibleConstraints.size());
        for (DBDAttributeConstraint constraint : visibleConstraints) {
            attributes.add(constraint.getAttribute());
        }
        return attributes;
    }

    public boolean isAnyConstraint() {
        return anyConstraint;
    }

    public void setAnyConstraint(boolean anyConstraint) {
        this.anyConstraint = anyConstraint;
    }

    public String getOrder() {
        return order;
    }

    public void setOrder(@Nullable String order) {
        this.order = order;
    }

    public String getWhere() {
        return where;
    }

    public void setWhere(@Nullable String where) {
        this.where = where;
    }

    public boolean hasFilters() {
        if (!CommonUtils.isEmpty(this.order) || !CommonUtils.isEmpty(this.where)) {
            return true;
        }
        for (DBDAttributeConstraint constraint : constraints) {
            if (constraint.hasFilter()) {
                return true;
            }
        }
        return false;
    }

    public boolean hasConditions() {
        if (!CommonUtils.isEmpty(where)) {
            return true;
        }
        for (DBDAttributeConstraint constraint : constraints) {
            if (constraint.hasCondition()) {
                return true;
            }
        }
        return false;
    }

    public boolean hasOrdering() {
        if (!CommonUtils.isEmpty(order)) {
            return true;
        }
        for (DBDAttributeConstraint constraint : constraints) {
            if (constraint.getOrderPosition() > 0) {
                return true;
            }
        }
        return false;
    }

    public boolean isDirty() {
        if (!CommonUtils.isEmpty(this.order) || !CommonUtils.isEmpty(this.where)) {
            return true;
        }
        for (DBDAttributeConstraint constraint : constraints) {
            if (constraint.isDirty()) {
                return true;
            }
        }
        return false;
    }

    public List<DBDAttributeConstraint> getOrderConstraints() {
        List<DBDAttributeConstraint> result = null;
        for (DBDAttributeConstraint constraint : constraints) {
            if (constraint.getOrderPosition() > 0) {
                if (result == null) {
                    result = new ArrayList<>(constraints.size());
                }
                result.add(constraint);
            }
        }
        if (result != null && result.size() > 1) {
            result.sort(Comparator.comparingInt(DBDAttributeConstraintBase::getOrderPosition));
        }
        return result == null ? Collections.emptyList() : result;
    }

    public int getMaxOrderingPosition() {
        int maxPosition = 0;
        for (DBDAttributeConstraint constraint : constraints) {
            if (constraint.getOrderPosition() > maxPosition) {
                maxPosition = constraint.getOrderPosition();
            }
        }
        return maxPosition;
    }

    public void resetOrderBy() {
        this.order = null;
        for (DBDAttributeConstraint constraint : constraints) {
            constraint.setOrderPosition(0);
            constraint.setOrderDescending(false);
        }
    }

    public void reset() {
        for (DBDAttributeConstraint constraint : constraints) {
            constraint.reset();
        }
        this.order = null;
        this.where = null;
    }

    public void bindAttributes(DBDAttributeBinding[] bindings) {
        for (DBDAttributeConstraint constr : constraints) {
            DBDAttributeBinding attrBinding = DBUtils.findObject(bindings, constr.getAttributeName());
            if (attrBinding != null) {
                constr.setAttribute(attrBinding);
            }
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof DBDDataFilter)) {
            return false;
        }
        DBDDataFilter source = (DBDDataFilter) obj;
        if (constraints.size() != source.constraints.size()) {
            return false;
        }
        if (anyConstraint != source.anyConstraint) {
            return false;
        }
        for (int i = 0, orderColumnsSize = source.constraints.size(); i < orderColumnsSize; i++) {
            if (!constraints.get(i).equals(source.constraints.get(i))) {
                return false;
            }
        }
        return CommonUtils.equalObjects(this.order, source.order) &&
            CommonUtils.equalObjects(this.where, source.where);
    }

    /**
     * compares only filers (criteria and ordering)
     *
     * @param source object to compare to
     * @return true if filters equals
     */
    public boolean equalFilters(DBDDataFilter source, boolean compareOrders) {
        if (anyConstraint != source.anyConstraint) {
            return false;
        }
        if (constraints.size() != source.constraints.size()) {
            return false;
        }
        for (int i = 0; i < source.constraints.size(); i++) {
            if (!constraints.get(i).equalFilters(source.constraints.get(i), compareOrders)) {
                return false;
            }
        }
        return CommonUtils.equalObjects(this.order, source.order) &&
            CommonUtils.equalObjects(this.where, source.where);
    }

    public boolean equalVisibility(DBDDataFilter dataFilter) {
        if (dataFilter.constraints.size() != constraints.size()) {
            return false;
        }
        for (int i = 0; i < dataFilter.constraints.size(); i++) {
            if (!constraints.get(i).equalVisibility(dataFilter.constraints.get(i))) {
                return false;
            }

        }
        return true;
    }

    public boolean hasNameDuplicates(String name) {
        int count = 0;
        for (DBDAttributeConstraint c : constraints) {
            if (name.equalsIgnoreCase(c.getFullAttributeName())) {
                count++;
            }
        }
        return count > 1;
    }

    public void serialize(Map<String, Object> state) {

    }

    /**
     * Changes interpretation of constraint values as disjunctive normal form (DNF).
     * <p>
     * Let's say we have two constraints:
     * <ul>
     *     <li>{@code x IN (1, 2, 3)}</li>
     *     <li>{@code y IN (4, 5, 6)}</li>
     * </ul>
     * <p>
     * If {@code useDisjunctiveNormalForm} is {@code true} constraints will be transformed as follows:
     * <ul>
     *     <li>{@code (x = 1 AND y = 4) OR (x = 2 AND y = 5) OR (x = 3 AND y = 6)}</li>
     * </ul>
     * <p>
     * <b>Limitations:</b>
     * <ul>
     *     <li>All constraints must use the {@code IN} operator</li>
     *     <li>All constraints must have the same number of values</li>
     * </ul>
     */
    public void setUseDisjunctiveNormalForm(boolean useDisjunctiveNormalForm) {
        this.useDisjunctiveNormalForm = useDisjunctiveNormalForm;
    }

    public boolean isUseDisjunctiveNormalForm() {
        return useDisjunctiveNormalForm;
    }
}
