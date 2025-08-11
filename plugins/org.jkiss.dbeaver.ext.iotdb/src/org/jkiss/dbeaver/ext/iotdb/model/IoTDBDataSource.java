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

package org.jkiss.dbeaver.ext.iotdb.model;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBDatabaseException;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.generic.model.GenericDataSource;
import org.jkiss.dbeaver.ext.generic.model.GenericSQLDialect;
import org.jkiss.dbeaver.ext.generic.model.meta.GenericMetaModel;
import org.jkiss.dbeaver.ext.iotdb.IoTDBPrivilegeInfo;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;

import java.util.ArrayList;
import java.util.List;

public class IoTDBDataSource extends GenericDataSource {

    private static final Log log = Log.getLog(IoTDBDataSource.class);

    private List<IoTDBAbstractUser> users;
    private final boolean isTree;
    private List<IoTDBPrivilege> privileges;    // All global and schema privileges

    public IoTDBDataSource(DBRProgressMonitor monitor,
                           @NotNull DBPDataSourceContainer container,
                           @NotNull GenericMetaModel metaModel,
                           boolean tree) throws DBException {
        super(monitor, container, metaModel, new GenericSQLDialect());
        this.isTree = tree;
    }

    /**
     * Get the list of users
     *
     * @param monitor progress monitor
     *
     * @return List of IoTDBUser
     *
     * @throws DBException if an error occurs
     */
    public List<IoTDBAbstractUser> getUsers(DBRProgressMonitor monitor) throws DBException {
        if (users == null) {
            users = loadUsers(monitor);
        }
        return users;
    }

    /**
     * Load users from the database
     *
     * @param monitor progress monitor
     *
     * @return List of IoTDBUser
     *
     * @throws DBException if an error occurs
     */
    private List<IoTDBAbstractUser> loadUsers(DBRProgressMonitor monitor) throws DBException {

        List<IoTDBAbstractUser> userList = new ArrayList<>();
        String currentUserName = null;
        boolean hasManageUserPrivilege = false;

        try (JDBCSession session = DBUtils.openMetaSession(monitor, this, "Show Current User & Check Privileges")) {
            String sql = "show current_user";
            JDBCStatement stmt = session.createStatement();
            JDBCResultSet rs = stmt.executeQuery(sql);
            while (rs.next()) {
                currentUserName = rs.getString("CurrentUser");
                IoTDBAbstractUser user = isTree ? new IoTDBUser(this, currentUserName) :
                        new IoTDBRelationalUser(this, currentUserName, monitor);
                userList.add(user);
            }

            sql = "list privileges of user " + currentUserName;
            stmt = session.createStatement();
            rs = stmt.executeQuery(sql);
            while (rs.next()) {
                if (rs.getString("Privileges").equals("MANAGE_USER")) {
                    hasManageUserPrivilege = true;
                    break;
                }
            }
        } catch (Exception e) {
            log.error("Error showing current user or checking privileges", e);
            throw new DBDatabaseException(e, this);
        }

        if (!hasManageUserPrivilege) {
            return userList;
        }

        try (JDBCSession session = DBUtils.openMetaSession(monitor, this, "Load Users")) {
            String sql = "list user";
            JDBCStatement stmt = session.createStatement();
            JDBCResultSet rs = stmt.executeQuery(sql);
            while (rs.next()) {
                String tmpUserName = rs.getString("User");
                if (tmpUserName.equals(currentUserName)) {
                    continue;
                }
                IoTDBAbstractUser user = isTree ? new IoTDBUser(this, tmpUserName) :
                        new IoTDBRelationalUser(this, tmpUserName, monitor);
                userList.add(user);
            }
            return userList;
        } catch (Exception e) {
            log.error("Error loading users", e);
            throw new DBDatabaseException(e, this);
        }
    }

    @Override
    public DBSObject refreshObject(@NotNull DBRProgressMonitor monitor) throws DBException {
        super.refreshObject(monitor);
        this.users = loadUsers(monitor);
        return this;
    }

    /**
     * Get the list of privileges
     *
     * @return List of IoTDBPrivilege according to tree or table model
     */
    public List<IoTDBPrivilege> getAllPrivileges() {
        if (privileges == null) {
            generatePrivilegesList();
        }
        return privileges;
    }

    /**
     * Get the list of privileges by kind
     *
     * @param isGlobal true if global, false if schema
     *
     * @return List of IoTDBPrivilege
     */
    public List<IoTDBPrivilege> getPrivilegesByKind(boolean isGlobal) {
        List<IoTDBPrivilege> privs = new ArrayList<>();
        IoTDBPrivilegeInfo.Kind k = isGlobal ? IoTDBPrivilegeInfo.Kind.GLOBAL :
                (isTree ? IoTDBPrivilegeInfo.Kind.SERIES : IoTDBPrivilegeInfo.Kind.DATABASE);
        for (IoTDBPrivilege priv : getAllPrivileges()) {
            if (priv.getKind() == k) {
                privs.add(priv);
            }
        }
        return privs;
    }

    /**
     * Generate the list of privileges according to tree or table model
     */
    public void generatePrivilegesList() {
        String[] globalPrivileges = isTree ? IoTDBPrivilegeInfo.treeGlobalPrivileges : IoTDBPrivilegeInfo.tableGlobalPrivileges;
        String[] dbPrivileges = isTree ? IoTDBPrivilegeInfo.treeSeriesPrivileges : IoTDBPrivilegeInfo.tableDatabasePrivileges;

        List<IoTDBPrivilege> newPrivileges = new ArrayList<>();
        for (String privilege : globalPrivileges) {
            newPrivileges.add(new IoTDBPrivilege(this, privilege, IoTDBPrivilegeInfo.Kind.GLOBAL));
        }

        IoTDBPrivilegeInfo.Kind kind = isTree ? IoTDBPrivilegeInfo.Kind.SERIES : IoTDBPrivilegeInfo.Kind.DATABASE;
        for (String privilege : dbPrivileges) {
            newPrivileges.add(new IoTDBPrivilege(this, privilege, kind));
        }

        this.privileges = newPrivileges;
    }

    /**
     * Get the privilege by name
     *
     * @param monitor progress monitor
     * @param name name of the privilege
     *
     * @return IoTDBPrivilege
     *
     * @throws DBException if an error occurs
     */
    public IoTDBPrivilege getPrivilege(DBRProgressMonitor monitor,
                                       String name) throws DBException {
        return DBUtils.findObject(getAllPrivileges(), name, true);
    }

    public boolean isTree() {
        return isTree;
    }

    public boolean isTable() {
        return !isTree;
    }
}
