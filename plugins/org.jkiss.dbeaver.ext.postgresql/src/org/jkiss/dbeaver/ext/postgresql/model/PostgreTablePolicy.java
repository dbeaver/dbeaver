/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2026 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.postgresql.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.*;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.IPropertyValueListProvider;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLConstants;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.utils.ArrayUtils;
import org.jkiss.utils.CommonUtils;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * An object describing row-level security policy.
 *
 * @see <a href="5.8. Row Security Policies">https://www.postgresql.org/docs/current/ddl-rowsecurity.html</a>
 */
public class PostgreTablePolicy implements DBSObject, DBPNamedObject2, DBPSaveableObject, DBPScriptObject {
    private static final Log log = Log.getLog(PostgreTablePolicy.class);

    private final PostgreTable table;
    private String name;
    private final List<PostgreRole> roles = new ArrayList<>();
    private PolicyType type;
    private PolicyEvent event;
    private String using;
    private String check;
    private boolean persisted;

    public PostgreTablePolicy(
        @NotNull DBRProgressMonitor monitor,
        @NotNull PostgreTable table,
        @NotNull ResultSet results
    ) throws DBException {
        final PostgreDatabase database = table.getDatabase();

        this.table = table;
        this.name = JDBCUtils.safeGetString(results, "policyname");

        String[] roleNames = JDBCUtils.<String[]> safeGetArray(results, "roles");
        if (!ArrayUtils.isEmpty(roleNames)) {
            for (String roleName : roleNames) {
                PostgreRole role = database.getRoleByReference(monitor, new PostgreRoleReference(database, roleName, null));
                if (role == null) {
                    log.debug("Role '" + roleName + "' not found");
                    continue;
                }
                roles.add(role);
            }
        }
        this.type = CommonUtils.valueOf(PolicyType.class, JDBCUtils.safeGetString(results, "permissive"));
        this.event = CommonUtils.valueOf(PolicyEvent.class, JDBCUtils.safeGetString(results, "cmd"));
        this.using = JDBCUtils.safeGetString(results, "qual");
        this.check = JDBCUtils.safeGetString(results, "with_check");
        this.persisted = true;
    }

    public PostgreTablePolicy(@NotNull PostgreTable table, @NotNull String name) {
        this.table = table;
        this.name = name;
        this.type = PolicyType.PERMISSIVE;
        this.event = PolicyEvent.ALL;
        this.using = "";
        this.check = "";
        this.persisted = false;
    }

    @NotNull
    @Override
    @Property(order = 1, viewable = true, editable = true)
    public String getName() {
        return name;
    }

    @Override
    public void setName(@NotNull String name) {
        this.name = name;
    }

    @Nullable
    @Override
    public String getDescription() {
        return null;
    }

    @NotNull
    @Property(order = 2, viewable = true, editable = true, listProvider = RoleListProvider.class)
    public List<PostgreRole> getRoles() {
        return roles;
    }

    public void setRoles(@Nullable List<PostgreRole> roles) {
        this.roles.clear();
        if (roles != null) {
            this.roles.addAll(roles);
        }
    }

    @NotNull
    @Property(order = 3, viewable = true, editable = true)
    public PolicyType getType() {
        return type;
    }

    public void setType(@NotNull PolicyType type) {
        this.type = type;
    }

    @NotNull
    @Property(order = 4, viewable = true, editable = true)
    public PolicyEvent getEvent() {
        return event;
    }

    public void setEvent(@NotNull PolicyEvent event) {
        this.event = event;
    }

    @NotNull
    @Property(order = 5, viewable = true, editable = true, updatable = true)
    public String getUsing() {
        return using;
    }

    public void setUsing(@NotNull String using) {
        this.using = using;
    }

    @NotNull
    @Property(order = 6, viewable = true, editable = true, updatable = true)
    public String getCheck() {
        return check;
    }

    public void setCheck(@NotNull String check) {
        this.check = check;
    }

    @Override
    public boolean isPersisted() {
        return persisted;
    }

    @Override
    public void setPersisted(boolean persisted) {
        this.persisted = persisted;
    }

    @NotNull
    @Override
    public PostgreTable getParentObject() {
        return table;
    }

    @NotNull
    @Override
    public DBPDataSource getDataSource() {
        return table.getDataSource();
    }

    @NotNull
    @Override
    public String getObjectDefinitionText(@NotNull DBRProgressMonitor monitor, @NotNull Map<String, Object> options) throws DBException {
        StringBuilder sql = new StringBuilder();
        sql.append("CREATE POLICY ")
            .append(DBUtils.getQuotedIdentifier(this))
            .append(" ON ")
            .append(table.getFullyQualifiedName(DBPEvaluationContext.DDL))
            .append("\n AS ").append(type)
            .append("\n FOR ").append(event);

        if (!roles.isEmpty()) {
            sql.append("\n TO ");
            sql.append(roles.stream().map(DBUtils::getQuotedIdentifier).collect(Collectors.joining(",")));
        }

        if (CommonUtils.isNotEmpty(using)) {
            sql.append("\n USING (").append(using).append(")");
        }

        if (CommonUtils.isNotEmpty(check)) {
            sql.append("\n WITH CHECK (").append(check).append(")");
        }

        return sql.toString();
    }

    public enum PolicyType implements DBPNamedObject {
        PERMISSIVE("Permissive"),
        RESTRICTIVE("Restrictive");

        private final String name;

        PolicyType(@NotNull String name) {
            this.name = name;
        }


        @NotNull
        @Override
        public String getName() {
            return name;
        }
    }

    public enum PolicyEvent implements DBPNamedObject {
        ALL("All"),
        SELECT(SQLConstants.KEYWORD_SELECT),
        INSERT(SQLConstants.KEYWORD_INSERT),
        UPDATE(SQLConstants.KEYWORD_UPDATE),
        DELETE(SQLConstants.KEYWORD_DELETE);

        private final String name;

        PolicyEvent(@NotNull String name) {
            this.name = name;
        }


        @NotNull
        @Override
        public String getName() {
            return name;
        }
    }

    public static class RoleListProvider implements IPropertyValueListProvider<PostgreTablePolicy> {
        @Override
        public boolean allowCustomValue() {
            return false;
        }

        @Nullable
        @Override
        public Object[] getPossibleValues(PostgreTablePolicy object) {
            try {
                final List<PostgreRole> roles = new ArrayList<>();
                roles.add(null); // PUBLIC
                roles.addAll(object.table.getDatabase().getUsers(new VoidProgressMonitor()));
                return roles.toArray();
            } catch (DBException e) {
                log.error("Error reading roles", e);
                return new Object[0];
            }
        }
    }
}
