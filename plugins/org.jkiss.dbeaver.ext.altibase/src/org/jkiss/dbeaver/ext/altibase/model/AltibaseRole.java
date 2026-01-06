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
package org.jkiss.dbeaver.ext.altibase.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.access.DBARole;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCObjectCache;
import org.jkiss.dbeaver.model.meta.Association;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;

/**
 * AltibaseRole
 */
public class AltibaseRole extends AltibaseGrantee implements DBARole {

    private final UserPrivCache userPrivCache = new UserPrivCache();

    public AltibaseRole(AltibaseDataSource dataSource, ResultSet resultSet) {
        super(dataSource, JDBCUtils.safeGetString(resultSet, "USER_NAME"));
    }

    @NotNull
    @Override
    @Property(viewable = true, order = 2)
    public String getName() {
        return name;
    }

    /**
     * Get grantee and grantor pair
     */
    @Association
    public Collection<AltibasePrivUser> getUserPrivs(DBRProgressMonitor monitor) throws DBException {
        return userPrivCache.getAllObjects(monitor, this);
    }

    @Nullable
    @Override
    public DBSObject refreshObject(@NotNull DBRProgressMonitor monitor) throws DBException {
        userPrivCache.clearCache();
        return super.refreshObject(monitor);
    }

    static class UserPrivCache extends JDBCObjectCache<AltibaseRole, AltibasePrivUser> {
        @NotNull
        @Override
        protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull AltibaseRole owner) throws SQLException {
            final JDBCPreparedStatement dbStat = session.prepareStatement(
                    "SELECT"
                            + " ge.user_name AS grantee_name"
                            + " , gr.user_name AS grantor_name"
                        + " FROM"
                            + " system_.sys_users_ ge"
                            + " , system_.sys_users_ gr"
                            + " , system_.sys_users_ u"
                            + " , system_.sys_user_roles_ r"
                        + " WHERE"
                            + " u.user_name = ?"
                            + " AND u.user_id = r.role_id"
                            + " AND r.grantee_id = ge.user_id"
                            + " AND r.grantor_id = gr.user_id"
                        + " ORDER BY grantee_name, grantor_name");
            dbStat.setString(1, owner.getName());
            return dbStat;
        }

        @Override
        protected AltibasePrivUser fetchObject(@NotNull JDBCSession session, @NotNull AltibaseRole owner, 
                @NotNull JDBCResultSet resultSet) throws SQLException, DBException {
            return new AltibasePrivUser(owner, resultSet);
        }
    }

    @Nullable
    @Override
    public Object getLazyReference(Object propertyId) {
        return null;
    }

}
