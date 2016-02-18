/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2016 Serge Rieder (serge@jkiss.org)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (version 2)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package org.jkiss.dbeaver.ext.oracle.model;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.impl.DBObjectNameCaseTransformer;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.struct.rdb.DBSSequence;

import java.sql.ResultSet;

/**
 * Oracle sequence
 */
public class OracleSequence extends OracleSchemaObject implements DBSSequence {

    private Number minValue;
    private Number maxValue;
    private long incrementBy;
    private long cacheSize;
    private Number lastValue;
    private boolean flagCycle;
    private boolean flagOrder;

    public OracleSequence(OracleSchema schema, ResultSet dbResult)
    {
        super(schema, JDBCUtils.safeGetString(dbResult, "SEQUENCE_NAME"), true);
        this.minValue = (Number) JDBCUtils.safeGetObject(dbResult, "MIN_VALUE");
        this.maxValue = (Number) JDBCUtils.safeGetObject(dbResult, "MAX_VALUE");
        this.incrementBy = JDBCUtils.safeGetLong(dbResult, "INCREMENT_BY");
        this.cacheSize = JDBCUtils.safeGetLong(dbResult, "CACHE_SIZE");
        this.lastValue = (Number) JDBCUtils.safeGetObject(dbResult, "LAST_NUMBER");
        this.flagCycle = JDBCUtils.safeGetBoolean(dbResult, "CYCLE_FLAG", "Y");
        this.flagOrder = JDBCUtils.safeGetBoolean(dbResult, "ORDER_FLAG", "Y");
    }

    @NotNull
    @Override
    @Property(viewable = true, editable = true, valueTransformer = DBObjectNameCaseTransformer.class, order = 1)
    public String getName()
    {
        return super.getName();
    }

    @Property(viewable = true, editable = true, order = 2)
    public Number getLastValue()
    {
        return lastValue;
    }

    @Property(viewable = true, editable = true, order = 3)
    public Number getMinValue()
    {
        return minValue;
    }

    @Property(viewable = true, editable = true, order = 4)
    public Number getMaxValue()
    {
        return maxValue;
    }

    @Property(viewable = true, editable = true, order = 5)
    public Number getIncrementBy()
    {
        return incrementBy;
    }

    @Property(viewable = true, editable = true, order = 6)
    public long getCacheSize()
    {
        return cacheSize;
    }

    @Property(viewable = true, editable = true, order = 7)
    public boolean isCycle()
    {
        return flagCycle;
    }

    @Property(viewable = true, editable = true, order = 8)
    public boolean isOrder()
    {
        return flagOrder;
    }

}
