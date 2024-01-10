/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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

package org.jkiss.dbeaver.ext.dameng.model;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.dameng.DamengConstants;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPObjectWithLongId;
import org.jkiss.dbeaver.model.DBPScriptObject;
import org.jkiss.dbeaver.model.access.DBAUser;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;

import java.sql.Timestamp;
import java.util.Map;

/**
 * @author Shengkai Bai
 */
public class DamengUser implements DBAUser, DBPScriptObject, DBPObjectWithLongId {

    private DamengDataSource dataSource;

    private long id;

    private String name;

    private Timestamp createTime;
    private Type type;

    private LockedStatus lockedStatus;

    private long tablespaceId;

    public DamengUser(DamengDataSource dataSource, JDBCResultSet dbResult) {
        this.dataSource = dataSource;
        this.id = JDBCUtils.safeGetLong(dbResult, DamengConstants.ID);
        this.name = JDBCUtils.safeGetString(dbResult, DamengConstants.NAME);
        int typeValue = JDBCUtils.safeGetInt(dbResult, DamengConstants.INFO1);
        this.type = Type.values()[typeValue];
        this.tablespaceId = JDBCUtils.safeGetInt(dbResult, "TABLESPACE_ID");
        this.createTime = JDBCUtils.safeGetTimestamp(dbResult, DamengConstants.CRTDATE);
        this.lockedStatus = JDBCUtils.safeGetInt(dbResult, "LOCKED_STATUS") == 2 ? LockedStatus.UNLOCKED : LockedStatus.LOCKED;
    }

    @Override
    @Property(viewable = true, order = 1)
    public long getObjectId() {
        return id;
    }

    @Override
    @Property(viewable = true, order = 2)
    public String getName() {
        return name;
    }

    @Override
    public String getDescription() {
        return null;
    }

    @Override
    public boolean isPersisted() {
        return true;
    }

    @Override
    public DBSObject getParentObject() {
        return getDataSource().getContainer();
    }

    @Override
    public DBPDataSource getDataSource() {
        return dataSource;
    }

    @Property(viewable = true, order = 4)
    public Type getType() {
        return type;
    }

    @Override
    public String getObjectDefinitionText(DBRProgressMonitor monitor, Map<String, Object> options) throws DBException {
        return DamengUtils.getDDL(monitor, this, DamengConstants.ObjectType.USER, null);
    }

    @Property(viewable = true, order = 3)
    public DamengTablespace getTablespace(DBRProgressMonitor monitor) throws DBException {
        return this.dataSource.getTablespaceById(monitor, tablespaceId);
    }

    @Property(viewable = true, order = 6)
    public Timestamp getCreateTime() {
        return createTime;
    }

    @Property(viewable = true, order = 5)
    public LockedStatus getLockedStatus() {
        return lockedStatus;
    }

    public enum Type {
        DBA,
        AUDITOR,
        SSO,
        DBO,
        SYS
    }

    public enum LockedStatus {
        LOCKED,
        UNLOCKED
    }
}
