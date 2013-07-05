/*
 * Copyright (C) 2010-2012 Serge Rieder
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

package org.jkiss.dbeaver.model.data.query;

import org.jkiss.utils.CommonUtils;

/**
 * Attribute constraint
 */
public class DBQConstraint {

    private final String attributeName;
    private DBQOrder orderBy;
    private String criteria;
    private boolean visible;

    public DBQConstraint(String attributeName)
    {
        this.attributeName = attributeName;
    }

    public DBQConstraint(DBQConstraint source)
    {
        this.attributeName = source.attributeName;
        this.orderBy = source.orderBy;
        this.criteria = source.criteria;
        this.visible = source.visible;
    }

    public String getAttributeName()
    {
        return attributeName;
    }

    public DBQOrder getOrderBy()
    {
        return orderBy;
    }

    public void setOrderBy(DBQOrder orderBy)
    {
        this.orderBy = orderBy;
    }

    public String getCriteria()
    {
        return criteria;
    }

    public void setCriteria(String criteria)
    {
        this.criteria = criteria;
    }

    public boolean hasCriteria()
    {
        return !CommonUtils.isEmpty(criteria);
    }

    public boolean isVisible()
    {
        return visible;
    }

    public void setVisible(boolean visible)
    {
        this.visible = visible;
    }

    @Override
    public int hashCode()
    {
        return this.attributeName.hashCode() +
            (this.orderBy == null ? 0 : this.orderBy.hashCode()) +
            (this.criteria == null ? 0 : this.criteria.hashCode()) +
            (this.visible ? 1 : 0);
    }

    @Override
    public boolean equals(Object obj)
    {
        if (!(obj instanceof DBQConstraint)) {
            return false;
        }
        DBQConstraint source = (DBQConstraint)obj;
        return CommonUtils.equalObjects(this.attributeName, source.attributeName) &&
            CommonUtils.equalObjects(this.orderBy, source.orderBy) &&
            CommonUtils.equalObjects(this.criteria, source.criteria) &&
            this.visible == source.visible;
    }
}
