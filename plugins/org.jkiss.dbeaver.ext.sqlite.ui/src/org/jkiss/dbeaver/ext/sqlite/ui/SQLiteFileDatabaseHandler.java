/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2025 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.sqlite.ui;

import org.jkiss.api.DriverReference;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.ext.sqlite.SQLiteUtils;
import org.jkiss.dbeaver.ui.actions.AbstractFileDatabaseHandler;

import java.nio.file.Path;
import java.util.List;

/**
 * SQLite file handler
 */
public class SQLiteFileDatabaseHandler extends AbstractFileDatabaseHandler {

    @Override
    protected String getDatabaseTerm() {
        return "sqlite file";
    }

    @Override
    protected String createDatabaseName(@NotNull List<Path> fileList) {
        return fileList.isEmpty() ? "" : fileList.get(0).toString();
    }

    @Override
    protected String createConnectionName(List<Path> fileList) {
        return createDatabaseName(fileList);
    }

    @Override
    protected DriverReference getDriverReference() {
        return SQLiteUtils.DRIVER_REFERENCE;
    }

    @Override
    protected boolean isSingleDatabaseConnection() {
        return false;
    }
}
