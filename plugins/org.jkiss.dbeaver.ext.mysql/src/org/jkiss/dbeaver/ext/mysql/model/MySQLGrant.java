/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2016 Serge Rieder (serge@jkiss.org)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (version 2)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
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
