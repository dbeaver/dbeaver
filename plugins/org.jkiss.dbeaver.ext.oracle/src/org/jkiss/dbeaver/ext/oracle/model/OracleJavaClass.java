/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2016 Serge Rieder (serge@jkiss.org)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (version 2)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package org.jkiss.dbeaver.ext.oracle.model;

import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;
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

    @Property(viewable = true, order = 2)
    public Accessibility getAccessibility()
    {
        return accessibility;
    }

    @Property(viewable = true, order = 3)
    public boolean isInterface()
    {
        return isInterface;
    }

}
