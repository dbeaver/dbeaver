/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2023 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.altibase.model;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.LazyProperty;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

import java.sql.Timestamp;

public class AltibaseUser extends AltibaseGrantee {

    private Timestamp lockDate;
    private Timestamp passwordexpiryDate;
    private int passwordGraceTime;
    private Object defaultTablespace;
    private Object tempTablespace;
    private Object profile;
    private Timestamp createDate;
        
    public AltibaseUser(AltibaseDataSource dataSource, JDBCResultSet resultSet) {
        super(dataSource, JDBCUtils.safeGetString(resultSet, "USER_NAME"));
        
        this.name = JDBCUtils.safeGetString(resultSet, "USER_NAME");
        this.lockDate = JDBCUtils.safeGetTimestamp(resultSet, "ACCOUNT_LOCK_DATE");
        this.passwordexpiryDate = JDBCUtils.safeGetTimestamp(resultSet, "PASSWORD_EXPIRY_DATE");
        this.passwordGraceTime = JDBCUtils.safeGetInt(resultSet, "PASSWORD_GRACE_TIME");
        this.defaultTablespace = JDBCUtils.safeGetString(resultSet, "DEFAULT_TBS_NAME");
        this.tempTablespace = JDBCUtils.safeGetString(resultSet, "TEMP_TBS_NAME");
        this.createDate = JDBCUtils.safeGetTimestamp(resultSet, "CREATED");
    }
    
    @NotNull
    @Override
    @Property(viewable = true, order = 2)
    public String getName() {
        return name;
    }
    
    @Property(viewable = true, order = 5)
    @LazyProperty(cacheValidator = AltibaseTablespace.TablespaceReferenceValidator.class)
    public Object getDefaultTablespace(DBRProgressMonitor monitor) throws DBException {
        return AltibaseTablespace.resolveTablespaceReference(monitor, this, "defaultTablespace");
    }

    @Property(viewable = true, order = 6)
    @LazyProperty(cacheValidator = AltibaseTablespace.TablespaceReferenceValidator.class)
    public Object getTempTablespace(DBRProgressMonitor monitor) throws DBException {
        return AltibaseTablespace.resolveTablespaceReference(monitor, this, "tempTablespace");
    }
    
    @Property(viewable = true, order = 9)
    public Timestamp getLockDate() {
        return lockDate;
    }
    
    @Property(viewable = true, order = 10)
    public Timestamp getPasswordexpiryDate() {
        return passwordexpiryDate;
    }
    
    @Property(viewable = true, order = 11)
    public int getPasswordGraceTime() {
        return passwordGraceTime;
    }
    
    @Property(viewable = true, order = 15)
    public Timestamp getCreateDate() {
        return createDate;
    }
    
    @Override
    public Object getLazyReference(Object propertyId) {
        if ("defaultTablespace".equals(propertyId)) {
            return defaultTablespace;
        } else if ("tempTablespace".equals(propertyId)) {
            return tempTablespace;
        } else if ("profile".equals(propertyId)) {
            return profile;
        } else {
            return null;
        }
    }
}