/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
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

package org.jkiss.dbeaver.ext.postgresql.model.impls.yellowbrick;

import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreTableBase;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

/**
 * YellowBrickUtils
 */
public class YellowBrickUtils {

    private static final Log log = Log.getLog(YellowBrickUtils.class);
    
    private static final int UNKNOWN_LENGTH = -1;

    public static String extractTableDDL(DBRProgressMonitor monitor, PostgreTableBase tableBase)
    {
        try (JDBCSession session = DBUtils.openMetaSession(monitor, tableBase, "Load Yellowbrick DDL")) {
            try (JDBCPreparedStatement dbStat = session.prepareStatement("describe " + tableBase.getFullyQualifiedName(DBPEvaluationContext.DDL) + " only ddl")) {
                try (JDBCResultSet resultSet = dbStat.executeQuery()) {
                    StringBuilder sql = new StringBuilder();
                    boolean ddlStarted = false;
                    while (resultSet.next()) {
                        String line = resultSet.getString(1);
                        if (line == null) {
                            continue;
                        }
                        if (!ddlStarted) {
                            if (line.startsWith("CREATE ")) {
                                ddlStarted = true;
                            } else {
                                continue;
                            }
                        }
                        if (sql.length() > 0) sql.append("\n");
                        sql.append(line);
                    }
                    String ddl = sql.toString();
                    if (ddl.endsWith(";")) {
                        ddl = ddl.substring(0, ddl.length() - 1);
                    }
                    return ddl;
                }
            }
        } catch (Exception e) {
            log.debug("Error generating Yellowbrick DDL", e);
            return null;
        }
    }

}
