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
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.generic.model.GenericScriptObject;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBPQualifiedObject;
import org.jkiss.dbeaver.model.DBPSystemObject;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCObjectCache;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.meta.PropertyLength;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLUtils;
import org.jkiss.utils.CommonUtils;

import java.sql.SQLException;
import java.util.Collection;
import java.util.Map;

/**
 * Firebird package (Firebird 3+), stored in RDB$PACKAGES. Consists of a header with the public routine
 * declarations and a body with their implementation and private routines.
 */
public class FireBirdPackage implements GenericScriptObject, DBPQualifiedObject, DBPSystemObject {

    /** Statement terminator used in the generated script, because package sources contain ';' */
    private static final String SCRIPT_TERMINATOR = "^";

    @NotNull
    private final FireBirdDataSource dataSource;
    @NotNull
    private final String name;
    @Nullable
    private final String ownerName;
    @Nullable
    private final String headerSource;
    @Nullable
    private final String bodySource;
    private final boolean validBody;
    @Nullable
    private final Boolean sqlSecurityDefiner;
    @Nullable
    private final String description;
    private final boolean system;
    private final RoutineCache routineCache = new RoutineCache();

    public FireBirdPackage(
        @NotNull FireBirdDataSource dataSource,
        @NotNull String name,
        @Nullable String ownerName,
        @Nullable String headerSource,
        @Nullable String bodySource,
        boolean validBody,
        @Nullable Boolean sqlSecurityDefiner,
        @Nullable String description,
        boolean system
    ) {
        this.dataSource = dataSource;
        this.name = name;
        this.ownerName = ownerName;
        this.headerSource = headerSource;
        this.bodySource = bodySource;
        this.validBody = validBody;
        this.sqlSecurityDefiner = sqlSecurityDefiner;
        this.description = description;
        this.system = system;
    }

    @NotNull
    @Override
    @Property(viewable = true, order = 1)
    public String getName() {
        return name;
    }

    @Nullable
    @Property(viewable = true, order = 2)
    public String getOwnerName() {
        return ownerName;
    }

    /**
     * False if the body is missing or has to be recompiled after the header was changed.
     */
    @Property(viewable = true, order = 3)
    public boolean isValidBody() {
        return validBody;
    }

    /**
     * SQL SECURITY of the package (Firebird 4+), empty if the database default is used.
     */
    @Nullable
    @Property(viewable = true, order = 4)
    public String getSqlSecurity() {
        return sqlSecurityDefiner == null ? null : getSqlSecurityKeyword(sqlSecurityDefiner);
    }

    @Nullable
    @Override
    @Property(viewable = true, length = PropertyLength.MULTILINE, order = 10)
    public String getDescription() {
        return description;
    }

    @Override
    public boolean isSystem() {
        return system;
    }

    /**
     * Procedures and functions of the package, including private ones declared in the body only.
     */
    @NotNull
    public Collection<FireBirdPackageRoutine> getRoutines(@NotNull DBRProgressMonitor monitor) throws DBException {
        return routineCache.getAllObjects(monitor, this);
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
    public String getObjectDefinitionText(@NotNull DBRProgressMonitor monitor, @NotNull Map<String, Object> options) {
        String quotedName = getFullyQualifiedName(DBPEvaluationContext.DDL);
        StringBuilder ddl = new StringBuilder();
        if (!CommonUtils.isEmpty(headerSource)) {
            ddl.append("SET TERM ").append(SCRIPT_TERMINATOR).append(" ;\n\n");
            ddl.append("CREATE OR ALTER PACKAGE ").append(quotedName).append('\n');
            if (sqlSecurityDefiner != null) {
                ddl.append("SQL SECURITY ").append(getSqlSecurityKeyword(sqlSecurityDefiner)).append('\n');
            }
            ddl.append("AS\n").append(headerSource.trim()).append(SCRIPT_TERMINATOR).append('\n');
            if (!CommonUtils.isEmpty(bodySource)) {
                ddl.append("\nRECREATE PACKAGE BODY ").append(quotedName).append('\n')
                    .append("AS\n").append(bodySource.trim()).append(SCRIPT_TERMINATOR).append('\n');
            }
            ddl.append("\nSET TERM ; ").append(SCRIPT_TERMINATOR).append('\n');
        }
        if (!CommonUtils.isEmpty(description)) {
            ddl.append("\nCOMMENT ON PACKAGE ").append(quotedName)
                .append(" IS ").append(SQLUtils.quoteString(this, description)).append(";\n");
        }
        return ddl.toString().trim();
    }

    @NotNull
    private static String getSqlSecurityKeyword(boolean definer) {
        return definer ? "DEFINER" : "INVOKER";
    }

    @Override
    public String toString() {
        return name;
    }

    /**
     * Loads the procedures and functions of a package.
     */
    static class RoutineCache extends JDBCObjectCache<FireBirdPackage, FireBirdPackageRoutine> {

        @NotNull
        @Override
        protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull FireBirdPackage owner)
            throws SQLException {
            JDBCPreparedStatement dbStat = session.prepareStatement(
                "SELECT RDB$PROCEDURE_NAME AS ROUTINE_NAME,0 AS IS_FUNCTION,RDB$PRIVATE_FLAG,RDB$DESCRIPTION\n" +
                    "FROM RDB$PROCEDURES WHERE RDB$PACKAGE_NAME=?\n" +
                    "UNION ALL\n" +
                    "SELECT RDB$FUNCTION_NAME,1,RDB$PRIVATE_FLAG,RDB$DESCRIPTION\n" +
                    "FROM RDB$FUNCTIONS WHERE RDB$PACKAGE_NAME=?\n" +
                    "ORDER BY 1");
            dbStat.setString(1, owner.getName());
            dbStat.setString(2, owner.getName());
            return dbStat;
        }

        @Nullable
        @Override
        protected FireBirdPackageRoutine fetchObject(
            @NotNull JDBCSession session,
            @NotNull FireBirdPackage owner,
            @NotNull JDBCResultSet resultSet
        ) {
            String routineName = JDBCUtils.safeGetStringTrimmed(resultSet, "ROUTINE_NAME");
            if (routineName == null) {
                return null;
            }
            return new FireBirdPackageRoutine(
                owner,
                routineName,
                JDBCUtils.safeGetInt(resultSet, "IS_FUNCTION") != 0,
                JDBCUtils.safeGetInt(resultSet, "RDB$PRIVATE_FLAG") != 0,
                JDBCUtils.safeGetString(resultSet, "RDB$DESCRIPTION")
            );
        }
    }
}
