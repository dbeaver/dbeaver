package org.jkiss.dbeaver.ext.dm.edit;

import java.util.Map;

import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.dm.model.DmTableIndex;
import org.jkiss.dbeaver.ext.dm.model.DmTablePhysical;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.impl.sql.edit.struct.SQLIndexManager;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.cache.DBSObjectCache;
import org.jkiss.dbeaver.model.struct.rdb.DBSIndexType;

/**
 * DM index manager
 * 
 * @author caosw
 *
 */
public class DmIndexManager extends SQLIndexManager<DmTableIndex, DmTablePhysical> {

	@Nullable
	@Override
	public DBSObjectCache<? extends DBSObject, DmTableIndex> getObjectsCache(DmTableIndex object) {
		return object.getParentObject().getSchema().indexCache;
	}

	@Override
	protected DmTableIndex createDatabaseObject(DBRProgressMonitor monitor, DBECommandContext context, Object container,
			Object copyFrom, Map<String, Object> options) throws DBException {
		DmTablePhysical table = (DmTablePhysical) container;
		return new DmTableIndex(table.getSchema(), table, "INDEX", true, DBSIndexType.UNKNOWN);
	}

	@Override
	protected String getDropIndexPattern(DmTableIndex index) {
		return "DROP INDEX " + PATTERN_ITEM_INDEX;
	}

}
