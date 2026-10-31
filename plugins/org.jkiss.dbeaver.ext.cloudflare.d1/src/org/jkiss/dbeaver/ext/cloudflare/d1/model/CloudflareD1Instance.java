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
package org.jkiss.dbeaver.ext.cloudflare.d1.model;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCExecutionContext;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCRemoteInstance;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

final class CloudflareD1Instance extends JDBCRemoteInstance {
    private final String databaseId;

    CloudflareD1Instance(@NotNull CloudflareD1DataSource dataSource, @NotNull String databaseId) {
        super(dataSource);
        this.databaseId = databaseId;
    }

    @NotNull
    String getDatabaseId() {
        return this.databaseId;
    }

    @NotNull
    @Override
    public String getName() {
        return this.databaseId;
    }

    @NotNull
    @Override
    public synchronized JDBCExecutionContext getDefaultContext(@NotNull DBRProgressMonitor monitor, boolean meta) {
        if (this.executionContext == null) {
            this.executionContext = new CloudflareD1ExecutionContext(this, "Main");
        }
        return this.executionContext;
    }
}
