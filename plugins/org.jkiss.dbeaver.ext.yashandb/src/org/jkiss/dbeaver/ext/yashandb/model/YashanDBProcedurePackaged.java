package org.jkiss.dbeaver.ext.yashandb.model;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBPUniqueObject;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.struct.rdb.DBSProcedureType;

import java.sql.ResultSet;
import java.util.Objects;

/**
 * @Author: donghy
 * @Date: 2022/08
 * @Description:
 */
public class YashanDBProcedurePackaged extends YashanDBProcedureBase<YashanDBPackage> implements DBPUniqueObject {
    private Integer overload;

    public YashanDBProcedurePackaged(
            YashanDBPackage ownerPackage,
            ResultSet dbResult) {
        super(ownerPackage,
                JDBCUtils.safeGetString(dbResult, "PROCEDURE_NAME"),
                0l,
                DBSProcedureType.valueOf(JDBCUtils.safeGetString(dbResult, "OBJECT_TYPE").equals("UDF") ? "FUNCTION" : "PROCEDURE"));
//                Objects.equals(JDBCUtils.safeGetString(dbResult, "OBJECT_TYPE"), "UDF") ?
//                        DBSProcedureType.FUNCTION:DBSProcedureType.PROCEDURE);
    }

    @NotNull
    @Override
    public String getFullyQualifiedName(DBPEvaluationContext context) {
        return DBUtils.getFullQualifiedName(getDataSource(),
                getSchema(),
                getParentObject(),
                this);
    }

    @Override
    public YashanDBSchema getSchema() {
        return getParentObject().getSchema();
    }

    @Override
    public Integer getOverloadNumber() {
        return overload;
    }

    public void setOverload(int overload) {
        this.overload = overload;
    }

    @NotNull
    @Override
    public String getUniqueName() {
        return overload == null || overload <= 1 ? getName() : getName() + "#" + overload;
    }

}
