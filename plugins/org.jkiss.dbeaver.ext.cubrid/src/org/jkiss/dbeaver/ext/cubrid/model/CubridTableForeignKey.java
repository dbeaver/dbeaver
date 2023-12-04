package org.jkiss.dbeaver.ext.cubrid.model;

import org.jkiss.dbeaver.ext.generic.model.GenericTableForeignKey;
import org.jkiss.dbeaver.model.struct.DBSEntityReferrer;
import org.jkiss.dbeaver.model.struct.rdb.DBSForeignKeyDeferability;
import org.jkiss.dbeaver.model.struct.rdb.DBSForeignKeyModifyRule;

public class CubridTableForeignKey extends GenericTableForeignKey {

	public CubridTableForeignKey(CubridTable table, String name, String remarks, DBSEntityReferrer referencedKey,
		DBSForeignKeyModifyRule deleteRule, DBSForeignKeyModifyRule updateRule,
		DBSForeignKeyDeferability deferability, boolean persisted) {
		super(table, name, remarks, referencedKey, deleteRule, updateRule, deferability, persisted);
	}

}
