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
package org.jkiss.dbeaver.ext.doris.edit;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.generic.edit.GenericTableManager;
import org.jkiss.dbeaver.ext.generic.model.GenericTableBase;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

import java.sql.SQLException;
import java.util.Map;

/**
 * Doris table manager.
 */
public class DorisTableManager extends GenericTableManager {

    private static final Log log = Log.getLog(DorisTableManager.class);

    private static final String DEFAULT_DISTRIBUTION = "DISTRIBUTED BY RANDOM BUCKETS 1"; //$NON-NLS-1$
    private static final String SINGLE_BACKEND_PROPERTIES = "PROPERTIES (\"replication_num\" = \"1\")"; //$NON-NLS-1$

    @Override
    protected void appendTableModifiers(
        @NotNull DBRProgressMonitor monitor,
        @NotNull GenericTableBase table,
        @NotNull NestedObjectCommand tableProps,
        @NotNull StringBuilder ddl,
        boolean alter,
        @NotNull Map<String, Object> options
    ) {
        String delimiter = getDelimiter(options);
        ddl.append(delimiter).append(DEFAULT_DISTRIBUTION);
        if (hasSingleBackend(monitor, table)) {
            ddl.append(delimiter).append(SINGLE_BACKEND_PROPERTIES);
        }
    }

    private static boolean hasSingleBackend(@NotNull DBRProgressMonitor monitor, @NotNull GenericTableBase table) {
        try (JDBCSession session = DBUtils.openMetaSession(monitor, table, "Read Doris backends");
             JDBCPreparedStatement statement = session.prepareStatement("SHOW BACKENDS"); //$NON-NLS-1$
             JDBCResultSet resultSet = statement.executeQuery()) {
            int backendCount = 0;
            while (resultSet.next()) {
                if (++backendCount > 1) {
                    return false;
                }
            }
            return backendCount == 1;
        } catch (DBException | SQLException e) {
            log.debug("Unable to determine Doris backend count", e); //$NON-NLS-1$
            return false;
        }
    }
}
