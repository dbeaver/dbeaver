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
import org.jkiss.dbeaver.model.exec.DBCLogicalOperator;
import org.jkiss.utils.ArrayUtils;
import org.jkiss.utils.CommonUtils;

import java.util.Arrays;

/**
 * Attribute constraint
 */
public class DBDAttributeConstraintBase {

    public static int NULL_VISUAL_POSITION = -1;

    private int orderPosition;
    private boolean orderDescending;

    @Nullable
    private String criteria;
    @Nullable
    private DBCLogicalOperator operator;
    private boolean reverseOperator;
    @Nullable
    private Object value;

    private boolean visible;
    private int visualPosition;

    // Used to generate expressions
    @Nullable
    private String entityAlias;

    @Nullable
    private Object[] options;

    public DBDAttributeConstraintBase() {
    }

    public DBDAttributeConstraintBase(@NotNull DBDAttributeConstraintBase source) {
        copyFrom(source);
    }

    public void copyFrom(@NotNull DBDAttributeConstraintBase source) {
        this.orderPosition = source.orderPosition;
        this.orderDescending = source.orderDescending;
        this.criteria = source.criteria;
        this.operator = source.operator;
        this.reverseOperator = source.reverseOperator;
        this.value = source.value;
        this.visible = source.visible;
        this.visualPosition = source.visualPosition;
        this.options = source.options;
    }

    public int getOrderPosition() {
        return orderPosition;
    }

    public void setOrderPosition(int orderPosition) {
        this.orderPosition = orderPosition;
    }

    public boolean isOrderDescending() {
        return orderDescending;
    }

    public void setOrderDescending(boolean orderDescending) {
        this.orderDescending = orderDescending;
    }

    @Nullable
    public String getCriteria() {
        return criteria;
    }

    public void setCriteria(@Nullable String criteria) {
        this.criteria = criteria;
        this.operator = null;
        this.reverseOperator = false;
        this.value = null;
    }

    @Nullable
    public DBCLogicalOperator getOperator() {
        return operator;
    }

    public void setOperator(@Nullable DBCLogicalOperator operator) {
        this.criteria = null;
        this.operator = operator;
    }

    public boolean isReverseOperator() {
        return reverseOperator;
    }

    public void setReverseOperator(boolean reverseOperator) {
        this.reverseOperator = reverseOperator;
    }

    @Nullable
    public Object getValue() {
        return value;
    }

    public void setValue(@Nullable Object value) {
        this.criteria = null;
        this.value = value;
    }

    public boolean hasFilter() {
        return hasCondition();
    }

    public boolean isDirty() {
        return hasFilter() || !visible || orderPosition > 0 || !ArrayUtils.isEmpty(options);
    }

    public boolean hasCondition() {
        return !CommonUtils.isEmpty(criteria) || operator != null;
    }

    public boolean isVisible() {
        return visible;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    public int getVisualPosition() {
        return visualPosition;
    }

    public void setVisualPosition(int visualPosition) {
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

    @Nullable
    public Object[] getOptions() {
        return options;
    }

    public void setOptions(@Nullable Object[] options) {
        this.options = options;
    }

    public boolean hasOption(String option) {
        if (options == null) {
            return false;
        }
        for (int i = 0; i < options.length; i += 2) {
            if (options[i].equals(option)) {
                return true;
            }
        }
        return false;
    }

    public <T> T getOption(String option) {
        if (options == null) {
            return null;
        }
        for (int i = 0; i < options.length; i += 2) {
            if (options[i].equals(option)) {
                return (T) options[i + 1];
            }
        }
        return null;
    }

    public void setOption(String option, Object value) {
        Object[] newOptions = { option, value };
        if (options == null) {
            options = newOptions;
        } else {
            for (int i = 0; i < options.length; i += 2) {
                if (options[i].equals(option)) {
                    options[i + 1] = value;
                    return;
                }
            }
            options = ArrayUtils.concatArrays(options, newOptions);
        }
    }

    public boolean removeOption(String option) {
        if (options == null) {
            return false;
        }
        for (int i = 0; i < options.length; i += 2) {
            if (options[i].equals(option)) {
                options =
                    ArrayUtils.remove(Object.class,
                        ArrayUtils.remove(Object.class, options, i), i);
                return true;
            }
        }
        return false;
    }

    public void reset() {
        this.orderPosition = 0;
        this.orderDescending = false;
        this.criteria = null;
        this.operator = null;
        this.reverseOperator = false;
        this.value = null;
        this.visible = true;
        this.options = null;
    }

    public boolean equalFilters(DBDAttributeConstraintBase obj, boolean compareOrders) {
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
    public int hashCode() {
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
    public boolean equals(Object obj) {
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
                    this.visualPosition == source.visualPosition &&
                    Arrays.equals(this.options, source.options);
        } else {
            return false;
        }
    }

}
