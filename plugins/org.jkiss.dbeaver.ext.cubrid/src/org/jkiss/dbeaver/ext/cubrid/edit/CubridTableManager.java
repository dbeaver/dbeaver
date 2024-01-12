/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.cubrid.edit;

import java.util.Map;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.cubrid.model.CubridObjectContainer;
import org.jkiss.dbeaver.ext.cubrid.model.CubridTable;
import org.jkiss.dbeaver.ext.cubrid.model.CubridTableColumn;
import org.jkiss.dbeaver.ext.cubrid.model.CubridUser;
import org.jkiss.dbeaver.ext.generic.model.GenericTableForeignKey;
import org.jkiss.dbeaver.ext.generic.model.GenericTableIndex;
import org.jkiss.dbeaver.ext.generic.model.GenericUniqueKey;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.impl.sql.edit.struct.SQLTableManager;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.cache.DBSObjectCache;
import org.jkiss.utils.CommonUtils;

public class CubridTableManager extends SQLTableManager<CubridTable, CubridObjectContainer> {

	private static final String NEW_TABLE_NAME = "NEW_TABLE";
	private static final String TABLE_TYPE_TABLE = "TABLE";
	private static final Class<? extends DBSObject>[] CHILD_TYPES = CommonUtils.array(CubridTableColumn.class,
			GenericUniqueKey.class, GenericTableForeignKey.class, GenericTableIndex.class);

	@Override
	public Class<? extends DBSObject>[] getChildTypes() {
		return CHILD_TYPES;
	}

	@Override
	public boolean canCreateObject(Object container) {
		return super.canCreateObject(((CubridUser) container).getParentObject());
	}

	@Override
	public DBSObjectCache<? extends DBSObject, CubridTable> getObjectsCache(CubridTable object) {
		return object.getContainer().getCubridTableCache();
	}

	@Override
	protected CubridTable createDatabaseObject(DBRProgressMonitor monitor, DBECommandContext context, Object container,
			Object copyFrom, Map<String, Object> options) throws DBException {

		CubridObjectContainer objectContainer = ((CubridUser) container).getParentObject();

		CubridTable table = new CubridTable(monitor, objectContainer, NEW_TABLE_NAME, TABLE_TYPE_TABLE, null);
		setNewObjectName(monitor, objectContainer, table);
		return table;
	}

}
