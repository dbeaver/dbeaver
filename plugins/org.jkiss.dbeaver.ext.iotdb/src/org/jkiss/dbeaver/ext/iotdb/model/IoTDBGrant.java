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
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.access.DBAPrivilege;
import org.jkiss.dbeaver.model.access.DBAPrivilegeGrant;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;

import java.util.List;

public class IoTDBGrant implements DBSObject, DBAPrivilegeGrant {

    private final IoTDBAbstractUser user;
    private final List<IoTDBPrivilege> privileges;
    private String role;
    private String scope;
    private Boolean grantOption;

    public IoTDBGrant(IoTDBAbstractUser user,
                      List<IoTDBPrivilege> privileges,
                      String role,
                      String scope,
                      boolean grantOption) {
        this.user = user;
        this.privileges = privileges;
        this.role = role;
        this.scope = scope;
        this.grantOption = grantOption;
    }

    @Override
    public Object getSubject(@NotNull DBRProgressMonitor dbrProgressMonitor) throws DBException {
        return user;
    }

    @Override
    public Object getObject(@NotNull DBRProgressMonitor dbrProgressMonitor) throws DBException {
        return "testObject";
    }

    @Override
    public DBAPrivilege[] getPrivileges() {
        return privileges.toArray(new DBAPrivilege[0]);
    }

    @Override
    public boolean isGranted() {
        return grantOption;
    }

    @Override
    public DBSObject getParentObject() {
        return this.user;
    }

    @NotNull
    @Override
    public DBPDataSource getDataSource() {
        return this.user.getDataSource();
    }

    @NotNull
    @Override
    @Property(viewable = true, order = 1)
    public String getName() {
        return privileges.get(0).name;
    }

    @Nullable
    @Override
    public String getDescription() {
        return null;
    }

    @Override
    public boolean isPersisted() {
        return true;
    }

    @Nullable
    @Property(viewable = true, order = 2)
    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    @Nullable
    @Property(viewable = true, order = 3)
    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    @Property(viewable = true, editable = true, order = 4)
    public Boolean getGrantOption() {
        return grantOption;
    }

    public void setGrantOption(Boolean grantOption) {
        this.grantOption = grantOption;
    }

    public boolean matches(String db, String tb) {
        return scope.equals("*.*") || scope.equals(db + "." + tb) || scope.equals(db + "." + "*");
    }

    public boolean canHighlightDatabase(String db) {
        return scope.startsWith("*.") || scope.startsWith(db + ".");
    }

    public boolean canHighlightTable(String db, String tb) {
        return scope.startsWith("*.") || scope.equals(db + ".*") || scope.equals(db + "." + tb);
    }
}
