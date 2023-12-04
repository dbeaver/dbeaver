package org.jkiss.dbeaver.ext.cubrid.edit;

import java.util.Map;

import org.jkiss.dbeaver.ext.cubrid.model.CubridTable;
import org.jkiss.dbeaver.ext.cubrid.model.CubridTableIndex;
import org.jkiss.dbeaver.ext.generic.edit.GenericIndexManager;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.rdb.DBSIndexType;

public class CubridIndexManager extends GenericIndexManager {

	@Override
	public boolean canCreateObject(Object container) {
		return true;
	}

	@Override
	protected CubridTableIndex createDatabaseObject(DBRProgressMonitor monitor, DBECommandContext context, 
		final Object container, Object from, Map<String, Object> options) {		
		CubridTable table = (CubridTable) container;
		CubridTableIndex index = new CubridTableIndex(table, true, null, 0, null, DBSIndexType.OTHER, false);
		return index;
	}

}
