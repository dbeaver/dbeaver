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
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCObjectCache;

import java.sql.SQLException;

/**
 * Loads one {@link FrostlakeObjectKind} for a schema by running its SHOW command.
 *
 * <p>One cache instance per (schema, kind), so expanding Stages does not fetch Tasks, and each folder
 * refreshes independently.
 */
public class FrostlakeObjectCache extends JDBCObjectCache<FrostlakeSchema, FrostlakeObject> {

    private final FrostlakeObjectKind kind;

    public FrostlakeObjectCache(@NotNull FrostlakeObjectKind kind) {
        this.kind = kind;
    }

    @NotNull
    @Override
    protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session,
                                                    @NotNull FrostlakeSchema schema) throws SQLException {
        // SHOW takes no bind parameters, so the scope is spelled into the statement. Both identifiers
        // come from the catalog, not from user input.
        return session.prepareStatement(kind.getSchemaListQuery(
            DBUtils.getFullyQualifiedName(schema.getDataSource(),
                schema.getCatalog().getName(), schema.getName())));
    }

    @Override
    protected FrostlakeObject fetchObject(@NotNull JDBCSession session,
                                          @NotNull FrostlakeSchema schema,
                                          @NotNull JDBCResultSet resultSet) throws SQLException, DBException {
        return new FrostlakeObject(schema, schema.getDataSource(), kind, resultSet);
    }
}
