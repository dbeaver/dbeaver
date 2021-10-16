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
package org.jkiss.dbeaver.ext.postgresql.tasks;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreServerExtension;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreTableBase;
import org.jkiss.dbeaver.ext.postgresql.model.impls.PostgreServerExtensionBase;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.exec.DBCTransactionManager;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistAction;
import org.jkiss.utils.CommonUtils;

import java.util.List;

public class PostgreToolTableTruncate extends PostgreToolWithStatus<PostgreTableBase, PostgreToolTableTruncateSettings> {

    private static final Log log = Log.getLog(PostgreToolTableTruncate.class);

    @NotNull
    @Override
    public PostgreToolTableTruncateSettings createToolSettings() {
        return new PostgreToolTableTruncateSettings();
    }

    @Override
    public void generateObjectQueries(DBCSession session, PostgreToolTableTruncateSettings settings, List<DBEPersistAction> queries, PostgreTableBase table) throws DBCException {
        if (settings.isRunning()) {
            commitChanges(session);
        }
        PostgreServerExtension serverType = table.getDataSource().getServerType();
        String sql = "TRUNCATE TABLE";
        if (settings.isOnly() && CommonUtils.isBitSet(serverType.getTruncateToolModes(), PostgreServerExtensionBase.TRUNCATE_TOOL_MODE_SUPPORT_ONLY_ONE_TABLE)) sql += " ONLY";
        sql += " " + table.getFullyQualifiedName(DBPEvaluationContext.DDL);
        if (CommonUtils.isBitSet(serverType.getTruncateToolModes(), PostgreServerExtensionBase.TRUNCATE_TOOL_MODE_SUPPORT_IDENTITIES)) {
            if (settings.isRestarting())
                sql += " RESTART IDENTITY";
            else
                sql += " CONTINUE IDENTITY";
        }
        if (CommonUtils.isBitSet(serverType.getTruncateToolModes(), PostgreServerExtensionBase.TRUNCATE_TOOL_MODE_SUPPORT_CASCADE)) {
            if (settings.isCascading())
                sql += " CASCADE";
            else
                sql += " RESTRICT";
        }
        queries.add(new SQLDatabasePersistAction(sql));
    }

    private void commitChanges(DBCSession session) {
        try {
            DBCTransactionManager txnManager = DBUtils.getTransactionManager(session.getExecutionContext());
            if (txnManager != null && !txnManager.isAutoCommit()) {
                txnManager.commit(session);
            }
        } catch (Throwable e) {
            log.error("Error committing transactions", e);
        }
    }

    public boolean needsRefreshOnFinish() {
        return true;
    }
}
