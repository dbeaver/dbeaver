/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2021 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.mssql.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.ext.mssql.SQLServerUtils;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBPQualifiedObject;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.meta.PropertyLength;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.rdb.DBSSequence;

/**
 * SQLServerSequence
 */
public class SQLServerSequence implements DBSSequence, DBPQualifiedObject
{
    private SQLServerSchema schema;
    private long objectId;
    private String name;
    private String description;
    private Number lastValue;
    private Number minValue;
    private Number maxValue;
    private Number incrementBy;
    private boolean persisted;

    public SQLServerSequence(SQLServerSchema schema, long objectId, String name, Number lastValue, Number minValue, Number maxValue, Number incrementBy, boolean persisted) {
        this.schema = schema;
        this.name = name;
        this.objectId = objectId;
        this.description = description;
        this.lastValue = lastValue;
        this.minValue = minValue;
        this.maxValue = maxValue;
        this.incrementBy = incrementBy;

        this.persisted = persisted;
    }

    @NotNull
    @Override
    @Property(viewable = true, order = 1)
    public String getName() {
        return name;
    }

    @Override
    public boolean isPersisted() {
        return true;
    }

    @Nullable
    @Override
    @Property(viewable = true, length = PropertyLength.MULTILINE, order = 10)
    public String getDescription() {
        return description;
    }

    @Nullable
    @Override
    public DBSObject getParentObject() {
        return schema;
    }

    @NotNull
    @Override
    public SQLServerDataSource getDataSource() {
        return schema.getDataSource();
    }

    @NotNull
    @Override
    public String getFullyQualifiedName(DBPEvaluationContext context) {
        if (!SQLServerUtils.supportsCrossDatabaseQueries(getDataSource())) {
            return DBUtils.getFullQualifiedName(getDataSource(), schema, this);
        }
        return DBUtils.getFullQualifiedName(getDataSource(),
            schema.getDatabase(),
            schema,
            this);
    }

    @Override
    @Property(viewable = true, order = 2)
    public Number getLastValue() {
        return lastValue;
    }

    public void setLastValue(Number lastValue) {
        this.lastValue = lastValue;
    }

    @Override
    @Property(viewable = true, order = 3)
    public Number getMinValue() {
        return minValue;
    }

    @Override
    @Property(viewable = true, order = 4)
    public Number getMaxValue() {
        return maxValue;
    }

    @Override
    @Property(viewable = true, order = 5)
    public Number getIncrementBy() {
        return incrementBy;
    }

}
