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

package org.jkiss.dbeaver.model.sql.schema;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.impl.preferences.SimplePreferenceStore;
import org.jkiss.dbeaver.model.impl.sql.BasicSQLDialect;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLDialect;
import org.jkiss.dbeaver.model.sql.SQLModelPreferences;
import org.jkiss.dbeaver.model.sql.translate.SQLQueryTranslator;
import org.jkiss.dbeaver.utils.ContentUtils;
import org.jkiss.utils.IOUtils;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * SQL schema manager.
 * Upgrades schema version if needed.
 * Converts schema create/update scripts into target database dialect.
 *
 */
public final class SQLSchemaManager {
    private static final Log log = Log.getLog(SQLSchemaManager.class);

    private final String schemaId;
    private final SQLSchemaScriptSource scriptSource;
    private final SQLSchemaConnectionProvider connectionProvider;
    private final SQLSchemaVersionManager versionManager;

    private SQLDialect targetDatabaseDialect;
    private String targetDatabaseName;

    private int schemaVersionActual;
    private int schemaVersionObsolete;

    public SQLSchemaManager(
        String schemaId,
        SQLSchemaScriptSource scriptSource,
        SQLSchemaConnectionProvider connectionProvider,
        SQLSchemaVersionManager versionManager,
        SQLDialect targetDatabaseDialect,
        String targetDatabaseName,
        int schemaVersionActual,
        int schemaVersionObsolete)
    {
        this.schemaId = schemaId;

        this.scriptSource = scriptSource;
        this.connectionProvider = connectionProvider;
        this.versionManager = versionManager;
        this.targetDatabaseDialect = targetDatabaseDialect;
        this.targetDatabaseName = targetDatabaseName;

        this.schemaVersionActual = schemaVersionActual;
        this.schemaVersionObsolete = schemaVersionObsolete;
    }

    public void updateSchema(DBRProgressMonitor monitor) throws DBException {
        try {
            Connection dbCon = connectionProvider.getDatabaseConnection(monitor);
            boolean switchToAC = false;
            try {
                if (dbCon.getAutoCommit()) {
                    dbCon.setAutoCommit(false);
                    switchToAC = true;
                }
                int currentSchemaVersion = versionManager.getCurrentSchemaVersion(monitor, dbCon, targetDatabaseName);
                if (currentSchemaVersion < 0) {
                    createNewSchema(monitor, dbCon);
                } else if (schemaVersionObsolete > 0 && currentSchemaVersion < schemaVersionObsolete) {
                    dropSchema(monitor, dbCon);
                    createNewSchema(monitor, dbCon);
                } else if (schemaVersionActual > currentSchemaVersion) {
                    upgradeSchemaVersion(monitor, dbCon, currentSchemaVersion);
                }

                dbCon.commit();
            } catch (IOException e) {
                throw new DBException("IO error while updating " + schemaId + " schema version", e);
            }
            if (switchToAC) {
                dbCon.setAutoCommit(true);
            }
        }
        catch (SQLException e) {
            throw new DBException("Error updating " + schemaId + " schema version", e);
        }
    }

    private void upgradeSchemaVersion(DBRProgressMonitor monitor, Connection connection, int currentSchemaVersion) throws IOException, DBException, SQLException {
        for (int curVer = currentSchemaVersion; curVer < schemaVersionActual; curVer++) {
            int updateToVer = curVer + 1;
            Reader ddlStream = scriptSource.openSchemaUpdateScript(monitor, updateToVer);
            if (ddlStream != null) {
                log.debug("Update schema " + schemaId + " version from " + curVer + " to " + updateToVer);
                try {
                    executeScript(monitor, connection, ddlStream);
                } catch (Exception e) {
                    log.warn("Error updating " + schemaId + " schema version from " + curVer + " to " + updateToVer, e);
                } finally {
                    ContentUtils.close(ddlStream);
                }
                // Update schema version
                versionManager.updateCurrentSchemaVersion(
                    monitor, connection, targetDatabaseName, updateToVer);
            }
        }
    }

    private void createNewSchema(DBRProgressMonitor monitor, Connection connection) throws IOException, DBException, SQLException {
        log.debug("Create new schema " + schemaId);
        try (Reader ddlStream = scriptSource.openSchemaCreateScript(monitor)) {
            executeScript(monitor, connection, ddlStream);
        }
    }

    private void dropSchema(DBRProgressMonitor monitor, Connection connection) throws DBException, SQLException, IOException {
        log.debug("Drop schema " + schemaId);
        executeScript(monitor, connection, new StringReader("DROP ALL OBJECTS"));
    }

    private void executeScript(DBRProgressMonitor monitor, Connection connection, Reader ddlStream) throws SQLException, IOException, DBException {
        // Read DDL script
        String ddlText = IOUtils.readToString(ddlStream);

        // Translate script to target dialect
        DBPPreferenceStore prefStore = new SimplePreferenceStore() {
            @Override
            public void save() throws IOException {

            }
        };
        prefStore.setValue(SQLModelPreferences.SQL_FORMAT_FORMATTER, "default");

        BasicSQLDialect sourceDialect = new BasicSQLDialect() {

        };
        ddlText = SQLQueryTranslator.translateScript(sourceDialect, targetDatabaseDialect, prefStore, ddlText);

        String[] ddl = ddlText.split(";");
        for (String line : ddl) {
            line = line.trim();
            if (line.isEmpty()) {
                continue;
            }
            try (Statement dbStat = connection.createStatement()) {
                dbStat.execute(line);
            }
        }
    }

}
