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
package org.jkiss.dbeaver.ext.tibero.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBPNamedObject2;
import org.jkiss.dbeaver.model.DBPQualifiedObject;
import org.jkiss.dbeaver.model.DBPScriptObject;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.impl.DBObjectNameCaseTransformer;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.rdb.DBSSequence;

import java.math.BigDecimal;
import java.util.Map;

public class TiberoSequence implements DBSSequence, DBPQualifiedObject, DBPNamedObject2, DBPScriptObject {

    private static final String RESULT_YES_VALUE = "Y";

    private final TiberoSchema schema;
    private String name;
    private BigDecimal minValue;
    private BigDecimal maxValue;
    private BigDecimal lastValue;
    private long incrementBy;
    private long cacheSize;
    private boolean flagCycle;
    private boolean flagOrder;
    private boolean persisted;

    private String sourceText;

    public TiberoSequence(@NotNull TiberoSchema schema, @NotNull JDBCResultSet dbResult) {
        this.schema = schema;
        this.name = JDBCUtils.safeGetString(dbResult, "SEQUENCE_NAME");
        this.minValue = readBigDecimalAsString(dbResult, "MIN_VALUE");
        this.maxValue = readBigDecimalAsString(dbResult, "MAX_VALUE");
        this.incrementBy = JDBCUtils.safeGetLong(dbResult, "INCREMENT_BY");
        this.cacheSize = JDBCUtils.safeGetLong(dbResult, "CACHE_SIZE");
        this.lastValue = readBigDecimalAsString(dbResult, "LAST_NUMBER");
        this.flagCycle = JDBCUtils.safeGetBoolean(dbResult, "CYCLE_FLAG", RESULT_YES_VALUE);
        this.flagOrder = JDBCUtils.safeGetBoolean(dbResult, "ORDER_FLAG", RESULT_YES_VALUE);
        this.persisted = true;
    }

    @Nullable
    private static BigDecimal readBigDecimalAsString(@NotNull JDBCResultSet dbResult, @NotNull String columnName) {
        String raw = JDBCUtils.safeGetString(dbResult, columnName);
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return new BigDecimal(raw.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @NotNull
    @Override
    @Property(viewable = true, editable = true, valueTransformer = DBObjectNameCaseTransformer.class, order = 1)
    public String getName() {
        return name;
    }

    @Override
    public void setName(@NotNull String name) {
        this.name = name;
    }

    @Nullable
    @Override
    public String getDescription() {
        return null;
    }

    @Override
    public DBSObject getParentObject() {
        return schema;
    }

    @NotNull
    @Override
    public TiberoDataSource getDataSource() {
        return schema.getDataSource();
    }

    @Override
    public boolean isPersisted() {
        return persisted;
    }

    public void setPersisted(boolean persisted) {
        this.persisted = persisted;
    }

    @Override
    @Property(viewable = true, order = 2)
    public BigDecimal getLastValue() {
        return lastValue;
    }

    public void setLastValue(BigDecimal lastValue) {
        this.lastValue = lastValue;
    }

    @Override
    @Property(viewable = true, order = 3)
    public BigDecimal getMinValue() {
        return minValue;
    }

    public void setMinValue(BigDecimal minValue) {
        this.minValue = minValue;
    }

    @Override
    @Property(viewable = true, order = 4)
    public BigDecimal getMaxValue() {
        return maxValue;
    }

    public void setMaxValue(BigDecimal maxValue) {
        this.maxValue = maxValue;
    }

    @Override
    @Property(viewable = true, order = 5)
    public Long getIncrementBy() {
        return incrementBy;
    }

    public void setIncrementBy(Long incrementBy) {
        this.incrementBy = incrementBy;
    }

    @Property(viewable = true, order = 6)
    public long getCacheSize() {
        return cacheSize;
    }

    public void setCacheSize(long cacheSize) {
        this.cacheSize = cacheSize;
    }

    @Property(viewable = true, order = 7)
    public boolean isCycle() {
        return flagCycle;
    }

    public void setCycle(boolean flagCycle) {
        this.flagCycle = flagCycle;
    }

    @Property(viewable = true, order = 8)
    public boolean isOrder() {
        return flagOrder;
    }

    public void setOrder(boolean flagOrder) {
        this.flagOrder = flagOrder;
    }

    @NotNull
    @Override
    public String getFullyQualifiedName(@NotNull DBPEvaluationContext context) {
        return DBUtils.getFullQualifiedName(getDataSource(), schema, this);
    }

    @NotNull
    @Override
    public String getObjectDefinitionText(
        @NotNull DBRProgressMonitor monitor,
        @Nullable Map<String, Object> options
    ) throws DBException {
        if (sourceText == null || (options != null && Boolean.TRUE.equals(options.get(OPTION_REFRESH)))) {
            sourceText = buildStatement(false);
        }
        return sourceText;
    }

    @NotNull
    public String buildStatement(boolean forUpdate) {
        StringBuilder sb = new StringBuilder();
        sb.append(forUpdate ? "ALTER SEQUENCE " : "CREATE SEQUENCE ");
        sb.append(getFullyQualifiedName(DBPEvaluationContext.DDL));
        if (incrementBy != 0) {
            sb.append("\n  INCREMENT BY ").append(incrementBy);
        }
        if (minValue != null) {
            sb.append("\n  MINVALUE ").append(minValue);
        }
        if (maxValue != null) {
            sb.append("\n  MAXVALUE ").append(maxValue);
        }
        sb.append(flagCycle ? "\n  CYCLE" : "\n  NOCYCLE");
        sb.append(cacheSize > 0 ? "\n  CACHE " + cacheSize : "\n  NOCACHE");
        sb.append(flagOrder ? "\n  ORDER;" : "\n  NOORDER;");
        return sb.toString();
    }
}
