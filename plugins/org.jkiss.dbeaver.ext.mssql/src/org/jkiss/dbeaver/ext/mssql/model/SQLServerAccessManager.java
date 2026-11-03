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
package org.jkiss.dbeaver.ext.mssql.model;

import org.eclipse.osgi.util.NLS;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.mssql.SQLServerConstants;
import org.jkiss.dbeaver.ext.mssql.SQLServerMessages;
import org.jkiss.dbeaver.ext.mssql.SQLServerUtils;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.access.DBAAccessManager;
import org.jkiss.dbeaver.model.access.DBAAccessState;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistAction;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLUtils;
import org.jkiss.utils.CommonUtils;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;

public class SQLServerAccessManager implements DBAAccessManager {
    private static final Set<String> COLUMN_PERMISSIONS = Set.of("SELECT", "UPDATE", "REFERENCES", "UNMASK");
    private static final Set<String> COMMON_OBJECT_PERMISSIONS = Set.of("ALTER", "CONTROL", "TAKE OWNERSHIP", "VIEW DEFINITION");

    private record PermissionChange(@Nullable DBAAccessState.Permission before, @NotNull DBAAccessState.Permission after) {
        private boolean isChanged() {
            return before == null || before.state() != after.state();
        }
    }

    private final SQLServerLogin login;

    public SQLServerAccessManager(@NotNull SQLServerLogin login) {
        this.login = login;
    }

    @NotNull
    @Override
    public List<DBAAccessState.Scope> getScopes(@NotNull DBRProgressMonitor monitor) throws DBException {
        List<DBAAccessState.Scope> scopes = new ArrayList<>();
        if (!isAzure()) {
            scopes.add(new DBAAccessState.Scope("", SQLServerMessages.security_scope_server));
        }
        if (login.getLoginType() == SQLServerLogin.LoginType.R) {
            return scopes;
        }
        try (JDBCSession session = DBUtils.openMetaSession(monitor, login, "Read access scopes")) {
            read(session, isAzure() ? "SELECT DB_NAME() AS name"
                : "SELECT name FROM sys.databases WHERE state = 0 AND HAS_DBACCESS(name) = 1 ORDER BY name",
                rs -> {
                    String name = JDBCUtils.safeGetString(rs, "name");
                    if (name != null) {
                        scopes.add(new DBAAccessState.Scope(name, name));
                    }
                });
        } catch (SQLException e) {
            throw new DBException("Error reading access scopes", e);
        }
        return scopes;
    }

