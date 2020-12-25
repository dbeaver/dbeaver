/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2013-2015 Denis Forveille (titou10.titou10@gmail.com)
 * Copyright (C) 2010-2020 DBeaver Corp and others
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

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.db2.DB2Constants;
import org.jkiss.dbeaver.ext.db2.model.DB2Schema;
import org.jkiss.dbeaver.ext.db2.model.DB2Variable;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.utils.CommonUtils;

import java.sql.ResultSet;

/**
 * DB2 Authorisations on Variables
 * 
 * @author Denis Forveille
 */
public class DB2AuthVariable extends DB2AuthBase {

    private DB2AuthHeldType read;
    private DB2AuthHeldType write;

    // -----------------------
    // Constructors
    // -----------------------
    public DB2AuthVariable(DBRProgressMonitor monitor, DB2Grantee db2Grantee, DB2Variable db2Variable, ResultSet resultSet)
        throws DBException
    {
        super(monitor, db2Grantee, db2Variable, resultSet);

        this.read = CommonUtils.valueOf(DB2AuthHeldType.class, JDBCUtils.safeGetString(resultSet, "USAGEAUTH"));
        this.write = CommonUtils.valueOf(DB2AuthHeldType.class, JDBCUtils.safeGetString(resultSet, "ALTERINAUTH"));
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
    public DB2AuthHeldType getRead()
    {
        return read;
    }

    @Property(viewable = true, order = 21, category = DB2Constants.CAT_AUTH)
    public DB2AuthHeldType getWrite()
    {
        return write;
    }

}
