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

import org.jkiss.dbeaver.model.admin.locks.DBAServerLockItem;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;

import java.sql.ResultSet;

public class OracleLockItem implements DBAServerLockItem{

	private String lockType;
	private String modeHeld;
	private String modeRequest;
	private String ids;
	private Integer lastConvert;
	private String blocking;
	
	public OracleLockItem(ResultSet dbResult) {

     this.lockType = JDBCUtils.safeGetString(dbResult, "lock_type");
     this.modeHeld = JDBCUtils.safeGetString(dbResult, "mode_held");
     this.modeRequest = JDBCUtils.safeGetString(dbResult, "mode_requested");
     StringBuilder sb = new StringBuilder(String.valueOf(JDBCUtils.safeGetLong(dbResult, "lock_id1")));
	 sb.append("/");
	 sb.append(String.valueOf(JDBCUtils.safeGetLong(dbResult, "lock_id2")));
	 this.ids = sb.toString();
     this.lastConvert = JDBCUtils.safeGetInt(dbResult, "last_convert");
     this.blocking = JDBCUtils.safeGetString(dbResult, "blocking_others");

    }

	@Property(viewable = true, order = 1)
	public String getLockType()
	{
		return lockType;
	}

	@Property(viewable = true, order = 2)
	public String getModeHeld()
	{
		return modeHeld;
	}

	@Property(viewable = true, order = 3)
	public String getModeRequest()
	{
		return modeRequest;
	}

	@Property(viewable = true, order = 4)
	public String getIds()
	{
		return ids;
	}

	@Property(viewable = true, order = 5)
	public Integer getLastConvert()
	{
		return lastConvert;
	}

	@Property(viewable = true, order = 6)
	public String getBlocking()
	{
		return blocking;
	}
	
	
}