    @NotNull
    @Override
    public DBAAccessState load(@NotNull DBRProgressMonitor monitor, @NotNull DBAAccessState.Scope scope) throws DBException {
        boolean server = scope.id().isEmpty();
        String system = systemSchema(scope);
        try (JDBCSession session = DBUtils.openMetaSession(monitor, login, "Read principal access")) {
            DBAAccessState.Mapping mapping = server ? null : readMapping(session, system);
            boolean editable = mapping == null ? !login.isFixedRole() : mapping.editable();
            String principalName = mapping == null ? login.getName() : mapping.name();
            List<DBAAccessState.Role> roles = new ArrayList<>();
            String principals = system + (server ? "server_principals" : "database_principals");
            String members = system + (server ? "server_role_members" : "database_role_members");
            read(session, "SELECT roles.name, members.member_principal_id FROM " + principals + " roles LEFT JOIN " + members
                + " members ON members.role_principal_id = roles.principal_id AND members.member_principal_id = "
                + "(SELECT principal_id FROM " + principals + " WHERE name = ?) WHERE roles.type = 'R' ORDER BY roles.name",
                rs -> {
                    String name = CommonUtils.notEmpty(JDBCUtils.safeGetString(rs, "name"));
                    boolean implicit = "public".equals(name);
                    roles.add(new DBAAccessState.Role(name,
                        implicit || JDBCUtils.safeGetInt(rs, "member_principal_id") != 0,
                        editable && !implicit && !name.equals(principalName)));
                }, principalName);
            Map<String, List<String>> types = readPermissionTypes(session, system);
            Map<String, DBAAccessState.Target> targets = readTargets(session, system, server, types, editable);
            List<DBAAccessState.Permission> permissions = new ArrayList<>();
            if (mapping == null || mapping.present()) {
                read(session, "SELECT perm.class, perm.major_id, perm.minor_id, perm.permission_name, perm.state, "
                    + "grantor.name AS grantor FROM " + system + (server ? "server_permissions" : "database_permissions")
                    + " perm JOIN " + principals + " grantee ON grantee.principal_id = perm.grantee_principal_id LEFT JOIN "
                    + principals + " grantor ON grantor.principal_id = perm.grantor_principal_id WHERE grantee.name = ?",
                    rs -> {
                        String key = JDBCUtils.safeGetInt(rs, "class") + ":" + JDBCUtils.safeGetInt(rs, "major_id")
                            + ":" + JDBCUtils.safeGetInt(rs, "minor_id");
                        DBAAccessState.Target target = targets.computeIfAbsent(key,
                            ignored -> new DBAAccessState.Target(key, key, List.of(), false));
                        String state = JDBCUtils.safeGetString(rs, "state");
                        DBAAccessState.PermissionState permissionState = switch (CommonUtils.notEmpty(state)) {
                            case "G" -> DBAAccessState.PermissionState.GRANT;
                            case "W" -> DBAAccessState.PermissionState.GRANT_WITH_OPTION;
                            case "D" -> DBAAccessState.PermissionState.DENY;
                            default -> DBAAccessState.PermissionState.REVOKE;
                        };
                        permissions.add(new DBAAccessState.Permission(target,
                            CommonUtils.notEmpty(JDBCUtils.safeGetString(rs, "permission_name")), permissionState,
                            JDBCUtils.safeGetString(rs, "grantor"), editable && target.editable()));
                    }, principalName);
            }
            permissions.sort(Comparator.comparing((DBAAccessState.Permission permission) -> permission.target().label())
                .thenComparing(DBAAccessState.Permission::name).thenComparing(permission -> CommonUtils.notEmpty(permission.grantor())));
            List<DBAAccessState.Target> sortedTargets = targets.values().stream()
                .sorted(Comparator.comparing(DBAAccessState.Target::label)).toList();
            return new DBAAccessState(scope, mapping, roles, sortedTargets, permissions);
        } catch (SQLException e) {
            throw new DBException("Error reading principal access in " + scope.label(), e);
        }
    }

    @NotNull
    private DBAAccessState.Mapping readMapping(@NotNull JDBCSession session, @NotNull String system) throws SQLException {
        List<DBAAccessState.Mapping> mappings = new ArrayList<>();
        read(session, "SELECT name, default_schema_name, principal_id, type FROM " + system
            + "database_principals WHERE sid = SUSER_SID(?) AND type IN ('S','U','G','E','X') ORDER BY principal_id DESC",
            rs -> {
                boolean editable = JDBCUtils.safeGetInt(rs, "principal_id") > 4;
                boolean group = "G".equals(JDBCUtils.safeGetString(rs, "type"));
                boolean groupSchema = group && supportsGroupDefaultSchema();
                mappings.add(new DBAAccessState.Mapping(true, CommonUtils.notEmpty(JDBCUtils.safeGetString(rs, "name")),
                    CommonUtils.notEmpty(JDBCUtils.safeGetString(rs, "default_schema_name")), editable,
                    editable && (!group || groupSchema), editable && groupSchema));
            }, login.getName());
        if (!mappings.isEmpty()) {
            return mappings.getFirst();
        }
        boolean[] group = {login.getLoginType() == SQLServerLogin.LoginType.G};
        if (!isAzure()) {
            read(session, "SELECT type FROM sys.server_principals WHERE name = ?",
                rs -> group[0] = "G".equals(JDBCUtils.safeGetString(rs, "type")), login.getName());
        }
        boolean groupSchema = group[0] && supportsGroupDefaultSchema();
        return new DBAAccessState.Mapping(false, login.getName(), "", true, !group[0] || groupSchema, groupSchema);
    }

