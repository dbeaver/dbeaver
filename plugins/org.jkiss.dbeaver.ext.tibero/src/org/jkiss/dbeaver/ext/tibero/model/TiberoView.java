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
package org.jkiss.dbeaver.ext.tibero.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPRefreshableObject;
import org.jkiss.dbeaver.model.DBPScriptObject;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.rdb.DBSView;
import org.jkiss.utils.CommonUtils;

import java.sql.SQLException;
import java.util.Map;

public class TiberoView extends TiberoTableBase implements DBSView, DBPScriptObject, DBPRefreshableObject {

    private String viewText;

    public TiberoView(
        @NotNull TiberoSchema schema,
        @NotNull String name,
        @NotNull String tableType,
        @Nullable String description
    ) {
        super(schema, name, tableType, description);
    }

    @Override
    public boolean isView() {
        return true;
    }

    @NotNull
    @Override
    public String getObjectDefinitionText(
        @NotNull DBRProgressMonitor monitor,
        @Nullable Map<String, Object> options
    ) throws DBException {
        if (viewText == null || (options != null && Boolean.TRUE.equals(options.get(OPTION_REFRESH)))) {
            String body = loadViewText(monitor);
            if (CommonUtils.isEmpty(body)) {
                viewText = TiberoUtils.loadObjectDDL(monitor, this, "VIEW", getContainer().getName());
            } else {
                viewText = "CREATE OR REPLACE VIEW " +
                    DBUtils.getQuotedIdentifier(getDataSource(), getContainer().getName()) + "." +
                    DBUtils.getQuotedIdentifier(this) + " AS\n" +
                    body.trim();
            }
        }
        return viewText;
    }

    @Nullable
    private String loadViewText(@NotNull DBRProgressMonitor monitor) throws DBException {
        try (JDBCSession session = DBUtils.openMetaSession(monitor, this, "Load Tibero view definition")) {
            try (JDBCPreparedStatement dbStat = session.prepareStatement(
                "SELECT TEXT FROM ALL_VIEWS WHERE OWNER = ? AND VIEW_NAME = ?"
            )) {
                dbStat.setString(1, getContainer().getName());
                dbStat.setString(2, getName());
                try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                    if (dbResult.next()) {
                        return JDBCUtils.safeGetString(dbResult, "TEXT");
                    }
                }
            }
        } catch (SQLException e) {
            return null;
        }
        return null;
    }

    @Nullable
    @Override
    public DBSObject refreshObject(@NotNull DBRProgressMonitor monitor) throws DBException {
        viewText = null;
        return refreshTableObject(monitor);
    }
}
