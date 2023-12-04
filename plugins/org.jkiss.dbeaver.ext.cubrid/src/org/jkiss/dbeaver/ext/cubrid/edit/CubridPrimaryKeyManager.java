package org.jkiss.dbeaver.ext.cubrid.edit;

import java.util.Map;

import org.jkiss.dbeaver.ext.cubrid.model.CubridTable;
import org.jkiss.dbeaver.ext.cubrid.model.CubridUniqueKey;
import org.jkiss.dbeaver.ext.generic.GenericConstants;
import org.jkiss.dbeaver.ext.generic.edit.GenericPrimaryKeyManager;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSEntityConstraintType;

public class CubridPrimaryKeyManager extends GenericPrimaryKeyManager {

	@Override
	public boolean canCreateObject(Object container) {
		return (container instanceof CubridTable);
	}
	
	@Override
	protected CubridUniqueKey createDatabaseObject(DBRProgressMonitor monitor, DBECommandContext context, 
		final Object container, Object from, Map<String, Object> options) {	
		CubridTable table = (CubridTable)container;
		return new CubridUniqueKey(table, GenericConstants.BASE_CONSTRAINT_NAME, null, DBSEntityConstraintType.PRIMARY_KEY, false);
    }

}