    private boolean supportsGroupDefaultSchema() {
        return isAzure() || login.getDataSource().isServerVersionAtLeast(SQLServerConstants.SQL_SERVER_2012_VERSION_MAJOR, 0);
    }

    @NotNull
    private Map<String, List<String>> readPermissionTypes(@NotNull JDBCSession session, @NotNull String system) throws SQLException {
        Map<String, List<String>> types = new LinkedHashMap<>();
        read(session, "SELECT DISTINCT class_desc, permission_name FROM " + system
            + "fn_builtin_permissions(DEFAULT) ORDER BY class_desc, permission_name", rs -> {
                String type = CommonUtils.notEmpty(JDBCUtils.safeGetString(rs, "class_desc"));
                types.computeIfAbsent(type, ignored -> new ArrayList<>())
                    .add(CommonUtils.notEmpty(JDBCUtils.safeGetString(rs, "permission_name")));
            });
        return types;
    }

    @NotNull
    private Map<String, DBAAccessState.Target> readTargets(
        @NotNull JDBCSession session,
        @NotNull String system,
        boolean server,
        @NotNull Map<String, List<String>> types,
        boolean editable
    ) throws SQLException {
        Map<String, DBAAccessState.Target> targets = new LinkedHashMap<>();
        targets.put((server ? "100" : "0") + ":0:0", new DBAAccessState.Target("",
            server ? SQLServerMessages.security_scope_server : SQLServerMessages.security_scope_database,
            types.getOrDefault(server ? "SERVER" : "DATABASE", List.of()), editable));
        if (server) {
            read(session, "SELECT principal_id, name, type FROM " + system + "server_principals", rs -> {
                String type = "R".equals(JDBCUtils.safeGetString(rs, "type")) ? "SERVER ROLE" : "LOGIN";
                addTarget(targets, "101:" + JDBCUtils.safeGetInt(rs, "principal_id") + ":0", type,
                    quote(CommonUtils.notEmpty(JDBCUtils.safeGetString(rs, "name"))), types, editable);
            });
            read(session, "SELECT endpoint_id, name FROM " + system + "endpoints", rs ->
                addTarget(targets, "105:" + JDBCUtils.safeGetInt(rs, "endpoint_id") + ":0", "ENDPOINT",
                    quote(CommonUtils.notEmpty(JDBCUtils.safeGetString(rs, "name"))), types, editable));
        } else {
            read(session, "SELECT schema_id, name FROM " + system + "schemas", rs ->
                addTarget(targets, "3:" + JDBCUtils.safeGetInt(rs, "schema_id") + ":0", "SCHEMA",
                    quote(CommonUtils.notEmpty(JDBCUtils.safeGetString(rs, "name"))), types, editable));
            read(session, "SELECT obj.object_id, obj.name, obj.type, sch.name AS schema_name, col.column_id, col.name AS column_name FROM "
                + system + "objects obj JOIN " + system + "schemas sch ON sch.schema_id = obj.schema_id LEFT JOIN "
                + system + "columns col ON col.object_id = obj.object_id WHERE obj.is_ms_shipped = 0", rs -> {
                    int objectId = JDBCUtils.safeGetInt(rs, "object_id");
                    int columnId = JDBCUtils.safeGetInt(rs, "column_id");
                    String object = quote(CommonUtils.notEmpty(JDBCUtils.safeGetString(rs, "schema_name"))) + "."
                        + quote(CommonUtils.notEmpty(JDBCUtils.safeGetString(rs, "name")));
                    String objectType = CommonUtils.notEmpty(JDBCUtils.safeGetString(rs, "type")).trim();
                    List<String> objectPermissions = getObjectPermissions(objectType, types.getOrDefault("OBJECT", List.of()), false);
                    targets.put("1:" + objectId + ":0", new DBAAccessState.Target("OBJECT::" + object,
                        NLS.bind(SQLServerMessages.security_target_object, object), objectPermissions,
                        editable && !objectPermissions.isEmpty()));
                    if (columnId > 0) {
                        List<String> columnPermissions = getObjectPermissions(objectType, types.getOrDefault("OBJECT", List.of()), true);
                        String clause = "OBJECT::" + object + " (" + quote(
                            CommonUtils.notEmpty(JDBCUtils.safeGetString(rs, "column_name"))) + ")";
                        targets.put("1:" + objectId + ":" + columnId, new DBAAccessState.Target(clause,
                            NLS.bind(SQLServerMessages.security_target_column,
                                object + "." + quote(CommonUtils.notEmpty(JDBCUtils.safeGetString(rs, "column_name")))),
                            columnPermissions, editable && !columnPermissions.isEmpty()));
                    }
                });
            read(session, "SELECT principal_id, name, type FROM " + system + "database_principals", rs -> {
                String type = switch (CommonUtils.notEmpty(JDBCUtils.safeGetString(rs, "type"))) {
                    case "R" -> "ROLE";
                    case "A" -> "APPLICATION ROLE";
                    default -> "USER";
                };
                addTarget(targets, "4:" + JDBCUtils.safeGetInt(rs, "principal_id") + ":0", type,
                    quote(CommonUtils.notEmpty(JDBCUtils.safeGetString(rs, "name"))), types, editable);
            });
        }
        return targets;
    }

