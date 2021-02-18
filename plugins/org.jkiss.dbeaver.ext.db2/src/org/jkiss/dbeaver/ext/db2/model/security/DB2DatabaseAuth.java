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
import org.jkiss.dbeaver.ext.db2.DB2Constants;
import org.jkiss.dbeaver.ext.db2.model.DB2DataSource;
import org.jkiss.dbeaver.ext.db2.model.DB2GlobalObject;
import org.jkiss.dbeaver.ext.db2.model.dict.DB2YesNo;
import org.jkiss.dbeaver.model.access.DBAPrivilege;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.utils.CommonUtils;

import java.sql.ResultSet;

/**
 * DB2 Database Authorisation
 * 
 * @author Denis Forveille
 */
public class DB2DatabaseAuth extends DB2GlobalObject implements DBAPrivilege {

    private DBSObject grantor;
    private DB2GrantorGranteeType grantorType;

    private Boolean bindAdd;
    private Boolean connect;
    private Boolean createTab;
    private Boolean dbAdm;
    private Boolean externalRoutine;
    private Boolean implicitSchema;
    private Boolean load;
    private Boolean noFence;
    private Boolean quiesceConnect;
    private Boolean libraryAdmin;
    private Boolean securityAdmin;
    private Boolean sqlAdmin;
    private Boolean workLoadAdmin;
    private Boolean explain;
    private Boolean dataAccess;
    private Boolean accessControl;
    private Boolean createSecure;

    // -----------------------
    // Constructors
    // -----------------------

    public DB2DatabaseAuth(DBRProgressMonitor monitor, DB2DataSource dataSource, ResultSet resultSet) throws DBException
    {
        super(dataSource, true);

        String grantorName = JDBCUtils.safeGetStringTrimmed(resultSet, "GRANTOR");
        this.grantorType = CommonUtils.valueOf(DB2GrantorGranteeType.class,
            JDBCUtils.safeGetStringTrimmed(resultSet, "GRANTORTYPE"));
        switch (grantorType) {
        case U:
            this.grantor = dataSource.getUser(monitor, grantorName);
            break;
        case G:
            this.grantor = dataSource.getGroup(monitor, grantorName);
            break;
        default:
            break;
        }

        this.bindAdd = JDBCUtils.safeGetBoolean(resultSet, "BINDADDAUTH", DB2YesNo.Y.name());
        this.connect = JDBCUtils.safeGetBoolean(resultSet, "CONNECTAUTH", DB2YesNo.Y.name());
        this.createTab = JDBCUtils.safeGetBoolean(resultSet, "CREATETABAUTH", DB2YesNo.Y.name());
        this.dbAdm = JDBCUtils.safeGetBoolean(resultSet, "DBADMAUTH", DB2YesNo.Y.name());
        this.externalRoutine = JDBCUtils.safeGetBoolean(resultSet, "EXTERNALROUTINEAUTH", DB2YesNo.Y.name());
        this.implicitSchema = JDBCUtils.safeGetBoolean(resultSet, "IMPLSCHEMAAUTH", DB2YesNo.Y.name());
        this.load = JDBCUtils.safeGetBoolean(resultSet, "LOADAUTH", DB2YesNo.Y.name());
        this.noFence = JDBCUtils.safeGetBoolean(resultSet, "NOFENCEAUTH", DB2YesNo.Y.name());
        this.quiesceConnect = JDBCUtils.safeGetBoolean(resultSet, "QUIESCECONNECTAUTH", DB2YesNo.Y.name());
        this.libraryAdmin = JDBCUtils.safeGetBoolean(resultSet, "LIBRARYADMAUTH", DB2YesNo.Y.name());
        this.securityAdmin = JDBCUtils.safeGetBoolean(resultSet, "SECURITYADMAUTH", DB2YesNo.Y.name());
        this.sqlAdmin = JDBCUtils.safeGetBoolean(resultSet, "SQLADMAUTH", DB2YesNo.Y.name());
        this.workLoadAdmin = JDBCUtils.safeGetBoolean(resultSet, "WLMADMAUTH", DB2YesNo.Y.name());
        this.explain = JDBCUtils.safeGetBoolean(resultSet, "EXPLAINAUTH", DB2YesNo.Y.name());
        this.dataAccess = JDBCUtils.safeGetBoolean(resultSet, "DATAACCESSAUTH", DB2YesNo.Y.name());
        this.accessControl = JDBCUtils.safeGetBoolean(resultSet, "ACCESSCTRLAUTH", DB2YesNo.Y.name());

        if (dataSource.isAtLeastV10_1()) {
            this.createSecure = JDBCUtils.safeGetBoolean(resultSet, "CREATESECUREAUTH", DB2YesNo.Y.name());
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
        return "DBAUTH"; // Fake name
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

    @Property(viewable = true, order = 20, category = DB2Constants.CAT_AUTH)
    public Boolean getDbAdm()
    {
        return dbAdm;
    }

    @Property(viewable = true, order = 21, category = DB2Constants.CAT_AUTH)
    public Boolean getBindAdd()
    {
        return bindAdd;
    }

    @Property(viewable = true, order = 22, category = DB2Constants.CAT_AUTH)
    public Boolean getConnect()
    {
        return connect;
    }

    @Property(viewable = true, order = 23, category = DB2Constants.CAT_AUTH)
    public Boolean getCreateTab()
    {
        return createTab;
    }

    @Property(viewable = true, order = 24, category = DB2Constants.CAT_AUTH)
    public Boolean getExternalRoutine()
    {
        return externalRoutine;
    }

    @Property(viewable = true, order = 25, category = DB2Constants.CAT_AUTH)
    public Boolean getImplicitSchema()
    {
        return implicitSchema;
    }

    @Property(viewable = true, order = 26, category = DB2Constants.CAT_AUTH)
    public Boolean getLoad()
    {
        return load;
    }

    @Property(viewable = true, order = 27, category = DB2Constants.CAT_AUTH)
    public Boolean getDataAccess()
    {
        return dataAccess;
    }

    @Property(viewable = true, order = 28, category = DB2Constants.CAT_AUTH)
    public Boolean getAccessControl()
    {
        return accessControl;
    }

    @Property(viewable = false, order = 29, category = DB2Constants.CAT_AUTH)
    public Boolean getNoFence()
    {
        return noFence;
    }

    @Property(viewable = false, order = 30, category = DB2Constants.CAT_AUTH)
    public Boolean getQuiesceConnect()
    {
        return quiesceConnect;
    }

    @Property(viewable = false, order = 31, category = DB2Constants.CAT_AUTH)
    public Boolean getLibraryAdmin()
    {
        return libraryAdmin;
    }

    @Property(viewable = false, order = 32, category = DB2Constants.CAT_AUTH)
    public Boolean getSecurityAdmin()
    {
        return securityAdmin;
    }

    @Property(viewable = false, order = 33, category = DB2Constants.CAT_AUTH)
    public Boolean getSqlAdmin()
    {
        return sqlAdmin;
    }

    @Property(viewable = false, order = 34, category = DB2Constants.CAT_AUTH)
    public Boolean getWorkLoadAdmin()
    {
        return workLoadAdmin;
    }

    @Property(viewable = false, order = 35, category = DB2Constants.CAT_AUTH)
    public Boolean getExplain()
    {
        return explain;
    }

    @Property(viewable = false, order = 36, category = DB2Constants.CAT_AUTH)
    public Boolean getCreateSecure()
    {
        return createSecure;
    }

}
