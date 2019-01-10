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
import org.jkiss.dbeaver.ext.db2.model.dict.DB2YesNo;
import org.jkiss.dbeaver.model.DBPNamedObject;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;

import java.sql.ResultSet;
import java.sql.Timestamp;

/**
 * DB2 Storage Group
 * 
 * @author Denis Forveille
 */
public class DB2StorageGroup extends DB2GlobalObject implements DBPNamedObject {

    private String name;
    private Integer id;
    private String owner;
    private Timestamp createTime;
    private Boolean defautSG;
    private Double overhead;
    private Double deviceReadRate;
    private Double writeOverhead;
    private Double deviceWriteRate;
    private Integer dataTag;
    private String cachingTier;
    private String remarks;

    // -----------------------
    // Constructors
    // -----------------------

    public DB2StorageGroup(DB2DataSource db2DataSource, ResultSet dbResult) throws DBException
    {
        super(db2DataSource, dbResult != null);
        this.name = JDBCUtils.safeGetString(dbResult, "SGNAME");
        this.id = JDBCUtils.safeGetInteger(dbResult, "SGID");
        this.owner = JDBCUtils.safeGetString(dbResult, "OWNER");
        this.createTime = JDBCUtils.safeGetTimestamp(dbResult, "CREATE_TIME");
        this.defautSG = JDBCUtils.safeGetBoolean(dbResult, "DEFAULTSG", DB2YesNo.Y.name());
        this.overhead = JDBCUtils.safeGetDouble(dbResult, "OVERHEAD");
        this.deviceReadRate = JDBCUtils.safeGetDouble(dbResult, "DEVICEREADRATE");
        this.writeOverhead = JDBCUtils.safeGetDouble(dbResult, "WRITEOVERHEAD");
        this.deviceWriteRate = JDBCUtils.safeGetDouble(dbResult, "DEVICEWRITERATE");
        this.dataTag = JDBCUtils.safeGetInteger(dbResult, "DATATAG");
        this.remarks = JDBCUtils.safeGetString(dbResult, "REMARKS");

        // DF: it is declared "Integer" in infocenter but Varchar in the catalog...
        if (db2DataSource.isAtLeastV10_5()) {
            this.cachingTier = JDBCUtils.safeGetString(dbResult, "CACHINGTIER");
        }

    }

    // -----------------
    // Properties
    // -----------------

    @NotNull
    @Override
    @Property(viewable = true, order = 1)
    public String getName()
    {
        return name;
    }

    @Property(viewable = true, order = 2)
    public Integer getId()
    {
        return id;
    }

    @Property(viewable = true, order = 3)
    public Boolean getDefautSG()
    {
        return defautSG;
    }

    @Property(viewable = false, category = DB2Constants.CAT_OWNER)
    public String getOwner()
    {
        return owner;
    }

    @Property(viewable = false, category = DB2Constants.CAT_DATETIME)
    public Timestamp getCreateTime()
    {
        return createTime;
    }

    @Property(viewable = false)
    public String getCachingTier()
    {
        return cachingTier;
    }

    @Property(viewable = false)
    public Integer getDataTag()
    {
        return dataTag;
    }

    @Nullable
    @Override
    @Property(viewable = false, multiline = true)
    public String getDescription()
    {
        return remarks;
    }

    @Property(viewable = false, order = 100, category = DB2Constants.CAT_PERFORMANCE)
    public Double getOverhead()
    {
        return overhead;
    }

    @Property(viewable = false, order = 101, category = DB2Constants.CAT_PERFORMANCE)
    public Double getDeviceReadRate()
    {
        return deviceReadRate;
    }

    @Property(viewable = false, order = 102, category = DB2Constants.CAT_PERFORMANCE)
    public Double getWriteOverhead()
    {
        return writeOverhead;
    }

    @Property(viewable = false, order = 103, category = DB2Constants.CAT_PERFORMANCE)
    public Double getDeviceWriteRate()
    {
        return deviceWriteRate;
    }

}