    @NotNull
    static List<String> getObjectPermissions(@NotNull String objectType, @NotNull List<String> permissions, boolean column) {
        Set<String> specific = switch (objectType) {
            case "U" -> column ? COLUMN_PERMISSIONS
                : Set.of("DELETE", "INSERT", "REFERENCES", "SELECT", "UNMASK", "UPDATE", "VIEW CHANGE TRACKING");
            case "V", "IF" -> column ? Set.of("SELECT", "REFERENCES", "UPDATE")
                : Set.of("DELETE", "INSERT", "REFERENCES", "SELECT", "UPDATE");
            case "TF", "FT" -> Set.of("SELECT", "REFERENCES");
            case "P", "PC", "X", "FN", "FS", "AF" -> column ? Set.of() : Set.of("EXECUTE", "REFERENCES");
            case "SQ" -> column ? Set.of() : Set.of("SELECT", "RECEIVE", "REFERENCES");
            case "SN" -> column ? Set.of() : Set.of("DELETE", "EXECUTE", "INSERT", "SELECT", "UPDATE");
            case "SO" -> column ? Set.of() : Set.of("UPDATE", "REFERENCES");
            default -> Set.of();
        };
        if (specific.isEmpty()) {
            return List.of();
        }
        return permissions.stream().filter(permission -> specific.contains(permission)
            || !column && COMMON_OBJECT_PERMISSIONS.contains(permission)).toList();
    }

    private void addTarget(
        @NotNull Map<String, DBAAccessState.Target> targets,
        @NotNull String key,
        @NotNull String type,
        @NotNull String name,
        @NotNull Map<String, List<String>> types,
        boolean editable
    ) {
        String clause = type + "::" + name;
        String label = NLS.bind(switch (type) {
            case "LOGIN" -> SQLServerMessages.security_target_login;
            case "SERVER ROLE", "ROLE" -> SQLServerMessages.security_target_role;
            case "ENDPOINT" -> SQLServerMessages.security_target_endpoint;
            case "SCHEMA" -> SQLServerMessages.security_target_schema;
            case "APPLICATION ROLE" -> SQLServerMessages.security_target_application_role;
            case "USER" -> SQLServerMessages.security_target_user;
            default -> SQLServerMessages.security_target_object;
        }, name);
        targets.put(key, new DBAAccessState.Target(clause, label, types.getOrDefault(type, List.of()), editable));
    }

