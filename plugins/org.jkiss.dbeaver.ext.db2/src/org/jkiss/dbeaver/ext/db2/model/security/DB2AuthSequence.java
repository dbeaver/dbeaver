/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2013-2015 Denis Forveille (titou10.titou10@gmail.com)
 * Copyright (C) 2010-2015 Serge Rieder (serge@jkiss.org)
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
import org.jkiss.dbeaver.ext.db2.model.DB2Sequence;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.utils.CommonUtils;

import java.sql.ResultSet;

/**
 * DB2 Authorisations on Sequences
 * 
 * @author Denis Forveille
 */
public class DB2AuthSequence extends DB2AuthBase {

    private DB2AuthHeldType alter;
    private DB2AuthHeldType usage;

    // -----------------------
    // Constructors
    // -----------------------
    public DB2AuthSequence(DBRProgressMonitor monitor, DB2Grantee db2Grantee, DB2Sequence db2Sequence, ResultSet resultSet)
        throws DBException
    {
        super(monitor, db2Grantee, db2Sequence, resultSet);

        this.alter = CommonUtils.valueOf(DB2AuthHeldType.class, JDBCUtils.safeGetString(resultSet, "ALTERAUTH"));
        this.usage = CommonUtils.valueOf(DB2AuthHeldType.class, JDBCUtils.safeGetString(resultSet, "USAGEAUTH"));
    }

    // -----------------
    // Properties
    // -----------------

    @Property(viewable = true, order = 1)
    public DB2Schema getObjectSchema()
    {
        return super.getObjectSchema();
    }

    @Property(viewable = true, order = 2)
    public DBSObject getObject()
    {
        return super.getObject();
    }

    @Property(viewable = true, order = 20, category = DB2Constants.CAT_AUTH)
    public DB2AuthHeldType getAlter()
    {
        return alter;
    }

    @Property(viewable = true, order = 21, category = DB2Constants.CAT_AUTH)
    public DB2AuthHeldType getUsage()
    {
        return usage;
    }

}
