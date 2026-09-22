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
package org.jkiss.dbeaver.ext.mimer.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.generic.model.GenericExecutionContext;
import org.jkiss.dbeaver.ext.generic.model.GenericSchema;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCExecutionPurpose;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCRemoteInstance;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

import java.sql.SQLException;

/**
 * {@code GenericExecutionContext} with a fallback for "Execute -&gt; Set Active Schema".
 * {@code Connection#setSchema()} isn't reliable against older Mimer SQL servers, so this falls
 * back to running a plain {@code SET SCHEMA} statement instead when the JDBC call fails.
 * <p>
 * That fallback statement only exists on Mimer SQL 11.0+ ({@link
 * MimerDataSource#supportsSetSchemaStatement}) - 10.1 has no equivalent, so schema switching
 * isn't offered there at all.
 *
 * @author Mimer Information Technology
 */
public class MimerExecutionContext extends GenericExecutionContext {

    private static final Log log = Log.getLog(MimerExecutionContext.class);

    public MimerExecutionContext(@NotNull JDBCRemoteInstance instance, @NotNull String purpose) {
        super(instance, purpose);
    }

    @Override
    public boolean supportsSchemaChange() {
        if (getDataSource() instanceof MimerDataSource ds && !ds.supportsSetSchemaStatement()) {
            // Pre-11.0: neither the JDBC API nor a SET SCHEMA statement
            // exists, so there's genuinely no way to change the active schema - don't offer the
            // picker at all, rather than let a later SET SCHEMA fallback fail with invalid SQL.
            return false;
        }
        return super.supportsSchemaChange();
    }

    @Override
    public void setDefaultSchema(@NotNull DBRProgressMonitor monitor, @Nullable GenericSchema schema) throws DBCException {
        if (schema == null || !(getDataSource() instanceof MimerDataSource ds)) {
            super.setDefaultSchema(monitor, schema);
            return;
        }
        try {
            super.setDefaultSchema(monitor, schema);
        } catch (DBCException e) {
            if (!ds.supportsSetSchemaStatement()) {
                // No fallback exists pre-11.0 - surface the real failure.
                throw e;
            }
            log.debug("Connection#setSchema() failed for '" + schema.getName() + "', falling back to a SET SCHEMA statement", e);
            try (JDBCSession session = openSession(monitor, DBCExecutionPurpose.UTIL, "Set active schema")) {
                try (JDBCPreparedStatement dbStat = session.prepareStatement(
                    "SET SCHEMA " + DBUtils.getQuotedIdentifier(schema))
                ) {
                    dbStat.execute();
                }
            } catch (SQLException e2) {
                throw new DBCException(e2, this);
            }
            // The base class's own selected-entity tracking was never updated (we bypassed its
            // private setDefaultSchema(String) entirely) - re-derive it the same way connect
            // does, firing the usual selection-change event if it actually changed. Best-effort:
            // the SET SCHEMA statement above already succeeded, so a failure here only means the
            // tree's "active schema" highlighting may lag, not that the switch itself failed.
            try {
                refreshDefaults(monitor, false);
            } catch (DBException e2) {
                log.debug("Error refreshing default schema after a SET SCHEMA fallback", e2);
            }
        }
    }
}
