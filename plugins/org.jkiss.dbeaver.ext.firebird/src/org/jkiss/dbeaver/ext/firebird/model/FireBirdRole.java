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
 * Firebird role, stored in RDB$ROLES.
 */
public class FireBirdRole implements GenericScriptObject, DBPQualifiedObject, DBPSystemObject {

    @NotNull
    private final FireBirdDataSource dataSource;
    @NotNull
    private final String name;
    @Nullable
    private final String ownerName;
    @Nullable
    private final String description;
    private final boolean system;
    private final MemberCache memberCache = new MemberCache();

    public FireBirdRole(
        @NotNull FireBirdDataSource dataSource,
        @NotNull String name,
        @Nullable String ownerName,
        @Nullable String description,
        boolean system
    ) {
        this.dataSource = dataSource;
        this.name = name;
        this.ownerName = ownerName;
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
     * Users and roles this role is granted to.
     */
    @NotNull
    public Collection<FireBirdRoleMember> getMembers(@NotNull DBRProgressMonitor monitor) throws DBException {
        return memberCache.getAllObjects(monitor, this);
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
        StringBuilder ddl = new StringBuilder();
        if (!system) {
            // System roles (RDB$ADMIN) are built in and can't be created
            ddl.append("CREATE ROLE ").append(quotedName).append(";\n");
        }
        if (!CommonUtils.isEmpty(description)) {
            ddl.append("\nCOMMENT ON ROLE ").append(quotedName)
                .append(" IS ").append(SQLUtils.quoteString(this, description)).append(";\n");
        }
        Collection<FireBirdRoleMember> members = getMembers(monitor);
        if (!members.isEmpty()) {
            ddl.append('\n');
            for (FireBirdRoleMember member : members) {
                ddl.append(member.getGrantStatement()).append(";\n");
            }
        }
        return ddl.toString().trim();
    }

    @Override
    public String toString() {
        return name;
    }

    /**
     * Loads the users and roles this role is granted to from RDB$USER_PRIVILEGES.
     */
    static class MemberCache extends JDBCObjectCache<FireBirdRole, FireBirdRoleMember> {

        @NotNull
        @Override
        protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull FireBirdRole owner)
            throws SQLException {
            // Role membership is the privilege 'M'; RDB$FIELD_NAME is 'D' if the role is granted as default role
            JDBCPreparedStatement dbStat = session.prepareStatement(
                "SELECT RDB$USER,RDB$USER_TYPE,RDB$GRANTOR,RDB$GRANT_OPTION,RDB$FIELD_NAME\n" +
                    "FROM RDB$USER_PRIVILEGES WHERE RDB$PRIVILEGE='M' AND RDB$RELATION_NAME=? ORDER BY RDB$USER");
            dbStat.setString(1, owner.getName());
            return dbStat;
        }

        @Nullable
        @Override
        protected FireBirdRoleMember fetchObject(
            @NotNull JDBCSession session,
            @NotNull FireBirdRole owner,
            @NotNull JDBCResultSet resultSet
        ) {
            String memberName = JDBCUtils.safeGetStringTrimmed(resultSet, "RDB$USER");
            if (memberName == null) {
                return null;
            }
            return new FireBirdRoleMember(
                owner,
                memberName,
                JDBCUtils.safeGetInt(resultSet, "RDB$USER_TYPE") == FireBirdConstants.OBJECT_TYPE_ROLE,
                JDBCUtils.safeGetStringTrimmed(resultSet, "RDB$GRANTOR"),
                JDBCUtils.safeGetInt(resultSet, "RDB$GRANT_OPTION") != 0,
                FireBirdConstants.DEFAULT_ROLE_FLAG.equals(JDBCUtils.safeGetStringTrimmed(resultSet, "RDB$FIELD_NAME"))
            );
        }
    }
}
