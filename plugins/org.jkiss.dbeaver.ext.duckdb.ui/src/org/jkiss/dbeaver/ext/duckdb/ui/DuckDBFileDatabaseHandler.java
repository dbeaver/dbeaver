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
import org.jkiss.dbeaver.ui.actions.AbstractFileDatabaseHandler;

import java.nio.file.Path;
import java.util.List;

/**
 * DuckDB file handler
 */
public class DuckDBFileDatabaseHandler extends AbstractFileDatabaseHandler {

    @Override
    protected String getDatabaseTerm() {
        return "duckdb";
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
}
