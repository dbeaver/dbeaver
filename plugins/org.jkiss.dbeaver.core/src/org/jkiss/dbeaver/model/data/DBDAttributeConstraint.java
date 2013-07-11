/*
 * Copyright (C) 2010-2013 Serge Rieder
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

import org.jkiss.utils.CommonUtils;

/**
 * Attribute constraint
 */
public class DBDAttributeConstraint {

    private final DBDAttributeBinding attribute;
    private int orderPosition;
    private boolean orderDescending;
    private String criteria;
    private boolean visible;
    private int visualPosition;

    public DBDAttributeConstraint(DBDAttributeBinding attribute)
    {
        this.attribute = attribute;
        this.visualPosition = attribute.getAttributeIndex();
    }

    public DBDAttributeConstraint(DBDAttributeConstraint source)
    {
        this.attribute = source.attribute;
        this.orderPosition = source.orderPosition;
        this.orderDescending = source.orderDescending;
        this.criteria = source.criteria;
        this.visible = source.visible;
        this.visualPosition = source.visualPosition;
    }

    public DBDAttributeBinding getAttribute()
    {
        return attribute;
    }

    public int getOrderPosition()
    {
        return orderPosition;
    }

    public void setOrderPosition(int orderPosition)
    {
        this.orderPosition = orderPosition;
    }

    public boolean isOrderDescending()
    {
        return orderDescending;
    }

    public void setOrderDescending(boolean orderDescending)
    {
        this.orderDescending = orderDescending;
    }

    public String getCriteria()
    {
        return criteria;
    }

    public void setCriteria(String criteria)
    {
        this.criteria = criteria;
    }

    public boolean hasFilter()
    {
        return !CommonUtils.isEmpty(criteria) || orderPosition > 0;
    }

    public boolean isVisible()
    {
        return visible;
    }

    public void setVisible(boolean visible)
    {
        this.visible = visible;
    }

    public int getVisualPosition()
    {
        return visualPosition;
    }

    public void setVisualPosition(int visualPosition)
    {
        this.visualPosition = visualPosition;
    }

    public void reset()
    {
        this.orderPosition = 0;
        this.orderDescending = false;
        this.criteria = null;
        this.visible = true;
        this.visualPosition = attribute.getAttributeIndex();
    }

    public boolean equalFilters(DBDAttributeConstraint source)
    {
        return CommonUtils.equalObjects(this.attribute, source.attribute) &&
            this.orderPosition == source.orderPosition &&
            this.orderDescending == source.orderDescending &&
            CommonUtils.equalObjects(this.criteria, source.criteria);
    }

    @Override
    public int hashCode()
    {
        return this.attribute.hashCode() +
            (this.orderPosition) +
            (this.orderDescending ? 1 : 0) +
            (this.criteria == null ? 0 : this.criteria.hashCode()) +
            (this.visible ? 1 : 0) +
            (this.visualPosition);
    }

    @Override
    public boolean equals(Object obj)
    {
        if (!(obj instanceof DBDAttributeConstraint)) {
            return false;
        }
        DBDAttributeConstraint source = (DBDAttributeConstraint)obj;
        return equalFilters(source) &&
            this.visible == source.visible &&
            this.visualPosition == source.visualPosition;
    }

}
