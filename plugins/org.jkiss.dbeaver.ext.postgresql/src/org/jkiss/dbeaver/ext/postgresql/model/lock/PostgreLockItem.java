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

import org.jkiss.dbeaver.model.admin.locks.DBAServerLockItem;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;

import java.sql.ResultSet;

public class PostgreLockItem implements DBAServerLockItem{
	
    private String datname;
    private String locktype;
    private String relation;
    private String mode;
    private String tid;
    private String detail;
    private int pid;
    private boolean granted;
    

    public PostgreLockItem(ResultSet dbResult) {
    	
     this.datname = JDBCUtils.safeGetString(dbResult, "datname");
     this.locktype = JDBCUtils.safeGetString(dbResult, "locktype");
     this.relation = JDBCUtils.safeGetString(dbResult, "relation");
     this.mode = JDBCUtils.safeGetString(dbResult, "mode");
     String strRes = JDBCUtils.safeGetString(dbResult, "page");
     StringBuilder sb = new StringBuilder(strRes == null ? "-" : strRes);
     sb.append("/");
     strRes = JDBCUtils.safeGetString(dbResult, "tuple");
     sb.append(strRes == null ? "-" : strRes);
     this.detail = sb.toString();          
     this.tid = JDBCUtils.safeGetString(dbResult, "tid");
     this.pid = JDBCUtils.safeGetInt(dbResult, "pid");
     this.granted = JDBCUtils.safeGetBoolean(dbResult, "granted");
   	    
    }
    
    @Property(viewable = true, order = 1)
	public String getDatname() {
		return datname;
	}
    
    @Property(viewable = true, order = 2)
	public String getLocktype() {
		return locktype;		
	}
    
    @Property(viewable = true, order = 3)
	public String getRelation() {
		return relation;
	}
    
    @Property(viewable = true, order = 4)
	public String getMode() {
		return mode;
	}
    
    @Property(viewable = true, order = 5)
    public String getTid() {
		return tid;
	}
    
    @Property(viewable = true, order = 6)
    public String getDetail() {
		return detail;
	}    
	public int getPid() {
		return pid;
	}
    
    @Property(viewable = true, order = 7)
	public boolean isGranted() {
		return granted;
	}
    
    
	 
}
