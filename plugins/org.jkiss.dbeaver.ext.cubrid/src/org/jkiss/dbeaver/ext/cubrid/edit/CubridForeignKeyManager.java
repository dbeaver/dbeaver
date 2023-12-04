package org.jkiss.dbeaver.ext.cubrid.edit;

import java.util.Map;

import org.jkiss.dbeaver.ext.cubrid.model.CubridTable;
import org.jkiss.dbeaver.ext.cubrid.model.CubridTableForeignKey;
import org.jkiss.dbeaver.ext.generic.GenericConstants;
import org.jkiss.dbeaver.ext.generic.edit.GenericForeignKeyManager;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.rdb.DBSForeignKeyDeferability;
import org.jkiss.dbeaver.model.struct.rdb.DBSForeignKeyModifyRule;

public class CubridForeignKeyManager extends GenericForeignKeyManager {

	@Override
	public boolean canCreateObject(Object container) {
		return true;
	}
	
	@Override
	protected CubridTableForeignKey createDatabaseObject(DBRProgressMonitor monitor, DBECommandContext context, 
		final Object container, Object from, Map<String, Object> options) {
		CubridTable table = (CubridTable)container;
		return new CubridTableForeignKey(table, GenericConstants.BASE_CONSTRAINT_NAME, null, null, DBSForeignKeyModifyRule.NO_ACTION, DBSForeignKeyModifyRule.NO_ACTION, DBSForeignKeyDeferability.NOT_DEFERRABLE, false);
	}
}
