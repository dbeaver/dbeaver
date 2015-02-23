/*
 * Copyright (C) 2010-2015 Serge Rieder
 * serge@jkiss.org
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package org.jkiss.dbeaver.ext.mysql.model;

import java.util.List;

/**
 * User privilege grant
 */
public class MySQLGrant {

    public static final java.util.regex.Pattern GRANT_PATTERN = java.util.regex.Pattern.compile("GRANT\\s+(.+)\\sON\\s`?([^`]+)`?\\.`?([^`]+)`?\\sTO\\s");

    private MySQLUser user;
    private List<MySQLPrivilege> privileges;
    private String catalogName;
    private String tableName;
    private boolean allPrivileges;
    private boolean grantOption;

    public MySQLGrant(MySQLUser user, List<MySQLPrivilege> privileges, String catalogName, String tableName, boolean allPrivileges, boolean grantOption)
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

    public String getCatalog()
    {
        return catalogName;
    }

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
