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
package org.jkiss.dbeaver.ext.firebird.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBDatabaseException;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.generic.model.GenericScriptObject;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBPQualifiedObject;
import org.jkiss.dbeaver.model.DBPUniqueObject;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.meta.PropertyLength;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLUtils;
import org.jkiss.utils.CommonUtils;

import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Firebird user from the security database (SEC$USERS, Firebird 3+).
 * A user is identified by its name together with the user manager plugin.
 */
public class FireBirdUser implements GenericScriptObject, DBPQualifiedObject, DBPUniqueObject {

    /** Placeholder for the password in generated DDL: passwords are stored as hashes and can't be read */
    static final String PASSWORD_PLACEHOLDER = "********";

    @NotNull
    private final FireBirdDataSource dataSource;
    @NotNull
    private final String name;
    @Nullable
    private final String firstName;
    @Nullable
    private final String middleName;
    @Nullable
    private final String lastName;
    private final boolean active;
    private final boolean admin;
    @Nullable
    private final String plugin;
    @Nullable
    private final String description;
    @Nullable
    private Map<String, String> tags;

    public FireBirdUser(
        @NotNull FireBirdDataSource dataSource,
        @NotNull String name,
        @Nullable String firstName,
        @Nullable String middleName,
        @Nullable String lastName,
        boolean active,
        boolean admin,
        @Nullable String plugin,
        @Nullable String description
    ) {
        this.dataSource = dataSource;
        this.name = name;
        this.firstName = firstName;
        this.middleName = middleName;
        this.lastName = lastName;
        this.active = active;
        this.admin = admin;
        this.plugin = plugin;
        this.description = description;
    }

    @NotNull
    @Override
    @Property(viewable = true, order = 1)
    public String getName() {
        return name;
    }

    /**
     * The same user name may exist for several user manager plugins, so the plugin is part of the unique name.
     */
    @NotNull
    @Override
    public String getUniqueName() {
        return CommonUtils.isEmpty(plugin) ? name : name + " (" + plugin + ")";
    }

    @Nullable
    @Property(viewable = true, order = 2)
    public String getFirstName() {
        return firstName;
    }

    @Nullable
    @Property(viewable = true, order = 3)
    public String getMiddleName() {
        return middleName;
    }

    @Nullable
    @Property(viewable = true, order = 4)
    public String getLastName() {
        return lastName;
    }

    @Property(viewable = true, order = 5)
    public boolean isActive() {
        return active;
    }

    /**
     * True if the user has the RDB$ADMIN role in the security database.
     */
    @Property(viewable = true, order = 6)
    public boolean isAdmin() {
        return admin;
    }

    @Nullable
    @Property(viewable = true, order = 7)
    public String getPlugin() {
        return plugin;
    }

    @NotNull
    @Property(order = 8)
    public String getTagsText(@NotNull DBRProgressMonitor monitor) throws DBException {
        return getTags(monitor).entrySet().stream()
            .map(tag -> tag.getKey() + "=" + tag.getValue())
            .collect(Collectors.joining(", "));
    }

    @Nullable
    @Override
    @Property(viewable = true, length = PropertyLength.MULTILINE, order = 10)
    public String getDescription() {
        return description;
    }

    /**
     * Custom attributes of the user (TAGS clause), read from SEC$USER_ATTRIBUTES.
     */
    @NotNull
    public Map<String, String> getTags(@NotNull DBRProgressMonitor monitor) throws DBException {
        if (tags == null) {
            Map<String, String> result = new LinkedHashMap<>();
            try (JDBCSession session = DBUtils.openMetaSession(monitor, this, "Load Firebird user attributes")) {
                try (JDBCPreparedStatement dbStat = session.prepareStatement(
                    "SELECT SEC$KEY,SEC$VALUE FROM SEC$USER_ATTRIBUTES\n" +
                        "WHERE SEC$USER_NAME=? AND SEC$PLUGIN IS NOT DISTINCT FROM ? ORDER BY SEC$KEY")) {
                    dbStat.setString(1, name);
                    dbStat.setString(2, plugin);
                    try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                        while (dbResult.next()) {
                            String key = JDBCUtils.safeGetStringTrimmed(dbResult, "SEC$KEY");
                            if (key != null) {
                                result.put(key, CommonUtils.notEmpty(JDBCUtils.safeGetString(dbResult, "SEC$VALUE")));
                            }
                        }
                    }
                }
            } catch (SQLException e) {
                throw new DBDatabaseException(e, dataSource);
            }
            tags = result;
        }
        return tags;
    }

    @Override
    public boolean isPersisted() {
        return true;
    }

    @NotNull
    @Override
    public FireBirdDataSource getParentObject() {
        return dataSource;
    }

    @NotNull
    @Override
    public FireBirdDataSource getDataSource() {
        return dataSource;
    }

    @NotNull
    @Override
    public String getFullyQualifiedName(@NotNull DBPEvaluationContext context) {
        return DBUtils.getQuotedIdentifier(this);
    }

    @NotNull
    @Override
    public String getObjectDefinitionText(@NotNull DBRProgressMonitor monitor, @NotNull Map<String, Object> options)
        throws DBException {
        String quotedName = getFullyQualifiedName(DBPEvaluationContext.DDL);
        String usingPlugin = CommonUtils.isEmpty(plugin) ? "" : " USING PLUGIN " + plugin;
        StringBuilder ddl = new StringBuilder();
        ddl.append("-- The password can't be read from the database, set it before running the script\n");
        ddl.append("CREATE USER ").append(quotedName)
            .append(" PASSWORD ").append(SQLUtils.quoteString(this, PASSWORD_PLACEHOLDER));
        appendOption(ddl, "FIRSTNAME", firstName);
        appendOption(ddl, "MIDDLENAME", middleName);
        appendOption(ddl, "LASTNAME", lastName);
        if (!active) {
            ddl.append("\n    INACTIVE");
        }
        if (!usingPlugin.isEmpty()) {
            ddl.append("\n   ").append(usingPlugin);
        }
        Map<String, String> userTags = getTags(monitor);
        if (!userTags.isEmpty()) {
            ddl.append("\n    TAGS (").append(userTags.entrySet().stream()
                .map(tag -> DBUtils.getQuotedIdentifier(dataSource, tag.getKey()) + " = "
                    + SQLUtils.quoteString(this, tag.getValue()))
                .collect(Collectors.joining(", "))).append(')');
        }
        if (admin) {
            ddl.append("\n    GRANT ADMIN ROLE");
        }
        ddl.append(';');
        if (!CommonUtils.isEmpty(description)) {
            ddl.append("\n\nCOMMENT ON USER ").append(quotedName).append(usingPlugin)
                .append(" IS ").append(SQLUtils.quoteString(this, description)).append(';');
        }
        return ddl.toString();
    }

    private void appendOption(@NotNull StringBuilder ddl, @NotNull String option, @Nullable String value) {
        if (!CommonUtils.isEmpty(value)) {
            ddl.append("\n    ").append(option).append(' ').append(SQLUtils.quoteString(this, value));
        }
    }

    @Override
    public String toString() {
        return name;
    }
}
