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

import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.exec.DBCLogicalOperator;
import org.jkiss.dbeaver.model.struct.DBSAttributeBase;
import org.jkiss.utils.CommonUtils;

/**
 * Attribute constraint
 */
public class DBDAttributeConstraintBase {

    private int orderPosition;
    private boolean orderDescending;

    private String criteria;
    private DBCLogicalOperator operator;
    private boolean reverseOperator;
    private Object value;

    private boolean visible;
    private int visualPosition;

    // USed to generate expressions
    private String entityAlias;

    public DBDAttributeConstraintBase() {
    }

    public DBDAttributeConstraintBase(DBDAttributeConstraintBase source)
    {
        copyFrom(source);
    }

    public void copyFrom(DBDAttributeConstraintBase source) {
        this.orderPosition = source.orderPosition;
        this.orderDescending = source.orderDescending;
        this.criteria = source.criteria;
        this.operator = source.operator;
        this.reverseOperator = source.reverseOperator;
        this.value = source.value;
        this.visible = source.visible;
        this.visualPosition = source.visualPosition;
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

    public void setCriteria(@Nullable String criteria)
    {
        this.criteria = criteria;
        this.operator = null;
        this.reverseOperator = false;
        this.value = null;
    }

    public DBCLogicalOperator getOperator() {
        return operator;
    }

    public void setOperator(DBCLogicalOperator operator) {
        this.criteria = null;
        this.operator = operator;
    }

    public boolean isReverseOperator() {
        return reverseOperator;
    }

    public void setReverseOperator(boolean reverseOperator) {
        this.reverseOperator = reverseOperator;
    }

    public Object getValue() {
        return value;
    }

    public void setValue(@Nullable Object value) {
        this.criteria = null;
        this.value = value;
    }

    public boolean hasFilter()
    {
        return hasCondition() || orderPosition > 0;
    }

    public boolean hasCondition() {
        return !CommonUtils.isEmpty(criteria) || operator != null;
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

    /**
     * Attribute owner entity alias.
     * Null by default. Can be set by SQL generation routines after entity alias resolution.
     */
    public String getEntityAlias() {
        return entityAlias;
    }

    public void setEntityAlias(String entityAlias) {
        this.entityAlias = entityAlias;
    }

    public void reset()
    {
        this.orderPosition = 0;
        this.orderDescending = false;
        this.criteria = null;
        this.operator = null;
        this.reverseOperator = false;
        this.value = null;
        this.visible = true;
    }

    public boolean equalFilters(DBDAttributeConstraintBase obj, boolean compareOrders)
    {
        if (compareOrders) {
            if (this.orderPosition != obj.orderPosition ||
                this.orderDescending != obj.orderDescending) {
                return false;
            }
        }
        return
            CommonUtils.equalObjects(this.criteria, obj.criteria) &&
            CommonUtils.equalObjects(this.operator, obj.operator) &&
            CommonUtils.equalObjects(this.reverseOperator, obj.reverseOperator) &&
            CommonUtils.equalObjects(this.value, obj.value);
    }

    @Override
    public int hashCode()
    {
        return
            orderPosition +
            (orderDescending ? 1 : 0) +
            (this.criteria == null ? 0 : this.criteria.hashCode()) +
            (this.operator == null ? 0 : this.operator.hashCode()) +
            (reverseOperator ? 1 : 0) +
            (this.value == null ? 0 : this.value.hashCode()) +
            (visible ? 1 : 0) +
            visualPosition
            ;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (obj instanceof DBDAttributeConstraintBase) {
            DBDAttributeConstraintBase source = (DBDAttributeConstraintBase) obj;
            return
                this.orderPosition == source.orderPosition &&
                this.orderDescending == source.orderDescending &&
                CommonUtils.equalObjects(this.criteria, source.criteria) &&
                CommonUtils.equalObjects(this.operator, source.operator) &&
                this.reverseOperator == source.reverseOperator &&
                CommonUtils.equalObjects(this.value, source.value) &&
                this.visible == source.visible &&
                this.visualPosition == source.visualPosition;
        } else {
            return false;
        }
    }

}
