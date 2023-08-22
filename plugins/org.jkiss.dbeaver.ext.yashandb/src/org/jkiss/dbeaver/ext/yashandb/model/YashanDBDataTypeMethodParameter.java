package org.jkiss.dbeaver.ext.yashandb.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSParameter;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;

import java.sql.ResultSet;

/**
 * @Author: donghy
 * @Date: 2022/08
 * @Description:
 */
public class YashanDBDataTypeMethodParameter implements DBSParameter {
    private final YashanDBDataTypeMethod method;
    private String name;
    private int number;
    private YashanDBParameterMode mode;
    private YashanDBDataType type;
    private YashanDBDataTypeModifier typeMod;

    public YashanDBDataTypeMethodParameter(DBRProgressMonitor monitor, YashanDBDataTypeMethod method, ResultSet dbResult) {
        this.method = method;
        this.name = JDBCUtils.safeGetString(dbResult, "PARAM_NAME");
        this.number = JDBCUtils.safeGetInt(dbResult, "PARAM_NO");
        this.mode = YashanDBParameterMode.getMode(JDBCUtils.safeGetString(dbResult, "PARAM_MODE"));
        this.type = YashanDBDataType.resolveDataType(
                monitor,
                method.getDataSource(),
                JDBCUtils.safeGetString(dbResult, "PARAM_TYPE_OWNER"),
                JDBCUtils.safeGetString(dbResult, "PARAM_TYPE_NAME"));
        this.typeMod = YashanDBDataTypeModifier.resolveTypeModifier(
                JDBCUtils.safeGetString(dbResult, "PARAM_TYPE_MOD"));
    }

    @Override
    public DBSObject getParentObject() {
        return method;
    }

    @NotNull
    @Override
    public DBPDataSource getDataSource() {
        return method.getDataSource();
    }

    @Override
    public boolean isPersisted() {
        return true;
    }

    @Nullable
    @Override
    public String getDescription() {
        return null;
    }

    @NotNull
    @Override
    @Property(viewable = true, order = 1)
    public String getName() {
        return name;
    }

    @Property(viewable = true, order = 2)
    public int getNumber() {
        return number;
    }

    @Property(viewable = true, order = 3)
    public YashanDBParameterMode getMode() {
        return mode;
    }

    @Property(id = "dataType", viewable = true, order = 4)
    public YashanDBDataType getType() {
        return type;
    }

    @Property(id = "dataTypeMod", viewable = true, order = 5)
    public YashanDBDataTypeModifier getTypeMod() {
        return typeMod;
    }

    @NotNull
    @Override
    public DBSTypedObject getParameterType() {
        return type;
    }
}