    @NotNull
    @Override
    public DBEPersistAction makeChangeAction(@NotNull DBRProgressMonitor monitor, @NotNull List<Change> changes) throws DBException {
        StringBuilder script = new StringBuilder();
        for (Change change : changes) {
            String statements = generateChanges(change.before(), change.after());
            if (statements.isEmpty()) {
                continue;
            }
            String database = change.before().scope().id().isEmpty() ? "master" : change.before().scope().id();
            String body = (isAzure() ? "" : "USE " + quote(database) + ";\n") + statements;
            script.append("EXEC sys.sp_executesql ").append(SQLUtils.quoteString(login.getDataSource(), body)).append(";\n");
        }
        String atomic = "DECLARE @dbeaver_trancount int = @@TRANCOUNT;\n"
            + "IF @dbeaver_trancount = 0 BEGIN TRANSACTION; ELSE SAVE TRANSACTION dbeaver_access;\n"
            + "BEGIN TRY\n" + script
            + "IF @dbeaver_trancount = 0 COMMIT TRANSACTION;\nEND TRY\nBEGIN CATCH\n"
            + "IF @dbeaver_trancount = 0 AND XACT_STATE() <> 0 ROLLBACK TRANSACTION;\n"
            + "ELSE IF XACT_STATE() = 1 ROLLBACK TRANSACTION dbeaver_access;\nTHROW;\nEND CATCH;";
        return new SQLDatabasePersistAction(SQLServerMessages.security_action_change_access, atomic, true);
    }

