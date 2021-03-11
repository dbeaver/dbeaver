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
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.postgresql.internal.PostgreMessages;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBPReferentialIntegrityController;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;
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
        if (!supportsChangingReferentialIntegrity(monitor)) {
            throw new DBException("Changing referential integrity is not supported");
        }

        try (JDBCSession session = DBUtils.openMetaSession(monitor, this, "Disabling referential integrity")) {
            try (JDBCStatement statement = session.createStatement()) {
                StringBuilder sql = new StringBuilder("ALTER TABLE ");
                sql.append(getFullyQualifiedName(DBPEvaluationContext.DDL)).append(" ");
                if (enable) {
                    sql.append("ENABLE ");
                } else {
                    sql.append("DISABLE ");
                }
                sql.append("TRIGGER ALL");
                statement.execute(sql.toString());
            } catch (SQLException e) {
                throw new DBException("Unable to change referential integrity", e);
            }
        }
    }

    @NotNull
    @Override
    public String getReferentialIntegrityDisableWarning(@NotNull DBRProgressMonitor monitor) {
        if (supportsChangingReferentialIntegrity(monitor)) {
            return PostgreMessages.postgre_referential_integrity_disable_warning;
        }
        return "";
    }
}
