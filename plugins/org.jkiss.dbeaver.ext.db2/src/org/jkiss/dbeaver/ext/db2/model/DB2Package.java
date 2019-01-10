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
import org.jkiss.dbeaver.model.DBPRefreshableObject;
import org.jkiss.dbeaver.model.impl.DBSObjectCache;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCObjectSimpleCache;
import org.jkiss.dbeaver.model.meta.Association;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.utils.CommonUtils;

import java.sql.Date;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.Collection;

/**
 * DB2 Packages
 * 
 * @author Denis Forveille
 */
public class DB2Package extends DB2SchemaObject implements DBPRefreshableObject {

    private static final String C_DEP = "SELECT * FROM SYSCAT.PACKAGEDEP WHERE PKGSCHEMA = ? AND PKGNAME = ? ORDER BY BSCHEMA,BNAME WITH UR";
    private static final String C_STM = "SELECT * FROM SYSCAT.STATEMENTS WHERE PKGSCHEMA = ? AND PKGNAME = ? ORDER BY SECTNO WITH UR";

    private final DBSObjectCache<DB2Package, DB2PackageDep> packageDepCache;
    private final DBSObjectCache<DB2Package, DB2PackageStatement> packageStatementsCache;

    private Boolean valid;
    private String owner;
    private DB2OwnerType ownerType;

    private DB2Schema defaultSchema;
    private String uniqueId;
    private Long id;
    private String version;

    private Integer totalSections;
    private String dateTimeFormat;
    private String isolation;
    private String concurrentAccessResolution;
    private String blocking;
    private Boolean insertBuf;
    private String langLevel;
    private String funcPath;
    private Integer queryOpt;
    private String degree;
    private Boolean multiNodePlan;
    private String intraParallel;
    private String validate;
    private String dynamicRules;
    private String sqlerror;
    private Boolean busTimeSensitive;
    private Boolean sysTimeSensitive;
    private Boolean keepDynamic;
    private Boolean staticAsDynamic;

    private Timestamp firstBindTime;
    private Timestamp lastBindTime;
    private Timestamp explicitBindTime;
    private Timestamp alterTime;
    private Date lastUsed;

    private String remarks;

    // -----------------------
    // Constructors
    // -----------------------

    public DB2Package(DB2Schema schema, ResultSet dbResult) throws DBException
    {
        super(schema, JDBCUtils.safeGetStringTrimmed(dbResult, "PKGNAME"), true);

        DB2DataSource db2DataSource = schema.getDataSource();

        this.owner = JDBCUtils.safeGetString(dbResult, "OWNER");

        String defaultSchemaName = JDBCUtils.safeGetStringTrimmed(dbResult, "DEFAULT_SCHEMA");
        this.defaultSchema = getDataSource().getSchema(new VoidProgressMonitor(), defaultSchemaName);

        this.uniqueId = JDBCUtils.safeGetString(dbResult, "UNIQUE_ID");
        this.valid = JDBCUtils.safeGetBoolean(dbResult, "VALID", DB2YesNo.Y.name());
        this.version = JDBCUtils.safeGetString(dbResult, "PKGVERSION");

        this.totalSections = JDBCUtils.safeGetInteger(dbResult, "TOTAL_SECT");
        this.dateTimeFormat = JDBCUtils.safeGetString(dbResult, "FORMAT");
        this.isolation = JDBCUtils.safeGetString(dbResult, "ISOLATION");
        this.blocking = JDBCUtils.safeGetString(dbResult, "BLOCKING");
        this.insertBuf = JDBCUtils.safeGetBoolean(dbResult, "INSERT_BUF", DB2YesNo.Y.name());
        this.langLevel = JDBCUtils.safeGetString(dbResult, "LANG_LEVEL");
        this.funcPath = JDBCUtils.safeGetString(dbResult, "FUNC_PATH");
        this.queryOpt = JDBCUtils.safeGetInteger(dbResult, "QUERYOPT");
        this.degree = JDBCUtils.safeGetString(dbResult, "DEGREE");
        this.multiNodePlan = JDBCUtils.safeGetBoolean(dbResult, "MULTINODE_PLANS", DB2YesNo.Y.name());
        this.intraParallel = JDBCUtils.safeGetString(dbResult, "INTRA_PARALLEL");
        this.validate = JDBCUtils.safeGetString(dbResult, "VALIDATE");
        this.dynamicRules = JDBCUtils.safeGetString(dbResult, "DYNAMICRULES");
        this.sqlerror = JDBCUtils.safeGetString(dbResult, "SQLERROR");

        this.lastBindTime = JDBCUtils.safeGetTimestamp(dbResult, "LAST_BIND_TIME");
        this.explicitBindTime = JDBCUtils.safeGetTimestamp(dbResult, "EXPLICIT_BIND_TIME");
        this.remarks = JDBCUtils.safeGetString(dbResult, "REMARKS");

        if (db2DataSource.isAtLeastV9_5()) {
            this.ownerType = CommonUtils.valueOf(DB2OwnerType.class, JDBCUtils.safeGetString(dbResult, "OWNERTYPE"));
        }
        if (db2DataSource.isAtLeastV9_7()) {
            this.firstBindTime = JDBCUtils.safeGetTimestamp(dbResult, "CREATE_TIME");
            this.alterTime = JDBCUtils.safeGetTimestamp(dbResult, "ALTER_TIME");
            this.concurrentAccessResolution = JDBCUtils.safeGetString(dbResult, "CONCURRENTACCESSRESOLUTION");
            this.lastUsed = JDBCUtils.safeGetDate(dbResult, "LASTUSED");
            this.id = JDBCUtils.safeGetLong(dbResult, "PKGID");
        }
        if (schema.getDataSource().isAtLeastV10_1()) {
            this.busTimeSensitive = JDBCUtils.safeGetBoolean(dbResult, "BUSTIMESENSITIVE", DB2YesNo.Y.name());
            this.sysTimeSensitive = JDBCUtils.safeGetBoolean(dbResult, "SYSTIMESENSITIVE", DB2YesNo.Y.name());
            this.keepDynamic = JDBCUtils.safeGetBoolean(dbResult, "KEEPDYNAMIC", DB2YesNo.Y.name());
            this.staticAsDynamic = JDBCUtils.safeGetBoolean(dbResult, "STATICASDYNAMIC", DB2YesNo.Y.name());
        }

        packageDepCache = new JDBCObjectSimpleCache<>(DB2PackageDep.class, C_DEP, schema.getName(),
            getName());
        packageStatementsCache = new JDBCObjectSimpleCache<>(DB2PackageStatement.class, C_STM,
            schema.getName(), getName());
    }

