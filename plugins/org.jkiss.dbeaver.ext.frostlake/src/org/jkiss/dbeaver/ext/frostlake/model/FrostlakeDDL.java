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
package org.jkiss.dbeaver.ext.frostlake.model;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.utils.CommonUtils;

/**
 * DDL read back from the engine's own {@code GET_DDL}.
 *
 * <p>Worth preferring over DBeaver's reconstruction: Frostlake implements GET_DDL with Snowflake's
 * semantics and its output round-trips — re-running it reproduces the object. The generic model can
 * only rebuild DDL from JDBC metadata, which loses everything JDBC has no column for (clustering keys,
 * masking policies, transient/temporary, and the rest).
 */
public class FrostlakeDDL {

    private static final Log log = Log.getLog(FrostlakeDDL.class);

    private FrostlakeDDL() {
        // static use only
    }

    /**
     * {@code GET_DDL('<type>', '<qualified name>')}, or {@code fallback} when the engine cannot answer
     * — an older Frostlake, or an object type GET_DDL does not cover.
     */
    public static String readObjectDDL(@NotNull DBRProgressMonitor monitor,
                                       @NotNull DBSObject object,
                                       @NotNull String objectType,
                                       String fallback) {
        try (JDBCSession session = DBUtils.openMetaSession(monitor, object, "Read Frostlake object DDL")) {
            try (JDBCPreparedStatement statement = session.prepareStatement("SELECT GET_DDL(?, ?)")) {
                statement.setString(1, objectType);
                statement.setString(2, DBUtils.getObjectFullName(object, org.jkiss.dbeaver.model.DBPEvaluationContext.DDL));
                try (JDBCResultSet resultSet = statement.executeQuery()) {
                    if (resultSet.next()) {
                        final String ddl = resultSet.getString(1);
                        if (!CommonUtils.isEmpty(ddl)) {
                            return ddl;
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.debug("GET_DDL unavailable for " + object.getName() + ", using generated DDL", e);
        }
        return fallback;
    }
}
