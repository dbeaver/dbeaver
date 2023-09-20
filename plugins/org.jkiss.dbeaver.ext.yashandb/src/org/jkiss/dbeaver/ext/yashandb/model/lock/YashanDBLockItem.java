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

import org.jkiss.dbeaver.model.admin.locks.DBAServerLockItem;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;

import java.sql.ResultSet;

public class YashanDBLockItem implements DBAServerLockItem{

	private String lockType;

	private String ids;
	private String lastConvert;
	
	public YashanDBLockItem(ResultSet dbResult) {

     this.lockType = JDBCUtils.safeGetString(dbResult, "lock_type");
     StringBuilder sb = new StringBuilder(String.valueOf(JDBCUtils.safeGetLong(dbResult, "lock_id1")));
	 sb.append("/");
	 sb.append(String.valueOf(JDBCUtils.safeGetLong(dbResult, "lock_id2")));
	 this.ids = sb.toString();
     this.lastConvert = JDBCUtils.safeGetString(dbResult, "last_convert");

    }

	@Property(viewable = true, order = 1)
	public String getLockType()
	{
		return lockType;
	}

	@Property(viewable = true, order = 2)
	public String getIds()
	{
		return ids;
	}

	@Property(viewable = true, order = 3)
	public String getLastConvert()
	{
		return lastConvert;
	}


	
	
}
