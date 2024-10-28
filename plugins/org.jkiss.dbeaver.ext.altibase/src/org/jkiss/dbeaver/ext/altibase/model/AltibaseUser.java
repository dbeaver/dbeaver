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
package org.jkiss.dbeaver.ext.altibase.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBDatabaseException;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.altibase.AltibaseConstants;
import org.jkiss.dbeaver.model.DBConstants;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.LazyProperty;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

public class AltibaseUser extends AltibaseGrantee {

    private Timestamp lockDate;
    private Timestamp passwordexpiryDate;
    private int passwordGraceTime;
    private Object defaultTablespace;
    private Object tempTablespace;
    private Object profile;
    private Timestamp createDate;

    private ArrayList<AltibaseTablespace> userTbsList = null;
    private boolean isSysUser = false;

    public AltibaseUser(AltibaseDataSource dataSource, JDBCResultSet resultSet) {
        super(dataSource, JDBCUtils.safeGetString(resultSet, "USER_NAME"));

        this.name = JDBCUtils.safeGetString(resultSet, "USER_NAME");
        this.lockDate = JDBCUtils.safeGetTimestamp(resultSet, "ACCOUNT_LOCK_DATE");
        this.passwordexpiryDate = JDBCUtils.safeGetTimestamp(resultSet, "PASSWORD_EXPIRY_DATE");
        this.passwordGraceTime = JDBCUtils.safeGetInt(resultSet, "PASSWORD_GRACE_TIME");
        this.defaultTablespace = JDBCUtils.safeGetString(resultSet, "DEFAULT_TBS_NAME");
        this.tempTablespace = JDBCUtils.safeGetString(resultSet, "TEMP_TBS_NAME");
        this.createDate = JDBCUtils.safeGetTimestamp(resultSet, "CREATED");

        isSysUser = AltibaseConstants.isSysUser(name);
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

    @Nullable
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

    @Nullable
    @Override
    public DBSObject refreshObject(@NotNull DBRProgressMonitor monitor) throws DBException {
        super.refreshObject(monitor);
        userTbsList = null;

        return this;
    }

    public Collection<AltibaseTablespace> getTablespaces(DBRProgressMonitor monitor) throws DBException {

        userTbsList = new ArrayList<AltibaseTablespace>();
        Map<String, AltibaseTablespace> dbTbsMap = getDataSource().getTablespaces(monitor)
                .stream().collect(Collectors.toMap(AltibaseTablespace::getName, AltibaseTablespace -> AltibaseTablespace));
        String tbsName = null;

        try (JDBCSession session = DBUtils.openMetaSession(monitor, getParentObject(), "Load tablespaces for user")) {
            try (JDBCStatement dbStat = prepareTablespaceName4UserLoadStatement(session, this)) {
                dbStat.setFetchSize(DBConstants.METADATA_FETCH_SIZE);
                dbStat.executeStatement();
                JDBCResultSet dbResult = dbStat.getResultSet();
                if (dbResult != null) {
                    try {
                        while (dbResult.next()) {
                            if (monitor.isCanceled()) {
                                break;
                            }

                            tbsName = dbResult.getString(1);
                            if (dbTbsMap.containsKey(tbsName)) {
                                userTbsList.add(dbTbsMap.get(tbsName));
                            }
                        }
                    } finally {
                        dbResult.close();
                    }  
                }
            }

        } catch (SQLException e) {
            throw new DBDatabaseException(e, getDataSource());
        }

        return userTbsList;
    }

    public JDBCStatement prepareTablespaceName4UserLoadStatement(JDBCSession session, 
            AltibaseUser user) throws SQLException {
        String qry;

        // SYS and SYSTEM_ users are able to access all tablespaces in a DBMS.
        if (isSysUser) {
            qry = "SELECT t.name FROM  v$tablespaces t ORDER BY 1";
        } else {
            qry = "SELECT t.name FROM  v$tablespaces t, system_.sys_users_ u"
                    + " WHERE u.user_name = ? AND (u.default_tbs_id = t.id OR u.temp_tbs_id  = t.id)"
                    + " UNION ALL"
                    + " SELECT t.name FROM system_.sys_tbs_users_ tu, v$tablespaces t, system_.sys_users_ u"
                    + " WHERE u.user_name = ? AND u.user_id = tu.user_id AND tu.tbs_id = t.id AND tu.is_access = 1"
                    + " ORDER BY 1 ASC";
        }

        final JDBCPreparedStatement dbStat = session.prepareStatement(qry);
        if (!isSysUser) {
            dbStat.setString(1, user.getName());
            dbStat.setString(2, user.getName());
        }

        return dbStat;
    }
}