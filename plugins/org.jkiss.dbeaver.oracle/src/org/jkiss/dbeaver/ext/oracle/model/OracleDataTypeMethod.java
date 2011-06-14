/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.oracle.model;

import org.jkiss.dbeaver.ext.oracle.OracleConstants;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

import java.sql.ResultSet;

/**
 * Oracle data type attribute
 */
public class OracleDataTypeMethod extends OracleDataTypeMember {

    private String methodType;
    private boolean hasParameters;
    private int results;
    private boolean flagFinal;
    private boolean flagInstantiable;
    private boolean flagOverriding;

    public OracleDataTypeMethod(OracleDataType dataType)
    {
        super(dataType);
    }

    public OracleDataTypeMethod(DBRProgressMonitor monitor, OracleDataType dataType, ResultSet dbResult)
    {
        super(dataType, dbResult);
        this.name = JDBCUtils.safeGetString(dbResult, "METHOD_NAME");
        this.number = JDBCUtils.safeGetInt(dbResult, "METHOD_NO");

        this.methodType = JDBCUtils.safeGetString(dbResult, "METHOD_TYPE");
        this.hasParameters = JDBCUtils.safeGetInt(dbResult, "PARAMETERS") > 0;
        this.results = JDBCUtils.safeGetInt(dbResult, "RESULTS");

        this.flagFinal = JDBCUtils.safeGetBoolean(dbResult, "FINAL", OracleConstants.YES);
        this.flagInstantiable = JDBCUtils.safeGetBoolean(dbResult, "INSTANTIABLE", OracleConstants.YES);
        this.flagOverriding = JDBCUtils.safeGetBoolean(dbResult, "OVERRIDING", OracleConstants.YES);
    }

    @Property(name = "Method Type", viewable = true, editable = true, order = 5)
    public String getMethodType()
    {
        return methodType;
    }

    @Property(name = "Final", viewable = true, order = 6)
    public boolean isFinal()
    {
        return flagFinal;
    }

    @Property(name = "Instantiable", viewable = true, order = 7)
    public boolean isInstantiable()
    {
        return flagInstantiable;
    }

    @Property(name = "Overriding", viewable = true, order = 8)
    public boolean isOverriding()
    {
        return flagOverriding;
    }
}
