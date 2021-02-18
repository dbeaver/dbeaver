/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2013-2015 Denis Forveille (titou10.titou10@gmail.com)
 * Copyright (C) 2010-2021 DBeaver Corp and others
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

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.db2.model.DB2DataSource;
import org.jkiss.dbeaver.ext.db2.model.DB2Object;
import org.jkiss.dbeaver.ext.db2.model.DB2Schema;
import org.jkiss.dbeaver.ext.db2.model.DB2SchemaObject;
import org.jkiss.dbeaver.model.access.DBAPrivilege;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.utils.CommonUtils;

import java.sql.ResultSet;

/**
 * Base class for DB2 Authorisations
 * 
 * @author Denis Forveille
 */
public abstract class DB2AuthBase extends DB2Object<DB2Grantee> implements DBAPrivilege {

    private DBSObject grantor;
    private DB2GrantorGranteeType grantorType;

    private DB2Schema objectSchema;
    private DBSObject object;

    // -----------------------
    // Constructors
    // -----------------------

    public DB2AuthBase(DBRProgressMonitor monitor, DB2Grantee db2Grantee, DBSObject object, ResultSet resultSet) throws DBException
    {
        super(db2Grantee, JDBCUtils.safeGetStringTrimmed(resultSet, "OBJ_SCHEMA") + "."
            + JDBCUtils.safeGetString(resultSet, "OBJ_NAME"), true);

        // get schema from object itself if this is a DB2SchemaObject
        if (object instanceof DB2SchemaObject) {
            this.objectSchema = ((DB2SchemaObject) object).getSchema();
        } else {
            DB2DataSource db2DataSource = db2Grantee.getDataSource();
            String objectSchemaName = JDBCUtils.safeGetStringTrimmed(resultSet, "OBJ_SCHEMA");
            if (objectSchemaName != null) {
                this.objectSchema = db2DataSource.getSchema(monitor, objectSchemaName);
            }
        }

        this.object = object;

        String grantorName = JDBCUtils.safeGetStringTrimmed(resultSet, "GRANTOR");
        this.grantorType = CommonUtils.valueOf(DB2GrantorGranteeType.class,
            JDBCUtils.safeGetStringTrimmed(resultSet, "GRANTORTYPE"));
        switch (grantorType) {
        case U:
            this.grantor = db2Grantee.getDataSource().getUser(monitor, grantorName);
            break;
        case G:
            this.grantor = db2Grantee.getDataSource().getGroup(monitor, grantorName);
            break;
        default:
            break;
        }

    }

    // -----------------
    // Properties
    // -----------------

    @NotNull
    @Override
    @Property(hidden = true)
    public String getName()
    {
        return super.getName();
    }

    @Property(viewable = true, order = 1)
    public DB2Schema getObjectSchema()
    {
        return objectSchema;
    }

    @Property(viewable = true, order = 2)
    public DBSObject getObject()
    {
        return object;
    }

    @Property(viewable = true, order = 3)
    public DBSObject getGrantor()
    {
        return grantor;
    }

    @Property(viewable = true, order = 4)
    public DB2GrantorGranteeType getGrantorType()
    {
        return grantorType;
    }

}
