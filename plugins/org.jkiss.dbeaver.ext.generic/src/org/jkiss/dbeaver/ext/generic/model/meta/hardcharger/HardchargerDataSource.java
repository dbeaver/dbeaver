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
package org.jkiss.dbeaver.ext.generic.model.meta.hardcharger;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.generic.model.GenericDataSource;
import org.jkiss.dbeaver.ext.generic.model.GenericSQLDialect;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.exec.DBCQueryTransformProviderExt;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLQuery;

/** Uses SQL limits for Hardcharger's forward-only result streams, including in the SQL editor. */
public class HardchargerDataSource extends GenericDataSource implements DBCQueryTransformProviderExt {

    public HardchargerDataSource(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBPDataSourceContainer container,
        @NotNull HardchargerMetaModel metaModel
    ) throws DBException {
        super(monitor, container, metaModel, new GenericSQLDialect());
    }

    @Override
    public boolean isForceTransform(@NotNull DBCSession session, @NotNull SQLQuery sqlQuery) {
        return true;
    }

    @Override
    public boolean isLimitApplicableTo(@NotNull SQLQuery query) {
        // QueryTransformerLimit handles SELECTs and preserves an explicit limit or non-SELECT query.
        return true;
    }
}
