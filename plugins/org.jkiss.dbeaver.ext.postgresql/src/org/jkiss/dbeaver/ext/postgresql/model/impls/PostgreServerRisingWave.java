/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.postgresql.model.impls;

import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreDataSource;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreTableBase;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreViewBase;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

public class PostgreServerRisingWave extends PostgreServerExtensionBase {

    private static final Log log = Log.getLog(PostgreServerRisingWave.class);

    public PostgreServerRisingWave(PostgreDataSource dataSource) {
        super(dataSource);
    }

    @Override
    public String getServerTypeName() {
        return "RisingWave";
    }

    @Override
    public boolean supportsForeignKeys() {
        return false;
    }

    @Override
    public boolean supportsMaterializedViews() {
        return true;
    }

    @Override
    public boolean supportsPartitions() {
        return false;
    }

    @Override
    public boolean supportsInheritance() {
        return false;
    }

    @Override
    public boolean supportsTriggers() {
        return false;
    }

    @Override
    public boolean supportsDependencies() {
        return false;
    }

    @Override
    public boolean supportsFunctionCreate() {
        return false;
    }

    @Override
    public boolean supportsRules() {
        return false;
    }

    @Override
    public boolean supportsExtensions() {
        return false;
    }

    @Override
    public boolean supportsEncodings() {
        return false;
    }

    @Override
    public boolean supportsLanguages() {
        return false;
    }

    @Override
    public boolean supportsTablespaces() {
        return false;
    }

    @Override
    public boolean supportsSequences() {
        return false;
    }

    @Override
    public boolean supportsRoles() {
        return true;
    }

    @Override
    public boolean supportsSessionActivity() {
        return true;
    }

    @Override
    public boolean supportsLocks() {
        return false;
    }

    @Override
    public boolean supportsForeignServers() {
        return false;
    }

    @Override
    public boolean supportsAggregates() {
        return false;
    }

    @Override
    public boolean supportsRelationSizeCalc() {
        return false;
    }

    @Override
    public boolean supportsFunctionDefRead() {
        return false;
    }

    @Override
    public boolean supportsExplainPlan() {
        return true;
    }

    @Override
    public boolean supportsExplainPlanVerbose() {
        return false;
    }

    @Override
    public boolean supportsTablespaceLocation() {
        return false;
    }

    @Override
    public boolean supportsExplainPlanXML() {
        return false;
    }

    @Override
    public boolean supportsTemplates() {
        return false;
    }

    @Override
    public boolean supportsTableStatistics() {
        return false;
    }

    @Override
    public boolean supportsStoredProcedures() {
        return false;
    }

    @Override
    public boolean supportsPGConstraintExpressionColumn() {
        return false;
    }

    @Override
    public boolean supportsColumnsRequiring() {
        return false;
    }

    @Override
    public String readTableDDL(DBRProgressMonitor monitor, PostgreTableBase table) {
        try (JDBCSession session = DBUtils.openMetaSession(monitor, table, "Load table DDL")) {
            try (JDBCPreparedStatement dbStat = session.prepareStatement("SHOW CREATE TABLE " +
                table.getFullyQualifiedName(DBPEvaluationContext.DDL))
            ) {
                try (JDBCResultSet resultSet = dbStat.executeQuery()) {
                    if (resultSet.next()) {
                        return JDBCUtils.safeGetString(resultSet, 2);
                    } else {
                        return null;
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Can't get native DDL. Use generated one.", e);
            return null;
        }
    }

    @Override
    public String readViewDDL(DBRProgressMonitor monitor, PostgreViewBase view) {
        try (JDBCSession session = DBUtils.openMetaSession(monitor, view, "Load view DDL")) {
            try (JDBCPreparedStatement dbStat = session.prepareStatement("SHOW CREATE " + view.getTableTypeName() + " " +
                view.getFullyQualifiedName(DBPEvaluationContext.DDL))
            ) {
                try (JDBCResultSet resultSet = dbStat.executeQuery()) {
                    if (resultSet.next()) {
                        return JDBCUtils.safeGetString(resultSet, 2);
                    } else {
                        return null;
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Can't get native DDL. Use generated one.", e);
            return null;
        }
    }

    @Override
    public boolean supportsNativeClient() {
        return false;
    }
}
