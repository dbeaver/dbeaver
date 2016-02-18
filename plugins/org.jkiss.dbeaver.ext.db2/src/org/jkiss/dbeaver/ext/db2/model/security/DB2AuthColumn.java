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
import org.jkiss.dbeaver.ext.db2.model.DB2Schema;
import org.jkiss.dbeaver.ext.db2.model.DB2TableColumn;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;

import java.sql.ResultSet;

/**
 * DB2 Authorisations on Columns
 * 
 * @author Denis Forveille
 */
public class DB2AuthColumn extends DB2AuthBase {

    private static final String UPDATE_PRIVILEGE = "U";

    private DB2AuthHeldType reference = DB2AuthHeldType.N;
    private DB2AuthHeldType update = DB2AuthHeldType.N;;

    // -----------------------
    // Constructors
    // -----------------------
    public DB2AuthColumn(DBRProgressMonitor monitor, DB2Grantee db2Grantee, DB2TableColumn db2TableColumn, ResultSet resultSet)
        throws DBException
    {
        super(monitor, db2Grantee, db2TableColumn, resultSet);

        String privType = JDBCUtils.safeGetString(resultSet, "ALTERINAUTH");
        String grantable = JDBCUtils.safeGetString(resultSet, "ALTERINAUTH");

        if (privType.equals(UPDATE_PRIVILEGE)) {
            if (grantable.equals(DB2AuthHeldType.N)) {
                update = DB2AuthHeldType.Y;
            } else {
                update = DB2AuthHeldType.G;
            }
        } else {
            if (grantable.equals(DB2AuthHeldType.N)) {
                reference = DB2AuthHeldType.Y;
            } else {
                reference = DB2AuthHeldType.G;
            }
        }
    }

    // -----------------
    // Properties
    // -----------------

    @Property(viewable = true, order = 2)
    public DBSObject getObject()
    {
        return super.getObject();
    }

    @Property(viewable = true, order = 1)
    public DB2Schema getObjectSchema()
    {
        return super.getObjectSchema();
    }

    @Property(viewable = true, order = 20, category = DB2Constants.CAT_AUTH)
    public DB2AuthHeldType getReference()
    {
        return reference;
    }

    @Property(viewable = true, order = 21, category = DB2Constants.CAT_AUTH)
    public DB2AuthHeldType getUpdate()
    {
        return update;
    }

}
