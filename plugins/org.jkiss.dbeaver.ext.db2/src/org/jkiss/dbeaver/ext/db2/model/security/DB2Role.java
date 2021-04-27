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
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.db2.DB2Constants;
import org.jkiss.dbeaver.ext.db2.model.DB2DataSource;
import org.jkiss.dbeaver.model.DBPRefreshableObject;
import org.jkiss.dbeaver.model.DBPSaveableObject;
import org.jkiss.dbeaver.model.access.DBARole;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCObjectSimpleCache;
import org.jkiss.dbeaver.model.meta.Association;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.meta.PropertyLength;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.cache.DBSObjectCache;

import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.Collection;

/**
 * DB2 Role
 * 
 * @author Denis Forveille
 */
public class DB2Role extends DB2Grantee implements DBPSaveableObject, DBARole, DBPRefreshableObject {

    private static final String C_RL = "SELECT * FROM SYSCAT.ROLEAUTH WHERE ROLENAME=? ORDER BY GRANTEETYPE,GRANTEE WITH UR";

    private final DBSObjectCache<DB2Role, DB2RoleDep> roleDepCache;

    private Integer id;
    private Timestamp createTime;
    private Integer auditPolicyId;
    private String auditPolicyName;
    private String remarks;

    // -----------------------
    // Constructors
    // -----------------------

    public DB2Role(DB2DataSource db2DataSource, ResultSet resultSet)
    {
        super(new VoidProgressMonitor(), db2DataSource, resultSet, "ROLENAME");

        this.id = JDBCUtils.safeGetInteger(resultSet, "ROLEID");
        this.createTime = JDBCUtils.safeGetTimestamp(resultSet, "CREATE_TIME");
        this.remarks = JDBCUtils.safeGetString(resultSet, "REMARKS");
        if (db2DataSource.isAtLeastV10_1()) {
            this.auditPolicyId = JDBCUtils.safeGetInteger(resultSet, "AUDITPOLICYID");
            this.auditPolicyName = JDBCUtils.safeGetString(resultSet, "AUDITPOLICYNAME");
        }

        this.roleDepCache = new JDBCObjectSimpleCache<>(DB2RoleDep.class, C_RL, getName());
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
    public Collection<DB2RoleDep> getRoleDeps(DBRProgressMonitor monitor) throws DBException
    {
        return roleDepCache.getAllObjects(monitor, this);
    }

    @Override
    public DBSObject refreshObject(@NotNull DBRProgressMonitor monitor) throws DBException
    {
        roleDepCache.clearCache();
        return this;
    }

    // -----------------
    // Properties
    // -----------------

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

    @Nullable
    @Override
    @Property(viewable = true, length = PropertyLength.MULTILINE)
    public String getDescription()
    {
        return remarks;
    }

}