    @NotNull
    String generateChanges(@NotNull DBAAccessState before, @NotNull DBAAccessState after) throws DBException {
        if (!before.scope().equals(after.scope()) || !before.targets().equals(after.targets())) {
            throw new DBException("Access scope or securables cannot be changed");
        }
        StringBuilder script = new StringBuilder();
        DBAAccessState.Mapping oldMapping = before.mapping();
        DBAAccessState.Mapping newMapping = after.mapping();
        String principal = login.getName();
        if (oldMapping != null) {
            if (newMapping == null || !oldMapping.editable() && !oldMapping.equals(newMapping)) {
                throw new DBException("This database mapping cannot be changed");
            }
            principal = newMapping.name();
            if (!newMapping.present()) {
                if (oldMapping.present()) {
                    script.append("DROP USER ").append(quote(oldMapping.name())).append(";\n");
                }
                return script.toString();
            }
            if (principal.isBlank()) {
                throw new DBException("Database user name cannot be empty");
            }
            if (!oldMapping.present()) {
                script.append("CREATE USER ").append(quote(principal)).append(" FOR LOGIN ").append(quote(login.getName()));
                if (newMapping.schemaEditable() && !newMapping.defaultSchema().isBlank()) {
                    script.append(" WITH DEFAULT_SCHEMA = ").append(quote(newMapping.defaultSchema()));
                }
                script.append(";\n");
            } else if (!oldMapping.name().equals(principal)) {
                script.append("ALTER USER ").append(quote(oldMapping.name())).append(" WITH NAME = ").append(quote(principal))
                    .append(";\n");
            }
            if (oldMapping.present() && !oldMapping.defaultSchema().equals(newMapping.defaultSchema())) {
                if (!oldMapping.schemaEditable() || newMapping.defaultSchema().isBlank() && !oldMapping.schemaNullable()) {
                    throw new DBException("Default schema cannot be changed or empty for this user");
                }
                script.append("ALTER USER ").append(quote(principal)).append(" WITH DEFAULT_SCHEMA = ")
                    .append(newMapping.defaultSchema().isBlank() ? "NULL" : quote(newMapping.defaultSchema())).append(";\n");
            }
        } else if (newMapping != null) {
            throw new DBException("Server principals cannot have a database mapping in server scope");
        }
        Map<String, DBAAccessState.Role> oldRoles = new LinkedHashMap<>();
        before.roles().forEach(role -> oldRoles.put(role.name(), role));
        if (oldRoles.size() != after.roles().size()) {
            throw new DBException("The set of available roles cannot be changed");
        }
        for (DBAAccessState.Role role : after.roles()) {
            DBAAccessState.Role old = oldRoles.remove(role.name());
            if (old == null || !old.editable() && !old.equals(role)) {
                throw new DBException("This role membership cannot be changed");
            }
            if (old.granted() != role.granted()) {
                script.append(oldMapping == null ? "ALTER SERVER ROLE " : "ALTER ROLE ").append(quote(role.name()))
                    .append(role.granted() ? " ADD MEMBER " : " DROP MEMBER ").append(quote(principal)).append(";\n");
            }
        }
        Map<String, DBAAccessState.Permission> remaining = new LinkedHashMap<>();
        for (DBAAccessState.Permission permission : before.permissions()) {
            remaining.put(permissionKey(permission), permission);
        }
        List<PermissionChange> changes = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (DBAAccessState.Permission permission : after.permissions()) {
            if (!seen.add(permissionKey(permission))) {
                throw new DBException("Duplicate permission");
            }
            changes.add(new PermissionChange(remaining.remove(permissionKey(permission)), permission));
        }
        for (DBAAccessState.Permission permission : remaining.values()) {
            if (!permission.editable()) {
                throw new DBException("This permission cannot be removed");
            }
            changes.add(new PermissionChange(permission, new DBAAccessState.Permission(permission.target(), permission.name(),
                DBAAccessState.PermissionState.REVOKE, permission.grantor(), permission.editable(), permission.cascade())));
        }
        Map<String, Set<String>> changedObjects = new LinkedHashMap<>();
        for (PermissionChange change : changes) {
            if (change.isChanged() && change.after().target().id().startsWith("OBJECT::") && !isColumn(change.after())) {
                changedObjects.computeIfAbsent(change.after().target().id(), key -> new HashSet<>()).add(change.after().name());
            }
        }
        // table-level statements also remove column exceptions, so apply columns last and restore unchanged exceptions
        changes.sort(Comparator.comparing(change -> isColumn(change.after())));
        for (PermissionChange change : changes) {
            DBAAccessState.Permission old = change.before();
            DBAAccessState.Permission permission = change.after();
            String targetId = permission.target().id();
            boolean restoreColumn = isColumn(permission)
                && changedObjects.entrySet().stream().anyMatch(entry -> targetId.startsWith(entry.getKey() + " (")
                    && entry.getValue().contains(permission.name()));
            if (!change.isChanged() && !restoreColumn) {
                continue;
            }
            if (old != null && !old.editable() || !permission.editable() || !permission.target().editable()
                || !before.targets().contains(permission.target()) || !permission.target().permissions().contains(permission.name())) {
                throw new DBException("This permission cannot be changed");
            }
            if (change.isChanged() && old != null && old.state() == DBAAccessState.PermissionState.GRANT_WITH_OPTION
                && !permission.cascade()) {
                throw new DBException("Removing a grant option requires CASCADE. Enable cascading revocation for this permission");
            }
            if (restoreColumn) {
                // restoring an exception under a table grant option requires cascade
                if (!permission.cascade() && (permission.state() == DBAAccessState.PermissionState.DENY
                    || permission.state() == DBAAccessState.PermissionState.REVOKE)) {
                    appendPermissionState(script, new DBAAccessState.Permission(permission.target(), permission.name(), permission.state(),
                        permission.grantor(), permission.editable(), true), principal);
                } else {
                    appendPermissionState(script, permission, principal);
                }
            } else if (old != null && old.state() == DBAAccessState.PermissionState.GRANT_WITH_OPTION
                && permission.state() == DBAAccessState.PermissionState.GRANT) {
                appendPermission(script, "REVOKE GRANT OPTION FOR", permission, principal, false);
            } else {
                if (old != null && old.state() != DBAAccessState.PermissionState.REVOKE
                    && !(old.state() == DBAAccessState.PermissionState.GRANT
                        && permission.state() == DBAAccessState.PermissionState.GRANT_WITH_OPTION)) {
                    appendPermission(script, "REVOKE", new DBAAccessState.Permission(old.target(), old.name(), old.state(),
                        old.grantor(), old.editable(), permission.cascade()), principal, false);
                }
                if (old == null || permission.state() != DBAAccessState.PermissionState.REVOKE
                    || old.state() == DBAAccessState.PermissionState.REVOKE) {
                    appendPermissionState(script, permission, principal);
                }
            }
        }
        return script.toString();
    }

