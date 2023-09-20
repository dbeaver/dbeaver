/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2017 Andrew Khitrin (ahitrin@gmail.com) 
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
package org.jkiss.dbeaver.ext.yashandb.model.lock;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.yashandb.model.YashanDBDataSource;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.admin.locks.DBAServerLockManager;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.impl.admin.locks.LockGraphManager;

import java.sql.SQLException;
import java.util.*;

public class YashanDBLockManager extends LockGraphManager implements DBAServerLockManager<YashanDBLock, YashanDBLockItem> {

    public static final String sidHold = "hsid";
    public static final String sidWait = "wsid";

    private static final String LOCK_QUERY = "SELECT " +
            "wsession.sid waiting_session, " +
            "wsession.serial# serial, " +
            "wsession.logon_time, " +
            "wsession.status, " +
            "wsession.wait_event, " +
            "wsession.cli_osuser wcli_osuser, " +
            "wprocess.thread_id wait_pid, " +
            "nvl(obj.object_name, '-') oname, " +
            "nvl(obj.owner, '-') owner, " +
            "hsession.sid holding_session, " +
            "hprocess.thread_id hold_pid, " +
            "hsession.cli_osuser hcli_osuser " +
            "FROM " +
            "v$session wsession " +
            "JOIN v$lock loc ON " +
            "loc.sid = wsession.sid " +
            "JOIN v$process wprocess ON " +
            "wprocess.thread_addr = wsession.paddr " +
            "JOIN v$session hsession ON " +
            "hsession.xid = loc.id1 " +
            "JOIN v$process hprocess ON " +
            "hprocess.thread_addr = hsession.paddr " +
            "JOIN v$transaction tran ON " +
            "tran.xid = wsession.xid " +
            "JOIN v$locked_object locobj ON " +
            "locobj.xext = tran.xext " +
            "JOIN dba_objects obj ON " +
            "obj.object_id = locobj.object_id " +
            "WHERE " +
            "loc.request IS NOT NULL " +
            "AND loc.request NOT IN ('ts', 'tx') " +
            "UNION " +
            "SELECT  " +
            "  wsession.sid waiting_session, " +
            "  wsession.serial# serial, " +
            "  wsession.logon_time, " +
            "  wsession.status, " +
            "  wsession.wait_event, " +
            "  wsession.cli_osuser, " +
            "  wprocess.thread_id wait_pid, " +
            "  nvl(obj.object_name, '-') oname, " +
            "  nvl(obj.owner, '-') owner, " +
            "0, " +
            "0, " +
            "NULL " +
            "FROM " +
            "v$session wsession " +
            "JOIN v$lock loc ON " +
            "loc.sid = wsession.sid " +
            "JOIN v$process wprocess ON " +
            "wprocess.thread_addr = wsession.paddr " +
            "JOIN v$transaction tran ON " +
            "tran.xid = wsession.xid " +
            "JOIN v$locked_object locobj ON " +
            "locobj.xext = tran.xext " +
            "JOIN dba_objects obj ON " +
            "obj.object_id = locobj.object_id " +
            "WHERE " +
            "loc.lmode IS NOT NULL " +
            "AND loc.lmode NOT IN ('ts', 'tx') " +
            "UNION  " +
            "SELECT  " +
            " wsession.sid waiting_session, " +
            "  wsession.serial# serial, " +
            "  wsession.logon_time, " +
            "  wsession.status, " +
            "  wsession.wait_event, " +
            "  wsession.cli_osuser, " +
            "  wprocess.thread_id wait_pid, " +
            "  nvl(obj.object_name, '-') oname, " +
            "  nvl(obj.owner, '-') owner, " +
            "0, " +
            "0, " +
            "NULL " +
            "FROM " +
            "v$session wsession " +
            "JOIN v$lock loc ON " +
            "loc.sid = wsession.sid " +
            "JOIN v$process wprocess ON " +
            "wprocess.thread_addr = wsession.paddr " +
            "JOIN dba_objects obj ON " +
            "obj.object_id = loc.id1 " +
            "WHERE " +
            "loc.lmode IS NOT NULL " +
            "AND loc.lmode IN ('ts', 'tx') " +
            "AND loc.REQUEST IS NOT NULL " +
            "AND loc.REQUEST IN ('ts', 'tx') " +
            "ORDER BY CASE WHEN hcli_osuser IS NULL THEN 0 ELSE 1 END, waiting_session";

    private static final String LOCK_ITEM_QUERY = "SELECT LOC.sid, " +
            "CASE " +
            "   WHEN loc.LMODE IS NOT NULL THEN loc.LMODE " +
            "   ELSE loc.REQUEST " +
            "   END AS lock_type, " +
            "ID1 lock_id1, " +
            "ID2 lock_id2, " +
            "EXEC_START_TIME last_convert " +
            "FROM " +
            " v$lock loc " +
            "JOIN v$session S ON " +
            " s.sid = loc.sid " +
            "WHERE LOC.sid = ?";

    private final YashanDBDataSource dataSource;

    public YashanDBLockManager(YashanDBDataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public DBPDataSource getDataSource() {
        return dataSource;
    }

    @Override
    public Map<Object, YashanDBLock> getLocks(DBCSession session, Map<String, Object> options) throws DBException {
        try {

            Map<Object, YashanDBLock> locks = new HashMap<>(10);

            String sql = LOCK_QUERY;


            try (JDBCPreparedStatement dbStat = ((JDBCSession) session).prepareStatement(sql)) {
                try (JDBCResultSet dbResult = dbStat.executeQuery()) {

                    while (dbResult.next()) {
                        YashanDBLock l = new YashanDBLock(dbResult, dataSource);
                        locks.put(l.getId(), l);
                    }
                }

            }

            super.buildGraphs(locks);
            return locks;

        } catch (SQLException e) {
            throw new DBException(e, session.getDataSource());
        }

    }

    @Override
    public Collection<YashanDBLockItem> getLockItems(DBCSession session, Map<String, Object> options) throws DBException {
        try {

            List<YashanDBLockItem> locks = new ArrayList<>();

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
                        locks.add(new YashanDBLockItem(dbResult));
                    }
                }
            }

            return locks;

        } catch (SQLException e) {
            throw new DBException(e, session.getDataSource());
        }
    }

    @Override
    public void alterSession(DBCSession session, YashanDBLock lock, Map<String, Object> options) throws DBException {
        try {

            String sql =
                "ALTER SYSTEM KILL SESSION " + "'" + lock.getWait_sid() + ',' + lock.getSerial() + "'" ;
            try (JDBCPreparedStatement dbStat = ((JDBCSession) session).prepareStatement(sql)) {
                dbStat.execute();
            }
        } catch (SQLException e) {
            throw new DBException(e, session.getDataSource());
        }

    }

    @Override
    public Class<YashanDBLock> getLocksType() {
        return YashanDBLock.class;
    }

}
