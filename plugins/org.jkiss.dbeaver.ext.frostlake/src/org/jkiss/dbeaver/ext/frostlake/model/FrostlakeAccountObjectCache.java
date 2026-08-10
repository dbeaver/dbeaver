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
package org.jkiss.dbeaver.ext.frostlake.model;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCObjectCache;

import java.sql.SQLException;

/**
 * Loads an account-level kind — warehouses, roles, users.
 *
 * <p>A sibling of {@link FrostlakeObjectCache} rather than a parameter of it: {@code JDBCObjectCache} is
 * typed on the object that owns it, and these are owned by the data source, not by a schema. The
 * statement takes no scope either — {@code SHOW WAREHOUSES IN SCHEMA …} is a syntax error, because a
 * warehouse belongs to no schema.
 */
public class FrostlakeAccountObjectCache extends JDBCObjectCache<FrostlakeDataSource, FrostlakeObject> {

    private final FrostlakeObjectKind kind;

    public FrostlakeAccountObjectCache(@NotNull FrostlakeObjectKind kind) {
        if (kind.isSchemaScoped()) {
            throw new IllegalArgumentException(kind + " is schema-scoped; use FrostlakeObjectCache");
        }
        this.kind = kind;
    }

    @NotNull
    @Override
    protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session,
                                                    @NotNull FrostlakeDataSource dataSource) throws SQLException {
        return session.prepareStatement(kind.getAccountListQuery());
    }

    @Override
    protected FrostlakeObject fetchObject(@NotNull JDBCSession session,
                                          @NotNull FrostlakeDataSource dataSource,
                                          @NotNull JDBCResultSet resultSet) throws SQLException, DBException {
        return new FrostlakeObject(dataSource, dataSource, kind, resultSet);
    }
}
