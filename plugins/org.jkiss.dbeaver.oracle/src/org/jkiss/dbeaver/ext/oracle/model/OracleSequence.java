/*
 * Copyright (C) 2010-2015 Serge Rieder
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

    @Override
    @Property(viewable = true, editable = true, valueTransformer = DBObjectNameCaseTransformer.class, order = 1)
    public String getName()
    {
        return super.getName();
    }

    @Property(viewable = true, editable = true, order = 2)
    public Number getLastNumber()
    {
        return lastNumber;
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
    public long getIncrementBy()
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
