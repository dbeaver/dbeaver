/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2021 DBeaver Corp and others
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

package org.jkiss.dbeaver.model.sql.translate;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLDialect;

import java.io.File;

/**
 * SQL schema manager.
 * Upgrades schema version if needed.
 * Converts schema create/update scripts into target database dialect.
 *
 */
public final class SQLSchemaManager {

    private final static String SCHEMA_VERSION_INFO_DDL =
        "CREATE TABLE {schema}DB_SCHEMA_VERSIONS (" +
            "SCHEMA_ID VARCHAR(32) NOT NULL," +
            "SCHEMA_VERSION INTEGER NOT NULL," +
            "UPDATE_TIME VARCHAR(32) NOT NULL)";

    private final String schemaId;

    private final File schemaCreateScriptPath;
    private final String schemaUpdateScriptPrefix;

    private SQLDialect targetDatabaseDialect;
    private String targetDatabaseURL;
    private String targetDatabaseUser;
    private String targetDatabasePassword;

    private String targetDatabaseName;

    private int schemaVersionStart;

    public SQLSchemaManager(String schemaId, File schemaCreateScriptPath, String schemaUpdateScriptPrefix) {
        this.schemaId = schemaId;
        this.schemaCreateScriptPath = schemaCreateScriptPath;
        this.schemaUpdateScriptPrefix = schemaUpdateScriptPrefix;
    }

    public void initTargetDatabase(
        SQLDialect targetDatabaseDialect,
        String targetDatabaseURL,
        String targetDatabaseUser,
        String targetDatabasePassword,
        String targetDatabaseName) {

        this.targetDatabaseDialect = targetDatabaseDialect;
        this.targetDatabaseURL = targetDatabaseURL;
        this.targetDatabaseUser = targetDatabaseUser;
        this.targetDatabasePassword = targetDatabasePassword;
        this.targetDatabaseName = targetDatabaseName;
    }

    public void setSchemaVersionStart(int schemaVersionStart) {
        this.schemaVersionStart = schemaVersionStart;
    }

    public void updateSchema(DBRProgressMonitor monitor) throws DBException {

    }

}
