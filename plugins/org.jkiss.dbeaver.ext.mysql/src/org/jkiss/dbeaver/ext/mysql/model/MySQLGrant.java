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
import org.jkiss.dbeaver.model.struct.rdb.DBSProcedureType;
import org.jkiss.utils.CommonUtils;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * User privilege grant
 */
public class MySQLGrant implements DBSObject, DBAPrivilegeGrant {

    public static final Pattern TABLE_GRANT_PATTERN = Pattern.compile(
        "GRANT\\s+(.+)\\s+ON\\s+`?([^`]+)`?\\.`?([^`]+)`?\\s+TO\\s+", Pattern.CASE_INSENSITIVE);
    public static final Pattern PROCEDURE_GRANT_PATTERN = Pattern.compile(
        "GRANT\\s+(.+)\\s+ON\\s+(PROCEDURE|FUNCTION)\\s+`?([^`]+)`?\\.`?([^`]+)`?\\s+TO\\s+", Pattern.CASE_INSENSITIVE);
    public static final Pattern GLOBAL_GRANT_PATTERN = Pattern.compile("GRANT\\s+(.+)\\s+ON\\s+(.+)\\s+TO\\s+", Pattern.CASE_INSENSITIVE);

    public enum ObjectType {
        TABLE,
        PROCEDURE,
        FUNCTION
    }

    private final MySQLUser user;
    private final List<MySQLPrivilege> privileges;
    @Nullable
    private final String catalogName;
    @Nullable
    private final String tableName;
    private final boolean allPrivileges;
    private boolean grantOption;
    @NotNull
    private final ObjectType objectType;
    // Column-level privileges: privilege -> column names (original case, matched case-insensitively),
    // e.g. GRANT SELECT (col1, col2) ON db.tbl
    private final Map<MySQLPrivilege, Set<String>> columnPrivileges = new LinkedHashMap<>();

    public MySQLGrant(
        @NotNull MySQLUser user,
        @NotNull List<MySQLPrivilege> privileges,
        @Nullable String catalogName,
        @Nullable String tableName,
        boolean allPrivileges,
        boolean grantOption
    ) {
        this(user, privileges, catalogName, tableName, allPrivileges, grantOption, ObjectType.TABLE);
    }

    public MySQLGrant(
        @NotNull MySQLUser user,
        @NotNull List<MySQLPrivilege> privileges,
        @Nullable String catalogName,
        @Nullable String tableName,
        boolean allPrivileges,
        boolean grantOption,
        @NotNull ObjectType objectType
    ) {
        this.user = user;
        this.privileges = privileges;
        this.catalogName = catalogName;
        this.tableName = tableName;
        this.allPrivileges = allPrivileges;
        this.grantOption = grantOption;
        this.objectType = objectType;
    }

    @NotNull
    public ObjectType getObjectType() {
        return objectType;
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

    public MySQLUser getSubject(@NotNull DBRProgressMonitor monitor) {
        return user;
    }

    @Override
    public Object getObject(@NotNull DBRProgressMonitor monitor) throws DBException {
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

    public boolean isEmpty() {
        return privileges.isEmpty() && columnPrivileges.isEmpty() && !isAllPrivileges() && !isGrantOption();
    }

    /**
     * Adds a column to the column list of the given privilege (GRANT priv (column) ON catalog.table).
     * The original case of the column name is preserved: the server matches column names
     * case-sensitively when revoking column privileges.
     */
    public void addColumnPrivilege(@NotNull MySQLPrivilege privilege, @NotNull String columnName) {
        Set<String> columns = columnPrivileges.computeIfAbsent(privilege, p -> new LinkedHashSet<>());
        if (findColumn(columns, columnName) == null) {
            columns.add(columnName);
        }
    }

    public void removeColumnPrivilege(@NotNull MySQLPrivilege privilege, @NotNull String columnName) {
        Set<String> columns = columnPrivileges.get(privilege);
        if (columns != null) {
            String existing = findColumn(columns, columnName);
            if (existing != null) {
                columns.remove(existing);
            }
            if (columns.isEmpty()) {
                columnPrivileges.remove(privilege);
            }
        }
    }

    public boolean hasColumnPrivilege(@NotNull MySQLPrivilege privilege, @NotNull String columnName) {
        Set<String> columns = columnPrivileges.get(privilege);
        return columns != null && findColumn(columns, columnName) != null;
    }

    @NotNull
    public Map<MySQLPrivilege, Set<String>> getColumnPrivileges() {
        return columnPrivileges;
    }

    /**
     * Returns true if any privilege of this grant is restricted to the given column.
     */
    public boolean hasColumnPrivileges(@NotNull String columnName) {
        for (Set<String> columns : columnPrivileges.values()) {
            if (findColumn(columns, columnName) != null) {
                return true;
            }
        }
        return false;
    }

    /** Case-insensitive lookup that returns the stored column name (with its original case). */
    @Nullable
    private static String findColumn(@NotNull Set<String> columns, @NotNull String columnName) {
        for (String column : columns) {
            if (column.equalsIgnoreCase(columnName)) {
                return column;
            }
        }
        return null;
    }

    /**
     * Returns true if the given catalog exists and it is comparable to this grant catalog
     * or the given catalog is empty, but the grant applies to all catalogs.
     */
    public boolean matches(@Nullable MySQLCatalog catalog) {
        return (catalog == null && isAllCatalogs())
            || (catalog != null && CommonUtils.isNotEmpty(catalogName) && !isAllCatalogs()
            && SQLUtils.matchesLike(catalog.getName(), catalogName));
    }

    public boolean matches(@Nullable MySQLTableBase table) {
        return objectType == ObjectType.TABLE
            && ((table == null && isAllTables()) || (table != null && table.getName().equalsIgnoreCase(tableName)));
    }

    /**
     * Returns true if this is a routine grant and the given procedure/function is its target.
     */
    public boolean matchesProcedure(@Nullable MySQLProcedure procedure) {
        if (procedure == null || objectType == ObjectType.TABLE) {
            return false;
        }
        ObjectType procedureType = procedure.getProcedureType() == DBSProcedureType.FUNCTION ? ObjectType.FUNCTION : ObjectType.PROCEDURE;
        return objectType == procedureType && procedure.getName().equalsIgnoreCase(tableName);
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
