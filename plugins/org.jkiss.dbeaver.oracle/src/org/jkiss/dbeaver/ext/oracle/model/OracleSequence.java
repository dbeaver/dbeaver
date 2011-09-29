/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.oracle.model;

import org.jkiss.dbeaver.model.impl.DBObjectNameCaseTransformer;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;

import java.sql.ResultSet;

/**
 * Oracle sequence
 */
public class OracleSequence extends OracleSchemaObject {

    private Number minValue;
    private Number maxValue;
    private long incrementBy;
    private long cacheSize;
    private Number lastNumber;
    private boolean flagCycle;
    private boolean flagOrder;

    public OracleSequence(OracleSchema schema, ResultSet dbResult)
    {
        super(schema, JDBCUtils.safeGetString(dbResult, "SEQUENCE_NAME"), true);
        this.minValue = (Number) JDBCUtils.safeGetObject(dbResult, "MIN_VALUE");
        this.maxValue = (Number) JDBCUtils.safeGetObject(dbResult, "MAX_VALUE");
        this.incrementBy = JDBCUtils.safeGetLong(dbResult, "INCREMENT_BY");
        this.cacheSize = JDBCUtils.safeGetLong(dbResult, "CACHE_SIZE");
        this.lastNumber = (Number) JDBCUtils.safeGetObject(dbResult, "LAST_NUMBER");
        this.flagCycle = JDBCUtils.safeGetBoolean(dbResult, "CYCLE_FLAG", "Y");
        this.flagOrder = JDBCUtils.safeGetBoolean(dbResult, "ORDER_FLAG", "Y");
    }

    @Property(name = "Sequence Name", viewable = true, editable = true, valueTransformer = DBObjectNameCaseTransformer.class, order = 1)
    public String getName()
    {
        return super.getName();
    }

    @Property(name = "Value", viewable = true, editable = true, order = 2)
    public Number getLastNumber()
    {
        return lastNumber;
    }

    @Property(name = "Min Value", viewable = true, editable = true, order = 3)
    public Number getMinValue()
    {
        return minValue;
    }

    @Property(name = "Max Value", viewable = true, editable = true, order = 4)
    public Number getMaxValue()
    {
        return maxValue;
    }

    @Property(name = "Increment", viewable = true, editable = true, order = 5)
    public long getIncrementBy()
    {
        return incrementBy;
    }

    @Property(name = "Cache", viewable = true, editable = true, order = 6)
    public long getCacheSize()
    {
        return cacheSize;
    }

    @Property(name = "Cycle", viewable = true, editable = true, order = 7)
    public boolean isCycle()
    {
        return flagCycle;
    }

    @Property(name = "Ordered", viewable = true, editable = true, order = 8)
    public boolean isOrder()
    {
        return flagOrder;
    }

}
