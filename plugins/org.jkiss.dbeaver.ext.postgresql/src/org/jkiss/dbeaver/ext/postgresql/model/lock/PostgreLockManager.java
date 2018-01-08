/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2017 Andrew Khitrin (ahitrin@gmail.com)
 * 				 2017 Dmitriy.Gurov (dvgurov@gmail.com) 
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
package org.jkiss.dbeaver.ext.postgresql.model.lock;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.postgresql.edit.PostgreLockEditor;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreDataSource;
import org.jkiss.dbeaver.ext.ui.locks.manage.LockGraphManager;
import org.jkiss.dbeaver.ext.ui.locks.manage.LockManagerViewer;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.admin.locks.DBAServerLockManager;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Postgres lock manager
 */

public class PostgreLockManager extends LockGraphManager<PostgreLock,Integer> implements DBAServerLockManager<PostgreLock,PostgreLockItem> {

	public static final String LOCK_QUERY = "with locks as ( "+ 
														"				select  "+
														 "pid,locktype, mode,granted,transactionid tid,relation,page,tuple "+
														"from "+
														 "pg_locks "+ 
											 "), "+ 
											"conflict as ( "+ 
													"select "+ 
													 "*  "+ 
													"from (values "+ 
														   "('AccessShareLock','AccessExclusiveLock',1), "+ 
														   "('RowShareLock','ExclusiveLock',1), ('RowShareLock','AccessExclusiveLock',2),        "+ 
														   "('RowExclusiveLock','ShareLock', 1), ('RowExclusiveLock','ShareRowExclusiveLock',2),  ('RowExclusiveLock','ExclusiveLock',3), ('RowExclusiveLock','AccessExclusiveLock',4), "+ 
														   "('ShareUpdateExclusiveLock','ShareUpdateExclusiveLock',1), ('ShareUpdateExclusiveLock','ShareLock',2),  ('ShareUpdateExclusiveLock','ShareRowExclusiveLock',3), ('ShareUpdateExclusiveLock','ExclusiveLock', 4), ('ShareUpdateExclusiveLock','AccessExclusiveLock',5), "+ 
														   "('ShareLock','RowExclusiveLock',1),  ('ShareLock','ShareUpdateExclusiveLock',2),  ('ShareLock','ShareRowExclusiveLock',3),  ('ShareLock','ExclusiveLock',4),	   ('ShareLock','AccessExclusiveLock',5), "+ 
														   "('ShareRowExclusiveLock','RowExclusiveLock', 1),  ('ShareRowExclusiveLock','ShareUpdateExclusiveLock',    2),  ('ShareRowExclusiveLock','ShareLock',    3),  ('ShareRowExclusiveLock','ShareRowExclusiveLock',4),  ('ShareRowExclusiveLock','ExclusiveLock',5),  ('ShareRowExclusiveLock','AccessExclusiveLock', 6), "+ 
														   "('ExclusiveLock','RowShareLock',1), ('ExclusiveLock','RowExclusiveLock',2), ('ExclusiveLock','ShareUpdateExclusiveLock',3),  ('ExclusiveLock','ShareLock',4),  ('ExclusiveLock','ShareRowExclusiveLock',5),   ('ExclusiveLock','ExclusiveLock',6),   ('ExclusiveLock','AccessExclusiveLock',7), "+ 
														   "('AccessExclusiveLock','AccessShareLock',1), ('AccessExclusiveLock','RowShareLock',2), ('AccessExclusiveLock','RowExclusiveLock',3), ('AccessExclusiveLock','ShareUpdateExclusiveLock',4),   ('AccessExclusiveLock','ShareLock',5), ('AccessExclusiveLock','ShareRowExclusiveLock',6), ('AccessExclusiveLock','ExclusiveLock',7),  ('AccessExclusiveLock','AccessExclusiveLock',8) "+ 
													   ") as t (mode1,mode2,prt)     "+ 
											")	  "+ 
											",real_locks as ("+		   
											"select 	  "+ 
											"la.pid as blocked_pid, "+ 
											"blocked_activity.usename  AS blocked_user, "+ 
											"la.blocked     AS blocking_pid, "+ 
											"blocking_activity.usename AS blocking_user, "+ 
											"blocked_activity.query    AS blocked_statement, "+ 
											"blocking_activity.query   AS statement_in "+ 
											"from  "+ 
											"( "+ 
											"	select 				 "+ 
											"l.*, "+ 
											"c.mode2, "+ 
											"c.prt, "+ 
											"l2.pid blocked, "+ 
											"row_number() over(partition by l.pid order by c.prt) rid "+ 
											"from   "+ 
											"locks l "+ 
											"join conflict c on l.mode = c.mode1 "+ 
											"join locks l2 on l2.locktype = l.locktype and l2.mode = c.mode2 and l2.granted and l.pid != l2.pid and  "+ 
											                          "coalesce(l.tid::text,'*') ||':'|| coalesce(l.relation::text,'*') ||':'|| coalesce(l.page::text,'*') ||':'|| coalesce(l.tuple::text,'*') = "+ 
											                          "coalesce(l2.tid::text,'*') ||':'|| coalesce(l2.relation::text,'*') ||':'|| coalesce(l2.page::text,'*') ||':'|| coalesce(l2.tuple::text,'*') "+ 
											"where not l.granted "+ 
											") la "+ 
											"join pg_catalog.pg_stat_activity blocked_activity  ON blocked_activity.pid = la.pid "+ 
											"join pg_catalog.pg_stat_activity blocking_activity  ON blocking_activity.pid = la.blocked "+ 
											"where la.rid = 1) "+
											
", root_quest as ( "+
"   select blocking_pid as blocking_pid from real_locks "+ 
   " except "+
   " select blocked_pid from real_locks ) "+
" select blocked_pid, "+
"       blocked_user, "+
 "      blocking_pid, "+
  "     blocking_user, "+
   "    blocked_statement, "+
    "   statement_in "+
    " from"+
     "  real_locks"+
" union"+
" select"+
       " real_locks.blocking_pid, "+
       " real_locks.blocking_user, "+
       " null::integer, "+
       " null::text, "+
       " real_locks.statement_in, "+
       " null::text "+
    " from"+
       " real_locks, "+
       " root_quest "+
    " where"+
       " real_locks.blocking_pid = root_quest.blocking_pid "
											;
	
