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
package org.jkiss.dbeaver.ext.cubrid.model;

import java.math.BigDecimal;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.ext.cubrid.CubridConstants;
import org.jkiss.dbeaver.ext.generic.model.GenericSequence;
import org.jkiss.dbeaver.ext.generic.model.GenericStructContainer;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.meta.PropertyLength;

public class CubridSequence extends GenericSequence {

    private CubridUser owner;
    private BigDecimal startValue;
    private BigDecimal minValue;
    private BigDecimal maxValue;
    private BigDecimal incrementBy;
    private boolean cycle;
    private int cachedNum;
    private String description;

    public CubridSequence(
            @NotNull GenericStructContainer container,
            @NotNull String name,
            @NotNull JDBCResultSet dbResult) {
        super(container, name, "", null, null, null, null);
        this.owner = (CubridUser) container;
        this.startValue = JDBCUtils.safeGetBigDecimal(dbResult, "current_val");
        this.minValue = JDBCUtils.safeGetBigDecimal(dbResult, "min_val");
        this.maxValue = JDBCUtils.safeGetBigDecimal(dbResult, "max_val");
        this.incrementBy = JDBCUtils.safeGetBigDecimal(dbResult, "increment_val");
        this.cycle = JDBCUtils.safeGetBoolean(dbResult, "cyclic");
        this.cachedNum = JDBCUtils.safeGetInteger(dbResult, "cached_num");
        this.description = JDBCUtils.safeGetString(dbResult, CubridConstants.COMMENT);
    }

    public CubridSequence(@NotNull GenericStructContainer container, @NotNull String name) {
        super(container, name);
        this.owner = (CubridUser) container;
        this.startValue = new BigDecimal(1);
        this.minValue = new BigDecimal(1);
        this.maxValue = new BigDecimal(Long.MAX_VALUE);
        this.incrementBy = new BigDecimal(1);
        this.cycle = false;
        this.cachedNum = 0;
    }

    @NotNull
    @Override
    @Property(viewable = true, editable = true, order = 1)
    public String getName() {
        return super.getName();
    }

    @NotNull
    @Property(viewable = true, order = 2)
    public CubridUser getOwner() {
        return owner;
    }

    @Override
    @Property(hidden = true)
    public Number getLastValue() {
        return null;
    }

    @NotNull
    @Property(viewable = true, editable = true, updatable = true, order = 3)
    public BigDecimal getStartValue() {
        return startValue;
    }

    public void setStartValue(@NotNull BigDecimal startValue) {
        this.startValue = startValue;
    }

    @NotNull
    @Override
    @Property(viewable = true, editable = true, updatable = true, order = 4)
    public BigDecimal getMinValue() {
        return minValue;
    }

    public void setMinValue(@NotNull BigDecimal minValue) {
        this.minValue = minValue;
    }

    @NotNull
    @Override
    @Property(viewable = true, editable = true, updatable = true, order = 5)
    public BigDecimal getMaxValue() {
        return maxValue;
    }

    public void setMaxValue(@NotNull BigDecimal maxValue) {
        this.maxValue = maxValue;
    }

    @NotNull
    @Override
    @Property(viewable = true, editable = true, updatable = true, order = 6)
    public BigDecimal getIncrementBy() {
        return incrementBy;
    }

    public void setIncrementBy(@NotNull BigDecimal incrementBy) {
        this.incrementBy = incrementBy;
    }

    @NotNull
    @Property(viewable = true, editable = true, updatable = true, order = 7)
    public boolean getCycle() {
        return cycle;
    }

    public void setCycle(@NotNull boolean cycle) {
        this.cycle = cycle;
    }

    @NotNull
    @Property(viewable = true, editable = true, updatable = true, order = 8)
    public int getCachedNum() {
        return cachedNum;
    }

    public void setCachedNum(@NotNull int cachedNum) {
        this.cachedNum = cachedNum;
    }

    @Nullable
    @Override
    @Property(viewable = true, editable = true, updatable = true, length = PropertyLength.MULTILINE, order = 10)
    public String getDescription() {
        return description;
    }

    public void setDescription(@Nullable String description) {
        this.description = description;
    }

    @NotNull
    @Override
    public String getFullyQualifiedName(@NotNull DBPEvaluationContext context) {
        if (((CubridDataSource) owner.getDataSource()).getSupportMultiSchema()) {
            return DBUtils.getFullQualifiedName(getDataSource(), this.getOwner(), this);
        } else {
            return DBUtils.getFullQualifiedName(getDataSource(), this);
        }
    }
}
