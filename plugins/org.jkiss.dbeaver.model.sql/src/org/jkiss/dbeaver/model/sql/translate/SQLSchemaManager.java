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

import org.osgi.framework.Version;

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

    private File schemaCreateScriptPath;
    private String schemaUpdateScriptPrefix;

    private String targetDatabaseURL;
    private String targetDatabaseUser;
    private String targetDatabasePassword;

    private String targetSchemaName;
    private String versionTableName;

    private Version obsoleteVersionNumber;

}
