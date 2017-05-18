/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2016-2016 Karl Griesser (fullref@gmail.com)
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
package org.jkiss.dbeaver.ext.exasol.model.lock;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.exasol.editors.ExasolLockEditor;
import org.jkiss.dbeaver.ext.exasol.model.ExasolDataSource;
import org.jkiss.dbeaver.ext.ui.locks.manage.LockGraphManager;
import org.jkiss.dbeaver.ext.ui.locks.manage.LockManagerViewer;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.admin.locks.DBAServerLockManager;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.SQLException;
import java.util.*;

public class ExasolLockManager extends LockGraphManager<ExasolLock, BigInteger>
		implements DBAServerLockManager<ExasolLock, ExasolLockItem> {
	
	public static final String LOCK_QUERY = 
			"WITH LOCKED AS (\r\n" + 
			"SELECT \r\n" + 
			"w.SESSION_ID AS w_session_id,w.login_time as w_login_time,\r\n" + 
			"w.user_name AS w_user_name,\r\n" + 
			"w.command_name AS w_command_name,\r\n" + 
			"w.os_user AS w_os_user,\r\n" + 
			"w.client AS w_client,\r\n" + 
			"'-' AS oname,\r\n" + 
			"h.session_id as h_session_id, h.CLIENT AS h_CLIENT,\r\n" + 
			"h.USER_NAME AS H_USER_NAME, h.status as h_status\r\n" + 
			"FROM SYS.EXA_DBA_SESSIONS w\r\n" + 
			"INNER JOIN SYS.EXA_DBA_SESSIONS h\r\n" + 
			"ON  CASE\r\n" + 
			"                    WHEN w.ACTIVITY LIKE 'Waiting for %' THEN CAST(\r\n" + 
			"                        REPLACE(\r\n" + 
			"                            w.ACTIVITY,\r\n" + 
			"                            'Waiting for session ',\r\n" + 
			"                            ''\r\n" + 
			"                        ) AS DECIMAL(\r\n" + 
			"                            20,\r\n" + 
			"                            0\r\n" + 
			"                        )\r\n" + 
			"                    )\r\n" + 
			"                    ELSE NULL\r\n" + 
			"                END = h.SESSION_ID\r\n" + 
			") \r\n" + 
			"SELECT * FROM locked\r\n" + 
			"UNION ALL\r\n" + 
			"SELECT\r\n" + 
			"w.SESSION_ID AS w_session_id,w.login_time as w_login_time,\r\n" + 
			"w.user_name AS w_user_name,\r\n" + 
			"w.command_name AS w_command_name,\r\n" + 
			"w.os_user AS w_os_user,\r\n" + 
			"w.client AS w_client,\r\n" + 
			"'-' AS oname,\r\n" + 
			"w.session_id as h_session_id, h.CLIENT AS h_CLIENT,\r\n" + 
			"h.USER_NAME AS H_USER_NAME, h.status as h_status\r\n" + 
			"FROM SYS.EXA_DBA_SESSIONS w \r\n" + 
			"LEFT OUTER JOIN SYS.EXA_DBA_SESSIONS h\r\n" + 
			"ON  CASE\r\n" + 
			"                    WHEN w.ACTIVITY LIKE 'Waiting for %' THEN CAST(\r\n" + 
			"                        REPLACE(\r\n" + 
			"                            w.ACTIVITY,\r\n" + 
			"                            'Waiting for session ',\r\n" + 
			"                            ''\r\n" + 
			"                        ) AS DECIMAL(\r\n" + 
			"                            20,\r\n" + 
			"                            0\r\n" + 
			"                        )\r\n" + 
			"                    )\r\n" + 
			"                    ELSE NULL\r\n" + 
			"                END = h.SESSION_ID\r\n" + 
			"WHERE w.SESSION_ID IN (SELECT h_session_id FROM locked)" 
			;
	
	public static final String LOCK_ITEM_QUERY = 
			"with\r\n" + 
			"	EXA_SQL as (\r\n" + 
			"		select\r\n" + 
			"			SESSION_ID,\r\n" + 
			"			STMT_ID,\r\n" + 
			"			COMMAND_CLASS,\r\n" + 
			"			COMMAND_NAME,\r\n" + 
			"			SUCCESS\r\n" + 
			"		from\r\n" + 
			"			--EXA_DBA_AUDIT_SQL                   -- delivers more exact results (if available)\r\n" + 
			"			EXA_SQL_LAST_DAY\r\n" + 
			"		where\r\n" + 
			"			SESSION_ID in (select SESSION_ID from EXA_DBA_SESSIONS)\r\n" + 
			"	),\r\n" + 
			"	SESSION_RISKS as (\r\n" + 
			"		select\r\n" + 
			"			SESSION_ID,\r\n" + 
			"			HAS_LOCKS\r\n" + 
			"		from\r\n" + 
			"			(\r\n" + 
			"				select\r\n" + 
			"					SESSION_ID,\r\n" + 
			"					decode(\r\n" + 
			"						greatest(CURRENT_ACCESS, LAST_ACCESS),\r\n" + 
			"						0,\r\n" + 
			"						'NONE',\r\n" + 
			"						1,\r\n" + 
			"						'READ LOCKS',\r\n" + 
			"						2,\r\n" + 
			"						'WRITE LOCKS'\r\n" + 
			"					) HAS_LOCKS\r\n" + 
			"				from\r\n" + 
			"					(\r\n" + 
			"						select\r\n" + 
			"							S.SESSION_ID,\r\n" + 
			"							case\r\n" + 
			"								when\r\n" + 
			"									(S.STATUS not in ('IDLE', 'DISCONNECTED')) OR\r\n" + 
			"									(\r\n" + 
			"										S.COMMAND_NAME not in ('COMMIT', 'ROLLBACK', 'NOT SPECIFIED')\r\n" + 
			"									)\r\n" + 
			"								then\r\n" + 
			"									case\r\n" + 
			"										when\r\n" + 
			"											S.COMMAND_NAME in (\r\n" + 
			"												'SELECT', 'DESCRIBE', 'OPEN SCHEMA', 'CLOSE SCHEMA', 'FLUSH STATISTICS', 'EXECUTE SCRIPT'\r\n" + 
			"											)\r\n" + 
			"										then\r\n" + 
			"											1\r\n" + 
			"										else\r\n" + 
			"											2\r\n" + 
			"									end\r\n" + 
			"								else\r\n" + 
			"									0\r\n" + 
			"							end CURRENT_ACCESS,\r\n" + 
			"							zeroifnull(A.ACCESS) LAST_ACCESS\r\n" + 
			"						from\r\n" + 
			"								EXA_DBA_SESSIONS S\r\n" + 
			"							left join\r\n" + 
			"								(\r\n" + 
			"									select\r\n" + 
			"										SESSION_ID,\r\n" + 
			"										max(ACCESS) ACCESS\r\n" + 
			"									FROM\r\n" + 
			"										(\r\n" + 
			"											select\r\n" + 
			"												SESSION_ID,\r\n" + 
			"												case\r\n" + 
			"													when\r\n" + 
			"														(\r\n" + 
			"															COMMAND_NAME not in ('COMMIT', 'ROLLBACK', 'NOT SPECIFIED')\r\n" + 
			"														)\r\n" + 
			"													then\r\n" + 
			"														case\r\n" + 
			"															when\r\n" + 
			"																COMMAND_NAME in (\r\n" + 
			"																	'SELECT',\r\n" + 
			"																	'DESCRIBE',\r\n" + 
			"																	'OPEN SCHEMA',\r\n" + 
			"																	'CLOSE SCHEMA',\r\n" + 
			"																	'FLUSH STATISTICS',\r\n" + 
			"																	'EXECUTE SCRIPT'\r\n" + 
			"																)\r\n" + 
			"															then\r\n" + 
			"																1\r\n" + 
			"															else\r\n" + 
			"																2\r\n" + 
			"														end\r\n" + 
			"													else\r\n" + 
			"														0\r\n" + 
			"												end ACCESS\r\n" + 
			"											from\r\n" + 
			"												EXA_SQL C\r\n" + 
			"											where\r\n" + 
			"												C.COMMAND_CLASS <> 'TRANSACTION' and\r\n" + 
			"												SUCCESS and\r\n" + 
			"												not exists(\r\n" + 
			"													select\r\n" + 
			"														*\r\n" + 
			"													from\r\n" + 
			"														EXA_SQL E\r\n" + 
			"													where\r\n" + 
			"														E.SESSION_ID = C.SESSION_ID and\r\n" + 
			"														E.STMT_ID > C.STMT_ID and\r\n" + 
			"														E.COMMAND_CLASS = 'TRANSACTION'\r\n" + 
			"												)\r\n" + 
			"										)\r\n" + 
			"									group by\r\n" + 
			"										SESSION_ID\r\n" + 
			"								) A\r\n" + 
			"							on\r\n" + 
			"								S.SESSION_ID = A.SESSION_ID\r\n" + 
			"					)\r\n" + 
			"				where\r\n" + 
			"					SESSION_ID <> 4\r\n" + 
			"			)\r\n" + 
			"	)\r\n" + 
			"select\r\n" + 
			"	HAS_LOCKS,\r\n" + 
			"	case\r\n" + 
			"		when\r\n" + 
			"			DURATION > '1:00:00' and\r\n" + 
			"			STATUS = 'IDLE'\r\n" + 
			"		then\r\n" + 
			"			decode(\r\n" + 
			"				HAS_LOCKS,\r\n" + 
			"				'READ LOCKS',\r\n" + 
			"				'CRITICAL',\r\n" + 
			"				'WRITE LOCKS',\r\n" + 
			"				'VERY CRITICAL',\r\n" + 
			"				NULL\r\n" + 
			"			)\r\n" + 
			"	end EVALUATION,\r\n" + 
			"	S.*\r\n" + 
			"from\r\n" + 
			"		EXA_DBA_SESSIONS S\r\n" + 
			"	left join\r\n" + 
			"		SESSION_RISKS R\r\n" + 
			"	on\r\n" + 
			"		(S.SESSION_ID = R.SESSION_ID) WHERE S.SESSION_ID = ?\r\n" + 
			"order by\r\n" + 
			"	EVALUATION desc,\r\n" + 
			"	LOGIN_TIME;\r\n" + 
			""
			;

	private final ExasolDataSource dataSource;
	
	public ExasolLockManager(ExasolDataSource dataSource)
	{
		this.dataSource = dataSource;
	}
	
	@Override
	public DBPDataSource getDataSource()
	{
		return this.dataSource;
	}

	@Override
	public Map<BigInteger, ExasolLock> getLocks(DBCSession session,Map<String, Object> options) throws DBException
	{
		try {
			Map<BigInteger, ExasolLock> locks = new HashMap<BigInteger,ExasolLock>(10);
			
			try (JDBCPreparedStatement dbStat = ((JDBCSession) session).prepareStatement(LOCK_QUERY)) {
				try (JDBCResultSet dbResult = dbStat.executeQuery()) {
					while(dbResult.next()) {
						ExasolLock l = new ExasolLock(dbResult);
						locks.put(l.getId(), l);
					}
				}
			}
			super.buildGraphs(locks);
			return locks;
		} catch( SQLException e) {
			throw new DBException(e, session.getDataSource());
		}
	}

	@Override
	public Collection<ExasolLockItem> getLockItems(DBCSession session,
			Map<String, Object> options) throws DBException
	{
		
		try {
			List<ExasolLockItem> locks = new ArrayList<>();
			
			try (JDBCPreparedStatement dbStat = ((JDBCSession) session).prepareStatement(LOCK_ITEM_QUERY)) {
				
				String otype = (String) options.get(LockManagerViewer.keyType);
				
				switch(otype) {
					case LockManagerViewer.typeWait:
						dbStat.setBigDecimal(1, new BigDecimal((BigInteger) options.get(ExasolLockEditor.sidWait)));
						break;
					case LockManagerViewer.typeHold:
						dbStat.setBigDecimal(1,  new BigDecimal((BigInteger) options.get(ExasolLockEditor.sidHold)));
						break;
						
					default:
						return locks;
				}
				
				try (JDBCResultSet dbResult = dbStat.executeQuery()) {
					
					while (dbResult.next()) {
						locks.add(new ExasolLockItem(dbResult));
					}
				}
			}
			
			return locks;
			
	     } catch (SQLException e) {
	         throw new DBException(e, session.getDataSource());
	     }
	}

	@Override
	public void alterSession(DBCSession session, ExasolLock lock,
			Map<String, Object> options) throws DBException
	{
        try {
        	
        	StringBuilder sql = new StringBuilder("KILL SESSION ");
        	sql.append(lock.getHold_sid());
        	try (JDBCPreparedStatement dbStat = ((JDBCSession) session).prepareStatement(sql.toString())) {
                dbStat.execute();
            }
        }
        catch (SQLException e) {
            throw new DBException(e, session.getDataSource());
        }
        
	}

	@Override
	public Class<ExasolLock> getLocksType()
	{
		return ExasolLock.class;
	}

}
