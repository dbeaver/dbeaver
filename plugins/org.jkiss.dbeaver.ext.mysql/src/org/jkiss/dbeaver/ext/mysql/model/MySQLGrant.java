/*
 * DBeaver - Universal Database Manager
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

package org.jkiss.dbeaver.ext.mysql.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.access.DBAPrivilegeGrant;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLUtils;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.utils.CommonUtils;

import java.util.List;
import java.util.regex.Pattern;

/**
 * User privilege grant
 */
public class MySQLGrant implements DBSObject, DBAPrivilegeGrant {

    public static final Pattern TABLE_GRANT_PATTERN = Pattern.compile("GRANT\\s+(.+)\\s+ON\\s+`?([^`]+)`?\\.`?([^`]+)`?\\s+TO\\s+");
    public static final Pattern GLOBAL_GRANT_PATTERN = Pattern.compile("GRANT\\s+(.+)\\s+ON\\s+(.+)\\s+TO\\s+");

    private final MySQLUser user;
    private final List<MySQLPrivilege> privileges;
    @Nullable
    private final String catalogName;
    @Nullable
    private final String tableName;
    private final boolean allPrivileges;
    private boolean grantOption;

    public MySQLGrant(MySQLUser user, List<MySQLPrivilege> privileges, @Nullable String catalogName, @Nullable String tableName, boolean allPrivileges, boolean grantOption)
    {
        this.user = user;
        this.privileges = privileges;
        this.catalogName = catalogName;
        this.tableName = tableName;
        this.allPrivileges = allPrivileges;
        this.grantOption = grantOption;
    }

    @Nullable
    @Override
    public MySQLUser getParentObject() {
        return this.user;
    }

    @NotNull
    @Override
    public DBPDataSource getDataSource() {
        return this.user.getDataSource();
    }

    @NotNull
    @Override
    public String getName() {
        return allPrivileges ? "ALL PRIVILEGES" : privileges.toString();
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

    public MySQLUser getSubject(DBRProgressMonitor monitor) {
        return user;
    }

    @Override
    public Object getObject(DBRProgressMonitor monitor) throws DBException {
        if (catalogName != null) {
            if (!isAllCatalogs()) {
                MySQLCatalog catalog = user.getDataSource().getCatalog(catalogName);
                if (catalog != null) {
                    if (!isAllTables()) {
                        MySQLTable table = catalog.getTable(monitor, tableName);
                        if (table != null) {
                            return table;
                        }
                    }
                }
            }
        }
        return catalogName + "." + tableName;
    }

    public MySQLPrivilege[] getPrivileges()
    {
        return privileges.toArray(new MySQLPrivilege[0]);
    }

    @Property(viewable = true, order = 1)
    public String getPrivilegeNames() {
        return allPrivileges ? "ALL PRIVILEGES" : privileges.toString();
    }

    @Override
    public boolean isGranted() {
        return true;
    }

    public boolean isAllCatalogs()
    {
        return "*".equals(catalogName);
    }

    @Property(viewable = true, order = 10)
    @Nullable
    public String getCatalog()
    {
        return catalogName;
    }

    @Property(viewable = true, order = 11)
    @Nullable
    public String getTable()
    {
        return tableName;
    }

    public boolean isAllTables()
    {
        return "*".equals(tableName);
    }

    public boolean isAllPrivileges()
    {
        return allPrivileges;
    }

    public void addPrivilege(MySQLPrivilege privilege)
    {
        privileges.add(privilege);
    }

    public void removePrivilege(MySQLPrivilege privilege)
    {
        privileges.remove(privilege);
    }

    public boolean isGrantOption()
    {
        return grantOption;
    }

    public void setGrantOption(boolean grantOption)
    {
        this.grantOption = grantOption;
    }

    public boolean isEmpty()
    {
        return privileges.isEmpty() && !isAllPrivileges() && !isGrantOption();
    }

    public boolean matches(MySQLCatalog catalog)
    {
        return (catalog == null && isAllCatalogs()) || (catalog != null && !isAllCatalogs() && SQLUtils.matchesLike(catalog.getName(), catalogName));
    }

    public boolean matches(MySQLTableBase table)
    {
        return (table == null && isAllTables()) || (table != null && table.getName().equalsIgnoreCase(tableName));
    }

    public boolean hasNonAdminPrivileges()
    {
        for (MySQLPrivilege priv : privileges) {
            if (priv.getKind() != MySQLPrivilege.Kind.ADMIN) {
                return true;
            }
        }
        return false;
    }

    public boolean isStatic() {
        return CommonUtils.isEmpty(catalogName) || "*".equals(catalogName);
    }
}
