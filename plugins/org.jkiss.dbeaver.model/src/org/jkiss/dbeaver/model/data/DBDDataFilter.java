/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2026 DBeaver Corp and others
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
import org.jkiss.dbeaver.model.struct.DBSAttributeBase;
import org.jkiss.utils.CommonUtils;

import java.util.*;
import java.util.stream.Collectors;

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

    public DBDDataFilter(@NotNull List<DBDAttributeConstraint> constraints) {
        this.constraints = new ArrayList<>(constraints);
    }

    public DBDDataFilter(@NotNull DBDDataFilter source) {
        constraints = new ArrayList<>(source.constraints.size());
        for (DBDAttributeConstraint column : source.constraints) {
            constraints.add(new DBDAttributeConstraint(column));
        }
        this.order = source.order;
        this.where = source.where;
        this.anyConstraint = source.anyConstraint;
        this.useDisjunctiveNormalForm = source.useDisjunctiveNormalForm;
    }

    public int getConstraintsCount() {
        synchronized (constraints) {
            return constraints.size();
        }
    }

    @NotNull
    public DBDAttributeConstraint[] getConstraints() {
        synchronized (constraints) {
            return constraints.toArray(DBDAttributeConstraint[]::new);
        }
    }

    @NotNull
    public List<DBDAttributeConstraint> getConstraintsWithCondition() {
        synchronized (constraints) {
            return constraints.stream()
                .filter(x -> x.getCriteria() != null || x.getOperator() != null)
                .collect(Collectors.toList());
        }
    }

    public boolean hasHiddenAttributes() {
        synchronized (constraints) {
            for (DBDAttributeConstraint ac : constraints) {
                DBSAttributeBase attribute = ac.getAttribute();
                if (!ac.isVisible() && attribute instanceof DBDAttributeBinding binding
                    && DBDAttributeConstraint.isVisibleByDefault(binding)) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean hasPinnedAttributes() {
        synchronized (constraints) {
            for (DBDAttributeConstraint ac : constraints) {
                if (ac.hasOption(DBDAttributeConstraintBase.ATTR_OPTION_PINNED)) {
                    return true;
                }
            }
        }
        return false;
    }

    @Nullable
    public DBDAttributeConstraint getConstraint(@NotNull DBDAttributeBinding binding) {
        synchronized (constraints) {
            for (DBDAttributeConstraint co : constraints) {
                if (binding.matches(co.getAttribute(), false)) {
                    return co;
                }
            }
        }
        return null;
    }

    @Nullable
    public DBDAttributeConstraint getConstraint(@NotNull DBSAttributeBase attribute, boolean metaChanged) {
        synchronized (constraints) {
            for (DBDAttributeConstraint co : constraints) {
                if (co.matches(attribute, metaChanged)) {
                    return co;
                }
            }
        }
        return null;
    }

    @Nullable
    public DBDAttributeConstraint getConstraint(@NotNull String name) {
        synchronized (constraints) {
            for (DBDAttributeConstraint co : constraints) {
                if (CommonUtils.equalObjects(co.getAttributeName(), name)) {
                    return co;
                }
            }
        }
        return null;
    }

    public void addConstraint(@NotNull DBDAttributeConstraint constraint) {
        synchronized (this.constraints) {
            this.constraints.add(constraint);
        }
    }

    public void addConstraints(@NotNull List<DBDAttributeConstraint> constraints) {
        synchronized (this.constraints) {
            this.constraints.addAll(constraints);
        }
    }

    @NotNull
    public List<DBSAttributeBase> getOrderedVisibleAttributes() {
        List<DBDAttributeConstraint> visibleConstraints = new ArrayList<>();
        synchronized (constraints) {
            for (DBDAttributeConstraint constraint : constraints) {
                if (constraint.isVisible()) {
                    visibleConstraints.add(constraint);
                }
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

    @Nullable
    public String getOrder() {
        return order;
    }

    public void setOrder(@Nullable String order) {
        this.order = order;
    }

    @Nullable
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
        synchronized (constraints) {
            for (DBDAttributeConstraint constraint : constraints) {
                if (constraint.hasFilter()) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean hasConditions() {
        if (!CommonUtils.isEmpty(where)) {
            return true;
        }
        synchronized (constraints) {
            for (DBDAttributeConstraint constraint : constraints) {
                if (constraint.hasCondition()) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean hasOrdering() {
        if (!CommonUtils.isEmpty(order)) {
            return true;
        }
        synchronized (constraints) {
            for (DBDAttributeConstraint constraint : constraints) {
                if (constraint.getOrderPosition() > 0) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean isDirty() {
        if (!CommonUtils.isEmpty(this.order) || !CommonUtils.isEmpty(this.where)) {
            return true;
        }
        synchronized (constraints) {
            for (DBDAttributeConstraint constraint : constraints) {
                if (constraint.isDirty()) {
                    return true;
                }
            }
        }
        return false;
    }

    public List<DBDAttributeConstraint> getOrderConstraints() {
        List<DBDAttributeConstraint> result = null;
        synchronized (constraints) {
            for (DBDAttributeConstraint constraint : constraints) {
                if (constraint.getOrderPosition() > 0) {
                    if (result == null) {
                        result = new ArrayList<>(constraints.size());
                    }
                    result.add(constraint);
                }
            }
        }
        if (result != null && result.size() > 1) {
            result.sort(Comparator.comparingInt(DBDAttributeConstraintBase::getOrderPosition));
        }
        return result == null ? Collections.emptyList() : result;
    }

    public int getMaxOrderingPosition() {
        int maxPosition = 0;
        synchronized (constraints) {
            for (DBDAttributeConstraint constraint : constraints) {
                if (constraint.getOrderPosition() > maxPosition) {
                    maxPosition = constraint.getOrderPosition();
                }
            }
        }
        return maxPosition;
    }

    public void resetOrderBy() {
        this.order = null;
        synchronized (constraints) {
            for (DBDAttributeConstraint constraint : constraints) {
                constraint.setOrderPosition(0);
                constraint.setOrderDescending(false);
            }
        }
    }

    public void reset() {
        synchronized (constraints) {
            for (DBDAttributeConstraint constraint : constraints) {
                constraint.reset();
            }
        }
        this.order = null;
        this.where = null;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof DBDDataFilter source)) {
            return false;
        }
        if (anyConstraint != source.anyConstraint) {
            return false;
        }
        DBDAttributeConstraint[] srcConstraints = source.getConstraints();
        synchronized (constraints) {
            if (constraints.size() != srcConstraints.length) {
                return false;
            }
            for (int i = 0, orderColumnsSize = srcConstraints.length; i < orderColumnsSize; i++) {
                if (!constraints.get(i).equals(srcConstraints[i])) {
                    return false;
                }
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
    public boolean equalFilters(@NotNull DBDDataFilter source, boolean compareOrders) {
        if (anyConstraint != source.anyConstraint) {
            return false;
        }
        DBDAttributeConstraint[] srcConstraints = source.getConstraints();
        synchronized (constraints) {
            if (constraints.size() != srcConstraints.length) {
                return false;
            }
            for (int i = 0; i < srcConstraints.length; i++) {
                if (!constraints.get(i).equalFilters(srcConstraints[i], compareOrders)) {
                    return false;
                }
            }
        }
        return CommonUtils.equalObjects(this.order, source.order) &&
            CommonUtils.equalObjects(this.where, source.where);
    }

    public boolean equalVisibility(@NotNull DBDDataFilter dataFilter) {
        DBDAttributeConstraint[] srcConstraints = dataFilter.getConstraints();
        synchronized (constraints) {
            if (srcConstraints.length != constraints.size()) {
                return false;
            }
            for (int i = 0; i < srcConstraints.length; i++) {
                if (!constraints.get(i).equalVisibility(srcConstraints[i])) {
                    return false;
                }

            }
        }
        return true;
    }

    public boolean hasNameDuplicates(@NotNull String name) {
        int count = 0;
        synchronized (constraints) {
            for (DBDAttributeConstraint c : constraints) {
                if (name.equalsIgnoreCase(c.getFullAttributeName())) {
                    count++;
                }
            }
        }
        return count > 1;
    }

    public void serialize(@NotNull Map<String, Object> state) {

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
