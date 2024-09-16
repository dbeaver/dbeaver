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

package org.jkiss.dbeaver.model.sql.schema;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * SQL schema version manager.
 */
public interface SQLSchemaVersionManager {

    /**
     * Returns current schema version.
     * Returns -1 if schema doesn't exist
     */
    int getCurrentSchemaVersion(DBRProgressMonitor monitor, Connection connection, String schemaName) throws DBException, SQLException;

    /**
     * Returns an actual schema version
     */
    int getLatestSchemaVersion();

    /**
     * Updates current schema version
     */
    void updateCurrentSchemaVersion(
        DBRProgressMonitor monitor,
        @NotNull Connection connection,
        @NotNull String schemaName,
        int version
    ) throws DBException, SQLException;

    default void fillInitialSchemaData(DBRProgressMonitor monitor, Connection connection) throws DBException, SQLException {

    }

}
