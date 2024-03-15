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
import org.jkiss.dbeaver.model.DBPRefreshableObject;
import org.jkiss.dbeaver.model.access.DBAUser;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCObjectCache;
import org.jkiss.dbeaver.model.meta.Association;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectLazy;

import java.sql.SQLException;
import java.util.Collection;

public abstract class AltibaseGrantee extends AltibaseGlobalObject 
    implements DBAUser, DBSObjectLazy<AltibaseDataSource>, DBPRefreshableObject {
    
    protected String name;
    
    protected final RolePrivCache rolePrivCache = new RolePrivCache();
    private final SystemPrivCache systemPrivCache = new SystemPrivCache();
    private final ObjectPrivCache objectPrivCache = new ObjectPrivCache();
    
    public AltibaseGrantee(AltibaseDataSource dataSource, String name) {
        super(dataSource, true);
        this.name = name;
    }
    
    /**
     * Returns role(s) belongs to a user 
     */
    @Association
    public Collection<AltibasePrivRole> getRolePrivs(DBRProgressMonitor monitor) throws DBException {
        return rolePrivCache.getAllObjects(monitor, this);
    }
    
    /**
     * Returns system privileges belongs to a user 
     */
    @Association
    public Collection<AltibasePrivSystem> getSystemPrivs(DBRProgressMonitor monitor) throws DBException {
        return systemPrivCache.getAllObjects(monitor, this);
    }

    /**
     * Returns object privileges belongs to a user 
     */
    @Association
    public Collection<AltibasePrivObject> getObjectPrivs(DBRProgressMonitor monitor) throws DBException {
        return objectPrivCache.getAllObjects(monitor, this);
    }

    @Nullable
    @Override
    public DBSObject refreshObject(@NotNull DBRProgressMonitor monitor) throws DBException {
        rolePrivCache.clearCache();
        systemPrivCache.clearCache();
        objectPrivCache.clearCache();

        return this;
    }
    

    static class RolePrivCache extends JDBCObjectCache<AltibaseGrantee, AltibasePrivRole> {
        @NotNull
        @Override
        protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull AltibaseGrantee owner) throws SQLException {
            final JDBCPreparedStatement dbStat = session.prepareStatement(
                    "SELECT"
                            + " ur.user_name AS role_name"
                            + " ,gr.user_name AS grantor_name"
                        + " FROM"
                            + " system_.sys_users_ ge"
                            + " ,system_.sys_users_ gr"
                            + " ,system_.sys_users_ ur"
                            + " ,system_.sys_user_roles_ r"
                        + " WHERE"
                            + " ge.user_name = ?"
                            + " AND ge.user_id = r.grantee_id"
                            + " AND r.role_id = ur.user_id"
                            + " AND gr.user_id = r.grantor_id"
                            + " AND ur.user_type = 'R'"
                            + " AND r.role_id != 0 /* exclude public */");
            dbStat.setString(1, owner.getName());
            return dbStat;
        }

        @Override
        protected AltibasePrivRole fetchObject(@NotNull JDBCSession session, @NotNull AltibaseGrantee owner, 
                @NotNull JDBCResultSet resultSet) throws SQLException, DBException {
            return new AltibasePrivRole(owner, resultSet);
        }
    }

    static class SystemPrivCache extends JDBCObjectCache<AltibaseGrantee, AltibasePrivSystem> {
        @NotNull
        @Override
        protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull AltibaseGrantee owner) throws SQLException {
            final JDBCPreparedStatement dbStat = session.prepareStatement(
                    "SELECT p.priv_name, gr.grantor_name "
                    + "FROM system_.sys_privileges_ p LEFT OUTER JOIN "
                    + "(SELECT priv_id, u1.user_name AS grantor_name "
                    + "FROM system_.sys_users_ u1,  system_.sys_users_ u2, system_.sys_grant_system_ g "
                    + "WHERE g.grantee_id = u2.user_id and u2.user_name = ? and u1.user_id = g.grantor_id"
                    + ") gr ON p.priv_id = gr.priv_id "
                    + "WHERE p.priv_type = 2 "
                    + "ORDER BY p.priv_name");
            dbStat.setString(1, owner.getName());
            return dbStat;
        }

        @Override
        protected AltibasePrivSystem fetchObject(@NotNull JDBCSession session, @NotNull AltibaseGrantee owner, 
                @NotNull JDBCResultSet resultSet) throws SQLException, DBException {
            return new AltibasePrivSystem(owner, resultSet);
        }
    }
    
    static class ObjectPrivCache extends JDBCObjectCache<AltibaseGrantee, AltibasePrivObject> {
        @NotNull
        @Override
        protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull AltibaseGrantee owner) throws SQLException {
            final JDBCPreparedStatement dbStat = session.prepareStatement(
                    "SELECT * FROM"
                            + " ((SELECT"
                                + " grantor.user_name AS grantor_name"
                                + " , DECODE(t.object_type, "
                                    + " 0, 'Procedure',"
                                    + " 1, 'Function',"
                                    + " 3, 'Typeset',"
                                    + " 'Unknown') as obj_type"
                                + " , schema.user_name AS schema_name"
                                + " , t.proc_name AS obj_name"
                                + " , p.priv_name AS priv_name"
                                + " , g.with_grant_option AS with_grant_option"
                            + " FROM"
                                + " system_.sys_users_ schema, system_.sys_users_ grantor, system_.sys_users_ grantee,"
                                + " system_.sys_grant_object_ g, "
                                + " system_.sys_privileges_ p, "
                                + " system_.sys_procedures_ t"
                            + " WHERE"
                                + " grantee.user_name = ? AND g.grantee_id = grantee.user_id"
                                + " AND g.grantor_id = grantor.user_id"
                                + " AND g.priv_id = p.priv_id AND p.priv_type = 1"
                                + " AND schema.user_id = g.user_id"
                                + " AND g.obj_id = t.proc_oid"
                                + " AND g.obj_type = 'P'"
                            + " ) UNION ALL ("
                            + " SELECT"
                                + " grantor.user_name AS grantor_name"
                                + " , DECODE(t.table_type, "
                                    + " 'T', 'Table',"
                                    + " 'S', 'Sequence', "
                                    + " 'V', 'View',"
                                    + " 'Q', 'Queue',"
                                    + " 'Unknown') as obj_type"
                                + " , schema.user_name AS schema_name"
                                + " , t.table_name AS obj_name"
                                + " , p.priv_name AS priv_name"
                                + " , g.with_grant_option AS with_grant_option"
                            + " FROM"
                                + " system_.sys_users_ schema, system_.sys_users_ grantor, system_.sys_users_ grantee,"
                                + " system_.sys_grant_object_ g, "
                                + " system_.sys_privileges_ p, "
                                + " system_.sys_tables_ t"
                            + " WHERE"
                                + " grantee.user_name = ? AND g.grantee_id = grantee.user_id"
                                + " AND g.grantor_id = grantor.user_id"
                                + " AND g.priv_id = p.priv_id AND p.priv_type = 1"
                                + " AND schema.user_id = g.user_id"
                                + " AND g.obj_id = t.table_id"
                                + " AND (g.obj_type = 'T' OR g.obj_type = 'S')"
                            + " ) UNION ALL ("
                            + " SELECT"
                                + " grantor.user_name AS grantor_name"
                                + " , 'Package' as obj_type"
                                + " , schema.user_name AS schema_name"
                                + " , t.package_name AS obj_name"
                                + " , p.priv_name AS priv_name"
                                + " , g.with_grant_option AS with_grant_option"
                            + " FROM"
                                + " system_.sys_users_ schema, system_.sys_users_ grantor, system_.sys_users_ grantee,"
                                + " system_.sys_grant_object_ g, "
                                + " system_.sys_privileges_ p, "
                                + " system_.sys_packages_ t"
                            + " WHERE"
                                + " grantee.user_name = ? AND g.grantee_id = grantee.user_id"
                                + " AND g.grantor_id = grantor.user_id"
                                + " AND g.priv_id = p.priv_id AND p.priv_type = 1"
                                + " AND schema.user_id = g.user_id"
                                + " AND g.obj_id = t.package_oid AND t.package_type = 6"
                                + " AND g.obj_type = 'A'"
                            + " ) UNION ALL ("
                            + " SELECT"
                                + " grantor.user_name AS grantor_name"
                                + " , 'Package' as obj_type"
                                + " , schema.user_name AS schema_name"
                                + " , t.directory_name AS obj_name"
                                + " , p.priv_name AS priv_name"
                                + " , g.with_grant_option AS with_grant_option"
                            + " FROM"
                                + " system_.sys_users_ schema, system_.sys_users_ grantor, system_.sys_users_ grantee,"
                                + " system_.sys_grant_object_ g, "
                                + " system_.sys_privileges_ p, "
                                + " system_.sys_directories_ t"
                            + " WHERE"
                                + " grantee.user_name = ? AND g.grantee_id = grantee.user_id"
                                + " AND g.grantor_id = grantor.user_id"
                                + " AND g.priv_id = p.priv_id AND p.priv_type = 1"
                                + " AND schema.user_id = g.user_id"
                                + " AND g.obj_id = t.directory_id"
                                + " AND g.obj_type = 'D'"
                            + " ) UNION ALL ("
                            + " SELECT"
                                + " grantor.user_name AS grantor_name"
                                + " , 'Package' as obj_type"
                                + " , schema.user_name  AS schema_name"
                                + " , t.library_name AS obj_name"
                                + " , p.priv_name AS priv_name"
                                + " , g.with_grant_option AS with_grant_option"
                            + " FROM"
                                + " system_.sys_users_ schema, system_.sys_users_ grantor, system_.sys_users_ grantee,"
                                + " system_.sys_grant_object_ g, "
                                + " system_.sys_privileges_ p, "
                                + " system_.sys_libraries_ t"
                            + " WHERE"
                                + " grantee.user_name = ? AND g.grantee_id = grantee.user_id"
                                + " AND g.grantor_id = grantor.user_id"
                                + " AND g.priv_id = p.priv_id AND p.priv_type = 1"
                                + " AND schema.user_id = g.user_id"
                                + " AND g.obj_id = t.library_id"
                                + " AND g.obj_type = 'Y'"
                            + " ))"
                            + " ORDER BY grantor_name, obj_type, schema_name, obj_name, priv_name");
            for (int i = 1; i < 6; i++) {
                dbStat.setString(i, owner.getName());
            }
            return dbStat;
        }

        @Override
        protected AltibasePrivObject fetchObject(@NotNull JDBCSession session, @NotNull AltibaseGrantee owner, 
                @NotNull JDBCResultSet resultSet) throws SQLException, DBException {
            return new AltibasePrivObject(owner, resultSet);
        }
    }
}
