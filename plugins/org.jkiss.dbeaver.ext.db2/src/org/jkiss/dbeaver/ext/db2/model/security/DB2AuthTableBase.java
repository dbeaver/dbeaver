/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2013-2015 Denis Forveille (titou10.titou10@gmail.com)
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
package org.jkiss.dbeaver.ext.db2.model.security;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.db2.DB2Constants;
import org.jkiss.dbeaver.ext.db2.model.DB2TableBase;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.utils.CommonUtils;

import java.sql.ResultSet;

/**
 * DB2 Common Authorisations on Tables and Views
 * 
 * @author Denis Forveille
 */
public abstract class DB2AuthTableBase extends DB2AuthBase {

    private DB2AuthHeldType control;
    private DB2AuthHeldType alter;
    private DB2AuthHeldType delete;
    private DB2AuthHeldType index;
    private DB2AuthHeldType insert;
    private DB2AuthHeldType reference;
    private DB2AuthHeldType select;
    private DB2AuthHeldType update;

    // -----------------------
    // Constructors
    // -----------------------
    public DB2AuthTableBase(DBRProgressMonitor monitor, DB2Grantee db2Grantee, DB2TableBase db2TableBase, ResultSet resultSet)
        throws DBException
    {
        super(monitor, db2Grantee, db2TableBase, resultSet);

        this.control = CommonUtils.valueOf(DB2AuthHeldType.class, JDBCUtils.safeGetString(resultSet, "CONTROLAUTH"));
        this.alter = CommonUtils.valueOf(DB2AuthHeldType.class, JDBCUtils.safeGetString(resultSet, "ALTERAUTH"));
        this.delete = CommonUtils.valueOf(DB2AuthHeldType.class, JDBCUtils.safeGetString(resultSet, "DELETEAUTH"));
        this.index = CommonUtils.valueOf(DB2AuthHeldType.class, JDBCUtils.safeGetString(resultSet, "INDEXAUTH"));
        this.insert = CommonUtils.valueOf(DB2AuthHeldType.class, JDBCUtils.safeGetString(resultSet, "INSERTAUTH"));
        this.reference = CommonUtils.valueOf(DB2AuthHeldType.class, JDBCUtils.safeGetString(resultSet, "REFAUTH"));
        this.select = CommonUtils.valueOf(DB2AuthHeldType.class, JDBCUtils.safeGetString(resultSet, "SELECTAUTH"));
        this.update = CommonUtils.valueOf(DB2AuthHeldType.class, JDBCUtils.safeGetString(resultSet, "UPDATEAUTH"));
    }

    // -----------------
    // Properties
    // -----------------

    @Property(viewable = true, order = 20, category = DB2Constants.CAT_AUTH)
    public DB2AuthHeldType getControl()
    {
        return control;
    }

    @Property(viewable = true, order = 21, category = DB2Constants.CAT_AUTH)
    public DB2AuthHeldType getAlter()
    {
        return alter;
    }

    @Property(viewable = true, order = 22, category = DB2Constants.CAT_AUTH)
    public DB2AuthHeldType getDelete()
    {
        return delete;
    }

    @Property(viewable = true, order = 23, category = DB2Constants.CAT_AUTH)
    public DB2AuthHeldType getIndex()
    {
        return index;
    }

    @Property(viewable = true, order = 24, category = DB2Constants.CAT_AUTH)
    public DB2AuthHeldType getInsert()
    {
        return insert;
    }

    @Property(viewable = true, order = 25, category = DB2Constants.CAT_AUTH)
    public DB2AuthHeldType getReference()
    {
        return reference;
    }

    @Property(viewable = true, order = 26, category = DB2Constants.CAT_AUTH)
    public DB2AuthHeldType getSelect()
    {
        return select;
    }

    @Property(viewable = true, order = 27, category = DB2Constants.CAT_AUTH)
    public DB2AuthHeldType getUpdate()
    {
        return update;
    }

}
