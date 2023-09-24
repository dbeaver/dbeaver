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


import org.jkiss.dbeaver.ext.yashandb.model.YashanDBDataSource;
import org.jkiss.dbeaver.model.admin.locks.DBAServerLock;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.IPropertyValueValidator;
import org.jkiss.dbeaver.model.meta.Property;

import java.sql.Date;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class YashanDBLock implements DBAServerLock {
	
	 private int    wait_sid;
	 private int    serial;
	 private int    wait_pid;
	 private String wait_user;
	 private String oname;
	 private String owner;
	 private int    hold_sid;
     private int    hold_pid;
     private String hold_user;
     private Date ltime;
     private String status;
     private String event;
     
     private DBAServerLock hold = null;
     private List<DBAServerLock> waiters = new ArrayList<>(0);

	 private YashanDBDataSource dataSource;
     
     public YashanDBLock(ResultSet dbResult, YashanDBDataSource dataSource) {
    	 this.wait_sid = JDBCUtils.safeGetInt(dbResult, "waiting_session");
    	 this.serial = JDBCUtils.safeGetInt(dbResult, "serial");
    	 this.hold_sid  = JDBCUtils.safeGetInt(dbResult, "holding_session");
    	 this.wait_pid = JDBCUtils.safeGetInt(dbResult, "wait_pid");
    	 this.hold_pid = JDBCUtils.safeGetInt(dbResult, "hold_pid");
    	 this.oname = JDBCUtils.safeGetString(dbResult, "oname");
    	 if (oname == null) {
    	 	oname = "name";
		 }
    	 this.owner = JDBCUtils.safeGetString(dbResult, "owner");
    	 this.wait_user = JDBCUtils.safeGetString(dbResult, "wcli_osuser");
    	 this.hold_user = JDBCUtils.safeGetString(dbResult, "hcli_osuser");
    	 this.ltime = JDBCUtils.safeGetDate(dbResult, "logon_time");
    	 this.status = JDBCUtils.safeGetString(dbResult, "status");
    	 this.event = JDBCUtils.safeGetString(dbResult, "wait_event");

    	 this.dataSource = dataSource;
     }
     

 	@Override
 	public String getTitle() {		
 		return String.valueOf(wait_sid);
 	}

 	@Override
 	public DBAServerLock getHoldBy() {
 		
 		return hold;
 	}

 	public DBAServerLock getHold() {
 		return hold;
 	}

 	@Override
 	public Integer getId() {
 		return wait_sid;
 	}


 	@Override
 	public List<DBAServerLock> waitThis() {
 		return this.waiters;
 	}

 	@Override
 	public Integer getHoldID() {
 		return hold_sid;
 	}

 	@SuppressWarnings("unchecked")
 	@Override
 	public void setHoldBy(DBAServerLock lock) {
 		this.hold = lock;
 	}

 	@Override
 	public String toString() {
 		return String.format("Wait %s - %d (%s) Hold - %d (%s)",oname, wait_sid,wait_user,hold_sid,hold_user);
 	}

 	@Property(viewable = true, order = 1)
	public int getWait_sid()
	{
		return wait_sid;
	}

 	@Property(viewable = true, order = 2)
	public int getWait_pid()
	{
		return wait_pid;
	}

 	@Property(viewable = true, order = 3, visibleIf = YashanDBLockColumnsValueValidator.class)
	public String getWait_user()
	{
		return wait_user;
	}

 	@Property(viewable = true, order = 4, visibleIf = YashanDBLockColumnsValueValidator.class)
	public String getOname()
	{
		return oname;
	}

 	@Property(viewable = true, order = 5)
	public String getOwner()
	{
		return owner;
	}


 	@Property(viewable = true, order = 6, visibleIf = YashanDBLockColumnsValueValidator.class)
	public int getHold_sid()
	{
		return hold_sid;
	}

 	@Property(viewable = true, order = 7, visibleIf = YashanDBLockColumnsValueValidator.class)
	public int getHold_pid()
	{
		return hold_pid;
	}

 	@Property(viewable = true, order = 8, visibleIf = YashanDBLockColumnsValueValidator.class)
	public String getHold_user()
	{
		return hold_user;
	}

 	@Property(viewable = true, order = 9)
	public Date getLtime()
	{
		return ltime;
	}

 	@Property(viewable = true, order = 10)
	public String getStatus()
	{
		return status;
	}

 	@Property(viewable = true, order = 11, visibleIf = YashanDBLockColumnsValueValidator.class)
	public String getEvent()
	{
		return event;
	}

 	public int getSerial()
	{
		return serial;
	}

	public YashanDBDataSource getDataSource() {
		return dataSource;
	}


	public static class YashanDBLockColumnsValueValidator implements IPropertyValueValidator<YashanDBLock, Object> {

		@Override
		public boolean isValidValue(YashanDBLock lock, Object value) throws IllegalArgumentException {
			return !lock.getDataSource().isDistributed();
		}
	}
}
