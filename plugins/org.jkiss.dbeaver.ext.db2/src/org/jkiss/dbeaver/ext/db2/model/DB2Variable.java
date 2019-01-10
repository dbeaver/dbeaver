/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2013-2015 Denis Forveille (titou10.titou10@gmail.com)
 * Copyright (C) 2010-2019 Serge Rider (serge@jkiss.org)
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
package org.jkiss.dbeaver.ext.db2.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.db2.DB2Constants;
import org.jkiss.dbeaver.ext.db2.model.dict.DB2OwnerType;
import org.jkiss.dbeaver.ext.db2.model.dict.DB2YesNo;
import org.jkiss.dbeaver.ext.db2.model.module.DB2Module;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.utils.CommonUtils;

import java.sql.ResultSet;
import java.sql.Timestamp;

/**
 * DB2 Variable (Global or Module)
 * 
 * @author Denis Forveille
 */
public class DB2Variable extends DB2Object<DBSObject> {

    private DB2Schema db2Schema;

    private Integer id;
    private String owner;
    private DB2OwnerType ownerType;
    private Timestamp createTime;
    private Timestamp lastRegenTime;
    private Boolean valid;
    private Boolean published;

    private String remarks;

    // -----------------------
    // Constructors
    // -----------------------

    public DB2Variable(DBSObject owner, ResultSet dbResult) throws DBException
    {

        super(owner, JDBCUtils.safeGetString(dbResult, "VARNAME"), true);

        this.id = JDBCUtils.safeGetInteger(dbResult, "VARID");
        this.owner = JDBCUtils.safeGetString(dbResult, "OWNER");
        this.ownerType = CommonUtils.valueOf(DB2OwnerType.class, JDBCUtils.safeGetString(dbResult, "OWNERTYPE"));
        this.createTime = JDBCUtils.safeGetTimestamp(dbResult, "CREATE_TIME");
        this.lastRegenTime = JDBCUtils.safeGetTimestamp(dbResult, "LAST_REGEN_TIME");
        this.valid = JDBCUtils.safeGetBoolean(dbResult, "VALID", DB2YesNo.Y.name());
        this.published = JDBCUtils.safeGetBoolean(dbResult, "PUBLISHED", DB2YesNo.Y.name());
        this.remarks = JDBCUtils.safeGetString(dbResult, "REMARKS");

        if (owner instanceof DB2Module) {
            db2Schema = ((DB2Module) owner).getSchema();
        } else {
            String schemaName = JDBCUtils.safeGetStringTrimmed(dbResult, "VARSCHEMA");
            this.db2Schema = ((DB2DataSource) owner).getSchema(new VoidProgressMonitor(), schemaName);
        }
    }

    @Nullable
    @Override
    public String getDescription()
    {
        return remarks;
    }

    // -----------------------
    // Properties
    // -----------------------

    @NotNull
    @Override
    @Property(viewable = true, order = 1)
    public String getName()
    {
        return name;
    }

    @Property(viewable = true, order = 2)
    public DB2Schema getSchema()
    {
        return db2Schema;
    }

    @Property(viewable = true, order = 3)
    public Integer getId()
    {
        return id;
    }

    @Property(viewable = true, order = 4, category = DB2Constants.CAT_OWNER)
    public String getOwner()
    {
        return owner;
    }

    @Property(viewable = true, order = 5, category = DB2Constants.CAT_OWNER)
    public DB2OwnerType getOwnerType()
    {
        return ownerType;
    }

    @Property(viewable = true, order = 6)
    public Boolean getValid()
    {
        return valid;
    }

    @Property(viewable = true, order = 7)
    public Boolean getPublished()
    {
        return published;
    }

    @Property(viewable = false, order = 8, category = DB2Constants.CAT_DATETIME)
    public Timestamp getCreateTime()
    {
        return createTime;
    }

    @Property(viewable = false, order = 9, category = DB2Constants.CAT_DATETIME)
    public Timestamp getLastRegenTime()
    {
        return lastRegenTime;
    }

}
