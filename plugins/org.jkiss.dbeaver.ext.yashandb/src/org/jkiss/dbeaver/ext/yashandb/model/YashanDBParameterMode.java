package org.jkiss.dbeaver.ext.yashandb.model;

import org.jkiss.dbeaver.model.struct.rdb.DBSProcedureParameterKind;
import org.jkiss.utils.CommonUtils;

/**
 * @Author: donghy
 * @Date: 2022/08
 * @Description:
 */
public enum YashanDBParameterMode {
    IN(DBSProcedureParameterKind.IN),

    OUT(DBSProcedureParameterKind.OUT),

    INOUT(DBSProcedureParameterKind.INOUT),

    RETURN(DBSProcedureParameterKind.RETURN);

    private final DBSProcedureParameterKind parameterKind;

    YashanDBParameterMode(DBSProcedureParameterKind parameterKind) {
        this.parameterKind = parameterKind;
    }

    public static YashanDBParameterMode getMode(String modeName) {
        if (CommonUtils.isEmpty(modeName)) {
            return null;
        } else if ("IN".equals(modeName)) {
            return IN;
        } else if ("OUT".equals(modeName)) {
            return YashanDBParameterMode.OUT;
        } else {
            return YashanDBParameterMode.INOUT;
        }
    }

    public DBSProcedureParameterKind getParameterKind() {
        return parameterKind;
    }
}
