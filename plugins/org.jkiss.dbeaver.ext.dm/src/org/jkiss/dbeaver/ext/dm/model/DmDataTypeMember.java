package org.jkiss.dbeaver.ext.dm.model;

import java.sql.ResultSet;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.dm.model.utils.DmConstants;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.impl.DBObjectNameCaseTransformer;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.struct.DBSEntity;
import org.jkiss.dbeaver.model.struct.DBSEntityElement;

public abstract class DmDataTypeMember implements DBSEntityElement{

    private static final Log log = Log.getLog(DmDataTypeMember.class);

    private DmDataType ownerType;
    protected String name;
    protected int number;
    private boolean inherited;
    private boolean persisted;
    
    
    protected DmDataTypeMember(DmDataType ownerType)
    {
        this.ownerType = ownerType;
        this.persisted = false;
    }

    protected DmDataTypeMember(DmDataType ownerType, ResultSet dbResult)
    {
        this.ownerType = ownerType;
        this.inherited = JDBCUtils.safeGetBoolean(dbResult, "INHERITED", DmConstants.YES);
        this.persisted = true;
    }

    @NotNull
    public DmDataType getOwnerType()
    {
        return ownerType;
    }

    @Nullable
    @Override
    public String getDescription()
    {
        return null;
    }

    @NotNull
    @Override
    public DmDataType getParentObject()
    {
        return ownerType;
    }

    @NotNull
    @Override
    public DmDataSource getDataSource()
    {
        return ownerType.getDataSource();
    }

    @Override
    public boolean isPersisted()
    {
        return persisted;
    }

    @NotNull
    @Override
    @Property(viewable = true, editable = true, valueTransformer = DBObjectNameCaseTransformer.class, order = 1)
    public String getName()
    {
        return name;
    }

    public int getNumber()
    {
        return number;
    }

    @Property(viewable = true, order = 20)
    public boolean isInherited()
    {
        return inherited;
    }
    

}
