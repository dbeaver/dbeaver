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
package org.jkiss.dbeaver.ext.cubrid.model.meta;

import java.sql.SQLException;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.cubrid.CubridConstants;
import org.jkiss.dbeaver.ext.cubrid.model.CubridObjectContainer;
import org.jkiss.dbeaver.ext.cubrid.model.CubridTable;
import org.jkiss.dbeaver.ext.cubrid.model.CubridView;
import org.jkiss.dbeaver.ext.generic.model.GenericStructContainer;
import org.jkiss.dbeaver.ext.generic.model.GenericTableBase;
import org.jkiss.dbeaver.ext.generic.model.meta.GenericMetaModel;
import org.jkiss.dbeaver.ext.generic.model.meta.GenericMetaObject;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCConstants;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;

public class CubridMetaModel extends GenericMetaModel {
	private static final Log log = Log.getLog(CubridMetaModel.class);

	public CubridMetaModel()
	{
	}

	public boolean isSupportMultiSchema(JDBCSession session) {
		try {
			int major = session.getMetaData().getDatabaseMajorVersion();
			int minor = session.getMetaData().getDatabaseMinorVersion();
			if (major > 11 || (major == 11 && minor >= 2)) {
				return true;
			}
		} catch (SQLException e) {
			log.error("Can't get database version", e);
		}
		return false;
	}

	@Override
	public JDBCStatement prepareTableColumnLoadStatement(@NotNull JDBCSession session, @NotNull GenericStructContainer owner, @Nullable GenericTableBase forTable) throws SQLException {
		if(isSupportMultiSchema(session) && forTable instanceof CubridTable) {
			CubridTable table = (CubridTable) forTable;
			return session.getMetaData().getColumns(null, null, table!=null?table.getUniqueName():null, null).getSourceStatement();
		}
		return super.prepareTableColumnLoadStatement(session, owner, forTable);
	}

	@Override
	public JDBCStatement prepareUniqueConstraintsLoadStatement(@NotNull JDBCSession session, @NotNull GenericStructContainer owner, @Nullable GenericTableBase forTable) throws SQLException, DBException {
		if(isSupportMultiSchema(session) && forTable instanceof CubridTable) {
			CubridTable table = (CubridTable) forTable;
			return session.getMetaData().getPrimaryKeys(null, null, table!=null?table.getUniqueName():null).getSourceStatement();
		}
		return super.prepareUniqueConstraintsLoadStatement(session, owner, forTable);
    }

	@Override
	public JDBCStatement prepareForeignKeysLoadStatement(@NotNull JDBCSession session, @NotNull GenericStructContainer owner, @Nullable GenericTableBase forTable) throws SQLException {
		if(isSupportMultiSchema(session) && forTable instanceof CubridTable) {
			CubridTable table = (CubridTable) forTable;
			return session.getMetaData().getImportedKeys(null, null, table!=null?table.getUniqueName():null).getSourceStatement();
		}
		return super.prepareForeignKeysLoadStatement(session, owner, forTable);
	}

	public JDBCStatement prepareIndexLoadStatement(@NotNull JDBCSession session, CubridTable table) throws SQLException {
		String tableName = isSupportMultiSchema(session)? table.getUniqueName():table.getName();
		return session.getMetaData().getIndexInfo(null, null, tableName, false, true).getSourceStatement();
	}

	public CubridTable createTableImpl(@NotNull JDBCSession session, @NotNull CubridObjectContainer owner, @NotNull GenericMetaObject tableObject, @NotNull JDBCResultSet dbResult) {

		String tableName = JDBCUtils.safeGetString( dbResult, CubridConstants.CLASS_NAME);
		String tableType = JDBCUtils.safeGetStringTrimmed(dbResult, JDBCConstants.TABLE_TYPE);

		CubridTable table = this.createTableImpl(owner, tableName, tableType, dbResult);
		if (table == null) {
			return null;
		}
		return table;
	}

	public CubridTable createTableImpl(CubridObjectContainer container, @Nullable String tableName, @Nullable String tableType, @Nullable JDBCResultSet dbResult) {
		if (tableType != null && isView(tableType)) {
			return new CubridView(container, tableName, tableType, dbResult);
		}
		return new CubridTable(container, tableName, tableType, dbResult);
	}

}
