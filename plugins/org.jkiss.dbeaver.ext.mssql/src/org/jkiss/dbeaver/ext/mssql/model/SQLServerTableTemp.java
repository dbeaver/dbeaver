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
package org.jkiss.dbeaver.ext.mssql.model;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.ext.mssql.SQLServerUtils;

import java.sql.ResultSet;

/**
 * Represents a user-created temporary table that resides in the {@code tempdb} database.
 */
public class SQLServerTableTemp extends SQLServerTable {
    private final String originalName;

    public SQLServerTableTemp(
        @NotNull SQLServerSchema catalog,
        @NotNull ResultSet dbResult,
        @NotNull String tempName,
        @NotNull String originalName
    ) {
        super(catalog, dbResult, tempName);
        this.originalName = originalName;
    }

    /**
     * Returns the original, stripped name of this temporary table.
     *
     * @see SQLServerUtils#stripTempdbTableName(String)
     */
    @NotNull
    public String getOriginalName() {
        return originalName;
    }
}
