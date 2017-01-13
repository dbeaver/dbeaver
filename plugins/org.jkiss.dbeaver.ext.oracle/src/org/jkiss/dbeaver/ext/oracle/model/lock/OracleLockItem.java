package org.jkiss.dbeaver.ext.oracle.model.lock;

import java.sql.ResultSet;

import org.jkiss.dbeaver.model.admin.locks.DBAServerLockItem;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;

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
