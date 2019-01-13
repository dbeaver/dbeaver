/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2019 Serge Rider (serge@jkiss.org)
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

import org.jkiss.code.Nullable;

import java.util.List;
import java.util.regex.Pattern;

/**
 * User privilege grant
 */
public class MySQLGrant {

    public static final Pattern TABLE_GRANT_PATTERN = Pattern.compile("GRANT\\s+(.+)\\s+ON\\s+`?([^`]+)`?\\.`?([^`]+)`?\\s+TO\\s+");
    public static final Pattern GLOBAL_GRANT_PATTERN = Pattern.compile("GRANT\\s+(.+)\\s+ON\\s+(.+)\\s+TO\\s+");

    private MySQLUser user;
    private List<MySQLPrivilege> privileges;
    @Nullable
    private String catalogName;
    @Nullable
    private String tableName;
    private boolean allPrivileges;
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

    public MySQLUser getUser()
    {
        return user;
    }

    public List<MySQLPrivilege> getPrivileges()
    {
        return privileges;
    }

    public boolean isAllCatalogs()
    {
        return "*".equals(catalogName);
    }

    @Nullable
    public String getCatalog()
    {
        return catalogName;
    }

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
        return (catalog == null && isAllCatalogs()) || (catalog != null && catalog.getName().equalsIgnoreCase(catalogName));
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

}
