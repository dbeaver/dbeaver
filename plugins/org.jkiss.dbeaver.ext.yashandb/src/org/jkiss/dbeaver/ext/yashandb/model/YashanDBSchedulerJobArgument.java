package org.jkiss.dbeaver.ext.yashandb.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.struct.DBSParameter;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;

import java.sql.ResultSet;

/**
 * @Description: 
 *
 * @Author dengqh
 * @Date 2023/7/5 11:07
 */public class YashanDBSchedulerJobArgument implements DBSParameter
{
    private final YashanDBSchedulerJob job;
    private String name;
    private int position;
    private final String type;
    private String value;
    private String anyDataValue;
    private String outArgument;

    public  YashanDBSchedulerJobArgument(
            YashanDBSchedulerJob job,
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
    public  YashanDBDataSource getDataSource()
    {
        return job.getDataSource();
    }

    @Override
    public  YashanDBSchedulerJob getParentObject()
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

