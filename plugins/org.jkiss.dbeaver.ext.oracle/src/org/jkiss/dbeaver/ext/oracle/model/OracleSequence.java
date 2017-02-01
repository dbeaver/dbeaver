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
