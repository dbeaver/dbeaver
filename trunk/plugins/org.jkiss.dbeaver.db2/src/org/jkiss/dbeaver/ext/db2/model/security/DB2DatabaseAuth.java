/*
 * Copyright (C) 2013      Denis Forveille titou10.titou10@gmail.com
 * Copyright (C) 2010-2013 Serge Rieder serge@jkiss.org
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package org.jkiss.dbeaver.ext.db2.model.security;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.db2.DB2Constants;
import org.jkiss.dbeaver.ext.db2.model.DB2DataSource;
import org.jkiss.dbeaver.ext.db2.model.DB2GlobalObject;
import org.jkiss.dbeaver.ext.db2.model.dict.DB2YesNo;
import org.jkiss.dbeaver.model.DBPSaveableObject;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.runtime.VoidProgressMonitor;
import org.jkiss.utils.CommonUtils;

import java.sql.ResultSet;

/**
 * DB2 Database Authorisation
 * 
 * @author Denis Forveille
 */
public class DB2DatabaseAuth extends DB2GlobalObject implements DBPSaveableObject {

    private DBSObject grantee;
    private DB2GrantorGranteeType granteeType;
    private String grantor;
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

    public DB2DatabaseAuth(DB2DataSource dataSource, ResultSet resultSet) throws DBException
    {
        super(dataSource, true);

        this.grantor = JDBCUtils.safeGetString(resultSet, "GRANTOR");
        this.grantorType = CommonUtils.valueOf(DB2GrantorGranteeType.class, JDBCUtils.safeGetString(resultSet, "GRANTORTYPE"));

        String granteeName = JDBCUtils.safeGetString(resultSet, "GRANTEE");
        this.granteeType = CommonUtils.valueOf(DB2GrantorGranteeType.class, JDBCUtils.safeGetString(resultSet, "GRANTEETYPE"));
        switch (granteeType) {
        case U:
            this.grantee = dataSource.getUser(VoidProgressMonitor.INSTANCE, granteeName);
            break;
        case G:
            this.grantee = dataSource.getGroup(VoidProgressMonitor.INSTANCE, granteeName);
            break;
        case R:
            this.grantee = dataSource.getRole(VoidProgressMonitor.INSTANCE, granteeName);
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
        this.createSecure = JDBCUtils.safeGetBoolean(resultSet, "CREATESECUREAUTH", DB2YesNo.Y.name());
    }

    // -----------------
    // Properties
    // -----------------

    @Override
    @Property(hidden = true)
    public String getName()
    {
        return grantee.getName();
    }

    @Property(viewable = true, order = 1)
    public DBSObject getGrantee()
    {
        return grantee;
    }

    @Property(viewable = true, order = 2)
    public DB2GrantorGranteeType getGranteeType()
    {
        return granteeType;
    }

    @Property(viewable = true, order = 4)
    public String getGrantor()
    {
        return grantor;
    }

    @Property(viewable = true, order = 3)
    public DB2GrantorGranteeType getGrantorType()
    {
        return grantorType;
    }

    @Property(viewable = true, order = 10, category = DB2Constants.CAT_AUTH)
    public Boolean getDbAdm()
    {
        return dbAdm;
    }

    @Property(viewable = true, order = 11, category = DB2Constants.CAT_AUTH)
    public Boolean getBindAdd()
    {
        return bindAdd;
    }

    @Property(viewable = true, order = 12, category = DB2Constants.CAT_AUTH)
    public Boolean getConnect()
    {
        return connect;
    }

    @Property(viewable = true, order = 13, category = DB2Constants.CAT_AUTH)
    public Boolean getCreateTab()
    {
        return createTab;
    }

    @Property(viewable = true, order = 14, category = DB2Constants.CAT_AUTH)
    public Boolean getExternalRoutine()
    {
        return externalRoutine;
    }

    @Property(viewable = true, order = 15, category = DB2Constants.CAT_AUTH)
    public Boolean getImplicitSchema()
    {
        return implicitSchema;
    }

    @Property(viewable = true, order = 16, category = DB2Constants.CAT_AUTH)
    public Boolean getLoad()
    {
        return load;
    }

    @Property(viewable = true, order = 17, category = DB2Constants.CAT_AUTH)
    public Boolean getDataAccess()
    {
        return dataAccess;
    }

    @Property(viewable = true, order = 18, category = DB2Constants.CAT_AUTH)
    public Boolean getAccessControl()
    {
        return accessControl;
    }

    @Property(viewable = false, order = 19, category = DB2Constants.CAT_AUTH)
    public Boolean getNoFence()
    {
        return noFence;
    }

    @Property(viewable = false, order = 20, category = DB2Constants.CAT_AUTH)
    public Boolean getQuiesceConnect()
    {
        return quiesceConnect;
    }

    @Property(viewable = false, order = 21, category = DB2Constants.CAT_AUTH)
    public Boolean getLibraryAdmin()
    {
        return libraryAdmin;
    }

    @Property(viewable = false, order = 22, category = DB2Constants.CAT_AUTH)
    public Boolean getSecurityAdmin()
    {
        return securityAdmin;
    }

    @Property(viewable = false, order = 23, category = DB2Constants.CAT_AUTH)
    public Boolean getSqlAdmin()
    {
        return sqlAdmin;
    }

    @Property(viewable = false, order = 24, category = DB2Constants.CAT_AUTH)
    public Boolean getWorkLoadAdmin()
    {
        return workLoadAdmin;
    }

    @Property(viewable = false, order = 25, category = DB2Constants.CAT_AUTH)
    public Boolean getExplain()
    {
        return explain;
    }

    @Property(viewable = false, order = 26, category = DB2Constants.CAT_AUTH)
    public Boolean getCreateSecure()
    {
        return createSecure;
    }
}
