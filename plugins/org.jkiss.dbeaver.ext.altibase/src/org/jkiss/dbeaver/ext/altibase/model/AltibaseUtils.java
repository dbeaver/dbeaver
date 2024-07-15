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
package org.jkiss.dbeaver.ext.altibase.model;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.altibase.AltibaseConstants;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectLazy;
import org.jkiss.dbeaver.model.struct.cache.DBSObjectCache;

import java.sql.SQLException;

public class AltibaseUtils {

    private static final Log log = Log.getLog(AltibaseUtils.class);

    static <PARENT extends DBSObject> Object resolveLazyReference(
            DBRProgressMonitor monitor,
            PARENT parent,
            DBSObjectCache<PARENT, ?> cache,
            DBSObjectLazy<?> referrer,
            Object propertyId) throws DBException {

        final Object reference = referrer.getLazyReference(propertyId);

        if (reference instanceof String) {
            Object object;
            if (monitor != null) {
                object = cache.getObject(
                        monitor,
                        parent,
                        (String) reference);
            } else {
                object = cache.getCachedObject((String) reference);
            }

            if (object != null) {
                return object;
            } else {
                log.warn("Object '" + reference + "' not found");
                return reference;
            }
        } else {
            return reference;
        }
    }
    
    /*
     * Generic function to get view/mview status: valid or invalid
     */
    static boolean getViewStatus(JDBCSession session, String tableType, String schemaName, String objName) throws DBCException {

        boolean isValid = false;
        String qry = null;

        // Materialized View
        if (AltibaseConstants.OBJ_TYPE_MATERIALIZED_VIEW.equals(tableType)) { 
            qry = "SELECT v.status FROM system_.sys_users_ u, system_.sys_materialized_views_ m, system_.sys_views_ v"
                    + " WHERE u.user_id = m.user_id AND u.user_id = v.user_id AND v.view_id = m.view_id"
                    + " AND u.user_name = ? AND m.mview_name = ?";
        // View
        } else {
            qry = "SELECT v.status FROM system_.sys_users_ u, system_.sys_tables_ t, system_.sys_views_ v"
                    + " WHERE u.user_id = t.user_id AND u.user_id = v.user_id AND v.view_id = t.table_id"
                    + " AND u.user_name = ? AND t.table_name = ?";
        }

        try (JDBCPreparedStatement dbStat = session.prepareStatement(qry)) {
            dbStat.setString(1, schemaName);
            dbStat.setString(2, objName);

            dbStat.executeStatement();

            try (JDBCResultSet dbResult = dbStat.getResultSet()) {
                if (dbResult != null && dbResult.next()) {
                    isValid = JDBCUtils.safeGetBoolean(dbResult, 1, "0"); // 0 is Valid, 1 is invalid
                }
            }
        } catch (SQLException e) {
            throw new DBCException(e, session.getExecutionContext());
        }

        return isValid;
    }
}
