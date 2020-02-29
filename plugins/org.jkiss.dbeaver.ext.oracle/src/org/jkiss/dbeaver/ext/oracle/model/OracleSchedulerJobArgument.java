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
package org.jkiss.dbeaver.ext.oracle.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.struct.DBSParameter;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;

import java.sql.ResultSet;

/**
 * OracleSchedulerJobArgument
 */
public class OracleSchedulerJobArgument implements DBSParameter
{
    private final OracleSchedulerJob job;
    private String name;
    private int position;
    private final String type;
    private String value;
    private String anyDataValue;
    private String outArgument;

    public OracleSchedulerJobArgument(
        OracleSchedulerJob job,
        ResultSet dbResult)
    {
        this.job = job;
        this.name = JDBCUtils.safeGetString(dbResult, "ARGUMENT_NAME");
        this.position = JDBCUtils.safeGetInt(dbResult, "ARGUMENT_POSITION");
        this.type = JDBCUtils.safeGetString(dbResult, "ARGUMENT_TYPE");
        this.value = JDBCUtils.safeGetString(dbResult, "VALUE");
        this.anyDataValue = JDBCUtils.safeGetString(dbResult, "ANYDATA_VALUE");
        this.outArgument = JDBCUtils.safeGetString(dbResult, "OUT_ARGUMENT");
    }

    @Nullable
    @Override
    public String getDescription()
    {
        return null;
    }

    @NotNull
    @Override
    public OracleDataSource getDataSource()
    {
        return job.getDataSource();
    }

    @Override
    public OracleSchedulerJob getParentObject()
    {
        return job;
    }

    @Override
    public boolean isPersisted()
    {
        return true;
    }

    @NotNull
    @Override
    @Property(viewable = true, order = 10)
    public String getName() {
        return name;
    }

    @Property(viewable = true, order = 11)
    public int getPosition()
    {
        return position;
    }

    @Property(viewable = true, order = 12)
    public String getType() {
        return type;
    }

    @Property(viewable = true, order = 14)
    public String getValue() {
        return value;
    }

    @Property(viewable = true, order = 15)
    public String getAnyDataValue() {
        return anyDataValue;
    }

    @Property(viewable = true, order = 16)
    public String getOutArgument() {
        return outArgument;
    }

    @NotNull
    @Override
    public DBSTypedObject getParameterType() {
        return getDataSource().getLocalDataType(type);
    }

}
