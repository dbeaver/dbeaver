/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
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

    public boolean matches(MySQLTable table)
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
