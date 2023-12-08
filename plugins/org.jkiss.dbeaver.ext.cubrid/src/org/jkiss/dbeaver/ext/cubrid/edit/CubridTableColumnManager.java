/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2023 DBeaver Corp and others
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

import java.sql.Types;
import java.util.Map;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.cubrid.model.CubridTable;
import org.jkiss.dbeaver.ext.cubrid.model.CubridTableColumn;
import org.jkiss.dbeaver.ext.generic.edit.GenericTableColumnManager;
import org.jkiss.dbeaver.model.DBConstants;
import org.jkiss.dbeaver.model.DBPDataKind;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSDataType;

public class CubridTableColumnManager extends GenericTableColumnManager {

	@Override
	public boolean canCreateObject(Object container) {
		return container instanceof CubridTable;
	}

	@Override
	protected CubridTableColumn createDatabaseObject(DBRProgressMonitor monitor, DBECommandContext context, Object container, Object copyFrom, Map<String, Object> options) throws DBException {
		CubridTable table = (CubridTable) container;
		DBSDataType columnType = findBestDataType(table, DBConstants.DEFAULT_DATATYPE_NAMES);

		int columnSize = columnType != null && columnType.getDataKind() == DBPDataKind.STRING ? 100 : 0;
		CubridTableColumn column = new CubridTableColumn(table, getNewColumnName(monitor, context, table),
				columnType == null ? "INTEGER" : columnType.getName(),
				columnType == null ? Types.INTEGER : columnType.getTypeID(),
				columnType == null ? Types.INTEGER : columnType.getTypeID(),
				-1,
				columnSize,
				columnSize,
				null,
				null,
				10,
				false,
				null,
				null,
				false,
				false);
		column.setPersisted(false);
		return column;
	}

}
