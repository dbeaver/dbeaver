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
package org.jkiss.dbeaver.ext.postgresql.model.lock;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

import org.jkiss.dbeaver.model.admin.locks.DBAServerLock;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;

public class PostgreLock implements DBAServerLock<Integer>{

     private int    wait_pid;
     private String wait_user;
     private int    hold_pid;
     private String hold_user;
     private String wait_statement;
     private String statement_in;
     
     DBAServerLock<Integer> hold = null;
     List<DBAServerLock<Integer>> waiters = new ArrayList<>(0); 
     
     public PostgreLock(ResultSet dbResult) {
    	 
    	 this.wait_pid = JDBCUtils.safeGetInt(dbResult, "blocked_pid");
    	 this.wait_user = JDBCUtils.safeGetString(dbResult, "blocked_user");
    	 this.hold_pid  = JDBCUtils.safeGetInt(dbResult, "blocking_pid");
    	 this.hold_user = JDBCUtils.safeGetString(dbResult, "blocking_user");
    	 this.wait_statement = JDBCUtils.safeGetString(dbResult, "blocked_statement");
    	 this.statement_in  = JDBCUtils.safeGetString(dbResult, "statement_in");
     }

    @Property(viewable = true, order = 1)
	public int getWait_pid() {
		return wait_pid;
	}

    @Property(viewable = true, order = 2)
	public String getWait_user() {
		return wait_user;
	}

    @Property(viewable = true, order = 3)
	public int getHold_pid() {
		return hold_pid;
	}

    @Property(viewable = true, order = 4)
	public String getHold_user() {
		return hold_user;
	}

    @Property(viewable = true, order = 5)
	public String getWait_statement() {
		return wait_statement;
	}

    @Property(viewable = true, order = 6)
	public String getStatement_in() {
		return statement_in;
	}

    
    
	@Override
	public String getTitle() {		
		return String.valueOf(wait_pid);
	}

	@Override
	public DBAServerLock<Integer> getHoldBy() {
		
		return hold;
	}

	public DBAServerLock<Integer> getHold() {
		return hold;
	}

	@Override
	public Integer getId() {
		return wait_pid;
	}


	@Override
	public List<DBAServerLock<Integer>> waitThis() {		
		return this.waiters;
	}

	@Override
	public Integer getHoldID() {
		return hold_pid;
	}

	@SuppressWarnings("unchecked")
	@Override
	public void setHoldBy(DBAServerLock<?> lock) {
		this.hold = (DBAServerLock<Integer>) lock;		
	}

	@Override
	public String toString() {
		return String.format("Wait - %d (%s) Hold - %d (%s)", wait_pid,wait_user,hold_pid,hold_user);
	}

}
