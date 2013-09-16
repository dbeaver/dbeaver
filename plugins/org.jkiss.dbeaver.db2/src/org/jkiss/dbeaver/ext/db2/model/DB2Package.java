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
package org.jkiss.dbeaver.ext.db2.model;

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
import org.jkiss.utils.CommonUtils;

import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.Collection;

/**
 * DB2 Packages
 *
 * @author Denis Forveille
 */
public class DB2Package extends DB2SchemaObject implements DBPRefreshableObject {

    private final DBSObjectCache<DB2Package, DB2PackageDep> packageDepCache;

    private Boolean valid;
    private String owner;
    private DB2OwnerType ownerType;
    private Timestamp createTime;
    private Timestamp alterTime;
    private String remarks;

    // TODO DF: Add other attributes
    // TODO DF: Add dependencies

    // -----------------------
    // Constructors
    // -----------------------

    public DB2Package(DB2Schema schema, ResultSet dbResult)
    {
        super(schema, JDBCUtils.safeGetString(dbResult, "PKGNAME"), true);

        this.valid = JDBCUtils.safeGetBoolean(dbResult, "VALID", DB2YesNo.Y.name());
        this.owner = JDBCUtils.safeGetString(dbResult, "OWNER");
        this.ownerType = CommonUtils.valueOf(DB2OwnerType.class, JDBCUtils.safeGetString(dbResult, "OWNERTYPE"));
        this.createTime = JDBCUtils.safeGetTimestamp(dbResult, "CREATE_TIME");
        this.alterTime = JDBCUtils.safeGetTimestamp(dbResult, "ALTER_TIME");
        this.remarks = JDBCUtils.safeGetString(dbResult, "REMARKS");
        packageDepCache = new JDBCObjectSimpleCache<DB2Package, DB2PackageDep>(
            DB2PackageDep.class, "SELECT * FROM SYSCAT.PACKAGEDEP WHERE PKGSCHEMA = ? AND PKGNAME = ? ORDER BY BSCHEMA,BNAME WITH UR",
            schema.getName(),
            getName());
    }


    // -----------------
    // Association
    // -----------------

    @Association
    public Collection<DB2PackageDep> getPackageDeps(DBRProgressMonitor monitor) throws DBException
    {
        return packageDepCache.getObjects(monitor, this);
    }

    @Override
    public boolean refreshObject(DBRProgressMonitor monitor) throws DBException
    {
        packageDepCache.clearCache();
        return true;
    }

    // -----------------
    // Properties
    // -----------------
    @Property(viewable = true, editable = false)
    public Boolean getValid()
    {
        return valid;
    }

    @Property(viewable = false, editable = false, category = DB2Constants.CAT_OWNER)
    public String getOwner()
    {
        return owner;
    }

    @Property(viewable = false, editable = false, category = DB2Constants.CAT_OWNER)
    public String getOwnerTypeDescription()
    {
        return ownerType.getDescription();
    }

    @Property(viewable = false, editable = false, category = DB2Constants.CAT_DATETIME)
    public Timestamp getCreateTime()
    {
        return createTime;
    }

    @Property(viewable = false, editable = false, category = DB2Constants.CAT_DATETIME)
    public Timestamp getAlterTime()
    {
        return alterTime;
    }

    @Override
    @Property(viewable = true, editable = false)
    public String getDescription()
    {
        return remarks;
    }

}
