/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2025 DBeaver Corp and others
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
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBDatabaseException;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.iotdb.IoTDBPrivilegeInfo;
import org.jkiss.dbeaver.model.*;
import org.jkiss.dbeaver.model.access.DBARole;
import org.jkiss.dbeaver.model.access.DBAUser;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;

import java.util.ArrayList;
import java.util.List;

public class IoTDBAbstractUser implements DBAUser, DBARole, DBPRefreshableObject, DBPSaveableObject, DBPQualifiedObject {

    private static final Log log = Log.getLog(IoTDBAbstractUser.class);

    protected IoTDBDataSource dataSource;
    public String userName;
    protected boolean persisted;
    protected List<IoTDBGrant> globalPrivileges;
    protected List<IoTDBGrant> schemaPrivileges;

    public IoTDBAbstractUser(IoTDBDataSource dataSource,
                             String userName)  {
        this.dataSource = dataSource;
        this.persisted = true;
        this.userName = userName;
    }

    @Override
    public DBSObject getParentObject() {
        return dataSource.getContainer();
    }

    @Override
    public IoTDBDataSource getDataSource() {
        return dataSource;
    }

    @NotNull
    @Override
    @Property(viewable = true, order = 1)
    public String getName() {
        return userName;
    }

    @NotNull
    @Override
    public String getFullyQualifiedName(@NotNull DBPEvaluationContext dbpEvaluationContext) {
        return "";
    }

    @Nullable
    @Override
    public String getDescription() {
        return null;
    }

    @Override
    public boolean isPersisted() {
        return persisted;
    }

    @Override
    public void setPersisted(boolean b) {
        this.persisted = b;
        DBUtils.fireObjectUpdate(this);
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getUserName() {
        return userName;
    }

    /**
     * Load grants from the database
     *
     * @param monitor progress monitor
     *
     * @throws DBException if an error occurs
     */
    public void loadGrants(DBRProgressMonitor monitor) throws DBException {
        try (JDBCSession session = DBUtils.openMetaSession(monitor, this, "Load Grants")) {
            String sql = String.format("list privileges of user %s", userName);

            JDBCStatement stmt = session.createStatement();
            JDBCResultSet rs = stmt.executeQuery(sql);
            List<IoTDBGrant> globalPri = new ArrayList<>();
            List<IoTDBGrant> schemaPri = new ArrayList<>();

            while (rs.next()) {
                List<IoTDBPrivilege> privileges = new ArrayList<>();

                String privilegeName = rs.getString("Privileges");
                IoTDBPrivilege pri = dataSource.getPrivilege(monitor, privilegeName);
                privileges.add(pri);
                String role = rs.getString("Role");
                String scope = rs.getString("Scope");
                boolean grantOption = rs.getBoolean("GrantOption");

                if (pri.kind == IoTDBPrivilegeInfo.Kind.GLOBAL) {
                    globalPri.add(new IoTDBGrant(this, privileges, role, scope, grantOption));
                } else {
                    schemaPri.add(new IoTDBGrant(this, privileges, role, scope, grantOption));
                }
            }
            this.globalPrivileges = globalPri;
            this.schemaPrivileges = schemaPri;
        } catch (Exception e) {
            log.error("Error loading grants", e);
            throw new DBDatabaseException(e, this.getDataSource());
        }
    }

    public List<IoTDBGrant> getGrants(DBRProgressMonitor monitor) throws DBException {
        List<IoTDBGrant> grants = new ArrayList<>();
        if (this.globalPrivileges == null) {
            loadGrants(monitor);
        }
        grants.addAll(getGlobalPrivileges(monitor));
        grants.addAll(getSchemaPrivileges(monitor));
        return grants;
    }

    public List<IoTDBGrant> getGlobalPrivileges(DBRProgressMonitor monitor) throws DBException {
        if (this.globalPrivileges != null) {
            return this.globalPrivileges;
        }
        if (!isPersisted()) {
            this.globalPrivileges = new ArrayList<>();
            return this.globalPrivileges;
        }
        loadGrants(monitor);
        return this.globalPrivileges;
    }

    public List<IoTDBGrant> getSchemaPrivileges(DBRProgressMonitor monitor) throws DBException {
        if (this.schemaPrivileges != null) {
            return this.schemaPrivileges;
        }
        if (!isPersisted()) {
            this.schemaPrivileges = new ArrayList<>();
            return this.schemaPrivileges;
        }
        loadGrants(monitor);
        return this.schemaPrivileges;
    }

    @Override
    public DBSObject refreshObject(@NotNull DBRProgressMonitor dbrProgressMonitor) throws DBException {
        this.globalPrivileges = null;
        this.schemaPrivileges = null;
        loadGrants(dbrProgressMonitor);
        return this;
    }
}
