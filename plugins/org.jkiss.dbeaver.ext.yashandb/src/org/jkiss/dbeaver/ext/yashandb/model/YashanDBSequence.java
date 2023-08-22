package org.jkiss.dbeaver.ext.yashandb.model;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBPScriptObject;
import org.jkiss.dbeaver.model.impl.DBObjectNameCaseTransformer;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.rdb.DBSSequence;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.util.Map;

/**
 * @Author: donghy
 * @Date: 2022/08
 * @Description:
 */
public class YashanDBSequence extends YashanDBSchemaObject implements DBSSequence, DBPScriptObject {

    private BigDecimal minValue;
    private BigDecimal maxValue;
    private long incrementBy;
    private long cacheSize;
    private BigDecimal lastValue;
    private boolean flagCycle;
    private boolean flagOrder;

    private String sourceText;

    public YashanDBSequence(YashanDBSchema schema, String name) {
        super(schema, name, false);
        this.minValue = null;
        this.maxValue = null;
        this.incrementBy = 0;
        this.cacheSize = 0;
        this.lastValue = new BigDecimal(0);
        this.flagCycle = false;
        this.flagOrder = false;
    }

    public YashanDBSequence(YashanDBSchema schema, ResultSet dbResult) {
        super(schema, JDBCUtils.safeGetString(dbResult, "SEQUENCE_NAME"), true);
        this.minValue = JDBCUtils.safeGetBigDecimal(dbResult, "MIN_VALUE");
        this.maxValue = JDBCUtils.safeGetBigDecimal(dbResult, "MAX_VALUE");
        this.incrementBy = JDBCUtils.safeGetLong(dbResult, "INCREMENT_BY");
        this.cacheSize = JDBCUtils.safeGetLong(dbResult, "CACHE_SIZE");
        this.lastValue = JDBCUtils.safeGetBigDecimal(dbResult, "LAST_NUMBER");
        this.flagCycle = JDBCUtils.safeGetBoolean(dbResult, "CYCLE_FLAG", "Y");
        this.flagOrder = JDBCUtils.safeGetBoolean(dbResult, "ORDER_FLAG", "Y");
    }

    @NotNull
    @Override
    @Property(viewable = true, editable = true, valueTransformer = DBObjectNameCaseTransformer.class, order = 1)
    public String getName() {
        return super.getName();
    }

    @Property(viewable = true, editable = true, updatable = true, order = 2)
    public BigDecimal getLastValue() {
        return lastValue;
    }

    public void setLastValue(BigDecimal lastValue) {
        this.lastValue = lastValue;
    }

    @Property(viewable = true, editable = true, updatable = true, order = 3)
    public BigDecimal getMinValue() {
        return minValue;
    }

    public void setMinValue(BigDecimal minValue) {
        this.minValue = minValue;
    }

    @Property(viewable = true, editable = true, updatable = true, order = 4)
    public BigDecimal getMaxValue() {
        return maxValue;
    }

    public void setMaxValue(BigDecimal maxValue) {
        this.maxValue = maxValue;
    }

    @Property(viewable = true, editable = true, updatable = true, order = 5)
    public Long getIncrementBy() {
        return incrementBy;
    }

    public void setIncrementBy(Long incrementBy) {
        this.incrementBy = incrementBy;
    }

    @Property(viewable = true, editable = true, updatable = true, order = 6)
    public long getCacheSize() {
        return cacheSize;
    }

    public void setCacheSize(long cacheSize) {
        this.cacheSize = cacheSize;
    }

    @Property(viewable = true, editable = true, updatable = true, order = 7)
    public boolean isCycle() {
        return flagCycle;
    }

    public void setCycle(boolean flagCycle) {
        this.flagCycle = flagCycle;
    }

    @Property(viewable = true, editable = true, updatable = true, order = 8)
    public boolean isOrder() {
        return flagOrder;
    }

    public void setOrder(boolean flagOrder) {
        this.flagOrder = flagOrder;
    }

    public String buildStatement(boolean forUpdate) {
        StringBuilder sb = new StringBuilder();
        if (forUpdate) {
            sb.append("ALTER SEQUENCE ");
        } else {
            sb.append("CREATE SEQUENCE ");
        }
        sb.append(getFullyQualifiedName(DBPEvaluationContext.DDL)).append(" ");

        if (getIncrementBy() != null) {
            sb.append("INCREMENT BY ").append(getIncrementBy()).append(" ");
        }
        if (getMinValue() != null) {
            sb.append("MINVALUE ").append(getMinValue()).append(" ");
        }
        if (getMaxValue() != null) {
            sb.append("MAXVALUE ").append(getMaxValue()).append(" ");
        }

        if (isCycle()) {
            sb.append("CYCLE ");
        } else {
            sb.append("NOCYCLE ");
        }

        if (getCacheSize() > 0) {
            sb.append("CACHE ").append(getCacheSize()).append(" ");
        } else {
            sb.append("NOCACHE ");
        }

        return sb.toString();
    }

    @Override
    public String getObjectDefinitionText(DBRProgressMonitor monitor, Map<String, Object> options) throws DBException {
        if (sourceText == null) {
            sourceText = buildStatement(false);
        }
        return sourceText;
    }

}
