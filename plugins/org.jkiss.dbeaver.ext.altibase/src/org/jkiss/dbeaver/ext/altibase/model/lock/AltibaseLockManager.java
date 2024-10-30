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
package org.jkiss.dbeaver.ext.altibase.model.lock;

import org.jkiss.dbeaver.DBDatabaseException;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.altibase.model.AltibaseDataSource;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.admin.locks.DBAServerLockManager;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.impl.admin.locks.LockGraphManager;

import java.sql.SQLException;
import java.util.*;

public class AltibaseLockManager extends LockGraphManager implements DBAServerLockManager<AltibaseLock, AltibaseLockItem> {

    public static final String sidHold = "hsid";
    public static final String sidWait = "wsid";

    private static final String LOCK_QUERY = "SELECT"
            + " w.id AS w_sid"
            + " ,w.client_pid AS w_pid"
            + " ,w.trans_id AS w_txid"
            + " ,w.db_username AS w_user_name"
            + " ,wobj.schema_name AS w_obj_schema"
            + " ,wobj.obj_name AS w_obj_name"
            + " ,wobj.stmt AS w_query"
            + " ,h.id AS h_sid"
            + " ,h.client_pid AS h_pid"
            + " ,h.trans_id AS h_txid"
            + " ,h.db_username AS h_user_name"
            + " ,stmt.query AS h_query"
        + " FROM"
            + " v$lock_wait vlw LEFT OUTER JOIN ("
                + " SELECT"
                        + " *"
                    + " FROM"
                        + " v$session"
            + " ) w"
                + " ON vlw.trans_id = w.trans_id LEFT OUTER JOIN ("
                + " SELECT"
                        + " u.user_name AS schema_name"
                        + " ,t.table_name AS obj_name"
                        + " ,ls.query AS stmt"
                        + " ,l.trans_id"
                    + " FROM"
                        + " v$lock l"
                        + " ,v$lock_statement ls"
                        + " ,system_.sys_tables_ t"
                        + " ,system_.sys_users_ u"
                    + " WHERE"
                        + " t.table_oid = l.table_oid"
                        + " AND u.user_id = t.user_id"
                        + " AND l.trans_id = ls.tx_id"
            + " ) wobj"
                + " ON vlw.trans_id = wobj.trans_id LEFT OUTER JOIN ("
                + " SELECT"
                        + " ss.*"
                    + " FROM"
                        + " v$session ss"
                        + " ,v$lock l"
                    + " WHERE"
                        + " ss.trans_id = l.trans_id"
                        + " AND l.is_grant = 1"
            + " ) h"
                + " ON vlw.wait_for_trans_id = h.trans_id LEFT OUTER JOIN ("
                + " SELECT "
                    + " s.* "
                + " FROM "
                    + " v$statement s"
            + " ) stmt"
                + " ON h.id = stmt.session_id";

    private static final String LOCK_ITEM_QUERY = "SELECT "
            + " lock_desc, lock_cnt, tbs_id, table_oid, dbf_id,"
            + " CASE2 (ls.lock_item_type  = 'TBL','TABLE',ls.lock_item_type) AS locked_obj_type,"
            + " tbs.name AS tbs_name"
            + " FROM "
            + " v$lock_statement ls"
            + " LEFT OUTER JOIN (SELECT * FROM v$tablespaces) tbs ON tbs.id = ls.tbs_id"
            + " WHERE "
            + " session_id = ?";

    private final AltibaseDataSource dataSource;

    public AltibaseLockManager(AltibaseDataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public DBPDataSource getDataSource() {
        return dataSource;
    }

    @Override
    public Map<Object, AltibaseLock> getLocks(DBCSession session, Map<String, Object> options) throws DBException {
        try {
            Map<Object, AltibaseLock> locks = new HashMap<>(10);
            String sql = LOCK_QUERY;

            try (JDBCPreparedStatement dbStat = ((JDBCSession) session).prepareStatement(sql)) {
                try (JDBCResultSet dbResult = dbStat.executeQuery()) {

                    while (dbResult.next()) {
                        AltibaseLock l = new AltibaseLock(dbResult, dataSource);
                        locks.put(l.getId(), l);
                    }
                }
            }

            super.buildGraphs(locks);
            return locks;

        } catch (SQLException e) {
            throw new DBDatabaseException(e, session.getDataSource());
        }
    }

    @Override
    public void alterSession(DBCSession session, AltibaseLock lock, Map<String, Object> options) throws DBException {
        try {
            String sql = String.format("ALTER DATABASE %s SESSION CLOSE %s",
                    dataSource.getDbName((JDBCSession) session), lock.getWait_sid());

            try (JDBCPreparedStatement dbStat = ((JDBCSession) session).prepareStatement(sql)) {
                dbStat.execute();
            }
        } catch (SQLException e) {
            throw new DBDatabaseException(e, session.getDataSource());
        }
    }

    @Override
    public Class<AltibaseLock> getLocksType() {
        return AltibaseLock.class;
    }

    @Override
    public Collection<AltibaseLockItem> getLockItems(DBCSession session, Map<String, Object> options)
            throws DBException {
        try {
            List<AltibaseLockItem> locks = new ArrayList<>();
            String sql = LOCK_ITEM_QUERY;

            try (JDBCPreparedStatement dbStat = ((JDBCSession) session).prepareStatement(sql)) {
                String otype = (String) options.get(LockGraphManager.keyType);

                switch (otype) {
                    case LockGraphManager.typeWait:
                        dbStat.setInt(1, (int) options.get(sidWait));
                        break;
                    case LockGraphManager.typeHold:
                        dbStat.setInt(1, (int) options.get(sidHold));
                        break;
                    default:
                        return locks;
                }

                try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                    while (dbResult.next()) {
                        locks.add(new AltibaseLockItem(dbResult));
                    }
                }
            }
            return locks;
        } catch (SQLException e) {
            throw new DBDatabaseException(e, session.getDataSource());
        }
    }
}