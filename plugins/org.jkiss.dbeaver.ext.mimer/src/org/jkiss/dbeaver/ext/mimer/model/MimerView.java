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
package org.jkiss.dbeaver.ext.mimer.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.generic.model.GenericStructContainer;
import org.jkiss.dbeaver.ext.generic.model.GenericView;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCObjectCache;
import org.jkiss.dbeaver.model.meta.Association;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;

import java.sql.SQLException;
import java.util.Collection;

/**
 * {@code GenericView} + a {@link MimerObjectPrivilege.PrivilegeCache} for its "Privileges"
 * folder. Mimer SQL's own GRANT/REVOKE syntax addresses views with {@code ON TABLE} (see {@link
 * MimerObjectPrivilege}), so this shares the exact same privilege model as {@link MimerTable}
 * rather than having its own. See {@link
 * org.jkiss.dbeaver.ext.mimer.model.MimerMetaModel#createTableOrViewImpl}.
 *
 * @author Mimer Information Technology
 */
public class MimerView extends GenericView {

    private final MimerObjectPrivilege.PrivilegeCache privilegeCache = new MimerObjectPrivilege.PrivilegeCache();
    private final UsedByCache usedByCache = new UsedByCache();
    private final UsesCache usesCache = new UsesCache();

    public MimerView(
        @NotNull GenericStructContainer container,
        @Nullable String tableName,
        @Nullable String tableType,
        @Nullable JDBCResultSet dbResult
    ) {
        super(container, tableName, tableType, dbResult);
    }

    @Association
    public Collection<MimerObjectPrivilege> getPrivileges(DBRProgressMonitor monitor) throws DBException {
        return privilegeCache.getAllObjects(monitor, this);
    }

    public MimerObjectPrivilege.PrivilegeCache getPrivilegeCache() {
        return privilegeCache;
    }

    @Association
    public Collection<MimerObjectUsedBy> getUsedBy(@NotNull DBRProgressMonitor monitor) throws DBException {
        return usedByCache.getAllObjects(monitor, this);
    }

    @Association
    public Collection<MimerObjectUses> getUses(@NotNull DBRProgressMonitor monitor) throws DBException {
        return usesCache.getAllObjects(monitor, this);
    }

    @Override
    public DBSObject refreshObject(@NotNull DBRProgressMonitor monitor) throws DBException {
        privilegeCache.clearCache();
        usedByCache.clearCache();
        usesCache.clearCache();
        return super.refreshObject(monitor);
    }

    static class UsedByCache extends JDBCObjectCache<MimerView, MimerObjectUsedBy> {
        @NotNull
        @Override
        protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull MimerView owner) throws SQLException {
            return MimerObjectUsedBy.prepareUsedByStatement(session, owner.getSchema().getName(), owner.getName(), "VIEW");
        }

        @Override
        protected MimerObjectUsedBy fetchObject(@NotNull JDBCSession session, @NotNull MimerView owner, @NotNull JDBCResultSet resultSet) throws SQLException, DBException {
            return new MimerObjectUsedBy(owner, resultSet);
        }
    }

    static class UsesCache extends JDBCObjectCache<MimerView, MimerObjectUses> {
        @NotNull
        @Override
        protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull MimerView owner) throws SQLException {
            return MimerObjectUses.prepareUsesStatement(session, owner.getSchema().getName(), owner.getName(), "VIEW");
        }

        @Override
        protected MimerObjectUses fetchObject(@NotNull JDBCSession session, @NotNull MimerView owner, @NotNull JDBCResultSet resultSet) throws SQLException, DBException {
            return new MimerObjectUses(owner, resultSet);
        }
    }
}
