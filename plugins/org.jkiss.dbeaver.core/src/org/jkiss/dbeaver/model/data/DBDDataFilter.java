/*
 * Copyright (C) 2010-2015 Serge Rieder
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

import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.struct.DBSAttributeBase;
import org.jkiss.utils.CommonUtils;

import java.util.*;

/**
 * Data filter
 */
public class DBDDataFilter {

    private final List<DBDAttributeConstraint> constraints;
    private boolean anyConstraint; // means OR condition
    private String order;
    private String where;

    public DBDDataFilter(List<DBDAttributeConstraint> constraints)
    {
        this.constraints = constraints;
    }

    public DBDDataFilter(DBDDataFilter source)
    {
        constraints = new ArrayList<DBDAttributeConstraint>(source.constraints.size());
        for (DBDAttributeConstraint column : source.constraints) {
            constraints.add(new DBDAttributeConstraint(column));
        }
        this.order = source.order;
        this.where = source.where;
        this.anyConstraint = source.anyConstraint;
    }

    public Collection<DBDAttributeConstraint> getConstraints()
    {
        return constraints;
    }

    @Nullable
    public DBDAttributeConstraint getConstraint(DBDAttributeBinding binding)
    {
        for (DBDAttributeConstraint co : constraints) {
            if (co.getAttribute() == binding) {
                return co;
            }
        }
        return null;
    }

    @Nullable
    public DBDAttributeConstraint getConstraint(DBSAttributeBase attribute, boolean metaChanged)
    {
        for (DBDAttributeConstraint co : constraints) {
            if (co.matches(attribute, metaChanged)) {
                return co;
            }
        }
        return null;
    }

    public void addConstraints(List<DBDAttributeConstraint> constraints) {
        this.constraints.addAll(constraints);
    }

    public List<DBSAttributeBase> getOrderedVisibleAttributes()
    {
        List<DBDAttributeConstraint> visibleConstraints = new ArrayList<DBDAttributeConstraint>();
        for (DBDAttributeConstraint constraint : constraints) {
            if (constraint.isVisible()) {
                visibleConstraints.add(constraint);
            }
        }
        Collections.sort(visibleConstraints, new Comparator<DBDAttributeConstraint>() {
            @Override
            public int compare(DBDAttributeConstraint o1, DBDAttributeConstraint o2)
            {
                return o1.getVisualPosition() - o2.getVisualPosition();
            }
        });
        List<DBSAttributeBase> attributes = new ArrayList<DBSAttributeBase>(visibleConstraints.size());
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

    public String getOrder()
    {
        return order;
    }

    public void setOrder(@Nullable String order)
    {
        this.order = order;
    }

    public String getWhere()
    {
        return where;
    }

    public void setWhere(@Nullable String where)
    {
        this.where = where;
    }

    public boolean hasFilters()
    {
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

    public boolean hasConditions()
    {
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

    public boolean hasOrdering()
    {
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

    public List<DBDAttributeConstraint> getOrderConstraints()
    {
        List<DBDAttributeConstraint> result = null;
        for (DBDAttributeConstraint constraint : constraints) {
            if (constraint.getOrderPosition() > 0) {
                if (result == null) {
                    result = new ArrayList<DBDAttributeConstraint>(constraints.size());
                }
                result.add(constraint);
            }
        }
        if (result != null && result.size() > 1) {
            Collections.sort(result, new Comparator<DBDAttributeConstraint>() {
                @Override
                public int compare(DBDAttributeConstraint o1, DBDAttributeConstraint o2)
                {
                    return o1.getOrderPosition() - o2.getOrderPosition();
                }
            });
        }
        return result == null ? Collections.<DBDAttributeConstraint>emptyList() : result;
    }

    public int getMaxOrderingPosition()
    {
        int maxPosition = 0;
        for (DBDAttributeConstraint constraint : constraints) {
            if (constraint.getOrderPosition() > maxPosition) {
                maxPosition = constraint.getOrderPosition();
            }
        }
        return maxPosition;
    }

    public void resetOrderBy()
    {
        this.order = null;
        for (DBDAttributeConstraint constraint : constraints) {
            constraint.setOrderPosition(0);
            constraint.setOrderDescending(false);
        }
    }

    public void reset()
    {
        for (DBDAttributeConstraint constraint : constraints) {
            constraint.reset();
        }
        this.order = null;
        this.where = null;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof DBDDataFilter)) {
            return false;
        }
        DBDDataFilter source = (DBDDataFilter)obj;
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
     * @param source object to compare to
     * @return true if filters equals
     */
    public boolean equalFilters(DBDDataFilter source)
    {
        if (anyConstraint != source.anyConstraint) {
            return false;
        }
        if (constraints.size() != source.constraints.size()) {
            return false;
        }
        for (int i = 0; i < source.constraints.size(); i++) {
            if (!constraints.get(i).equalFilters(source.constraints.get(i))) {
                return false;
            }
        }
        return CommonUtils.equalObjects(this.order, source.order) &&
            CommonUtils.equalObjects(this.where, source.where);
    }

}
