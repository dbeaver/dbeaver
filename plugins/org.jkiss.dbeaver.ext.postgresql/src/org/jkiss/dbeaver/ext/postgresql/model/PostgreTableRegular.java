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
package org.jkiss.dbeaver.ext.postgresql.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBPReferentialIntegrityController;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * PostgreTableRegular
 */
public class PostgreTableRegular extends PostgreTable implements DBPReferentialIntegrityController {
    public PostgreTableRegular(PostgreSchema catalog)
    {
        super(catalog);
    }

    private static final String DISABLE_REFERENTIAL_INTEGRITY_STATEMENT = "ALTER TABLE ? DISABLE TRIGGER ALL";
    private static final String ENABLE_REFERENTIAL_INTEGRITY_STATEMENT = "ALTER TABLE ? ENABLE TRIGGER ALL";

    public PostgreTableRegular(DBRProgressMonitor monitor, PostgreSchema catalog, PostgreTableRegular source) throws DBException {
        super(monitor, catalog, source, false);
    }

    public PostgreTableRegular(PostgreSchema catalog, ResultSet dbResult) {
        super(catalog, dbResult);
    }

    @Override
    public boolean supportsChangingReferentialIntegrity(@NotNull DBRProgressMonitor monitor) {
        return getDataSource().getServerType().supportsDisablingAllTriggers();
    }

    @Override
    public void enableReferentialIntegrity(@NotNull DBRProgressMonitor monitor, boolean enable) throws DBException {
        String sql = getChangeReferentialIntegrityStatement(monitor, enable);
        if (sql == null) {
            return;
        }
        sql = sql.replace("?", getFullyQualifiedName(DBPEvaluationContext.DDL));
        try {
            DBUtils.executeInMetaSession(monitor, this, "Changing referential integrity", sql);
        } catch (SQLException e) {
            throw new DBException("Unable to change referential integrity", e);
        }
    }

    @Nullable
    @Override
    public String getChangeReferentialIntegrityStatement(@NotNull DBRProgressMonitor monitor, boolean enable) {
        if (!supportsChangingReferentialIntegrity(monitor)) {
            return null;
        }
        if (enable) {
            return ENABLE_REFERENTIAL_INTEGRITY_STATEMENT;
        }
        return DISABLE_REFERENTIAL_INTEGRITY_STATEMENT;
    }
}