	public static final String LOCK_ITEM_QUERY = "select "+
			" coalesce(db.datname,'') as datname, "+
			" coalesce(lock.locktype,'') as locktype, "+
			" coalesce(lock.relation::regclass::varchar,'') as relation, "+
			" coalesce(lock.mode,'') as mode, "+
			" coalesce(lock.transactionid::varchar,'') as tid, "+
			" lock.page as page, "+
			" lock.tuple as tuple, "+
			" lock.pid as pid, "+
			" lock.granted"+
			" from pg_catalog.pg_locks lock "+
			"   left join pg_catalog.pg_database db "+
			"     on db.oid = lock.database "+
			" where  "+
			"  lock.pid = ? ";
    

    private final PostgreDataSource dataSource;

    public PostgreLockManager(PostgreDataSource dataSource)
    {
        this.dataSource = dataSource;
    }

    @Override
    public DBPDataSource getDataSource()
    {
        return dataSource;
    }

    @Override
    public Map<Integer,PostgreLock> getLocks(DBCSession session, Map<String, Object> options) throws DBException
    {
    	 try {
    		 
    		 Map<Integer,PostgreLock> locks = new HashMap<Integer,PostgreLock>(10);
    		 
             try (JDBCPreparedStatement dbStat = ((JDBCSession) session).prepareStatement(LOCK_QUERY)) {
                 try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                     
	                     while (dbResult.next()) {
	                    	 PostgreLock l = new PostgreLock(dbResult);
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
    public void alterSession(DBCSession session, PostgreLock lock, Map<String, Object> options) throws DBException
    {
        try {
            try (JDBCPreparedStatement dbStat = ((JDBCSession) session).prepareStatement("SELECT pg_catalog.pg_terminate_backend(?)")) {
            	  dbStat.setInt(1, lock.getWait_pid());
            	  dbStat.execute();
            }
        }
        catch (SQLException e) {
            throw new DBException(e, session.getDataSource());
        }
    }

    @Override
    public Class<PostgreLock> getLocksType() {
        return PostgreLock.class;
    }


    @Override
	public Collection<PostgreLockItem> getLockItems(DBCSession session, Map<String, Object> options)
			throws DBException {
   	 try {
   		 
   		List<PostgreLockItem> locks = new ArrayList<>();
   		 
         try (JDBCPreparedStatement dbStat = ((JDBCSession) session).prepareStatement(LOCK_ITEM_QUERY)) {
        	 
        	 String otype = (String) options.get(LockManagerViewer.keyType); 
        	 
        	 switch (otype) {
        	 
				case LockManagerViewer.typeWait:
					dbStat.setInt(1, (int) options.get(PostgreLockEditor.pidWait)); 
					break;
					
				case LockManagerViewer.typeHold:
					dbStat.setInt(1, (int) options.get(PostgreLockEditor.pidHold)); 
					break;
	
				default:
					return locks;
				}
        	 
             try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                 
                 while (dbResult.next()) {
                     locks.add(new PostgreLockItem(dbResult));
                 }
             }
         }
         
         return locks;
         
     } catch (SQLException e) {
         throw new DBException(e, session.getDataSource());
     }
	}

}
