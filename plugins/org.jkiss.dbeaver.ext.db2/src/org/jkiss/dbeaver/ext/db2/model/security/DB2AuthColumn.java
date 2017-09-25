/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2013-2017 Denis Forveille (titou10.titou10@gmail.com)
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jkiss.dbeaver.ext.db2.model.security;

import java.sql.ResultSet;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.db2.DB2Constants;
import org.jkiss.dbeaver.ext.db2.model.DB2Schema;
import org.jkiss.dbeaver.ext.db2.model.DB2TableColumn;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;

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

        // PRIVTYPE in ALTERINAUTH
        // GRANTABLE in CREATEINAUTH
        String privType = JDBCUtils.safeGetString(resultSet, "ALTERINAUTH");
        String grantable = JDBCUtils.safeGetString(resultSet, "CREATEINAUTH");

        if (privType.equals(UPDATE_PRIVILEGE)) {
            update = grantable.equals(DB2AuthHeldType.N.name()) ? DB2AuthHeldType.Y : DB2AuthHeldType.G;
        } else {
            reference = grantable.equals(DB2AuthHeldType.N.getName()) ? DB2AuthHeldType.Y : DB2AuthHeldType.G;
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
