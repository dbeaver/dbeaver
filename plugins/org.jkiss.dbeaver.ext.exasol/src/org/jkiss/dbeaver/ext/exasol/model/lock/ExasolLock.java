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

import org.jkiss.dbeaver.model.admin.locks.DBAServerLock;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;

import java.math.BigInteger;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;


public class ExasolLock implements DBAServerLock<BigInteger> {
	
	 private long    waitSessionId;
	 private String waitUserName;
	 private String waitCommandName;
	 private String waitOsUser;
	 private String waitClient;
	 private long    holdSessionId;
	 private String holdClient;
	 private String holdUserName;
	 private String oname;
	 private String status;
	 private Timestamp waitLoginTime;
    
    DBAServerLock<BigInteger> hold = null;
    List<DBAServerLock<BigInteger>> waiters = new ArrayList<>(0); 
    
    public ExasolLock(ResultSet dbResult) {
   	 this.waitSessionId = JDBCUtils.safeGetLong(dbResult, "W_SESSION_ID");
   	 this.waitUserName = JDBCUtils.safeGetString(dbResult, "W_USER_NAME");
   	 this.waitCommandName = JDBCUtils.safeGetString(dbResult, "W_COMMAND_NAME");
   	 this.waitOsUser = JDBCUtils.safeGetString(dbResult, "W_OS_USER");
   	 this.waitClient = JDBCUtils.safeGetString(dbResult, "W_CLIENT");
   	 this.holdSessionId  = JDBCUtils.safeGetLong(dbResult, "H_SESSION_ID");
   	 this.holdClient = JDBCUtils.safeGetString(dbResult, "H_CLIENT");
   	 this.oname = JDBCUtils.safeGetString(dbResult, "ONAME");
   	 this.holdUserName = JDBCUtils.safeGetString(dbResult, "H_USER_NAME");
   	 this.status = JDBCUtils.safeGetString(dbResult, "H_STATUS");
   	 this.waitLoginTime = JDBCUtils.safeGetTimestamp(dbResult, "W_LOGIN_TIME");
   	 
    }
	

 	@Override
 	public String getTitle() {		
 		return String.valueOf(waitSessionId);
 	}

 	@Override
 	public DBAServerLock<BigInteger> getHoldBy() {
 		return hold;
 	}

 	public DBAServerLock<BigInteger> getHold() {
 		return hold;
 	}

 	@Override
 	public BigInteger getId() {
 		return BigInteger.valueOf(waitSessionId);
 	}


 	@Override
 	public List<DBAServerLock<BigInteger>> waitThis() {		
 		return this.waiters;
 	}

 	@Override
 	public BigInteger getHoldID() {
 		return BigInteger.valueOf(holdSessionId);
 	}

 	@SuppressWarnings("unchecked")
 	@Override
 	public void setHoldBy(DBAServerLock<?> lock) {
 		this.hold = (DBAServerLock<BigInteger>) lock;		
 	}

 	@Override
 	public String toString() {
 		return String.format("Wait %s - %d (%s) Hold - %d (%s)",oname, waitSessionId,waitUserName,holdSessionId,holdUserName);
 	}

 	@Property(viewable = true, order = 1)
	public BigInteger getWait_sid()
	{
		return BigInteger.valueOf(waitSessionId);
	}

 	@Property(viewable = true, order = 2)
	public String getWait_osuser()
	{
		return waitOsUser;
	}

 	@Property(viewable = true, order = 3)
	public String getWait_user()
	{
		return waitUserName;
	}

 	@Property(viewable = true, order = 4)
	public String getOname()
	{
		return oname;
	}
 	
 	@Property(viewable = true, order = 5)
 	public String getWait_command()
 	{
 		return waitCommandName;
 	}

 	@Property(viewable = true, order = 6)
 	public String getWait_client()
 	{
 		return waitClient;
 	}

 	@Property(viewable = true, order = 7)
	public long getHold_sid()
	{
		return holdSessionId;
	}


 	@Property(viewable = true, order = 9)
	public String getHold_user()
	{
		return holdUserName;
	}

 	@Property(viewable = true, order = 10)
	public Timestamp getLtime()
	{
		return waitLoginTime;
	}

 	@Property(viewable = true, order = 11)
	public String getStatus()
	{
		return status;
	}
 	
 	@Property(viewable = true, order = 12)
 	public String getHold_client()
 	{
 		return this.holdClient;
 	}


}
