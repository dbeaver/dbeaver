/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.oracle.model;

import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.utils.CommonUtils;

import java.sql.ResultSet;

/**
 * Java class
 */
public class OracleJavaClass extends OracleSchemaObject {

    public enum Accessibility {
        PUBLIC,
        PRIVATE,
        PROTECTED
    }

    private boolean isInterface;
    private Accessibility accessibility;

    protected OracleJavaClass(OracleSchema schema, ResultSet dbResult)
    {
        super(schema, JDBCUtils.safeGetString(dbResult, "NAME"), true);
        this.isInterface = "INTERFACE".equals(JDBCUtils.safeGetString(dbResult, "KIND"));
        this.accessibility = CommonUtils.valueOf(Accessibility.class, JDBCUtils.safeGetString(dbResult, "ACCESSIBILITY"));

    }

    @Property(name = "Access", viewable = true, order = 2)
    public Accessibility getAccessibility()
    {
        return accessibility;
    }

    @Property(name = "Interface", viewable = true, order = 3)
    public boolean isInterface()
    {
        return isInterface;
    }

}
