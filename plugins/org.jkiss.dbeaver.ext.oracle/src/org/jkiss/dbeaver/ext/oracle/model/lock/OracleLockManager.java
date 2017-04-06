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
package org.jkiss.dbeaver.ext.oracle.model.lock;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.oracle.editors.OracleLockEditor;
import org.jkiss.dbeaver.ext.oracle.model.OracleDataSource;
import org.jkiss.dbeaver.ext.ui.locks.manage.LockGraphManager;
import org.jkiss.dbeaver.ext.ui.locks.manage.LockManagerViewer;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.admin.locks.DBAServerLockManager;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;

public class OracleLockManager extends LockGraphManager<OracleLock,Integer> implements DBAServerLockManager<OracleLock,OracleLockItem>{

	public static final String LOCK_QUERY =   "select "+ 
			                                  "wsession.sid waiting_session, "+
			                                  "wsession.serial# serial, "+
											  "wsession.logon_time, "+
											  "wsession.blocking_session_status, "+
											  "wsession.event, "+
											  "wsession.username waiting_user, "+ 
											  "wprocess.pid wait_pid, "+ 
											  "nvl(obj.object_name,'-') oname, "+ 
											  "nvl(obj.owner,'-') owner, "+ 
											  "wsession.row_wait_block# row_lock, "+ 
											  "wsession.blocking_session holding_session, "+ 
											  "hprocess.pid hold_pid, "+ 
											  "hsession.username holding_user   "+ 
											 "from "+ 
											  "v$session wsession "+ 
											  "join v$session hsession on wsession.blocking_session = hsession.sid "+ 
											  "join v$process  wprocess on wprocess.addr = wsession.paddr "+ 
											  "join v$process  hprocess on hprocess.addr = hsession.paddr "+ 
											  "left join dba_objects obj on obj.object_id = wsession.row_wait_obj# "+ 
											 "where  "+ 
											   "wsession.blocking_session is not NULL "+ 
											 "union    "+ 
											  "select "+ 
											  "wsession.sid waiting_session, "+ 
											  "wsession.serial# serial, "+
											  "wsession.logon_time, "+
											  "wsession.blocking_session_status, "+
											  "wsession.event, "+											  
											  "wsession.username waiting_user, "+ 
											  "wprocess.pid wait_pid, "+ 
											  "nvl(obj.object_name,'-') oname, "+ 
											  "nvl(obj.owner,'-') owner, "+ 
											  "wsession.row_wait_block# row_lock, "+ 
											  "nvl(wsession.blocking_session,0) holding_session, "+ 
											  "nvl(hprocess.pid,0) hold_pid, "+ 
											  "nvl(hsession.username,'-') holding_user   "+ 
											 "from "+ 
											  "v$session wsession "+ 
											  "left join v$session hsession on wsession.blocking_session = hsession.sid "+ 
											  "join v$process  wprocess on wprocess.addr = wsession.paddr "+ 
											  "left join v$process  hprocess on hprocess.addr = hsession.paddr "+ 
											  "left join dba_objects obj on obj.object_id = wsession.row_wait_obj# "+ 
											 "where  "+ 
											   "wsession.sid IN (SELECT blocking_session FROM v$session)";
	
	public static final String LOCK_ITEM_QUERY = "select lock_type,mode_held,mode_requested,lock_id1,lock_id2,last_convert,blocking_others from dba_lock where session_id = ?";
	
	private final OracleDataSource dataSource;
	 
	 public OracleLockManager(OracleDataSource dataSource)
	    {
	        this.dataSource = dataSource;
	    }

	    @Override
	    public DBPDataSource getDataSource()
	    {
	        return dataSource;
	    }

	    @Override
	    public Map<Integer,OracleLock> getLocks(DBCSession session, Map<String, Object> options) throws DBException
	    {
	    	 try {
	    		 
	    		 Map<Integer,OracleLock> locks = new HashMap<Integer,OracleLock>(10);
	    		 
	             try (JDBCPreparedStatement dbStat = ((JDBCSession) session).prepareStatement(LOCK_QUERY)) {
	                 try (JDBCResultSet dbResult = dbStat.executeQuery()) {
	                     
		                     while (dbResult.next()) {
		                    	 OracleLock l = new OracleLock(dbResult);
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
	    public void alterSession(DBCSession session, OracleLock lock, Map<String, Object> options) throws DBException
	    {
	        try {
	        	
	        	StringBuilder sql = new StringBuilder("ALTER SYSTEM KILL SESSION ");
	        	sql.append("'").append(lock.getWait_sid()).append(',').append(lock.getSerial()).append("'");
	        	sql.append(" IMMEDIATE");
	        	try (JDBCPreparedStatement dbStat = ((JDBCSession) session).prepareStatement(sql.toString())) {
	                dbStat.execute();
	            }
	        }
	        catch (SQLException e) {
	            throw new DBException(e, session.getDataSource());
	        }
	        
	    }

    @Override
    public Class<OracleLock> getLocksType() {
        return OracleLock.class;
    }


    @Override
		public Collection<OracleLockItem> getLockItems(DBCSession session, Map<String, Object> options)
				throws DBException {
	   	 try {
	   		 
	   		List<OracleLockItem> locks = new ArrayList<>();
	   		 
	         try (JDBCPreparedStatement dbStat = ((JDBCSession) session).prepareStatement(LOCK_ITEM_QUERY)) {
	        	 
	        	 String otype = (String) options.get(LockManagerViewer.keyType); 
	        	 
	        	 switch (otype) {
	        	 
					case LockManagerViewer.typeWait:
						dbStat.setInt(1, (int) options.get(OracleLockEditor.sidWait)); 
						break;
						
					case LockManagerViewer.typeHold:
						dbStat.setInt(1, (int) options.get(OracleLockEditor.sidHold)); 
						break;
		
					default:
						return locks;
					}
	        	 
	             try (JDBCResultSet dbResult = dbStat.executeQuery()) {
	                 
	                 while (dbResult.next()) {
	                     locks.add(new OracleLockItem(dbResult));
	                 }
	             }
	         }
	         
	         return locks;
	         
	     } catch (SQLException e) {
	         throw new DBException(e, session.getDataSource());
	     }
		} 
}
