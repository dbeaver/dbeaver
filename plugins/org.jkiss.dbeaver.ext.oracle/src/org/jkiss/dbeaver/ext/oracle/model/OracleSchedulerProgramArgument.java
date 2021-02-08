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
package org.jkiss.dbeaver.ext.oracle.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.struct.DBSParameter;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;

import java.sql.ResultSet;

/**
 * OracleSchedulerProgramArgument
 */
public class OracleSchedulerProgramArgument implements DBSParameter
{
    private final OracleSchedulerProgram program;
    private String name;
    private int position;
    private final String type;
    private String metadataAttribute;
    private String defaultValue;
    private String defaultAnyDataValue;
    private String outArgument;

    public OracleSchedulerProgramArgument(
        OracleSchedulerProgram program,
        ResultSet dbResult)
    {
        this.program = program;
        this.name = JDBCUtils.safeGetString(dbResult, "ARGUMENT_NAME");
        this.position = JDBCUtils.safeGetInt(dbResult, "ARGUMENT_POSITION");
        this.type = JDBCUtils.safeGetString(dbResult, "ARGUMENT_TYPE");
        this.metadataAttribute = JDBCUtils.safeGetString(dbResult, "METADATA_ATTRIBUTE");
        this.defaultValue = JDBCUtils.safeGetString(dbResult, "DEFAULT_VALUE");
        this.defaultAnyDataValue = JDBCUtils.safeGetString(dbResult, "DEFAULT_ANYDATA_VALUE");
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
        return program.getDataSource();
    }

    @Override
    public OracleSchedulerProgram getParentObject()
    {
        return program;
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

    @Property(viewable = true, order = 13)
    public String getMetadataAttribute() {
        return metadataAttribute;
    }

    @Property(viewable = true, order = 14)
    public String getDefaultValue() {
        return defaultValue;
    }

    @Property(viewable = true, order = 15)
    public String getDefaultAnyDataValue() {
        return defaultAnyDataValue;
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
