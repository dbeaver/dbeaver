/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.oracle.model;

import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCObjectNameCaseTransformer;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.struct.DBSObject;

import java.sql.ResultSet;

/**
 * Oracle sequence
 */
public class OracleSequence implements DBSObject {

    private final OracleSchema schema;
    private String name;
    private long minValue;
    private long maxValue;
    private long incrementBy;
    private long cacheSize;
    private long lastNumber;
    private boolean flagCycle;
    private boolean flagOrder;
    private boolean persisted;

    public OracleSequence(OracleSchema schema, ResultSet dbResult)
    {
        this.schema = schema;
        this.name = JDBCUtils.safeGetString(dbResult, "SEQUENCE_NAME");
        this.minValue = JDBCUtils.safeGetLong(dbResult, "MIN_VALUE");
        this.maxValue = JDBCUtils.safeGetLong(dbResult, "MAX_VALUE");
        this.incrementBy = JDBCUtils.safeGetLong(dbResult, "INCREMENT_BY");
        this.cacheSize = JDBCUtils.safeGetLong(dbResult, "CACHE_SIZE");
        this.lastNumber = JDBCUtils.safeGetLong(dbResult, "LAST_NUMBER");
        this.flagCycle = JDBCUtils.safeGetBoolean(dbResult, "CYCLE_FLAG", "Y");
        this.flagOrder = JDBCUtils.safeGetBoolean(dbResult, "ORDER_FLAG", "Y");
        this.persisted = true;
    }

    public OracleSchema getSchema()
    {
        return schema;
    }

    @Property(name = "Sequence Name", viewable = true, editable = true, valueTransformer = JDBCObjectNameCaseTransformer.class, order = 1)
    public String getName()
    {
        return name;
    }

    @Property(name = "Value", viewable = true, editable = true, order = 2)
    public long getLastNumber()
    {
        return lastNumber;
    }

    @Property(name = "Min Value", viewable = true, editable = true, order = 3)
    public long getMinValue()
    {
        return minValue;
    }

    @Property(name = "Max Value", viewable = true, editable = true, order = 4)
    public long getMaxValue()
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

    public boolean isPersisted()
    {
        return persisted;
    }

    public String getDescription()
    {
        return null;
    }

    public DBSObject getParentObject()
    {
        return schema;
    }

    public DBPDataSource getDataSource()
    {
        return schema.getDataSource();
    }
}
