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
package org.jkiss.dbeaver.ext.greptime.model;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.ext.generic.model.GenericSQLDialect;
import org.jkiss.dbeaver.model.sql.SQLDialect;

public class GreptimeDialect extends GenericSQLDialect {

    public GreptimeDialect() {
        super("GreptimeDB", "greptime");
    }

    @NotNull
    @Override
    public String getDialectName() {
        return "GreptimeDB";
    }

    @Override
    public boolean supportsAliasInSelect() {
        return true;
    }

    @Override
    public boolean supportsIndexCreateAndDrop() {
        return false;
    }

    @Override
    public int getCatalogUsage() {
        return SQLDialect.USAGE_ALL;
    }

    @Override
    public int getSchemaUsage() {
        return SQLDialect.USAGE_NONE;
    }
}
