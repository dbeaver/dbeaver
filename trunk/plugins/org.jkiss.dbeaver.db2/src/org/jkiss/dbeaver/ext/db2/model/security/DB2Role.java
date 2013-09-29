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
import org.jkiss.dbeaver.model.DBPRefreshableObject;
import org.jkiss.dbeaver.model.DBPSaveableObject;
import org.jkiss.dbeaver.model.access.DBARole;
import org.jkiss.dbeaver.model.impl.DBSObjectCache;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCObjectSimpleCache;
import org.jkiss.dbeaver.model.meta.Association;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.runtime.VoidProgressMonitor;

import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.Collection;

/**
 * DB2 Role
 * 
 * @author Denis Forveille
 */
public class DB2Role extends DB2Grantee implements DBPSaveableObject, DBARole, DBPRefreshableObject {

    private static final String C_RL = "SELECT * FROM SYSCAT.ROLEAUTH WHERE ROLENAME=? ORDER BY GRANTOR,GRANTEE WITH UR";

    private final DBSObjectCache<DB2Role, DB2RoleAuth> roleAuthCache;

    private String name;
    private Integer id;
    private Timestamp createTime;
    private Integer auditPolicyId;
    private String auditPolicyName;
    private String remarks;

    // -----------------------
    // Constructors
    // -----------------------

    public DB2Role(DB2DataSource dataSource, ResultSet resultSet)
    {
        super(VoidProgressMonitor.INSTANCE, dataSource, resultSet);

        this.name = JDBCUtils.safeGetString(resultSet, "ROLENAME");
        this.id = JDBCUtils.safeGetInteger(resultSet, "ROLEID");
        this.createTime = JDBCUtils.safeGetTimestamp(resultSet, "CREATE_TIME");
        // DB2 v10 this.auditPolicyId = JDBCUtils.safeGetInteger(resultSet, "AUDITPOLICYID");
        // DB2 v10 this.auditPolicyName = JDBCUtils.safeGetString(resultSet, "AUDITPOLICYNAME");
        this.remarks = JDBCUtils.safeGetString(resultSet, "REMARKS");
        this.roleAuthCache = new JDBCObjectSimpleCache<DB2Role, DB2RoleAuth>(DB2RoleAuth.class, C_RL, name);
    }

    // -----------------------
    // Business Contract
    // -----------------------

    @Override
    public DB2AuthIDType getType()
    {
        return DB2AuthIDType.R;
    }

    // -----------------
    // Associations
    // -----------------

    @Association
    public Collection<DB2RoleAuth> getRoleAuths(DBRProgressMonitor monitor) throws DBException
    {
        return roleAuthCache.getObjects(monitor, this);
    }

    @Override
    public boolean refreshObject(DBRProgressMonitor monitor) throws DBException
    {
        roleAuthCache.clearCache();
        return true;
    }

    // -----------------
    // Properties
    // -----------------

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

    @Property(viewable = false, category = DB2Constants.CAT_DATETIME)
    public Timestamp getCreateTime()
    {
        return createTime;
    }

    @Property(viewable = false, category = DB2Constants.CAT_AUDIT)
    public Integer getAuditPolicyId()
    {
        return auditPolicyId;
    }

    @Property(viewable = false, category = DB2Constants.CAT_AUDIT)
    public String getAuditPolicyName()
    {
        return auditPolicyName;
    }

    @Override
    @Property(viewable = true)
    public String getDescription()
    {
        return remarks;
    }

}
