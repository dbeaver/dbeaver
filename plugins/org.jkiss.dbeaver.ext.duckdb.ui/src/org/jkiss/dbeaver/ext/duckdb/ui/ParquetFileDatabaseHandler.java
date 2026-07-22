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
package org.jkiss.dbeaver.ext.duckdb.ui;

import org.jkiss.api.CompositeObjectId;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.model.connection.DBPDriverConfigurationType;
import org.jkiss.dbeaver.model.file.FileTypeAction;
import org.jkiss.dbeaver.ui.actions.AbstractFileDatabaseHandler;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Parquet file handler.
 * Parquet file is not a DuckDB database, so it is opened through an in-memory
 * DuckDB connection with a view over the file contents.
 */
public class ParquetFileDatabaseHandler extends AbstractFileDatabaseHandler {

    private static final String DUCKDB_IN_MEMORY_URL = "jdbc:duckdb:";

    @Override
    protected String getDatabaseTerm() {
        return "parquet file";
    }

    @Override
    protected String createDatabaseName(@NotNull List<Path> fileList) {
        return fileList.isEmpty() ? "" : fileList.getFirst().toString();
    }

    @Override
    protected String createConnectionName(@NotNull List<Path> fileList) {
        return createDatabaseName(fileList);
    }

    @Override
    protected CompositeObjectId getDriverReference() {
        return new CompositeObjectId("duckdb", "duckdb_jdbc");
    }

    @Override
    protected boolean isSingleDatabaseConnection() {
        return false;
    }

    @NotNull
    @Override
    public Set<FileTypeAction> supportedActions() {
        // Parquet is a binary format, opening it in a text editor makes no sense
        return Set.of(FileTypeAction.DATABASE);
    }

    @Override
    protected void configureConnection(@NotNull DBPConnectionConfiguration configuration, @NotNull List<Path> fileList) {
        configuration.setConfigurationType(DBPDriverConfigurationType.URL);
        configuration.setUrl(DUCKDB_IN_MEMORY_URL);
        // Bootstrap queries are executed for every new execution context,
        // so the view exists in each in-memory database instance
        List<String> initQueries = new ArrayList<>();
        for (Path file : fileList) {
            initQueries.add(
                "CREATE OR REPLACE VIEW \"" + getViewName(file).replace("\"", "\"\"") +
                    "\" AS SELECT * FROM read_parquet('" + file.toString().replace("'", "''") + "')");
        }
        configuration.getBootstrap().setInitQueries(initQueries);
    }

    @NotNull
    private static String getViewName(@NotNull Path file) {
        String fileName = file.getFileName().toString();
        int divPos = fileName.lastIndexOf('.');
        return divPos <= 0 ? fileName : fileName.substring(0, divPos);
    }
}