    // -----------------
    // Association
    // -----------------

    @Association
    public Collection<DB2PackageDep> getPackageDeps(DBRProgressMonitor monitor) throws DBException
    {
        return packageDepCache.getAllObjects(monitor, this);
    }

    @Association
    public Collection<DB2PackageStatement> getStatements(DBRProgressMonitor monitor) throws DBException
    {
        return packageStatementsCache.getAllObjects(monitor, this);
    }

    @Override
    public DBSObject refreshObject(@NotNull DBRProgressMonitor monitor) throws DBException
    {
        packageDepCache.clearCache();
        return this;
    }

    // -----------------
    // Properties
    // -----------------

    @Property(viewable = true, order = 3, category = DB2Constants.CAT_OWNER)
    public DB2OwnerType getOwnerType()
    {
        return ownerType;
    }

    @Property(viewable = true, order = 4, category = DB2Constants.CAT_OWNER)
    public String getOwner()
    {
        return owner;
    }

    @Property(viewable = false, order = 5)
    public String getUniqueId()
    {
        return uniqueId;
    }

    @Property(viewable = true, order = 6)
    public Long getId()
    {
        return id;
    }

    @Property(viewable = true, order = 7)
    public String getVersion()
    {
        return version;
    }

    @Property(viewable = true, order = 8)
    public Boolean getValid()
    {
        return valid;
    }

    @Nullable
    @Override
    @Property(viewable = false, multiline = true, order = 9)
    public String getDescription()
    {
        return remarks;
    }

    @Property(viewable = true, order = 20)
    public DB2Schema getDefaultSchema()
    {
        return defaultSchema;
    }

    @Property(viewable = true, order = 21)
    public Integer getTotalSections()
    {
        return totalSections;
    }

    @Property(viewable = false, order = 22)
    public String getDateTimeFormat()
    {
        return dateTimeFormat;
    }

    @Property(viewable = false, order = 23)
    public String getIsolation()
    {
        return isolation;
    }

    @Property(viewable = false, order = 24)
    public String getConcurrentAccessResolution()
    {
        return concurrentAccessResolution;
    }

    @Property(viewable = true, order = 25)
    public String getBlocking()
    {
        return blocking;
    }

    @Property(viewable = false, order = 26)
    public Boolean getInsertBuf()
    {
        return insertBuf;
    }

    @Property(viewable = false, order = 27)
    public String getLangLevel()
    {
        return langLevel;
    }

    @Property(viewable = false, order = 28)
    public String getFuncPath()
    {
        return funcPath;
    }

    @Property(viewable = true, order = 29)
    public Integer getQueryOpt()
    {
        return queryOpt;
    }

    @Property(viewable = true, order = 30)
    public String getDegree()
    {
        return degree;
    }

    @Property(viewable = false, order = 31)
    public Boolean getMultiNodePlan()
    {
        return multiNodePlan;
    }

    @Property(viewable = false, order = 32)
    public String getIntraParallel()
    {
        return intraParallel;
    }

    @Property(viewable = false, order = 33)
    public String getValidate()
    {
        return validate;
    }

    @Property(viewable = false, order = 34)
    public String getDynamicRules()
    {
        return dynamicRules;
    }

    @Property(viewable = false, order = 35)
    public String getSqlerror()
    {
        return sqlerror;
    }

    @Property(viewable = false, order = 36)
    public Boolean getBusTimeSensitive()
    {
        return busTimeSensitive;
    }

    @Property(viewable = false, order = 37)
    public Boolean getSysTimeSensitive()
    {
        return sysTimeSensitive;
    }

    @Property(viewable = false, order = 38)
    public Boolean getKeepDynamic()
    {
        return keepDynamic;
    }

    @Property(viewable = false, order = 39)
    public Boolean getStaticAsDynamic()
    {
        return staticAsDynamic;
    }

    @Property(viewable = false, order = 50, category = DB2Constants.CAT_DATETIME)
    public Timestamp getFirstBindTime()
    {
        return firstBindTime;
    }

    @Property(viewable = false, order = 51, category = DB2Constants.CAT_DATETIME)
    public Timestamp getLastBindTime()
    {
        return lastBindTime;
    }

    @Property(viewable = false, order = 52, category = DB2Constants.CAT_DATETIME)
    public Timestamp getExplicitBindTime()
    {
        return explicitBindTime;
    }

    @Property(viewable = false, order = 53, category = DB2Constants.CAT_DATETIME)
    public Timestamp getAlterTime()
    {
        return alterTime;
    }

    @Property(viewable = false, order = 54, category = DB2Constants.CAT_DATETIME)
    public Date getLastUsed()
    {
        return lastUsed;
    }

}