    private boolean isColumn(@NotNull DBAAccessState.Permission permission) {
        return permission.target().id().startsWith("OBJECT::") && permission.target().id().endsWith(")");
    }

    private void appendPermissionState(
        @NotNull StringBuilder script,
        @NotNull DBAAccessState.Permission permission,
        @NotNull String principal
    ) {
        appendPermission(script, permission.state() == DBAAccessState.PermissionState.DENY ? "DENY"
            : permission.state() == DBAAccessState.PermissionState.REVOKE ? "REVOKE" : "GRANT", permission, principal,
            permission.state() == DBAAccessState.PermissionState.GRANT_WITH_OPTION);
    }

    private void appendPermission(
        @NotNull StringBuilder script,
        @NotNull String operation,
        @NotNull DBAAccessState.Permission permission,
        @NotNull String principal,
        boolean grantOption
    ) {
        script.append(operation).append(' ').append(permission.name());
        if (!permission.target().id().isEmpty()) {
            script.append(" ON ").append(permission.target().id());
        }
        script.append(operation.startsWith("REVOKE") ? " FROM " : " TO ").append(quote(principal));
        if (grantOption) {
            script.append(" WITH GRANT OPTION");
        }
        if (permission.cascade() && (operation.startsWith("REVOKE") || "DENY".equals(operation))) {
            script.append(" CASCADE");
        }
        if (permission.grantor() != null) {
            script.append(" AS ").append(quote(permission.grantor()));
        }
        script.append(";\n");
    }

    @NotNull
    private String permissionKey(@NotNull DBAAccessState.Permission permission) {
        return permission.target().id() + "\u0000" + permission.name() + "\u0000" + Objects.toString(permission.grantor(), "");
    }

    @NotNull
    private String systemSchema(@NotNull DBAAccessState.Scope scope) {
        if (isAzure()) {
            return "sys.";
        }
        return quote(scope.id().isEmpty() ? "master" : scope.id()) + ".sys.";
    }

    private boolean isAzure() {
        return SQLServerUtils.isDriverAzure(login.getDataSource().getContainer().getDriver());
    }

    @NotNull
    private String quote(@NotNull String name) {
        return DBUtils.getQuotedIdentifier(login.getDataSource(), name, true, true);
    }

    private void read(
        @NotNull JDBCSession session,
        @NotNull String sql,
        @NotNull Consumer<JDBCResultSet> consumer,
        @NotNull Object... parameters
    ) throws SQLException {
        try (JDBCPreparedStatement statement = session.prepareStatement(sql)) {
            for (int index = 0; index < parameters.length; index++) {
                statement.setObject(index + 1, parameters[index]);
            }
            try (JDBCResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    if (session.getProgressMonitor().isCanceled()) {
                        throw new SQLException("Operation canceled");
                    }
                    consumer.accept(rs);
                }
            }
        }
    }
}
