package org.jkiss.dbeaver.ext.dm.model.lock;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.dm.model.DmDataSource;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.admin.locks.DBAServerLockManager;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.impl.admin.locks.LockGraphManager;

public class DmLockManager extends LockGraphManager implements DBAServerLockManager<DmLock, DmLockItem> {

    public static final String sidHold = "hsid";
    public static final String sidWait = "wsid";

    private static final String LOCK_QUERY = "select " +
        "wsession.sid waiting_session, " +
        "wsession.serial# serial, " +
        "wsession.logon_time, " +
        "wsession.blocking_session_status, " +
        "wsession.event, " +
        "wsession.username waiting_user, " +
        "wprocess.pid wait_pid, " +
        "nvl(obj.object_name,'-') oname, " +
        "nvl(obj.owner,'-') owner, " +
        "wsession.row_wait_block# row_lock, " +
        "wsession.blocking_session holding_session, " +
        "hprocess.pid hold_pid, " +
        "hsession.username holding_user   " +
        "from " +
        "v$session wsession " +
        "join v$session hsession on wsession.blocking_session = hsession.sid " +
        "join v$process  wprocess on wprocess.addr = wsession.paddr " +
        "join v$process  hprocess on hprocess.addr = hsession.paddr " +
        "left join dba_objects obj on obj.object_id = wsession.row_wait_obj# " +
        "where  " +
        "wsession.blocking_session is not NULL " +
        "union    " +
        "select " +
        "wsession.sid waiting_session, " +
        "wsession.serial# serial, " +
        "wsession.logon_time, " +
        "wsession.blocking_session_status, " +
        "wsession.event, " +
        "wsession.username waiting_user, " +
        "wprocess.pid wait_pid, " +
        "nvl(obj.object_name,'-') oname, " +
        "nvl(obj.owner,'-') owner, " +
        "wsession.row_wait_block# row_lock, " +
        "nvl(wsession.blocking_session,0) holding_session, " +
        "nvl(hprocess.pid,0) hold_pid, " +
        "nvl(hsession.username,'-') holding_user   " +
        "from " +
        "v$session wsession " +
        "left join v$session hsession on wsession.blocking_session = hsession.sid " +
        "join v$process  wprocess on wprocess.addr = wsession.paddr " +
        "left join v$process  hprocess on hprocess.addr = hsession.paddr " +
        "left join dba_objects obj on obj.object_id = wsession.row_wait_obj# " +
        "where  " +
        "wsession.sid IN (SELECT blocking_session FROM v$session)";

    public static final String LOCK_ITEM_QUERY = "select lock_type,mode_held,mode_requested,lock_id1,lock_id2,last_convert,blocking_others from dba_lock where session_id = ?";

    private final DmDataSource dataSource;

    public DmLockManager(DmDataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public DBPDataSource getDataSource() {
        return dataSource;
    }

    @Override
    public Map<Object, DmLock> getLocks(DBCSession session, Map<String, Object> options) throws DBException {
        try {

            Map<Object, DmLock> locks = new HashMap<>(10);

            try (JDBCPreparedStatement dbStat = ((JDBCSession) session).prepareStatement(LOCK_QUERY)) {
                try (JDBCResultSet dbResult = dbStat.executeQuery()) {

                    while (dbResult.next()) {
                        DmLock l = new DmLock(dbResult);
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
    public void alterSession(DBCSession session, DmLock lock, Map<String, Object> options) throws DBException {
        try {

            String sql =
                "ALTER SYSTEM KILL SESSION " + "'" + lock.getWait_sid() + ',' + lock.getSerial() + "'" +
                    " IMMEDIATE";
            try (JDBCPreparedStatement dbStat = ((JDBCSession) session).prepareStatement(sql)) {
                dbStat.execute();
            }
        } catch (SQLException e) {
            throw new DBException(e, session.getDataSource());
        }

    }

    @Override
    public Class<DmLock> getLocksType() {
        return DmLock.class;
    }


    @Override
    public Collection<DmLockItem> getLockItems(DBCSession session, Map<String, Object> options)
        throws DBException {
        try {

            List<DmLockItem> locks = new ArrayList<>();

            try (JDBCPreparedStatement dbStat = ((JDBCSession) session).prepareStatement(LOCK_ITEM_QUERY)) {

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
                        locks.add(new DmLockItem(dbResult));
                    }
                }
            }

            return locks;

        } catch (SQLException e) {
            throw new DBException(e, session.getDataSource());
        }
    }
}