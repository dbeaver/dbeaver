package org.jkiss.dbeaver.ext.yashandb.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.impl.DBObjectNameCaseTransformer;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.struct.DBSEntityElement;

import java.sql.ResultSet;

/**
 * @Author: donghy
 * @Date: 2022/08
 * @Description:
 */
public class YashanDBDataTypeMember implements DBSEntityElement {
    private static final Log log = Log.getLog(YashanDBDataTypeMember.class);

    private YashanDBDataType ownerType;
    protected String name;
    protected int number;
    private boolean inherited;
    private boolean persisted;

    protected YashanDBDataTypeMember(YashanDBDataType ownerType) {
        this.ownerType = ownerType;
        this.persisted = false;
    }

    protected YashanDBDataTypeMember(YashanDBDataType ownerType, ResultSet dbResult) {
        this.ownerType = ownerType;
        this.inherited = JDBCUtils.safeGetBoolean(dbResult, "INHERITED", YashanDBConstants.YES);
        this.persisted = true;
    }

    @NotNull
    public YashanDBDataType getOwnerType() {
        return ownerType;
    }

    @Nullable
    @Override
    public String getDescription() {
        return null;
    }

    @NotNull
    @Override
    public YashanDBDataType getParentObject() {
        return ownerType;
    }

    @NotNull
    @Override
    public YashanDBDataSource getDataSource() {
        return ownerType.getDataSource();
    }

    @Override
    public boolean isPersisted() {
        return persisted;
    }

    @NotNull
    @Override
    @Property(viewable = true, editable = true, valueTransformer = DBObjectNameCaseTransformer.class, order = 1)
    public String getName() {
        return name;
    }

    public int getNumber() {
        return number;
    }

    @Property(viewable = true, order = 20)
    public boolean isInherited() {
        return inherited;
    }
}
