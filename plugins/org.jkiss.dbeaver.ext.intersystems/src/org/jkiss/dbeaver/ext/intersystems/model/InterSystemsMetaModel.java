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
package org.jkiss.dbeaver.ext.intersystems.model;

import java.util.Map;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.generic.model.GenericView;
import org.jkiss.dbeaver.ext.generic.model.meta.GenericMetaModel;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.sql.format.SQLFormatUtils;

public class InterSystemsMetaModel extends GenericMetaModel
{
    private static final Log log = Log.getLog(InterSystemsMetaModel.class);

    public InterSystemsMetaModel() {
        super();
    }

    public String getViewDDL(
            @NotNull DBRProgressMonitor monitor,
            @NotNull GenericView view,
            @NotNull Map<String, Object> options) throws DBException {
        String sqlStatement = "SELECT View_Definition "
                + " FROM INFORMATION_SCHEMA.VIEWS "
                + "where TABLE_NAME=? and TABLE_SCHEMA=? ";
        try (JDBCSession session = DBUtils.openMetaSession(monitor, view, "Load source code")) {
            try (JDBCPreparedStatement dbStat = session.prepareStatement(sqlStatement)) {
                dbStat.setString(1, view.getName());
                dbStat.setString(2, view.getContainer().getSchema().getName());
                try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                    if (dbResult.nextRow()) {
                         String viewplain = dbResult.getString(1);
                         return SQLFormatUtils.formatSQL(view.getDataSource(), viewplain);
                    }
                }
            }
        } catch (Exception e) {
            throw new DBException("Can't read source code of '" + view.getFullyQualifiedName(DBPEvaluationContext.DDL) + "'", e);
        }
        return super.getViewDDL(monitor, view, options);
    }
}